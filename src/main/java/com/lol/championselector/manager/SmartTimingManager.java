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
    public void handleSmartBan(int actionId, AutoAcceptConfig.ChampionInfo selectedBanChampion, String playerPosition) {
        handleSmartBan(actionId, selectedBanChampion, playerPosition, new HashSet<>());
    }
    
    /**
     * 智能处理Ban操作（接收已选定的英雄）
     */
    public void handleSmartBan(int actionId, AutoAcceptConfig.ChampionInfo selectedBanChampion, String playerPosition, Set<Integer> bannedChampions) {
        if (!config.getChampionSelect().isSmartTimingEnabled()) {
            // 如果未启用智能时机，直接执行Ban
            executeBan(actionId, selectedBanChampion);
            return;
        }
        
        if (selectedBanChampion == null || selectedBanChampion.getChampionId() == null) {
            logger.warn("No valid ban champion provided for action {}", actionId);
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
     * 智能处理Pick操作（接收已选定的英雄）
     */
    public void handleSmartPick(int actionId, AutoAcceptConfig.ChampionInfo selectedPickChampion, String playerPosition) {
        if (!config.getChampionSelect().isSmartTimingEnabled()) {
            // 如果未启用智能时机，直接执行Pick
            executePick(actionId, selectedPickChampion);
            return;
        }
        
        if (selectedPickChampion == null || selectedPickChampion.getChampionId() == null) {
            logger.warn("No valid pick champion provided for action {}", actionId);
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
    
    // 英雄选择逻辑已移至AutoAcceptController，SmartTimingManager只负责时机控制
    
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