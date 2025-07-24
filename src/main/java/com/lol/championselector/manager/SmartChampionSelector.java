package com.lol.championselector.manager;

import com.lol.championselector.config.AutoAcceptConfig;
import com.lol.championselector.manager.DraftPickEngine.DraftAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能英雄选择策略 - 基于draft pick分析动态调整英雄选择
 */
public class SmartChampionSelector {
    private static final Logger logger = LoggerFactory.getLogger(SmartChampionSelector.class);
    
    /**
     * 英雄选择策略结果
     */
    public static class SelectionStrategy {
        private final AutoAcceptConfig.ChampionInfo recommendedChampion;
        private final String reason;
        private final int priority; // 1-5, 1为最高优先级
        private final boolean shouldDelay;
        private final List<String> alternatives;
        
        public SelectionStrategy(AutoAcceptConfig.ChampionInfo recommendedChampion, String reason, 
                               int priority, boolean shouldDelay, List<String> alternatives) {
            this.recommendedChampion = recommendedChampion;
            this.reason = reason;
            this.priority = priority;
            this.shouldDelay = shouldDelay;
            this.alternatives = alternatives;
        }
        
        public AutoAcceptConfig.ChampionInfo getRecommendedChampion() { return recommendedChampion; }
        public String getReason() { return reason; }
        public int getPriority() { return priority; }
        public boolean shouldDelay() { return shouldDelay; }
        public List<String> getAlternatives() { return alternatives; }
        
        @Override
        public String toString() {
            return String.format("SelectionStrategy{champion=%s, reason='%s', priority=%d, delay=%s, alternatives=%d}", 
                                recommendedChampion, reason, priority, shouldDelay, alternatives.size());
        }
    }
    
    // 英雄角色分类 - 基于实际游戏中的角色定位
    private static final Map<String, Set<Integer>> ROLE_CHAMPIONS = new HashMap<>();
    
    static {
        // 这里可以根据实际需要扩展英雄分类
        // 示例：一些常见英雄的角色分类
        ROLE_CHAMPIONS.put("ADC", Set.of(22, 18, 51, 119, 96)); // Ashe, Tristana, Caitlyn, Draven, Kog'Maw
        ROLE_CHAMPIONS.put("Support", Set.of(12, 40, 89, 25, 111)); // Alistar, Janna, Leona, Morgana, Nautilus
        ROLE_CHAMPIONS.put("Tank", Set.of(54, 57, 78, 14, 113)); // Malphite, Maokai, Poppy, Sion, Sejuani
        ROLE_CHAMPIONS.put("AP_Carry", Set.of(1, 7, 99, 69, 134)); // Annie, LeBlanc, Lux, Cassiopeia, Syndra
        ROLE_CHAMPIONS.put("Assassin", Set.of(238, 91, 121, 107, 245)); // Zed, Talon, Kha'Zix, Rengar, Ekko
    }
    
    /**
     * 基于draft分析选择最佳英雄
     */
    public SelectionStrategy selectOptimalChampion(DraftAnalysis analysis, 
                                                 AutoAcceptConfig.ChampionInfo defaultChampion,
                                                 List<AutoAcceptConfig.ChampionInfo> championQueue,
                                                 String currentPosition) {
        return selectOptimalChampion(analysis, defaultChampion, championQueue, currentPosition, 30); // 默认30秒
    }
    
