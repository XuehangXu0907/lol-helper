package com.lol.championselector.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.lol.championselector.config.AutoAcceptConfig;
import com.lol.championselector.lcu.GamePhase;
import com.lol.championselector.lcu.LCUMonitor;
import com.lol.championselector.model.Champion;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class AutoAcceptController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(AutoAcceptController.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    @FXML private TabPane mainTabPane;
    @FXML private Label connectionStatusLabel;
    @FXML private Label gamePhaseLabel;
    @FXML private Label clientStatusLabel;
    
    @FXML private Button connectButton;
    @FXML private Button disconnectButton;
    
    @FXML private CheckBox autoAcceptCheckBox;
    @FXML private Spinner<Integer> checkIntervalSpinner;
    
    @FXML private CheckBox autoBanCheckBox;
    @FXML private Button selectBanChampionButton;
    @FXML private Label banChampionLabel;
    
    @FXML private CheckBox autoPickCheckBox;
    @FXML private Button selectPickChampionButton;
    @FXML private Label pickChampionLabel;
    
    @FXML private TextArea statusTextArea;
    
    private LCUMonitor lcuMonitor;
    private AutoAcceptConfig config;
    private boolean isAutoAcceptActive = false;
    private boolean isAutoBanActive = false;
    private boolean isAutoPickActive = false;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeComponents();
        loadConfiguration();
        setupLCUMonitor();
        updateUI();
    }
    
    private void initializeComponents() {
        // 设置检查间隔spinner
        checkIntervalSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(500, 5000, 1000, 100));
        
        // 初始状态
        updateConnectionStatus(false);
        updateGamePhase(GamePhase.NONE);
        
        appendStatus("应用程序已启动，等待连接到游戏客户端...");
    }
    
    private void loadConfiguration() {
        config = AutoAcceptConfig.load();
        
        // 应用配置到UI
        autoAcceptCheckBox.setSelected(config.isAutoAcceptEnabled());
        checkIntervalSpinner.getValueFactory().setValue(config.getCheckIntervalMs());
        
        autoBanCheckBox.setSelected(config.getChampionSelect().isAutoBanEnabled());
        autoPickCheckBox.setSelected(config.getChampionSelect().isAutoPickEnabled());
        
        updateChampionLabels();
    }
    
    private void updateChampionLabels() {
        if (config.getChampionSelect().getBanChampion() != null) {
            banChampionLabel.setText(config.getChampionSelect().getBanChampion().toString());
        }
        
        if (config.getChampionSelect().getPickChampion() != null) {
            pickChampionLabel.setText(config.getChampionSelect().getPickChampion().toString());
        }
    }
    
    private void setupLCUMonitor() {
        lcuMonitor = new LCUMonitor();
        
        // 设置回调
        lcuMonitor.setOnConnectionChanged(this::updateConnectionStatus);
        lcuMonitor.setOnPhaseChanged(this::updateGamePhase);
        lcuMonitor.setOnReadyCheckChanged(this::handleReadyCheckChanged);
        lcuMonitor.setOnChampSelectSessionChanged(this::handleChampSelectSessionChanged);
    }
    
    @FXML
    private void onConnectClicked() {
        connectButton.setDisable(true);
        clientStatusLabel.setText("正在连接...");
        
        lcuMonitor.connect()
            .thenAccept(connected -> Platform.runLater(() -> {
                if (connected) {
                    appendStatus("成功连接到游戏客户端");
                    lcuMonitor.startMonitoring();
                    disconnectButton.setDisable(false);
                } else {
                    appendStatus("连接失败：未找到游戏客户端或连接被拒绝");
                    connectButton.setDisable(false);
                    clientStatusLabel.setText("连接失败");
                }
            }))
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    appendStatus("连接错误：" + throwable.getMessage());
                    connectButton.setDisable(false);
                    clientStatusLabel.setText("连接错误");
                });
                return null;
            });
    }
    
    @FXML
    private void onDisconnectClicked() {
        lcuMonitor.stopMonitoring();
        updateConnectionStatus(false);
        updateGamePhase(GamePhase.NONE);
        
        connectButton.setDisable(false);
        disconnectButton.setDisable(true);
        clientStatusLabel.setText("已断开连接");
        
        appendStatus("已断开与游戏客户端的连接");
    }
    
    @FXML
    private void onAutoAcceptToggled() {
        config.setAutoAcceptEnabled(autoAcceptCheckBox.isSelected());
        saveConfiguration();
        
        String status = autoAcceptCheckBox.isSelected() ? "启用" : "禁用";
        appendStatus("自动接受对局已" + status);
    }
    
    @FXML
    private void onAutoBanToggled() {
        config.getChampionSelect().setAutoBanEnabled(autoBanCheckBox.isSelected());
        saveConfiguration();
        
        String status = autoBanCheckBox.isSelected() ? "启用" : "禁用";
        appendStatus("自动Ban英雄已" + status);
    }
    
    @FXML
    private void onAutoPickToggled() {
        config.getChampionSelect().setAutoPickEnabled(autoPickCheckBox.isSelected());
        saveConfiguration();
        
        String status = autoPickCheckBox.isSelected() ? "启用" : "禁用";
        appendStatus("自动Pick英雄已" + status);
    }
    
    @FXML
    private void onSelectBanChampionClicked() {
        selectChampion("Ban", (champion) -> {
            AutoAcceptConfig.ChampionInfo championInfo = new AutoAcceptConfig.ChampionInfo(champion);
            config.getChampionSelect().setBanChampion(championInfo);
            banChampionLabel.setText(championInfo.toString());
            saveConfiguration();
            appendStatus("已设置Ban英雄：" + championInfo.toString());
        });
    }
    
    @FXML
    private void onSelectPickChampionClicked() {
        selectChampion("Pick", (champion) -> {
            AutoAcceptConfig.ChampionInfo championInfo = new AutoAcceptConfig.ChampionInfo(champion);
            config.getChampionSelect().setPickChampion(championInfo);
            pickChampionLabel.setText(championInfo.toString());
            saveConfiguration();
            appendStatus("已设置Pick英雄：" + championInfo.toString());
        });
    }
    
    private void selectChampion(String mode, ChampionSelectionCallback callback) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ChampionSelectorView.fxml"));
            Stage stage = new Stage();
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
            stage.setTitle("选择" + mode + "英雄");
            stage.initModality(Modality.APPLICATION_MODAL);
            
            // 设置弹窗大小为更大的尺寸
            stage.setWidth(1000);
            stage.setHeight(600);
            stage.setMinWidth(800);
            stage.setMinHeight(500);
            
            ChampionSelectorController controller = loader.getController();
            controller.setSelectionMode(true);
            controller.setOnChampionSelected(callback::onChampionSelected);
            
            stage.showAndWait();
        } catch (IOException e) {
            logger.error("Failed to open champion selector", e);
            appendStatus("打开英雄选择器失败：" + e.getMessage());
        }
    }
    
    private void updateConnectionStatus(boolean connected) {
        Platform.runLater(() -> {
            connectionStatusLabel.setText(connected ? "已连接" : "未连接");
            connectionStatusLabel.setStyle(connected ? 
                "-fx-text-fill: #4CAF50; -fx-font-weight: bold;" : 
                "-fx-text-fill: #F44336; -fx-font-weight: bold;");
            
            clientStatusLabel.setText(connected ? "游戏客户端已连接" : "等待连接...");
        });
    }
    
    private void updateGamePhase(GamePhase phase) {
        Platform.runLater(() -> {
            gamePhaseLabel.setText(phase.getDisplayName());
            appendStatus("游戏状态更新：" + phase.getDisplayName());
        });
    }
    
    private void handleReadyCheckChanged(boolean inReadyCheck) {
        Platform.runLater(() -> {
            if (inReadyCheck && config.isAutoAcceptEnabled()) {
                appendStatus("检测到准备检查，正在自动接受...");
                
                lcuMonitor.acceptReadyCheck()
                    .thenAccept(success -> Platform.runLater(() -> {
                        if (success) {
                            appendStatus("✓ 已自动接受对局");
                        } else {
                            appendStatus("✗ 自动接受失败");
                        }
                    }));
            } else if (inReadyCheck) {
                appendStatus("检测到准备检查（自动接受已禁用）");
            }
        });
    }
    
    private void handleChampSelectSessionChanged(JsonNode session) {
        Platform.runLater(() -> {
            if (session == null || session.isMissingNode()) {
                return;
            }
            
            // 获取当前召唤师ID
            JsonNode localPlayerCell = session.path("localPlayerCellId");
            if (localPlayerCell.isMissingNode()) {
                return;
            }
            int localCellId = localPlayerCell.asInt();
            
            // 遍历所有动作找到属于当前玩家的
            JsonNode actions = session.path("actions");
            if (actions.isArray()) {
                for (JsonNode actionGroup : actions) {
                    if (actionGroup.isArray()) {
                        for (JsonNode action : actionGroup) {
                            int actorCellId = action.path("actorCellId").asInt();
                            String type = action.path("type").asText("");
                            boolean isInProgress = action.path("isInProgress").asBoolean(false);
                            boolean completed = action.path("completed").asBoolean(false);
                            int actionId = action.path("id").asInt();
                            
                            // 只处理属于当前玩家且正在进行中的动作
                            if (actorCellId == localCellId && isInProgress && !completed) {
                                if ("ban".equals(type) && config.getChampionSelect().isAutoBanEnabled()) {
                                    handleAutoBan(actionId);
                                } else if ("pick".equals(type) && config.getChampionSelect().isAutoPickEnabled()) {
                                    handleAutoPick(actionId);
                                }
                            }
                        }
                    }
                }
            }
        });
    }
    
    private void handleAutoBan(int actionId) {
        AutoAcceptConfig.ChampionInfo banChampion = config.getChampionSelect().getBanChampion();
        if (banChampion == null || banChampion.getChampionId() == null) {
            appendStatus("自动Ban失败：未设置Ban英雄或英雄ID无效");
            return;
        }
        
        appendStatus("正在自动Ban英雄：" + banChampion.toString());
        lcuMonitor.banChampion(banChampion.getChampionId(), actionId)
            .thenAccept(success -> Platform.runLater(() -> {
                if (success) {
                    appendStatus("✓ 成功Ban英雄：" + banChampion.toString());
                } else {
                    appendStatus("✗ Ban英雄失败：" + banChampion.toString());
                }
            }));
    }
    
    private void handleAutoPick(int actionId) {
        AutoAcceptConfig.ChampionInfo pickChampion = config.getChampionSelect().getPickChampion();
        if (pickChampion == null || pickChampion.getChampionId() == null) {
            appendStatus("自动Pick失败：未设置Pick英雄或英雄ID无效");
            return;
        }
        
        appendStatus("正在自动Pick英雄：" + pickChampion.toString());
        lcuMonitor.pickChampion(pickChampion.getChampionId(), actionId)
            .thenAccept(success -> Platform.runLater(() -> {
                if (success) {
                    appendStatus("✓ 成功Pick英雄：" + pickChampion.toString());
                } else {
                    appendStatus("✗ Pick英雄失败：" + pickChampion.toString());
                }
            }));
    }
    
    private void appendStatus(String message) {
        Platform.runLater(() -> {
            String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
            String formattedMessage = String.format("[%s] %s\n", timestamp, message);
            statusTextArea.appendText(formattedMessage);
            
            // 滚动到底部
            statusTextArea.setScrollTop(Double.MAX_VALUE);
        });
    }
    
    private void saveConfiguration() {
        // 保存spinner的值
        config.setCheckIntervalMs(checkIntervalSpinner.getValue());
        config.save();
    }
    
    private void updateUI() {
        // 根据当前状态更新UI可用性
        boolean connected = lcuMonitor != null && lcuMonitor.isConnected();
        
        // 允许在未连接时也可以设置英雄
        selectBanChampionButton.setDisable(false);
        selectPickChampionButton.setDisable(false);
    }
    
    public void shutdown() {
        if (lcuMonitor != null) {
            lcuMonitor.shutdown();
        }
        saveConfiguration();
    }
    
    @FunctionalInterface
    private interface ChampionSelectionCallback {
        void onChampionSelected(Champion champion);
    }
}