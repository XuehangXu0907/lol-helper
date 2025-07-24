package com.lol.championselector.manager;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Draft Pick引擎 - 处理复杂的draft pick逻辑和交替选择
 */
public class DraftPickEngine {
    private static final Logger logger = LoggerFactory.getLogger(DraftPickEngine.class);
    
    /**
     * Draft Pick阶段信息
     */
    public static class DraftPhase {
        private final int phaseIndex;
        private final String type; // "ban" or "pick"
        private final List<DraftAction> actions;
        private final boolean isActive;
        
        public DraftPhase(int phaseIndex, String type, List<DraftAction> actions, boolean isActive) {
            this.phaseIndex = phaseIndex;
            this.type = type;
            this.actions = actions;
            this.isActive = isActive;
        }
        
        public int getPhaseIndex() { return phaseIndex; }
        public String getType() { return type; }
        public List<DraftAction> getActions() { return actions; }
        public boolean isActive() { return isActive; }
        
        @Override
        public String toString() {
            return String.format("DraftPhase{phase=%d, type=%s, actions=%d, active=%s}", 
                                phaseIndex, type, actions.size(), isActive);
        }
    }
    
    /**
     * Draft Action信息
     */
    public static class DraftAction {
        private final int actionId;
        private final int actorCellId;
        private final String type;
        private final int championId;
        private final boolean isInProgress;
        private final boolean completed;
        private final int pickTurn; // pick轮次
        private final boolean isAllyTurn; // 是否是己方回合
        
        public DraftAction(int actionId, int actorCellId, String type, int championId, 
                          boolean isInProgress, boolean completed, int pickTurn, boolean isAllyTurn) {
            this.actionId = actionId;
            this.actorCellId = actorCellId;
            this.type = type;
            this.championId = championId;
            this.isInProgress = isInProgress;
            this.completed = completed;
            this.pickTurn = pickTurn;
            this.isAllyTurn = isAllyTurn;
        }
        
        public int getActionId() { return actionId; }
        public int getActorCellId() { return actorCellId; }
        public String getType() { return type; }
        public int getChampionId() { return championId; }
        public boolean isInProgress() { return isInProgress; }
        public boolean isCompleted() { return completed; }
        public int getPickTurn() { return pickTurn; }
        public boolean isAllyTurn() { return isAllyTurn; }
        
        @Override
        public String toString() {
            return String.format("DraftAction{id=%d, actor=%d, type=%s, champion=%d, inProgress=%s, completed=%s, turn=%d, ally=%s}", 
                                actionId, actorCellId, type, championId, isInProgress, completed, pickTurn, isAllyTurn);
        }
    }
    
    /**
     * Draft状态分析结果
     */
    public static class DraftAnalysis {
        private final List<DraftPhase> phases;
        private final DraftPhase currentPhase;
        private final DraftAction currentPlayerAction;
        private final int currentTurn;
        private final boolean isDraftPick; // 是否是draft pick模式
        private final List<Integer> alliedTeam;
        private final List<Integer> enemyTeam;
        private final Set<Integer> bannedChampions;
        private final Set<Integer> pickedChampions;
        private final Map<Integer, Integer> playerChampions; // cellId -> championId
        
        public DraftAnalysis(List<DraftPhase> phases, DraftPhase currentPhase, DraftAction currentPlayerAction,
                           int currentTurn, boolean isDraftPick, List<Integer> alliedTeam, List<Integer> enemyTeam,
                           Set<Integer> bannedChampions, Set<Integer> pickedChampions, Map<Integer, Integer> playerChampions) {
            this.phases = phases;
            this.currentPhase = currentPhase;
            this.currentPlayerAction = currentPlayerAction;
            this.currentTurn = currentTurn;
            this.isDraftPick = isDraftPick;
            this.alliedTeam = alliedTeam;
            this.enemyTeam = enemyTeam;
            this.bannedChampions = bannedChampions;
            this.pickedChampions = pickedChampions;
            this.playerChampions = playerChampions;
        }
        