    /**
     * 基于draft分析选择最佳英雄（含时间信息）
     */
    public SelectionStrategy selectOptimalChampion(DraftAnalysis analysis, 
                                                 AutoAcceptConfig.ChampionInfo defaultChampion,
                                                 List<AutoAcceptConfig.ChampionInfo> championQueue,
                                                 String currentPosition,
                                                 int remainingTimeSeconds) {
        
        logger.info("开始智能英雄选择分析，默认英雄: {}, 队列大小: {}, 当前位置: {}, 剩余时间: {}秒", 
                   defaultChampion, championQueue.size(), currentPosition, remainingTimeSeconds);
        
        if (analysis == null || analysis.getCurrentPlayerAction() == null) {
            logger.debug("无有效draft分析或当前玩家action，使用默认策略");
            return new SelectionStrategy(defaultChampion, "无draft分析数据", 3, false, new ArrayList<>());
        }
        
        // 获取可用英雄（排除已ban和已pick的）
        List<AutoAcceptConfig.ChampionInfo> availableChampions = championQueue.stream()
                .filter(champion -> champion.getChampionId() != null)
                .filter(champion -> !analysis.getBannedChampions().contains(champion.getChampionId()))
                .filter(champion -> !analysis.getPickedChampions().contains(champion.getChampionId()))
                .collect(Collectors.toList());
        
        logger.debug("可用英雄数量: {} (排除{}个已ban, {}个已pick)", 
                    availableChampions.size(), analysis.getBannedChampions().size(), analysis.getPickedChampions().size());
        
        if (availableChampions.isEmpty()) {
            logger.warn("没有可用英雄，尝试使用默认英雄");
            if (defaultChampion != null && defaultChampion.getChampionId() != null &&
                !analysis.getBannedChampions().contains(defaultChampion.getChampionId()) &&
                !analysis.getPickedChampions().contains(defaultChampion.getChampionId())) {
                return new SelectionStrategy(defaultChampion, "队列中无可用英雄，使用默认", 4, false, new ArrayList<>());
            } else {
                logger.error("连默认英雄都不可用");
                return new SelectionStrategy(null, "无任何可用英雄", 5, false, new ArrayList<>());
            }
        }
        
        // 分析当前draft状态
        int currentTurn = analysis.getCurrentTurn();
        boolean isDraftPick = analysis.isDraftPick();
        
        // 策略1: 早期pick - 选择强势英雄
        if (currentTurn <= 2 && isDraftPick) {
            AutoAcceptConfig.ChampionInfo strongPick = selectStrongEarlyPick(availableChampions, analysis);
            if (strongPick != null) {
                return new SelectionStrategy(strongPick, "早期pick选择强势英雄", 1, false, 
                                           getAlternativeNames(availableChampions, strongPick));
            }
        }
        
        // 策略2: 后期pick - 针对敌方阵容
        if (currentTurn >= 4 && isDraftPick) {
            AutoAcceptConfig.ChampionInfo counterPick = selectCounterPick(availableChampions, analysis);
            if (counterPick != null) {
                return new SelectionStrategy(counterPick, "后期pick针对敌方阵容", 1, false,
                                           getAlternativeNames(availableChampions, counterPick));
            }
        }
        
        // 策略3: 团队构成平衡
        if (isDraftPick) {
            AutoAcceptConfig.ChampionInfo balancedPick = selectForTeamBalance(availableChampions, analysis, currentPosition);
            if (balancedPick != null) {
                return new SelectionStrategy(balancedPick, "平衡团队构成", 2, false,
                                           getAlternativeNames(availableChampions, balancedPick));
            }
        }
        
        // 策略4: 默认队列顺序
        AutoAcceptConfig.ChampionInfo firstAvailable = availableChampions.get(0);
        return new SelectionStrategy(firstAvailable, "按队列顺序选择", 3, false,
                                   getAlternativeNames(availableChampions, firstAvailable));
    }
    
    /**
     * 选择强势的早期pick英雄
     */
    private AutoAcceptConfig.ChampionInfo selectStrongEarlyPick(List<AutoAcceptConfig.ChampionInfo> availableChampions, 
                                                              DraftAnalysis analysis) {
        // 优先选择队列中靠前的英雄（假设用户已经按强度排序）
        // 同时避免选择容易被针对的英雄
        
        for (AutoAcceptConfig.ChampionInfo champion : availableChampions) {
            // 这里可以加入更复杂的强势判断逻辑
            // 暂时简单返回第一个可用的
            logger.debug("选择早期强势英雄: {}", champion);
            return champion;
        }
        return null;
    }
    
