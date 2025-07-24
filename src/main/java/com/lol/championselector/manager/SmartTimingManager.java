package com.lol.championselector.manager;

import com.lol.championselector.config.AutoAcceptConfig;
import com.lol.championselector.lcu.LCUMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 智能时机控制管理器
 * 负责控制Ban/Pick的时机，实现hover预选和延迟确认功能
 */
public class SmartTimingManager {
    private static final Logger logger = LoggerFactory.getLogger(SmartTimingManager.class);
    
    private final LCUMonitor lcuMonitor;
    private final AutoAcceptConfig config;
    private final ScheduledExecutorService scheduler;
    
    // 跟踪hover状态的Map: actionId -> PendingAction
    private final Map<Integer, PendingAction> pendingActions = new ConcurrentHashMap<>();
    private final AtomicBoolean isActive = new AtomicBoolean(false);
    
    public SmartTimingManager(LCUMonitor lcuMonitor, AutoAcceptConfig config) {
        this.lcuMonitor = lcuMonitor;
        this.config = config;
        this.scheduler = Executors.newScheduledThreadPool(2);
    }
    
    /**
     * 启动智能时机管理
     */
    public void start() {
        if (isActive.getAndSet(true)) {
            return;
        }
        
        logger.info("SmartTimingManager started");
        
        // 每500ms检查一次待处理的actions
        scheduler.scheduleWithFixedDelay(this::processPendingActions, 500, 500, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 停止智能时机管理
     */
    public void stop() {
        if (!isActive.getAndSet(false)) {
            return;
        }
        
        logger.info("SmartTimingManager stopped");
        
        // 取消所有待处理的任务
        pendingActions.clear();
    }
    
    /**
     * 智能处理Ban操作
     */
    public void handleSmartBan(int actionId, AutoAcceptConfig.ChampionInfo banChampion, String playerPosition) {
        handleSmartBan(actionId, banChampion, playerPosition, new HashSet<>());
    }
    
    /**
     * 智能处理Ban操作（带已ban英雄列表）
     */
    public void handleSmartBan(int actionId, AutoAcceptConfig.ChampionInfo banChampion, String playerPosition, Set<Integer> bannedChampions) {
        if (!config.getChampionSelect().isSmartTimingEnabled()) {
            // 如果未启用智能时机，直接执行Ban
            executeBan(actionId, banChampion);
            return;
        }
        
        AutoAcceptConfig.ChampionInfo selectedBanChampion = selectBanChampion(banChampion, playerPosition, bannedChampions);
        if (selectedBanChampion == null || selectedBanChampion.getChampionId() == null) {
            logger.warn("No valid ban champion selected for action {}", actionId);
            return;
        }
        
        PendingAction pendingAction = new PendingAction(
            actionId,
            "ban",
            selectedBanChampion,
            config.getChampionSelect().getBanExecutionDelaySeconds()
        );
        
        pendingActions.put(actionId, pendingAction);
        
        // 如果启用hover，立即hover选择的英雄
        if (config.getChampionSelect().isEnableHover()) {
            lcuMonitor.hoverChampion(selectedBanChampion.getChampionId(), actionId)
                .thenAccept(success -> {
                    if (success) {
                        logger.info("Successfully hovered ban champion {} for action {}", 
                                  selectedBanChampion, actionId);
                    } else {
                        logger.warn("Failed to hover ban champion {} for action {}", 
                                  selectedBanChampion, actionId);
                    }
                });
        }
        
        logger.info("Scheduled smart ban for action {} with champion {} (delay: {}s)", 
                   actionId, selectedBanChampion, config.getChampionSelect().getBanExecutionDelaySeconds());
    }
    
    /**
     * 智能处理Pick操作
     */
    public void handleSmartPick(int actionId, AutoAcceptConfig.ChampionInfo pickChampion, String playerPosition) {
        if (!config.getChampionSelect().isSmartTimingEnabled()) {
            // 如果未启用智能时机，直接执行Pick
            executePick(actionId, pickChampion);
            return;
        }
        
        AutoAcceptConfig.ChampionInfo selectedPickChampion = selectPickChampion(pickChampion, playerPosition);
        if (selectedPickChampion == null || selectedPickChampion.getChampionId() == null) {
            logger.warn("No valid pick champion selected for action {}", actionId);
            return;
        }
        
        PendingAction pendingAction = new PendingAction(
            actionId,
            "pick",
            selectedPickChampion,
            config.getChampionSelect().getPickExecutionDelaySeconds()
        );
        
        pendingActions.put(actionId, pendingAction);
        
        // 如果启用hover，立即hover选择的英雄
        if (config.getChampionSelect().isEnableHover()) {
            lcuMonitor.hoverChampion(selectedPickChampion.getChampionId(), actionId)
                .thenAccept(success -> {
                    if (success) {
                        logger.info("Successfully hovered pick champion {} for action {}", 
                                  selectedPickChampion, actionId);
                    } else {
                        logger.warn("Failed to hover pick champion {} for action {}", 
                                  selectedPickChampion, actionId);
                    }
                });
        }
        
        logger.info("Scheduled smart pick for action {} with champion {} (delay: {}s)", 
                   actionId, selectedPickChampion, config.getChampionSelect().getPickExecutionDelaySeconds());
    }
    
    /**
     * 根据分路选择Ban英雄
     */
    private AutoAcceptConfig.ChampionInfo selectBanChampion(AutoAcceptConfig.ChampionInfo defaultBanChampion, String playerPosition) {
        return selectBanChampion(defaultBanChampion, playerPosition, new HashSet<>());
    }
    
    /**
     * 根据分路和已ban英雄选择Ban英雄
     */
    private AutoAcceptConfig.ChampionInfo selectBanChampion(AutoAcceptConfig.ChampionInfo defaultBanChampion, String playerPosition, Set<Integer> bannedChampions) {
        // 如果未启用分路预设或没有位置信息，使用默认英雄
        if (!config.getChampionSelect().isUsePositionBasedSelection() || playerPosition == null) {
            logger.debug("Position-based selection disabled or no position, using default ban champion");
            if (defaultBanChampion != null && defaultBanChampion.getChampionId() != null &&
                !bannedChampions.contains(defaultBanChampion.getChampionId())) {
                logger.info("Selected default ban champion {} (position-based disabled)", defaultBanChampion);
                return defaultBanChampion;
            }
            logger.warn("Default ban champion {} is already banned", defaultBanChampion);
            return null;
        }
        
        // 优先级1：从分路配置队列中选择可用英雄
        AutoAcceptConfig.PositionConfig positionConfig = config.getChampionSelect().getPositionConfig(playerPosition);
        if (positionConfig != null) {
            AutoAcceptConfig.ChampionInfo availableChampion = positionConfig.getAlternateBanChampion(bannedChampions);
            if (availableChampion != null) {
                availableChampion.ensureChampionId();
                logger.info("Selected ban champion {} from position {} queue (smart timing, priority 1, skipping {} banned champions)", 
                           availableChampion, playerPosition, bannedChampions.size());
                return availableChampion;
            }
            logger.debug("No available champions in position {} ban queue, trying fallback", playerPosition);
        } else {
            logger.debug("No position config found for position: {}, trying fallback", playerPosition);
        }
        
        // 优先级2：回退到默认英雄（如果未被ban）
        if (defaultBanChampion != null && defaultBanChampion.getChampionId() != null &&
            !bannedChampions.contains(defaultBanChampion.getChampionId())) {
            logger.info("Using fallback default ban champion {} for position {} (smart timing, priority 2)", 
                       defaultBanChampion, playerPosition);
            return defaultBanChampion;
        }
        
        // 优先级3：所有选项都不可用
        logger.warn("No available ban champion found for position {} - queue exhausted and default banned (banned champions: {})", 
                   playerPosition, bannedChampions);
        return null;
    }
    
    /**
     * 根据分路选择Pick英雄（智能时机管理器版本，暂时保持简化逻辑）
     */
    private AutoAcceptConfig.ChampionInfo selectPickChampion(AutoAcceptConfig.ChampionInfo defaultPickChampion, String playerPosition) {
        if (!config.getChampionSelect().isUsePositionBasedSelection() || playerPosition == null) {
            logger.debug("Position-based selection disabled or no position, using default pick champion");
            return defaultPickChampion;
        }
        
        AutoAcceptConfig.PositionConfig positionConfig = config.getChampionSelect().getPositionConfig(playerPosition);
        if (positionConfig == null) {
            logger.debug("No position config found for position: {}, using default", playerPosition);
            return defaultPickChampion;
        }
        
        // 优先使用分路配置的首选pick英雄
        AutoAcceptConfig.ChampionInfo preferredPick = positionConfig.getPreferredPickChampion();
        if (preferredPick != null) {
            // 确保championId有效
            preferredPick.ensureChampionId();
            if (preferredPick.getChampionId() != null) {
                logger.info("Selected position-based pick champion {} for position {} in smart timing", preferredPick, playerPosition);
                return preferredPick;
            } else {
                logger.warn("Position-based pick champion {} has invalid championId", preferredPick);
            }
        }
        
        // 如果分路配置中有pick英雄列表，选择第一个有效的
        if (positionConfig.getPickChampions() != null && !positionConfig.getPickChampions().isEmpty()) {
            for (AutoAcceptConfig.ChampionInfo pickCandidate : positionConfig.getPickChampions()) {
                pickCandidate.ensureChampionId();
                if (pickCandidate.getChampionId() != null) {
                    logger.info("Selected pick champion {} from position config for {} in smart timing", pickCandidate, playerPosition);
                    return pickCandidate;
                }
            }
        }
        
        logger.debug("No valid position-based pick champion found for position {} in smart timing, using default", playerPosition);
        return defaultPickChampion;
    }
    
    /**
     * 处理待执行的actions
     */
    private void processPendingActions() {
        if (pendingActions.isEmpty()) {
            return;
        }
        
        lcuMonitor.getRemainingTimeInPhase()
            .thenAccept(remainingSeconds -> {
                pendingActions.entrySet().removeIf(entry -> {
                    int actionId = entry.getKey();
                    PendingAction pendingAction = entry.getValue();
                    
                    // 为ban和pick使用不同的时机策略
                    boolean shouldExecute = false;
                    String actionType = pendingAction.getActionType();
                    
                    if ("ban".equals(actionType)) {
                        // Ban操作：更宽松的时机控制，确保能够执行
                        shouldExecute = remainingSeconds <= pendingAction.getExecutionDelaySeconds() && remainingSeconds >= 0.2;
                    } else if ("pick".equals(actionType)) {
                        // Pick操作：更精确的时机控制
                        shouldExecute = remainingSeconds <= pendingAction.getExecutionDelaySeconds() && remainingSeconds >= 0.5;
                    }
                    
                    if (shouldExecute) {
                        logger.info("Executing {} for action {} (remaining time: {}s)", 
                                   actionType, actionId, remainingSeconds);
                        
                        if ("ban".equals(actionType)) {
                            // Ban操作前再次确认英雄可用性
                            lcuMonitor.getBannedChampions()
                                .thenAccept(currentBannedChampions -> {
                                    AutoAcceptConfig.ChampionInfo champion = pendingAction.getChampion();
                                    if (champion.getChampionId() != null && !currentBannedChampions.contains(champion.getChampionId())) {
                                        executeBan(actionId, champion);
                                    } else {
                                        logger.warn("Champion {} is already banned, skipping ban for action {}", champion, actionId);
                                    }
                                })
                                .exceptionally(throwable -> {
                                    logger.error("Failed to check banned champions, executing ban anyway", throwable);
                                    executeBan(actionId, pendingAction.getChampion());
                                    return null;
                                });
                        } else if ("pick".equals(actionType)) {
                            // Pick操作执行前再次确认剩余时间
                            lcuMonitor.getRemainingTimeInPhase().thenAccept(currentRemaining -> {
                                if (currentRemaining >= 0.5) {
                                    executePick(actionId, pendingAction.getChampion());
                                } else {
                                    logger.warn("Skipped pick for action {} due to insufficient remaining time: {}s", 
                                               actionId, currentRemaining);
                                }
                            }).exceptionally(throwable -> {
                                logger.warn("Failed to verify remaining time, executing pick for action {} anyway", actionId);
                                executePick(actionId, pendingAction.getChampion());
                                return null;
                            });
                        }
                        
                        return true; // 移除已执行的action
                    } else if (remainingSeconds < 0.2) {
                        // 剩余时间过少，跳过执行
                        logger.warn("Skipped {} for action {} due to insufficient time: {}s", 
                                   actionType, actionId, remainingSeconds);
                        return true; // 移除超时的action
                    }
                    
                    return false; // 保留未到执行时间的action
                });
            })
            .exceptionally(throwable -> {
                logger.debug("Failed to get remaining time for pending actions", throwable);
                return null;
            });
    }
    
    /**
     * 执行Ban操作
     */
    private void executeBan(int actionId, AutoAcceptConfig.ChampionInfo banChampion) {
        if (banChampion == null) {
            logger.warn("Ban champion is null for action {}", actionId);
            return;
        }
        
        // 确保championId有效
        banChampion.ensureChampionId();
        if (banChampion.getChampionId() == null) {
            logger.error("Invalid championId for ban champion {} (key: {}) for action {}", 
                        banChampion, banChampion.getKey(), actionId);
            return;
        }
        
        logger.info("Executing ban for champion {} (ID: {}) on action {}", 
                   banChampion, banChampion.getChampionId(), actionId);
        
        lcuMonitor.banChampion(banChampion.getChampionId(), actionId)
            .thenAccept(success -> {
                if (success) {
                    logger.info("Successfully executed ban {} for action {}", banChampion, actionId);
                } else {
                    logger.warn("Failed to execute ban {} for action {}", banChampion, actionId);
                }
            })
            .exceptionally(throwable -> {
                logger.error("Exception while executing ban {} for action {}", banChampion, actionId, throwable);
                return null;
            });
    }
    
    /**
     * 执行Pick操作
     */
    private void executePick(int actionId, AutoAcceptConfig.ChampionInfo pickChampion) {
        if (pickChampion == null) {
            logger.warn("Pick champion is null for action {}", actionId);
            return;
        }
        
        // 确保championId有效
        pickChampion.ensureChampionId();
        if (pickChampion.getChampionId() == null) {
            logger.error("Invalid championId for pick champion {} (key: {}) for action {}", 
                        pickChampion, pickChampion.getKey(), actionId);
            return;
        }
        
        logger.info("Executing pick for champion {} (ID: {}) on action {}", 
                   pickChampion, pickChampion.getChampionId(), actionId);
        
        lcuMonitor.pickChampion(pickChampion.getChampionId(), actionId)
            .thenAccept(success -> {
                if (success) {
                    logger.info("Successfully executed pick {} for action {}", pickChampion, actionId);
                } else {
                    logger.warn("Failed to execute pick {} for action {}", pickChampion, actionId);
                }
            })
            .exceptionally(throwable -> {
                logger.error("Exception while executing pick {} for action {}", pickChampion, actionId, throwable);
                return null;
            });
    }
    
    /**
     * 清理指定session的待处理actions
     */
    public void clearPendingActionsForSession() {
        int clearedCount = pendingActions.size();
        pendingActions.clear();
        if (clearedCount > 0) {
            logger.info("Cleared {} pending actions for new session", clearedCount);
        }
    }
    
    /**
     * 关闭管理器
     */
    public void shutdown() {
        stop();
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        logger.info("SmartTimingManager shutdown completed");
    }
    
    /**
     * 待处理的Action
     */
    private static class PendingAction {
        private final int actionId;
        private final String actionType;
        private final AutoAcceptConfig.ChampionInfo champion;
        private final int executionDelaySeconds;
        
        public PendingAction(int actionId, String actionType, AutoAcceptConfig.ChampionInfo champion, int executionDelaySeconds) {
            this.actionId = actionId;
            this.actionType = actionType;
            this.champion = champion;
            this.executionDelaySeconds = executionDelaySeconds;
        }
        
        public int getActionId() { return actionId; }
        public String getActionType() { return actionType; }
        public AutoAcceptConfig.ChampionInfo getChampion() { return champion; }
        public int getExecutionDelaySeconds() { return executionDelaySeconds; }
    }
}