        // Getters
        public List<DraftPhase> getPhases() { return phases; }
        public DraftPhase getCurrentPhase() { return currentPhase; }
        public DraftAction getCurrentPlayerAction() { return currentPlayerAction; }
        public int getCurrentTurn() { return currentTurn; }
        public boolean isDraftPick() { return isDraftPick; }
        public List<Integer> getAlliedTeam() { return alliedTeam; }
        public List<Integer> getEnemyTeam() { return enemyTeam; }
        public Set<Integer> getBannedChampions() { return bannedChampions; }
        public Set<Integer> getPickedChampions() { return pickedChampions; }
        public Map<Integer, Integer> getPlayerChampions() { return playerChampions; }
        
        @Override
        public String toString() {
            return String.format("DraftAnalysis{phases=%d, currentPhase=%s, currentPlayerAction=%s, turn=%d, isDraft=%s, banned=%d, picked=%d}", 
                                phases.size(), currentPhase, currentPlayerAction, currentTurn, isDraftPick, 
                                bannedChampions.size(), pickedChampions.size());
        }
    }
    
    /**
     * 分析champion select session的draft pick状态
     */
    public DraftAnalysis analyzeDraftSession(JsonNode session, int localPlayerCellId) {
        logger.debug("开始分析draft pick session，本地玩家cellId: {}", localPlayerCellId);
        
        if (session == null || session.isMissingNode()) {
            logger.warn("Session为空或缺失");
            return null;
        }
        
        // 解析actions数组
        JsonNode actions = session.path("actions");
        if (!actions.isArray()) {
            logger.warn("Actions不是数组类型");
            return null;
        }
        
        // 获取队伍信息
        List<Integer> alliedTeam = extractTeamCellIds(session.path("myTeam"));
        List<Integer> enemyTeam = extractTeamCellIds(session.path("theirTeam"));
        
        logger.debug("己方队伍: {}, 敌方队伍: {}", alliedTeam, enemyTeam);
        
        // 解析draft阶段
        List<DraftPhase> phases = new ArrayList<>();
        DraftPhase currentPhase = null;
        DraftAction currentPlayerAction = null;
        int currentTurn = 0;
        
        // 收集所有ban和pick的英雄
        Set<Integer> bannedChampions = new HashSet<>();
        Set<Integer> pickedChampions = new HashSet<>();
        Map<Integer, Integer> playerChampions = new HashMap<>();
        
        // 分析每个阶段
        for (int phaseIndex = 0; phaseIndex < actions.size(); phaseIndex++) {
            JsonNode actionGroup = actions.get(phaseIndex);
            if (!actionGroup.isArray()) continue;
            
            List<DraftAction> phaseActions = new ArrayList<>();
            String phaseType = null;
            boolean isPhaseActive = false;
            
            for (JsonNode action : actionGroup) {
                int actionId = action.path("id").asInt();
                int actorCellId = action.path("actorCellId").asInt();
                String type = action.path("type").asText("");
                int championId = action.path("championId").asInt(0);
                boolean isInProgress = action.path("isInProgress").asBoolean(false);
                boolean completed = action.path("completed").asBoolean(false);
                
                // 确定阶段类型
                if (phaseType == null) {
                    phaseType = type;
                }
                
                // 判断是否是己方回合
                boolean isAllyTurn = alliedTeam.contains(actorCellId);
                
                // 计算pick轮次（基于阶段和action在阶段中的位置）
                int pickTurn = phaseIndex + 1;
                
                DraftAction draftAction = new DraftAction(actionId, actorCellId, type, championId, 
                                                        isInProgress, completed, pickTurn, isAllyTurn);
                phaseActions.add(draftAction);
                
                // 如果是当前玩家且正在进行
                if (actorCellId == localPlayerCellId && isInProgress && !completed) {
                    currentPlayerAction = draftAction;
                    currentTurn = pickTurn;
                }
                
                // 检查阶段是否活跃
                if (isInProgress || !completed) {
                    isPhaseActive = true;
                }
                
                // 收集已ban/pick的英雄
                if (completed && championId > 0) {
                    if ("ban".equals(type)) {
                        bannedChampions.add(championId);
                    } else if ("pick".equals(type)) {
                        pickedChampions.add(championId);
                        playerChampions.put(actorCellId, championId);
                    }
                }
            }
            
            DraftPhase phase = new DraftPhase(phaseIndex, phaseType, phaseActions, isPhaseActive);
            phases.add(phase);
            
            if (isPhaseActive && currentPhase == null) {
                currentPhase = phase;
            }
        }
        
        // 判断是否是draft pick模式（通常draft pick有多个交替的ban/pick阶段）
        boolean isDraftPick = phases.size() > 2 && 
                             phases.stream().anyMatch(p -> "ban".equals(p.getType())) &&
                             phases.stream().anyMatch(p -> "pick".equals(p.getType()));
        
        DraftAnalysis analysis = new DraftAnalysis(phases, currentPhase, currentPlayerAction, currentTurn,
                                                 isDraftPick, alliedTeam, enemyTeam, bannedChampions, 
                                                 pickedChampions, playerChampions);
        
        logger.info("Draft分析完成: {}", analysis);
        return analysis;
    }
    
