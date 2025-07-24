package com.lol.championselector.lcu;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.Set;
import java.util.HashSet;

public class LCUMonitor {
    private static final Logger logger = LoggerFactory.getLogger(LCUMonitor.class);
    
    // Smart polling intervals - adaptive based on game phase
    private static final long PHASE_CHECK_INTERVAL_NORMAL = 2000; // 2 seconds during normal phases
    private static final long PHASE_CHECK_INTERVAL_ACTIVE = 1000; // 1 second during active phases
    private static final long READY_CHECK_INTERVAL = 300; // 300ms during ready check
    private static final long CHAMP_SELECT_INTERVAL = 500; // 500ms during champion select
    private static final long IDLE_INTERVAL = 5000; // 5 seconds when idle
    
    private LCUConnection connection;
    private ScheduledExecutorService scheduler;
    private boolean isMonitoring = false;
    private volatile boolean isShuttingDown = false;
    
    private GamePhase currentPhase = GamePhase.NONE;
    private boolean isInReadyCheck = false;
    private String currentMatchId = null;
    private String lastChampSelectSession = null;
    
    // Smart polling management
    private long lastPhaseChangeTime = System.currentTimeMillis();
    private int consecutiveFailures = 0;
    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    
    // 回调函数
    private Consumer<GamePhase> onPhaseChanged;
    private Consumer<Boolean> onReadyCheckChanged;
    private Consumer<Boolean> onConnectionChanged;
    private Consumer<JsonNode> onChampSelectSessionChanged;
    
