package com.lol.championselector.manager;

import com.lol.championselector.config.AutoAcceptConfig;
import com.lol.championselector.lcu.LCUMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.Map;
import java.util.Set;
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
        if (!config.getChampionSelect().isSmartTimingEnabled()) {
            // 如果未启用智能时机，直接执行Ban
            executeBan(actionId, banChampion);
            return;
        }
        
        AutoAcceptConfig.ChampionInfo selectedBanChampion = selectBanChampion(banChampion, playerPosition);
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
        if (!config.getChampionSelect().isUsePositionBasedSelection() || playerPosition == null) {
            return defaultBanChampion;
        }
        
        AutoAcceptConfig.PositionConfig positionConfig = config.getChampionSelect().getPositionConfig(playerPosition);
        if (positionConfig == null) {
            logger.debug("No position config found for position: {}, using default", playerPosition);
            return defaultBanChampion;
        }
        
        // 获取已被ban的英雄，选择备选英雄
        return lcuMonitor.getBannedChampions()
            .thenApply(bannedChampions -> {
                AutoAcceptConfig.ChampionInfo alternateBan = positionConfig.getAlternateBanChampion(bannedChampions);
                if (alternateBan != null) {
                    logger.info("Selected position-based ban champion {} for position {}", alternateBan, playerPosition);
                    return alternateBan;
                } else {
                    logger.debug("No available position-based ban champion for position {}, using default", playerPosition);
                    return defaultBanChampion;
                }
            })
            .exceptionally(throwable -> {
                logger.warn("Failed to get banned champions, using default ban", throwable);
                return defaultBanChampion;
            })
            .join(); // 同步等待结果
    }
    
    /**
     * 根据分路选择Pick英雄
     */
    private AutoAcceptConfig.ChampionInfo selectPickChampion(AutoAcceptConfig.ChampionInfo defaultPickChampion, String playerPosition) {
        if (!config.getChampionSelect().isUsePositionBasedSelection() || playerPosition == null) {
            return defaultPickChampion;
        }
        
        AutoAcceptConfig.PositionConfig positionConfig = config.getChampionSelect().getPositionConfig(playerPosition);
        if (positionConfig == null) {
            logger.debug("No position config found for position: {}, using default", playerPosition);
            return defaultPickChampion;
        }
        
        // 获取已被ban和pick的英雄，选择备选英雄
        CompletableFuture<Set<Integer>> bannedFuture = lcuMonitor.getBannedChampions();
        CompletableFuture<Set<Integer>> pickedFuture = lcuMonitor.getPickedChampions();
        
        return CompletableFuture.allOf(bannedFuture, pickedFuture)
            .thenApply(v -> {
                Set<Integer> bannedChampions = bannedFuture.join();
                Set<Integer> pickedChampions = pickedFuture.join();
                
                AutoAcceptConfig.ChampionInfo alternatePick = positionConfig.getAlternatePickChampion(bannedChampions, pickedChampions);
                if (alternatePick != null) {
                    logger.info("Selected position-based pick champion {} for position {}", alternatePick, playerPosition);
                    return alternatePick;
                } else {
                    logger.debug("No available position-based pick champion for position {}, using default", playerPosition);
                    return defaultPickChampion;
                }
            })
            .exceptionally(throwable -> {
                logger.warn("Failed to get banned/picked champions, using default pick", throwable);
                return defaultPickChampion;
            })
            .join(); // 同步等待结果
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
                    
                    // 检查是否到了执行时间
                    if (remainingSeconds <= pendingAction.getExecutionDelaySeconds()) {
                        logger.info("Executing {} for action {} (remaining time: {}s)", 
                                   pendingAction.getActionType(), actionId, remainingSeconds);
                        
                        if ("ban".equals(pendingAction.getActionType())) {
                            executeBan(actionId, pendingAction.getChampion());
                        } else if ("pick".equals(pendingAction.getActionType())) {
                            executePick(actionId, pendingAction.getChampion());
                        }
                        
                        return true; // 移除已执行的action
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
        if (banChampion == null || banChampion.getChampionId() == null) {
            logger.warn("Invalid ban champion for action {}", actionId);
            return;
        }
        
        lcuMonitor.banChampion(banChampion.getChampionId(), actionId)
            .thenAccept(success -> {
                if (success) {
                    logger.info("Successfully executed ban {} for action {}", banChampion, actionId);
                } else {
                    logger.warn("Failed to execute ban {} for action {}", banChampion, actionId);
                }
            });
    }
    
    /**
     * 执行Pick操作
     */
    private void executePick(int actionId, AutoAcceptConfig.ChampionInfo pickChampion) {
        if (pickChampion == null || pickChampion.getChampionId() == null) {
            logger.warn("Invalid pick champion for action {}", actionId);
            return;
        }
        
        lcuMonitor.pickChampion(pickChampion.getChampionId(), actionId)
            .thenAccept(success -> {
                if (success) {
                    logger.info("Successfully executed pick {} for action {}", pickChampion, actionId);
                } else {
                    logger.warn("Failed to execute pick {} for action {}", pickChampion, actionId);
                }
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