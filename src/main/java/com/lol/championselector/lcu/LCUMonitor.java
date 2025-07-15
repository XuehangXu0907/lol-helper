package com.lol.championselector.lcu;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class LCUMonitor {
    private static final Logger logger = LoggerFactory.getLogger(LCUMonitor.class);
    
    private LCUConnection connection;
    private ScheduledExecutorService scheduler;
    private boolean isMonitoring = false;
    
    private GamePhase currentPhase = GamePhase.NONE;
    private boolean isInReadyCheck = false;
    private String currentMatchId = null;
    
    // 回调函数
    private Consumer<GamePhase> onPhaseChanged;
    private Consumer<Boolean> onReadyCheckChanged;
    private Consumer<Boolean> onConnectionChanged;
    private Consumer<JsonNode> onChampSelectSessionChanged;
    
    public LCUMonitor() {
        this.scheduler = Executors.newScheduledThreadPool(2);
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
        if (isMonitoring || connection == null) {
            return;
        }
        
        isMonitoring = true;
        logger.info("Started LCU monitoring");
        
        // 监控游戏阶段
        scheduler.scheduleWithFixedDelay(this::checkGamePhase, 0, 1, TimeUnit.SECONDS);
        
        // 监控准备检查
        scheduler.scheduleWithFixedDelay(this::checkReadyCheck, 0, 500, TimeUnit.MILLISECONDS);
        
        // 监控英雄选择
        scheduler.scheduleWithFixedDelay(this::checkChampSelect, 0, 1, TimeUnit.SECONDS);
    }
    
    public void stopMonitoring() {
        isMonitoring = false;
        logger.info("Stopped LCU monitoring");
    }
    
    private void checkGamePhase() {
        if (!isMonitoring || connection == null) {
            return;
        }
        
        connection.get("/lol-gameflow/v1/gameflow-phase")
            .thenAccept(response -> {
                if (response != null && response.isTextual()) {
                    GamePhase newPhase = GamePhase.fromLcuName(response.asText());
                    if (newPhase != currentPhase) {
                        GamePhase oldPhase = currentPhase;
                        currentPhase = newPhase;
                        logger.debug("Game phase changed: {} -> {}", oldPhase, newPhase);
                        
                        if (onPhaseChanged != null) {
                            onPhaseChanged.accept(newPhase);
                        }
                    }
                }
            })
            .exceptionally(throwable -> {
                logger.debug("Failed to get game phase", throwable);
                return null;
            });
    }
    
    private void checkReadyCheck() {
        if (!isMonitoring || connection == null || currentPhase != GamePhase.READY_CHECK) {
            return;
        }
        
        connection.get("/lol-matchmaking/v1/ready-check")
            .thenAccept(response -> {
                if (response != null && !response.isMissingNode()) {
                    String state = response.path("state").asText("");
                    boolean inReadyCheck = "InProgress".equals(state);
                    
                    if (inReadyCheck != isInReadyCheck) {
                        isInReadyCheck = inReadyCheck;
                        logger.debug("Ready check state changed: {}", inReadyCheck);
                        
                        if (onReadyCheckChanged != null) {
                            onReadyCheckChanged.accept(inReadyCheck);
                        }
                    }
                }
            })
            .exceptionally(throwable -> {
                logger.debug("Failed to get ready check status", throwable);
                return null;
            });
    }
    
    private void checkChampSelect() {
        if (!isMonitoring || connection == null || currentPhase != GamePhase.CHAMP_SELECT) {
            return;
        }
        
        connection.get("/lol-champ-select/v1/session")
            .thenAccept(response -> {
                if (response != null && !response.isMissingNode()) {
                    String matchId = response.path("gameId").asText("");
                    
                    // 只有在新的对局或会话信息发生变化时才触发回调
                    if (!matchId.equals(currentMatchId)) {
                        currentMatchId = matchId;
                        logger.debug("Champion select session changed: {}", matchId);
                        
                        if (onChampSelectSessionChanged != null) {
                            onChampSelectSessionChanged.accept(response);
                        }
                    }
                }
            })
            .exceptionally(throwable -> {
                logger.debug("Failed to get champion select session", throwable);
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
            return CompletableFuture.completedFuture(false);
        }
        
        BanPickAction action = new BanPickAction(championId, true);
        return connection.patch("/lol-champ-select/v1/session/actions/" + actionId, action)
            .thenApply(response -> {
                logger.info("Banned champion ID: {}", championId);
                return true;
            })
            .exceptionally(throwable -> {
                logger.error("Failed to ban champion ID: {}", championId, throwable);
                return false;
            });
    }
    
    public CompletableFuture<Boolean> pickChampion(int championId, int actionId) {
        if (connection == null) {
            return CompletableFuture.completedFuture(false);
        }
        
        BanPickAction action = new BanPickAction(championId, true);
        return connection.patch("/lol-champ-select/v1/session/actions/" + actionId, action)
            .thenApply(response -> {
                logger.info("Picked champion ID: {}", championId);
                return true;
            })
            .exceptionally(throwable -> {
                logger.error("Failed to pick champion ID: {}", championId, throwable);
                return false;
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
        return connection != null && connection.isConnected();
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
        stopMonitoring();
        
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
        
        if (connection != null) {
            connection.shutdown();
        }
        
        logger.info("LCU Monitor shut down successfully");
    }
    
    // 内部类用于Ban/Pick操作
    private static class BanPickAction {
        public int championId;
        public boolean completed;
        
        public BanPickAction(int championId, boolean completed) {
            this.championId = championId;
            this.completed = completed;
        }
    }
}