    public LCUMonitor() {
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "LCUMonitor-" + System.currentTimeMillis());
            t.setDaemon(true); // Set as daemon thread to prevent hanging shutdown
            return t;
        });
    }
    
    public CompletableFuture<Boolean> connect() {
        return LCUDetector.detectLCU()
            .thenCompose(lcuInfoOpt -> {
                if (lcuInfoOpt.isPresent()) {
                    LCUDetector.LCUInfo info = lcuInfoOpt.get();
                    this.connection = new LCUConnection(info.getPort(), info.getPassword());
                    return this.connection.testConnection();
                } else {
                    return CompletableFuture.completedFuture(false);
                }
            })
            .thenApply(connected -> {
                if (connected && onConnectionChanged != null) {
                    onConnectionChanged.accept(true);
                }
                return connected;
            });
    }
    
    public void startMonitoring() {
        if (isMonitoring || connection == null || isShuttingDown) {
            return;
        }
        
        isMonitoring = true;
        consecutiveFailures = 0;
        logger.info("Started LCU monitoring with smart polling intervals");
        
        // 智能监控游戏阶段 - 使用自适应间隔
        scheduleSmartPhaseCheck();
        
        // 监控准备检查 - 仅在READY_CHECK阶段运行
        scheduleReadyCheckMonitoring();
        
        // 监控英雄选择 - 仅在CHAMP_SELECT阶段运行
        scheduleChampSelectMonitoring();
    }
    
    public void stopMonitoring() {
        isMonitoring = false;
        logger.info("Stopped LCU monitoring");
    }
    
    private void scheduleSmartPhaseCheck() {
        if (!isMonitoring || isShuttingDown) {
            return;
        }
        
        scheduler.schedule(() -> {
            if (!isMonitoring || isShuttingDown) {
                return;
            }
            
            checkGamePhase();
            
            // Adjust interval based on current phase and recent activity
            long interval = calculatePhaseCheckInterval();
            
            // Schedule next check with calculated interval
            scheduleSmartPhaseCheck();
        }, calculatePhaseCheckInterval(), TimeUnit.MILLISECONDS);
    }
    
    private void scheduleReadyCheckMonitoring() {
        if (!isMonitoring || isShuttingDown) {
            return;
        }
        
        scheduler.schedule(() -> {
            if (!isMonitoring || isShuttingDown) {
                return;
            }
            
            if (currentPhase == GamePhase.READY_CHECK) {
                checkReadyCheck();
                // Continue monitoring during ready check
                scheduleReadyCheckMonitoring();
            } else {
                // If not in ready check, schedule a longer interval to check again
                scheduler.schedule(this::scheduleReadyCheckMonitoring, IDLE_INTERVAL, TimeUnit.MILLISECONDS);
            }
        }, currentPhase == GamePhase.READY_CHECK ? READY_CHECK_INTERVAL : IDLE_INTERVAL, TimeUnit.MILLISECONDS);
    }
    
    private void scheduleChampSelectMonitoring() {
        if (!isMonitoring || isShuttingDown) {
            return;
        }
        
        scheduler.schedule(() -> {
            if (!isMonitoring || isShuttingDown) {
                return;
            }
            
            if (currentPhase == GamePhase.CHAMP_SELECT) {
                checkChampSelect();
                // Continue monitoring during champion select
                scheduleChampSelectMonitoring();
            } else {
                // If not in champion select, schedule a longer interval to check again
                scheduler.schedule(this::scheduleChampSelectMonitoring, IDLE_INTERVAL, TimeUnit.MILLISECONDS);
            }
        }, currentPhase == GamePhase.CHAMP_SELECT ? CHAMP_SELECT_INTERVAL : IDLE_INTERVAL, TimeUnit.MILLISECONDS);
    }
    
    private long calculatePhaseCheckInterval() {
        long timeSinceLastChange = System.currentTimeMillis() - lastPhaseChangeTime;
        
        // If we have consecutive failures, increase interval to reduce load
        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            return IDLE_INTERVAL;
        }
        
        // Use faster polling during active phases
        if (currentPhase == GamePhase.READY_CHECK || currentPhase == GamePhase.CHAMP_SELECT) {
            return PHASE_CHECK_INTERVAL_ACTIVE;
        }
        
        // If recent phase change, use faster polling for a while
        if (timeSinceLastChange < 30000) { // 30 seconds
            return PHASE_CHECK_INTERVAL_ACTIVE;
        }
        
        // Default to normal interval
        return PHASE_CHECK_INTERVAL_NORMAL;
    }
    
    private void checkGamePhase() {
        if (!isMonitoring || connection == null || isShuttingDown) {
            return;
        }
        
        connection.get("/lol-gameflow/v1/gameflow-phase")
            .thenAccept(response -> {
                if (isShuttingDown) {
                    return;
                }
                
                if (response != null && response.isTextual()) {
                    GamePhase newPhase = GamePhase.fromLcuName(response.asText());
                    if (newPhase != currentPhase) {
                        GamePhase oldPhase = currentPhase;
                        currentPhase = newPhase;
                        lastPhaseChangeTime = System.currentTimeMillis();
                        consecutiveFailures = 0; // Reset failure count on success
                        
                        logger.info("Game phase changed: {} -> {} (smart polling adjusted)", oldPhase, newPhase);
                        
                        if (onPhaseChanged != null) {
                            try {
                                onPhaseChanged.accept(newPhase);
                            } catch (Exception e) {
                                logger.error("Error in phase change callback", e);
                            }
                        }
                    } else {
                        consecutiveFailures = 0; // Reset on successful response
                    }
                } else {
                    consecutiveFailures++;
                    if (consecutiveFailures <= MAX_CONSECUTIVE_FAILURES) {
                        logger.debug("Empty response for game phase check (attempt {})", consecutiveFailures);
                    }
                }
            })
            .exceptionally(throwable -> {
                if (!isShuttingDown) {
                    consecutiveFailures++;
                    if (consecutiveFailures <= MAX_CONSECUTIVE_FAILURES) {
                        logger.debug("Failed to get game phase (attempt {}): {}", consecutiveFailures, throwable.getMessage());
                    } else {
                        logger.warn("Persistent failures getting game phase, reducing polling frequency");
                    }
                }
                return null;
            });
    }
    
    private void checkReadyCheck() {
        if (!isMonitoring || connection == null || currentPhase != GamePhase.READY_CHECK || isShuttingDown) {
            return;
        }
        
        connection.get("/lol-matchmaking/v1/ready-check")
            .thenAccept(response -> {
                if (isShuttingDown) {
                    return;
                }
                
                if (response != null && !response.isMissingNode()) {
                    String state = response.path("state").asText("");
                    boolean inReadyCheck = "InProgress".equals(state);
                    
                    if (inReadyCheck != isInReadyCheck) {
                        isInReadyCheck = inReadyCheck;
                        logger.debug("Ready check state changed: {}", inReadyCheck);
                        
                        if (onReadyCheckChanged != null) {
                            try {
                                onReadyCheckChanged.accept(inReadyCheck);
                            } catch (Exception e) {
                                logger.error("Error in ready check callback", e);
                            }
                        }
                    }
                }
            })
            .exceptionally(throwable -> {
                if (!isShuttingDown) {
                    logger.debug("Failed to get ready check status: {}", throwable.getMessage());
                }
                return null;
            });
    }
    
    private void checkChampSelect() {
        if (!isMonitoring || connection == null || isShuttingDown) {
            return;
        }
        
        if (currentPhase != GamePhase.CHAMP_SELECT) {
            // 如果离开了英雄选择阶段，重置相关状态
            if (currentMatchId != null || lastChampSelectSession != null) {
                currentMatchId = null;
                lastChampSelectSession = null;
                logger.debug("Reset champion select state as we left the phase");
            }
            return;
        }
        
        connection.get("/lol-champ-select/v1/session")
            .thenAccept(response -> {
                if (isShuttingDown) {
                    return;
                }
                
                if (response != null && !response.isMissingNode()) {
                    // 检查session是否真正发生了变化，减少不必要的处理
                    String currentSessionHash = response.toString();
                    if (!currentSessionHash.equals(lastChampSelectSession)) {
                        logger.debug("Champion select session changed, triggering callback");
                        lastChampSelectSession = currentSessionHash;
                        
                        if (onChampSelectSessionChanged != null) {
                            try {
                                onChampSelectSessionChanged.accept(response);
                            } catch (Exception e) {
                                logger.error("Error in champion select callback", e);
                            }
                        }
                    }
                }
            })
            .exceptionally(throwable -> {
                if (!isShuttingDown) {
                    logger.debug("Failed to get champion select session: {}", throwable.getMessage());
                }
                return null;
            });
    }
    
    public CompletableFuture<Boolean> acceptReadyCheck() {
        if (connection == null) {
            return CompletableFuture.completedFuture(false);
        }
        
        return connection.post("/lol-matchmaking/v1/ready-check/accept", null)
            .thenApply(response -> {
                logger.info("Accepted ready check");
                return true;
            })
            .exceptionally(throwable -> {
                logger.error("Failed to accept ready check", throwable);
                return false;
            });
    }
    
    public CompletableFuture<Boolean> banChampion(int championId, int actionId) {
        if (connection == null) {
            logger.error("Cannot ban champion - connection is null");
            return CompletableFuture.completedFuture(false);
        }
        
        logger.info("Attempting to ban champion ID: {} for action: {}", championId, actionId);
        BanPickAction action = new BanPickAction(championId, true);
        
        return connection.patch("/lol-champ-select/v1/session/actions/" + actionId, action)
            .thenApply(response -> {
                // Check for error response first
                if (response != null && response.has("error")) {
                    logger.error("Ban request failed with HTTP error for champion ID: {}, action: {}, status: {}", 
                               championId, actionId, response.get("status").asInt());
                    return false;
                }
                
                // Check if response contains meaningful data (not empty ObjectNode)
                if (response != null && response.size() > 0) {
                    // For successful operations, LCU might return the updated action or just success indicator
                    if (response.has("success")) {
                        logger.info("Successfully banned champion ID: {} for action: {} (success indicator)", championId, actionId);
                        return true;
                    }
                    
                    // Verify the action was actually updated
                    JsonNode championIdNode = response.get("championId");
                    JsonNode completedNode = response.get("completed");
                    
                    if (championIdNode != null && completedNode != null) {
                        int responseChampionId = championIdNode.asInt();
                        boolean responseCompleted = completedNode.asBoolean();
                        
                        if (responseChampionId == championId && responseCompleted) {
                            logger.info("Successfully banned champion ID: {} for action: {} (verified response)", championId, actionId);
                            return true;
                        } else {
                            logger.warn("Ban response mismatch - Expected: championId={}, completed=true, Got: championId={}, completed={}", 
                                       championId, responseChampionId, responseCompleted);
                            return false;
                        }
                    } else {
                        // Some successful operations might not return the full action data
                        logger.info("Ban request completed for champion ID: {} for action: {} (no validation data)", championId, actionId);
                        return true;
                    }
                } else {
                    logger.error("Ban request failed - empty or null response for champion ID: {}, action: {}", championId, actionId);
                    return false;
                }
            })
            .exceptionally(throwable -> {
                logger.error("Exception during ban champion ID: {} for action: {}", championId, actionId, throwable);
                return false;
            });
    }
    
    public CompletableFuture<Boolean> pickChampion(int championId, int actionId) {
        if (connection == null) {
            logger.error("Cannot pick champion - connection is null");
            return CompletableFuture.completedFuture(false);
        }
        
        logger.info("Attempting to pick champion ID: {} for action: {}", championId, actionId);
        BanPickAction action = new BanPickAction(championId, true);
        
        return connection.patch("/lol-champ-select/v1/session/actions/" + actionId, action)
            .thenApply(response -> {
                // Check for error response first
                if (response != null && response.has("error")) {
                    logger.error("Pick request failed with HTTP error for champion ID: {}, action: {}, status: {}", 
                               championId, actionId, response.get("status").asInt());
                    return false;
                }
                
                // Check if response contains meaningful data (not empty ObjectNode)
                if (response != null && response.size() > 0) {
                    // For successful operations, LCU might return the updated action or just success indicator
                    if (response.has("success")) {
                        logger.info("Successfully picked champion ID: {} for action: {} (success indicator)", championId, actionId);
                        return true;
                    }
                    
                    // Verify the action was actually updated
                    JsonNode championIdNode = response.get("championId");
                    JsonNode completedNode = response.get("completed");
                    
                    if (championIdNode != null && completedNode != null) {
                        int responseChampionId = championIdNode.asInt();
                        boolean responseCompleted = completedNode.asBoolean();
                        
                        if (responseChampionId == championId && responseCompleted) {
                            logger.info("Successfully picked champion ID: {} for action: {} (verified response)", championId, actionId);
                            return true;
                        } else {
                            logger.warn("Pick response mismatch - Expected: championId={}, completed=true, Got: championId={}, completed={}", 
                                       championId, responseChampionId, responseCompleted);
                            return false;
                        }
                    } else {
                        // Some successful operations might not return the full action data
                        logger.info("Pick request completed for champion ID: {} for action: {} (no validation data)", championId, actionId);
                        return true;
                    }
                } else {
                    logger.error("Pick request failed - empty or null response for champion ID: {}, action: {}", championId, actionId);
                    return false;
                }
            })
            .exceptionally(throwable -> {
                logger.error("Exception during pick champion ID: {} for action: {}", championId, actionId, throwable);
                return false;
            });
    }
    
    /**
     * Hover（预选）英雄，不立即确认
     */
    public CompletableFuture<Boolean> hoverChampion(int championId, int actionId) {
        if (connection == null) {
            return CompletableFuture.completedFuture(false);
        }
        
        BanPickAction action = new BanPickAction(championId, false); // completed = false 表示hover
        return connection.patch("/lol-champ-select/v1/session/actions/" + actionId, action)
            .thenApply(response -> {
                logger.info("Hovered champion ID: {}", championId);
                return true;
            })
            .exceptionally(throwable -> {
                logger.error("Failed to hover champion ID: {}", championId, throwable);
                return false;
            });
    }
    
    /**
     * 获取当前英雄选择会话的详细信息，包括计时器
     */
    public CompletableFuture<JsonNode> getChampSelectSessionDetails() {
        if (connection == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        return connection.get("/lol-champ-select/v1/session")
            .exceptionally(throwable -> {
                logger.debug("Failed to get champ select session details", throwable);
                return null;
            });
    }
    
    /**
     * 获取玩家的分路位置
     */
    public CompletableFuture<String> getPlayerPosition() {
        return getChampSelectSessionDetails()
            .thenApply(session -> {
                if (session == null || session.isMissingNode()) {
                    return null;
                }
                
                JsonNode localPlayerCell = session.path("localPlayerCellId");
                if (localPlayerCell.isMissingNode()) {
                    return null;
                }
                
                int localCellId = localPlayerCell.asInt();
                JsonNode myTeam = session.path("myTeam");
                
                if (myTeam.isArray()) {
                    for (JsonNode teamMember : myTeam) {
                        int cellId = teamMember.path("cellId").asInt();
                        if (cellId == localCellId) {
                            String position = teamMember.path("assignedPosition").asText("");
                            logger.debug("Player position detected: {}", position);
                            return position.isEmpty() ? null : position;
                        }
                    }
                }
                
                return null;
            });
    }
    
    /**
     * 获取阶段剩余时间（秒）
     */
    public CompletableFuture<Integer> getRemainingTimeInPhase() {
        return getChampSelectSessionDetails()
            .thenApply(session -> {
                if (session == null || session.isMissingNode()) {
                    return 0;
                }
                
                JsonNode timer = session.path("timer");
                if (timer.isMissingNode()) {
                    return 0;
                }
                
                long totalTimeInPhase = timer.path("totalTimeInPhase").asLong(0);
                long adjustedTimeLeftInPhase = timer.path("adjustedTimeLeftInPhase").asLong(0);
                
                // 如果adjustedTimeLeftInPhase可用，使用它；否则计算剩余时间
                if (adjustedTimeLeftInPhase > 0) {
                    return (int) (adjustedTimeLeftInPhase / 1000); // 转换为秒
                }
                
                // 备用计算方式
                long phaseStartTime = timer.path("internalNowInEpochMs").asLong(0) - 
                                     (totalTimeInPhase - adjustedTimeLeftInPhase);
                long currentTime = System.currentTimeMillis();
                long elapsedTime = currentTime - phaseStartTime;
                long remainingTime = Math.max(0, totalTimeInPhase - elapsedTime);
                
                return (int) (remainingTime / 1000);
            });
    }
    
    /**
     * 获取已被ban的英雄ID集合
     */
    public CompletableFuture<Set<Integer>> getBannedChampions() {
        return getChampSelectSessionDetails()
            .thenApply(session -> {
                Set<Integer> bannedChampions = new HashSet<>();
                
                if (session == null || session.isMissingNode()) {
                    logger.debug("getBannedChampions: session is null or missing");
                    return bannedChampions;
                }
                
                JsonNode bans = session.path("bans");
                logger.debug("getBannedChampions: bans node = {}", bans);
                
                if (bans.isArray()) {
                    // 旧格式：直接数组形式
                    logger.debug("getBannedChampions: processing array format with {} ban entries", bans.size());
                    for (JsonNode ban : bans) {
                        int championId = ban.path("championId").asInt(0);
                        logger.debug("getBannedChampions: ban entry = {}, championId = {}", ban, championId);
                        if (championId != 0) {
                            bannedChampions.add(championId);
                        }
                    }
                } else if (bans.isObject()) {
                    // 新格式：对象形式 {"myTeamBans":[104,432,234],"theirTeamBans":[]}
                    logger.debug("getBannedChampions: processing object format");
                    
                    // 处理我方队伍的ban - 直接是championId数组
                    JsonNode myTeamBans = bans.path("myTeamBans");
                    if (myTeamBans.isArray()) {
                        logger.debug("getBannedChampions: processing {} myTeamBans", myTeamBans.size());
                        for (JsonNode ban : myTeamBans) {
                            if (ban.isInt()) {
                                // 直接是championId
                                int championId = ban.asInt();
                                logger.debug("getBannedChampions: myTeam ban championId = {}", championId);
                                if (championId != 0) {
                                    bannedChampions.add(championId);
                                }
                            } else if (ban.isObject()) {
                                // 对象格式
                                int championId = ban.path("championId").asInt(0);
                                logger.debug("getBannedChampions: myTeam ban object = {}, championId = {}", ban, championId);
                                if (championId != 0) {
                                    bannedChampions.add(championId);
                                }
                            }
                        }
                    }
                    
                    // 处理敌方队伍的ban - 直接是championId数组
                    JsonNode theirTeamBans = bans.path("theirTeamBans");
                    if (theirTeamBans.isArray()) {
                        logger.debug("getBannedChampions: processing {} theirTeamBans", theirTeamBans.size());
                        for (JsonNode ban : theirTeamBans) {
                            if (ban.isInt()) {
                                // 直接是championId
                                int championId = ban.asInt();
                                logger.debug("getBannedChampions: theirTeam ban championId = {}", championId);
                                if (championId != 0) {
                                    bannedChampions.add(championId);
                                }
                            } else if (ban.isObject()) {
                                // 对象格式
                                int championId = ban.path("championId").asInt(0);
                                logger.debug("getBannedChampions: theirTeam ban object = {}, championId = {}", ban, championId);
                                if (championId != 0) {
                                    bannedChampions.add(championId);
                                }
                            }
                        }
                    }
                } else {
                    logger.debug("getBannedChampions: bans is neither array nor object: {}", bans);
                }
                
                logger.info("getBannedChampions: found {} banned champions: {}", bannedChampions.size(), bannedChampions);
                return bannedChampions;
            });
    }
    
    /**
     * 获取已被pick的英雄ID集合
     */
    public CompletableFuture<Set<Integer>> getPickedChampions() {
        return getChampSelectSessionDetails()
            .thenApply(session -> {
                Set<Integer> pickedChampions = new HashSet<>();
                
                if (session == null || session.isMissingNode()) {
                    return pickedChampions;
                }
                
                // 检查双方队伍已选择的英雄
                JsonNode myTeam = session.path("myTeam");
                JsonNode theirTeam = session.path("theirTeam");
                
                // 处理我方队伍
                if (myTeam.isArray()) {
                    for (JsonNode member : myTeam) {
                        int championId = member.path("championId").asInt(0);
                        if (championId != 0) {
                            pickedChampions.add(championId);
                        }
                    }
                }
                
                // 处理敌方队伍
                if (theirTeam.isArray()) {
                    for (JsonNode member : theirTeam) {
                        int championId = member.path("championId").asInt(0);
                        if (championId != 0) {
                            pickedChampions.add(championId);
                        }
                    }
                }
                
                return pickedChampions;
            });
    }
    
    // Getter和Setter方法
    public GamePhase getCurrentPhase() {
        return currentPhase;
    }
    
    public boolean isInReadyCheck() {
        return isInReadyCheck;
    }
    
    public boolean isConnected() {
        try {
            return connection != null && connection.isConnected();
        } catch (Exception e) {
            logger.debug("Error checking connection status", e);
            return false;
        }
    }
    
    public boolean isMonitoring() {
        return isMonitoring;
    }
    
    // 设置回调函数
    public void setOnPhaseChanged(Consumer<GamePhase> onPhaseChanged) {
        this.onPhaseChanged = onPhaseChanged;
    }
    
    public void setOnReadyCheckChanged(Consumer<Boolean> onReadyCheckChanged) {
        this.onReadyCheckChanged = onReadyCheckChanged;
    }
    
    public void setOnConnectionChanged(Consumer<Boolean> onConnectionChanged) {
        this.onConnectionChanged = onConnectionChanged;
    }
    
    public void setOnChampSelectSessionChanged(Consumer<JsonNode> onChampSelectSessionChanged) {
        this.onChampSelectSessionChanged = onChampSelectSessionChanged;
    }
    
    public void shutdown() {
        logger.info("Shutting down LCU Monitor...");
        isShuttingDown = true;
        stopMonitoring();
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                // Wait a bit longer for smart polling threads to finish
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    logger.warn("LCU Monitor scheduler did not terminate gracefully, forcing shutdown");
                    scheduler.shutdownNow();
                    
                    // Wait a bit more for forced shutdown
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        logger.error("LCU Monitor scheduler could not be stopped");
                    }
                }
            } catch (InterruptedException e) {
                logger.warn("Interrupted while shutting down LCU Monitor scheduler");
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (connection != null) {
            try {
                connection.shutdown();
            } catch (Exception e) {
                logger.warn("Error shutting down LCU connection", e);
            }
        }
        
        // Reset state
        currentPhase = GamePhase.NONE;
        isInReadyCheck = false;
        currentMatchId = null;
        lastChampSelectSession = null;
        consecutiveFailures = 0;
        
        logger.info("LCU Monitor shut down successfully");
    }
    
    // 内部类用于Ban/Pick操作
    @SuppressWarnings("unused") // Fields are used for JSON serialization
    private static class BanPickAction {
        public int championId;
        public boolean completed;
        
        public BanPickAction(int championId, boolean completed) {
            this.championId = championId;
            this.completed = completed;
        }
    }
    
    /**
     * Get teammate hover (prepick) champions
     * Returns a set of champion IDs that teammates have hovered but not locked in
     */
    public CompletableFuture<Set<Integer>> getTeammateHoveredChampions() {
        return getChampSelectSessionDetails()
            .thenApply(session -> {
                Set<Integer> hoveredChampions = new HashSet<>();
                
                if (session == null || session.isMissingNode()) {
                    logger.debug("getTeammateHoveredChampions: session is null or missing");
                    return hoveredChampions;
                }
                
                // Get local player cell ID to exclude self
                JsonNode localPlayerCell = session.path("localPlayerCellId");
                int localCellId = localPlayerCell.isMissingNode() ? -1 : localPlayerCell.asInt();
                
                // Check my team for hovered champions
                JsonNode myTeam = session.path("myTeam");
                if (myTeam.isArray()) {
                    for (JsonNode member : myTeam) {
                        JsonNode cellIdNode = member.path("cellId");
                        if (!cellIdNode.isMissingNode() && cellIdNode.asInt() != localCellId) {
                            // This is a teammate, check their champion pick intent
                            int championPickIntent = member.path("championPickIntent").asInt(0);
                            int championId = member.path("championId").asInt(0);
                            
                            // If they have a pick intent but haven't locked in yet
                            if (championPickIntent != 0 && championId == 0) {
                                hoveredChampions.add(championPickIntent);
                                logger.debug("Teammate hovered champion: {}", championPickIntent);
                            }
                        }
                    }
                }
                
                logger.debug("Found {} teammate hovered champions: {}", hoveredChampions.size(), hoveredChampions);
                return hoveredChampions;
            })
            .exceptionally(throwable -> {
                logger.error("Failed to get teammate hovered champions", throwable);
                return new HashSet<>();
            });
    }
    
    /**
     * Get current player's action ID for the current phase
     * Used for hover/pick operations
     */
    public CompletableFuture<Integer> getCurrentPlayerActionId() {
        return getChampSelectSessionDetails()
            .thenApply(session -> {
                if (session == null || session.isMissingNode()) {
                    return -1;
                }
                
                // Get local player cell ID
                JsonNode localPlayerCell = session.path("localPlayerCellId");
                if (localPlayerCell.isMissingNode()) {
                    return -1;
                }
                int localCellId = localPlayerCell.asInt();
                
                // Find current action for the local player
                JsonNode actions = session.path("actions");
                if (actions.isArray()) {
                    for (JsonNode actionGroup : actions) {
                        if (actionGroup.isArray()) {
                            for (JsonNode action : actionGroup) {
                                int actorCellId = action.path("actorCellId").asInt(-1);
                                boolean isInProgress = action.path("isInProgress").asBoolean(false);
                                
                                if (actorCellId == localCellId && isInProgress) {
                                    int actionId = action.path("id").asInt(-1);
                                    logger.debug("Found current player action ID: {}", actionId);
                                    return actionId;
                                }
                            }
                        }
                    }
                }
                
                logger.debug("No current action found for local player");
                return -1;
            })
            .exceptionally(throwable -> {
                logger.error("Failed to get current player action ID", throwable);
                return -1;
            });
    }
}