    /**
     * 选择针对敌方阵容的英雄
     */
    private AutoAcceptConfig.ChampionInfo selectCounterPick(List<AutoAcceptConfig.ChampionInfo> availableChampions,
                                                          DraftAnalysis analysis) {
        // 分析敌方已选英雄
        Map<Integer, Integer> enemyPicks = analysis.getPlayerChampions().entrySet().stream()
                .filter(entry -> analysis.getEnemyTeam().contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        if (enemyPicks.isEmpty()) {
            logger.debug("敌方尚未选择英雄，无法进行针对性选择");
            return availableChampions.isEmpty() ? null : availableChampions.get(0);
        }
        
        logger.debug("分析敌方英雄进行针对: {}", enemyPicks.values());
        
        // 检查敌方是否有特定类型的英雄需要针对
        Set<Integer> enemyChampions = new HashSet<>(enemyPicks.values());
        
        // 如果敌方有很多AD英雄，优先选择护甲类英雄
        long adCount = enemyChampions.stream()
                .filter(champId -> ROLE_CHAMPIONS.getOrDefault("ADC", Collections.emptySet()).contains(champId))
                .count();
        
        if (adCount >= 2) {
            // 寻找坦克或护甲类英雄
            for (AutoAcceptConfig.ChampionInfo champion : availableChampions) {
                if (ROLE_CHAMPIONS.getOrDefault("Tank", Collections.emptySet()).contains(champion.getChampionId())) {
                    logger.debug("针对多AD阵容选择坦克: {}", champion);
                    return champion;
                }
            }
        }
        
        // 默认返回队列中第一个可用英雄
        return availableChampions.isEmpty() ? null : availableChampions.get(0);
    }
    
    /**
     * 选择平衡团队构成的英雄
     */
    private AutoAcceptConfig.ChampionInfo selectForTeamBalance(List<AutoAcceptConfig.ChampionInfo> availableChampions,
                                                             DraftAnalysis analysis, String currentPosition) {
        // 分析己方已选英雄
        Map<Integer, Integer> allyPicks = analysis.getPlayerChampions().entrySet().stream()
                .filter(entry -> analysis.getAlliedTeam().contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        if (allyPicks.isEmpty()) {
            logger.debug("己方尚未选择英雄，按位置选择");
            return selectByPosition(availableChampions, currentPosition);
        }
        
        // 分析团队构成缺失的角色
        Set<Integer> allyChampions = new HashSet<>(allyPicks.values());
        
        boolean hasADC = allyChampions.stream().anyMatch(champId -> 
                ROLE_CHAMPIONS.getOrDefault("ADC", Collections.emptySet()).contains(champId));
        boolean hasSupport = allyChampions.stream().anyMatch(champId -> 
                ROLE_CHAMPIONS.getOrDefault("Support", Collections.emptySet()).contains(champId));
        boolean hasTank = allyChampions.stream().anyMatch(champId -> 
                ROLE_CHAMPIONS.getOrDefault("Tank", Collections.emptySet()).contains(champId));
        
        logger.debug("团队构成分析 - ADC: {}, Support: {}, Tank: {}", hasADC, hasSupport, hasTank);
        
        // 根据位置和团队需求选择
        if ("bottom".equals(currentPosition) && !hasADC) {
            return selectByRole(availableChampions, "ADC");
        } else if ("utility".equals(currentPosition) && !hasSupport) {
            return selectByRole(availableChampions, "Support");
        } else if ("top".equals(currentPosition) && !hasTank) {
            return selectByRole(availableChampions, "Tank");
        }
        
        return availableChampions.isEmpty() ? null : availableChampions.get(0);
    }
    
    /**
     * 按角色选择英雄
     */
    private AutoAcceptConfig.ChampionInfo selectByRole(List<AutoAcceptConfig.ChampionInfo> availableChampions, String role) {
        Set<Integer> roleChampions = ROLE_CHAMPIONS.getOrDefault(role, Collections.emptySet());
        
        for (AutoAcceptConfig.ChampionInfo champion : availableChampions) {
            if (roleChampions.contains(champion.getChampionId())) {
                logger.debug("按角色{}选择英雄: {}", role, champion);
                return champion;
            }
        }
        
        logger.debug("未找到角色{}的英雄，选择队列中第一个", role);
        return availableChampions.isEmpty() ? null : availableChampions.get(0);
    }
    
    /**
     * 按位置选择英雄
     */
    private AutoAcceptConfig.ChampionInfo selectByPosition(List<AutoAcceptConfig.ChampionInfo> availableChampions, String position) {
        // 简单的位置到角色映射
        String targetRole = switch (position) {
            case "bottom" -> "ADC";
            case "utility" -> "Support";
            case "top" -> "Tank";
            case "middle" -> "AP_Carry";
            case "jungle" -> "Assassin";
            default -> null;
        };
        
        if (targetRole != null) {
            return selectByRole(availableChampions, targetRole);
        }
        
        return availableChampions.isEmpty() ? null : availableChampions.get(0);
    }
    
    /**
     * 获取备选英雄名称列表
     */
    private List<String> getAlternativeNames(List<AutoAcceptConfig.ChampionInfo> availableChampions, 
                                           AutoAcceptConfig.ChampionInfo selected) {
        return availableChampions.stream()
                .filter(champion -> !champion.equals(selected))
                .limit(3) // 只显示前3个备选
                .map(champion -> champion.getNameCn() != null ? champion.getNameCn() : champion.getKey())
                .collect(Collectors.toList());
    }
    
    /**
     * 判断是否应该延迟pick等待更多信息
     */
    public boolean shouldDelayForStrategy(DraftAnalysis analysis, int remainingTimeMs) {
        if (analysis == null || analysis.getCurrentPlayerAction() == null) {
            return false;
        }
        
        // 如果剩余时间少于10秒，不要延迟
        if (remainingTimeMs < 10000) {
            return false;
        }
        
        // 如果是最后的pick，不要延迟
        if (analysis.getCurrentTurn() >= 5) {
            return false;
        }
        
        // 如果敌方还有更多pick要做，可以短暂延迟观察
        long enemyPendingPicks = analysis.getPhases().stream()
                .flatMap(phase -> phase.getActions().stream())
                .filter(action -> !action.isAllyTurn() && !action.isCompleted() && "pick".equals(action.getType()))
                .count();
        
        return enemyPendingPicks > 0 && remainingTimeMs > 15000;
    }
}