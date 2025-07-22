package com.lol.championselector.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.lol.championselector.lcu.GamePhase;
import com.lol.championselector.lcu.LCUConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 弹窗抑制管理器
 * 负责在自动化功能启用时抑制游戏客户端的会话弹窗
 * 提供无干扰的自动化体验
 */
public class PopupSuppressionManager {
    private static final Logger logger = LoggerFactory.getLogger(PopupSuppressionManager.class);
    
    private LCUConnection connection;
    private ScheduledExecutorService scheduler;
    
    // 抑制配置
    private AtomicBoolean suppressReadyCheckPopup = new AtomicBoolean(false);
    private AtomicBoolean suppressBanPhasePopup = new AtomicBoolean(false);
    private AtomicBoolean suppressPickPhasePopup = new AtomicBoolean(false);
    
    // 当前状态跟踪
    private GamePhase currentPhase = GamePhase.NONE;
    private boolean isMonitoring = false;
    private boolean lastMinimizedState = false;
    
    // Session级别抑制状态跟踪
    private String currentSessionId = null;
    private final Set<String> suppressedActions = ConcurrentHashMap.newKeySet();
    private String lastReadyCheckSessionId = null;
    
    // 错误处理和安全机制
    private int consecutiveFailures = 0;
    private static final int MAX_CONSECUTIVE_FAILURES = 5;
    private long lastSuppressionTime = 0;
    private static final long MIN_SUPPRESSION_INTERVAL = 2000; // 2秒最小间隔
    private AtomicBoolean isTemporarilyDisabled = new AtomicBoolean(false);
    private long temporaryDisableEndTime = 0;
    
    // 回调函数
    private Runnable onSuppressionStateChanged;
    
    public PopupSuppressionManager(LCUConnection connection) {
        this.connection = connection;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }
    