    /**
     * 从team节点提取cellId列表
     */
    private List<Integer> extractTeamCellIds(JsonNode team) {
        List<Integer> cellIds = new ArrayList<>();
        if (team.isArray()) {
            for (JsonNode player : team) {
                int cellId = player.path("cellId").asInt(-1);
                if (cellId >= 0) {
                    cellIds.add(cellId);
                }
            }
        }
        return cellIds;
    }
    
    /**
     * 预测下一个pick轮次的策略建议
     */
    public List<String> getStrategicRecommendations(DraftAnalysis analysis) {
        List<String> recommendations = new ArrayList<>();
        
        if (analysis == null || !analysis.isDraftPick()) {
            return recommendations;
        }
        
        // 分析敌方已选英雄，提供针对性建议
        Map<Integer, Integer> enemyPicks = analysis.getPlayerChampions().entrySet().stream()
                .filter(entry -> analysis.getEnemyTeam().contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        if (!enemyPicks.isEmpty()) {
            recommendations.add("敌方已选择: " + enemyPicks.values().stream()
                    .map(String::valueOf).collect(Collectors.joining(", ")));
        }
        
        // 分析当前轮次的重要性
        if (analysis.getCurrentPlayerAction() != null) {
            DraftAction action = analysis.getCurrentPlayerAction();
            if ("pick".equals(action.getType())) {
                if (action.getPickTurn() <= 2) {
                    recommendations.add("前期pick，建议选择强势或版本英雄");
                } else if (action.getPickTurn() >= 4) {
                    recommendations.add("后期pick，建议针对敌方阵容选择");
                }
            }
        }
        
        return recommendations;
    }
    
    /**
     * 判断是否应该延迟pick（等待更多信息）
     */
    public boolean shouldDelayPick(DraftAnalysis analysis) {
        if (analysis == null || analysis.getCurrentPlayerAction() == null) {
            return false;
        }
        
        DraftAction action = analysis.getCurrentPlayerAction();
        
        // 如果是最后一个pick，不应该延迟
        if (action.getPickTurn() >= 5) {
            return false;
        }
        
        // 如果敌方还没有显示关键pick，可以稍微延迟
        Map<Integer, Integer> enemyPicks = analysis.getPlayerChampions().entrySet().stream()
                .filter(entry -> analysis.getEnemyTeam().contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        // 如果敌方pick数量少于己方，可以延迟
        Map<Integer, Integer> allyPicks = analysis.getPlayerChampions().entrySet().stream()
                .filter(entry -> analysis.getAlliedTeam().contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        return enemyPicks.size() < allyPicks.size();
    }
}