    /**
     * 开始弹窗抑制监控
     */
    public void startMonitoring() {
        if (isMonitoring || connection == null) {
            return;
        }
        
        isMonitoring = true;
        logger.info("开始弹窗抑制监控");
        
        // 每500ms检查一次弹窗状态
        scheduler.scheduleWithFixedDelay(this::checkAndSuppressPopups, 0, 500, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 停止弹窗抑制监控
     */
    public void stopMonitoring() {
        isMonitoring = false;
        logger.info("停止弹窗抑制监控");
        
        // 恢复窗口状态
        restoreWindowState();
    }
    
    /**
     * 检查并抑制弹窗
     */
    private void checkAndSuppressPopups() {
        if (!isMonitoring || connection == null) {
            return;
        }
        
        // 检查是否临时禁用
        if (isTemporarilyDisabled.get()) {
            if (System.currentTimeMillis() > temporaryDisableEndTime) {
                logger.info("弹窗抑制功能恢复正常");
                isTemporarilyDisabled.set(false);
                consecutiveFailures = 0;
            } else {
                return; // 仍在禁用期间
            }
        }
        
        // 检查最小间隔
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSuppressionTime < MIN_SUPPRESSION_INTERVAL) {
            return;
        }
        
        try {
            switch (currentPhase) {
                case READY_CHECK:
                    if (suppressReadyCheckPopup.get()) {
                        suppressReadyCheckWindow();
                    }
                    break;
                case CHAMP_SELECT:
                    if (suppressBanPhasePopup.get() || suppressPickPhasePopup.get()) {
                        suppressChampSelectWindow();
                    }
                    break;
                default:
                    // 其他阶段恢复正常状态
                    if (lastMinimizedState) {
                        restoreWindowState();
                    }
                    break;
            }
            
            // 成功执行，重置失败计数
            consecutiveFailures = 0;
            
        } catch (Exception e) {
            handleSuppressionError(e);
        }
    }
    
    /**
     * 抑制准备检查弹窗
     */
    private void suppressReadyCheckWindow() {
        // 获取当前准备检查会话信息
        connection.get("/lol-matchmaking/v1/ready-check")
            .thenAccept(readyCheckResponse -> {
                if (readyCheckResponse != null && !readyCheckResponse.isMissingNode()) {
                    String state = readyCheckResponse.path("state").asText("");
                    
                    // 只在准备检查状态为InProgress时进行抑制
                    if ("InProgress".equals(state)) {
                        // 使用时间戳作为session标识
                        long declaredTime = readyCheckResponse.path("declaredTime").asLong(System.currentTimeMillis());
                        String sessionKey = "readycheck_" + declaredTime;
                        
                        // 检查是否已经抑制过这个准备检查
                        if (!sessionKey.equals(lastReadyCheckSessionId)) {
                            String oldSessionId = lastReadyCheckSessionId;
                            lastReadyCheckSessionId = sessionKey;
                            logger.info("检测到新的准备检查session: {} -> {}", oldSessionId, sessionKey);
                            
                            // 检查窗口状态并抑制
                            connection.get("/riotclient/ux-state")
                                .thenAccept(response -> {
                                    if (response != null && !response.isMissingNode()) {
                                        boolean isVisible = response.path("isVisible").asBoolean(true);
                                        
                                        if (isVisible) {
                                            minimizeClientWindow("准备检查阶段 (Session: " + sessionKey + ")");
                                            logger.info("抑制准备检查弹窗 - Session: {}", sessionKey);
                                        } else {
                                            logger.debug("窗口已隐藏，无需抑制 - Session: {}", sessionKey);
                                        }
                                    }
                                })
                                .exceptionally(throwable -> {
                                    logger.debug("检查UX状态失败", throwable);
                                    return null;
                                });
                        } else {
                            logger.debug("跳过已抑制的准备检查session: {}", sessionKey);
                        }
                    }
                }
            })
            .exceptionally(throwable -> {
                logger.debug("获取准备检查状态失败", throwable);
                return null;
            });
    }
    
    /**
     * 抑制英雄选择弹窗
     */
    private void suppressChampSelectWindow() {
        // 检查当前是Ban阶段还是Pick阶段
        connection.get("/lol-champ-select/v1/session")
            .thenAccept(response -> {
                if (response != null && !response.isMissingNode()) {
                    analyzeChampSelectPhase(response);
                }
            })
            .exceptionally(throwable -> {
                logger.debug("获取英雄选择会话失败", throwable);
                return null;
            });
    }
    
    /**
     * 生成稳定的英雄选择session ID
     */
    private String generateStableSessionId(JsonNode session) {
        // 优先使用gameId作为session标识
        String gameId = session.path("gameId").asText("");
        if (!gameId.isEmpty() && !"0".equals(gameId)) {
            return "champselect_game_" + gameId;
        }
        
        // 备用方案1：使用聊天室名称
        String chatRoomName = session.path("chatDetails").path("chatRoomName").asText("");
        if (!chatRoomName.isEmpty()) {
            return "champselect_chat_" + chatRoomName;
        }
        
        // 备用方案2：使用myTeam信息的哈希值
        JsonNode myTeam = session.path("myTeam");
        if (!myTeam.isMissingNode() && myTeam.isArray() && myTeam.size() > 0) {
            String teamHash = String.valueOf(myTeam.toString().hashCode());
            return "champselect_team_" + teamHash;
        }
        
        // 备用方案3：使用timer信息
        JsonNode timer = session.path("timer");
        if (!timer.isMissingNode()) {
            String totalTimeInPhase = timer.path("totalTimeInPhase").asText("");
            String phase = timer.path("phase").asText("");
            if (!totalTimeInPhase.isEmpty() && !phase.isEmpty()) {
                return "champselect_timer_" + phase + "_" + totalTimeInPhase;
            }
        }
        
        // 最后备用方案：使用session对象的哈希值（不再使用时间戳）
        String sessionHash = String.valueOf(Math.abs(session.toString().hashCode()));
        logger.warn("使用session哈希值作为session ID: {}", sessionHash);
        return "champselect_hash_" + sessionHash;
    }
    
    /**
     * 分析英雄选择阶段并决定是否抑制弹窗
     */
    private void analyzeChampSelectPhase(JsonNode session) {
        JsonNode actions = session.path("actions");
        JsonNode localPlayerCell = session.path("localPlayerCellId");
        
        if (localPlayerCell.isMissingNode()) {
            return;
        }
        
        // 获取当前session ID - 使用更稳定的标识符
        String sessionId = generateStableSessionId(session);
        
        // 如果session发生变化，重置抑制状态
        if (!sessionId.equals(currentSessionId)) {
            String oldSessionId = currentSessionId;
            currentSessionId = sessionId;
            suppressedActions.clear();
            logger.info("检测到新的英雄选择session: {} -> {}, 已清空抑制状态", oldSessionId, sessionId);
        }
        
        int localCellId = localPlayerCell.asInt();
        
        // 遍历当前激活的actions
        if (actions.isArray()) {
            for (JsonNode actionGroup : actions) {
                if (actionGroup.isArray()) {
                    for (JsonNode action : actionGroup) {
                        int actorCellId = action.path("actorCellId").asInt();
                        String type = action.path("type").asText("");
                        boolean isInProgress = action.path("isInProgress").asBoolean(false);
                        int actionId = action.path("id").asInt();
                        
                        // 检查是否是当前玩家的活跃action
                        if (actorCellId == localCellId && isInProgress) {
                            // 构建action的唯一标识符
                            String actionKey = sessionId + "_" + type + "_" + actionId;
                            
                            // 检查是否已经抑制过这个action
                            if (!suppressedActions.contains(actionKey)) {
                                boolean shouldSuppress = false;
                                String phaseType = "";
                                
                                if ("ban".equals(type) && suppressBanPhasePopup.get()) {
                                    shouldSuppress = true;
                                    phaseType = "Ban";
                                } else if ("pick".equals(type) && suppressPickPhasePopup.get()) {
                                    shouldSuppress = true;
                                    phaseType = "Pick";
                                }
                                
                                if (shouldSuppress) {
                                    // 记录已抑制的action
                                    suppressedActions.add(actionKey);
                                    minimizeClientWindow(phaseType + "阶段 (Session: " + sessionId + ", Action: " + actionId + ")");
                                    logger.info("抑制{}阶段弹窗 - Session: {}, Action ID: {}, Action Key: {}", 
                                              phaseType, sessionId, actionId, actionKey);
                                } else {
                                    logger.debug("跳过{}阶段action - 抑制功能未启用, Action ID: {}", type, actionId);
                                }
                            } else {
                                logger.debug("跳过已抑制的action - Action Key: {}", actionKey);
                            }
                        } else {
                            logger.debug("跳过非当前玩家或非进行中的action - ActorCellId: {}, LocalCellId: {}, InProgress: {}, Action ID: {}", 
                                       actorCellId, localCellId, isInProgress, actionId);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 最小化客户端窗口
     */
    private void minimizeClientWindow(String reason) {
        // 更新最后抑制时间
        lastSuppressionTime = System.currentTimeMillis();
        
        connection.post("/riotclient/ux-minimize", null)
            .thenAccept(response -> {
                lastMinimizedState = true;
                logger.debug("成功最小化客户端窗口 - {}", reason);
                
                // 重置失败计数
                consecutiveFailures = 0;
                
                if (onSuppressionStateChanged != null) {
                    onSuppressionStateChanged.run();
                }
            })
            .exceptionally(throwable -> {
                logger.debug("最小化窗口失败", throwable);
                handleSuppressionError(new RuntimeException("最小化窗口失败: " + reason, throwable));
                return null;
            });
    }
    
    /**
     * 恢复窗口状态
     */
    private void restoreWindowState() {
        if (!lastMinimizedState) {
            return;
        }
        
        connection.post("/riotclient/ux-show", null)
            .thenAccept(response -> {
                lastMinimizedState = false;
                logger.debug("恢复客户端窗口显示");
                
                if (onSuppressionStateChanged != null) {
                    onSuppressionStateChanged.run();
                }
            })
            .exceptionally(throwable -> {
                logger.debug("恢复窗口失败", throwable);
                return null;
            });
    }
    
    /**
     * 获取当前抑制状态信息
     */
    public String getSuppressionStatus() {
        StringBuilder status = new StringBuilder();
        
        if (suppressReadyCheckPopup.get()) {
            status.append("准备检查抑制已启用 ");
        }
        if (suppressBanPhasePopup.get()) {
            status.append("Ban阶段抑制已启用 ");
        }
        if (suppressPickPhasePopup.get()) {
            status.append("Pick阶段抑制已启用 ");
        }
        
        if (status.length() == 0) {
            return "弹窗抑制未启用";
        }
        
        if (lastMinimizedState) {
            status.append("(窗口已最小化)");
        }
        
        return status.toString().trim();
    }
    
    // === 配置方法 ===
    
    /**
     * 设置准备检查弹窗抑制
     */
    public void setSuppressReadyCheckPopup(boolean suppress) {
        boolean changed = suppressReadyCheckPopup.getAndSet(suppress) != suppress;
        if (changed) {
            logger.info("准备检查弹窗抑制: {}", suppress ? "启用" : "禁用");
        }
    }
    
    /**
     * 设置Ban阶段弹窗抑制
     */
    public void setSuppressBanPhasePopup(boolean suppress) {
        boolean changed = suppressBanPhasePopup.getAndSet(suppress) != suppress;
        if (changed) {
            logger.info("Ban阶段弹窗抑制: {}", suppress ? "启用" : "禁用");
        }
    }
    
    /**
     * 设置Pick阶段弹窗抑制
     */
    public void setSuppressPickPhasePopup(boolean suppress) {
        boolean changed = suppressPickPhasePopup.getAndSet(suppress) != suppress;
        if (changed) {
            logger.info("Pick阶段弹窗抑制: {}", suppress ? "启用" : "禁用");
        }
    }
    
    /**
     * 更新当前游戏阶段
     */
    public void updateGamePhase(GamePhase newPhase) {
        if (currentPhase != newPhase) {
            GamePhase oldPhase = currentPhase;
            currentPhase = newPhase;
            
            logger.debug("游戏阶段变化: {} -> {}", oldPhase, newPhase);
            
            // 在阶段转换时重置session状态
            if (newPhase != oldPhase) {
                if (newPhase == GamePhase.CHAMP_SELECT) {
                    // 进入英雄选择阶段，清空之前的抑制记录
                    suppressedActions.clear();
                    currentSessionId = null;
                    logger.debug("进入英雄选择阶段，重置抑制状态");
                } else if (newPhase == GamePhase.READY_CHECK) {
                    // 进入准备检查阶段，重置准备检查session
                    lastReadyCheckSessionId = null;
                    logger.debug("进入准备检查阶段，重置准备检查状态");
                } else if (newPhase == GamePhase.NONE || newPhase == GamePhase.LOBBY) {
                    // 游戏结束或回到大厅，清空所有状态
                    suppressedActions.clear();
                    currentSessionId = null;
                    lastReadyCheckSessionId = null;
                    restoreWindowState();
                    logger.debug("游戏结束或回到大厅，清空所有抑制状态");
                }
            }
        }
    }
    
    // === Getter方法 ===
    
    public boolean isSuppressReadyCheckPopup() {
        return suppressReadyCheckPopup.get();
    }
    
    public boolean isSuppressBanPhasePopup() {
        return suppressBanPhasePopup.get();
    }
    
    public boolean isSuppressPickPhasePopup() {
        return suppressPickPhasePopup.get();
    }
    
    public boolean isMonitoring() {
        return isMonitoring;
    }
    
    public boolean isWindowMinimized() {
        return lastMinimizedState;
    }
    
    // === 回调设置 ===
    
    public void setOnSuppressionStateChanged(Runnable callback) {
        this.onSuppressionStateChanged = callback;
    }
    
    // === 生命周期管理 ===
    
    /**
     * 关闭弹窗抑制管理器
     */
    public void shutdown() {
        stopMonitoring();
        
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
        
        logger.info("弹窗抑制管理器已关闭");
    }
    
    /**
     * 手动触发弹窗抑制检查
     */
    public CompletableFuture<Void> triggerSuppressionCheck() {
        return CompletableFuture.runAsync(this::checkAndSuppressPopups, scheduler);
    }
    
    /**
     * 测试弹窗抑制功能
     */
    public CompletableFuture<Boolean> testSuppressionCapability() {
        logger.info("测试弹窗抑制能力...");
        
        return connection.get("/riotclient/ux-state")
            .thenCompose(response -> {
                if (response != null && !response.isMissingNode()) {
                    logger.info("✓ UX状态API可用");
                    return connection.post("/riotclient/ux-minimize", null);
                } else {
                    logger.warn("✗ UX状态API不可用");
                    return CompletableFuture.completedFuture(null);
                }
            })
            .thenCompose(response -> {
                if (response != null) {
                    logger.info("✓ 窗口最小化API可用");
                    // 立即恢复窗口
                    return connection.post("/riotclient/ux-show", null);
                } else {
                    logger.warn("✗ 窗口最小化API不可用");
                    return CompletableFuture.completedFuture(null);
                }
            })
            .thenApply(response -> {
                boolean capable = response != null;
                if (capable) {
                    logger.info("✓ 弹窗抑制功能测试通过");
                } else {
                    logger.warn("✗ 弹窗抑制功能不可用");
                }
                return capable;
            })
            .exceptionally(throwable -> {
                logger.error("弹窗抑制功能测试失败", throwable);
                return false;
            });
    }
    
    /**
     * 处理抑制错误
     */
    private void handleSuppressionError(Exception e) {
        consecutiveFailures++;
        logger.warn("弹窗抑制操作失败 (连续失败次数: {}/{}): {}", 
                   consecutiveFailures, MAX_CONSECUTIVE_FAILURES, e.getMessage());
        
        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            // 临时禁用弹窗抑制功能
            temporarilyDisableSupression();
        }
    }
    
    /**
     * 临时禁用弹窗抑制功能
     */
    private void temporarilyDisableSupression() {
        isTemporarilyDisabled.set(true);
        temporaryDisableEndTime = System.currentTimeMillis() + 30000; // 30秒后恢复
        
        logger.warn("由于连续失败，弹窗抑制功能已临时禁用30秒");
        
        // 恢复窗口状态
        if (lastMinimizedState) {
            connection.post("/riotclient/ux-show", null)
                .thenAccept(response -> {
                    lastMinimizedState = false;
                    logger.info("已恢复客户端窗口显示");
                })
                .exceptionally(throwable -> {
                    logger.debug("恢复窗口失败", throwable);
                    return null;
                });
        }
        
        if (onSuppressionStateChanged != null) {
            onSuppressionStateChanged.run();
        }
    }
    
    /**
     * 检查抑制功能是否可用
     */
    public boolean isSuppressionAvailable() {
        return !isTemporarilyDisabled.get() && connection != null;
    }
    
    /**
     * 手动重置错误状态
     */
    public void resetErrorState() {
        consecutiveFailures = 0;
        isTemporarilyDisabled.set(false);
        temporaryDisableEndTime = 0;
        logger.info("弹窗抑制错误状态已重置");
    }
    
    /**
     * 获取详细状态信息
     */
    public String getDetailedStatus() {
        StringBuilder status = new StringBuilder();
        
        if (isTemporarilyDisabled.get()) {
            long remainingTime = (temporaryDisableEndTime - System.currentTimeMillis()) / 1000;
            status.append("功能已临时禁用 (").append(Math.max(0, remainingTime)).append("秒后恢复) ");
        } else if (consecutiveFailures > 0) {
            status.append("连续失败次数: ").append(consecutiveFailures).append("/").append(MAX_CONSECUTIVE_FAILURES).append(" ");
        }
        
        if (suppressReadyCheckPopup.get()) {
            status.append("准备检查抑制✓ ");
        }
        if (suppressBanPhasePopup.get()) {
            status.append("Ban阶段抑制✓ ");
        }
        if (suppressPickPhasePopup.get()) {
            status.append("Pick阶段抑制✓ ");
        }
        
        if (status.length() == 0) {
            return "弹窗抑制未启用";
        }
        
        if (lastMinimizedState) {
            status.append("(窗口已最小化)");
        }
        
        // 添加session信息
        if (currentSessionId != null) {
            status.append(" Session: ").append(currentSessionId);
            status.append(" 已抑制: ").append(suppressedActions.size()).append("个action");
        }
        
        return status.toString().trim();
    }
    
    /**
     * 获取当前session状态信息（调试用）
     */
    public String getSessionDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("当前游戏阶段: ").append(currentPhase).append("\n");
        info.append("当前session ID: ").append(currentSessionId != null ? currentSessionId : "无").append("\n");
        info.append("准备检查session ID: ").append(lastReadyCheckSessionId != null ? lastReadyCheckSessionId : "无").append("\n");
        info.append("已抑制的actions数量: ").append(suppressedActions.size()).append("\n");
        
        if (!suppressedActions.isEmpty()) {
            info.append("已抑制的actions列表:\n");
            for (String actionKey : suppressedActions) {
                info.append("  - ").append(actionKey).append("\n");
            }
        }
        
        info.append("监控状态: ").append(isMonitoring ? "运行中" : "已停止").append("\n");
        info.append("窗口最小化状态: ").append(lastMinimizedState ? "已最小化" : "正常显示").append("\n");
        info.append("临时禁用状态: ").append(isTemporarilyDisabled.get() ? "是" : "否").append("\n");
        
        return info.toString();
    }
    
    /**
     * 清除session状态（调试用）
     */
    public void clearSessionState() {
        logger.info("手动清除session状态 - 当前session: {}, 已抑制actions: {}", 
                   currentSessionId, suppressedActions.size());
        currentSessionId = null;
        lastReadyCheckSessionId = null;
        suppressedActions.clear();
        resetErrorState();
        logger.info("session状态已清除");
    }
    
    /**
     * 紧急恢复窗口
     */
    public CompletableFuture<Boolean> emergencyRestore() {
        logger.info("执行紧急窗口恢复...");
        
        return connection.post("/riotclient/ux-show", null)
            .thenApply(response -> {
                lastMinimizedState = false;
                logger.info("紧急恢复成功");
                return true;
            })
            .exceptionally(throwable -> {
                logger.error("紧急恢复失败", throwable);
                return false;
            });
    }
}