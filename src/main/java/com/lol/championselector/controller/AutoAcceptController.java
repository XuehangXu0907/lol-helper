package com.lol.championselector.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.lol.championselector.config.AutoAcceptConfig;
import com.lol.championselector.lcu.GamePhase;
import com.lol.championselector.lcu.LCUMonitor;
import com.lol.championselector.model.Champion;
import com.lol.championselector.manager.LanguageManager;
import com.lol.championselector.manager.PopupSuppressionManager;
import com.lol.championselector.manager.SmartTimingManager;
import com.lol.championselector.manager.SystemTrayManager;
import com.lol.championselector.manager.WindowsAutoStartManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.util.Duration;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.util.List;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

public class AutoAcceptController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(AutoAcceptController.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    @FXML private Label connectionStatusLabel;
    
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
    
    // Champion avatars
    @FXML private ImageView banChampionAvatar;
    @FXML private ImageView pickChampionAvatar;
    
    @FXML private TextArea statusTextArea;
    
    @FXML private MenuButton languageMenuButton;
    @FXML private MenuItem chineseMenuItem;
    @FXML private MenuItem englishMenuItem;
    
    // System Tray and Auto Start settings
    @FXML private CheckBox systemTrayCheckBox;
    @FXML private CheckBox autoStartCheckBox;
    @FXML private CheckBox minimizeOnCloseCheckBox;
    
    // Auto connect settings
    @FXML private CheckBox autoConnectCheckBox;
    @FXML private CheckBox autoReconnectCheckBox;
    @FXML private Label autoStartStatusLabel;
    @FXML private Button testAutoStartButton;
    @FXML private Label trayStatusLabel;
    
    // Popup suppression settings
    @FXML private CheckBox suppressReadyCheckCheckBox;
    @FXML private CheckBox suppressBanPhaseCheckBox;
    @FXML private CheckBox suppressPickPhaseCheckBox;
    @FXML private Label suppressionStatusLabel;
    
    // Position presets settings
    @FXML private CheckBox usePositionPresetsCheckBox;
    @FXML private ComboBox<String> positionComboBox;
    @FXML private HBox positionPresetsContainer;
    @FXML private Button editPositionConfigButton;
    @FXML private Label currentPositionStatusLabel;
    @FXML private HBox positionPreviewContainer;
    
    // UI Layout elements
    @FXML private TabPane mainTabPane;
    @FXML private ComboBox<String> logLevelComboBox;
    
    private LCUMonitor lcuMonitor;
    private AutoAcceptConfig config;
    private LanguageManager languageManager;
    private WindowsAutoStartManager autoStartManager;
    private SystemTrayManager systemTrayManager;
    
    // Auto reconnect mechanism
    private Timeline autoReconnectTimeline;
    private PopupSuppressionManager popupSuppressionManager;
    private SmartTimingManager smartTimingManager;
    private com.lol.championselector.ChampionSelectorApplication application;
    
    // Action tracking to prevent duplicate operations
    private Set<Integer> processedActions = new HashSet<>();
    private String lastSessionId = null;
    
    // Player position tracking
    private String currentPlayerPosition = null;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        languageManager = LanguageManager.getInstance();
        autoStartManager = new WindowsAutoStartManager();
        initializeComponents();
        loadConfiguration();
        setupLCUMonitor();
        updateUI();
        updateTexts();
        
        // Listen for language changes
        languageManager.currentLocaleProperty().addListener((obs, oldVal, newVal) -> {
            updateTexts();
        });
    }
    
    private void initializeComponents() {
        // 设置检查间隔spinner
        checkIntervalSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(500, 5000, 1000, 100));
        
        // 初始化分路下拉框
        initializePositionComboBox();
        
        // 初始状态
        updateConnectionStatus(false);
        updateGamePhase(GamePhase.NONE);
        
        // 初始化头像显示
        initializeAvatars();
        
        appendStatus("应用程序已启动...");
        
        // 设置自动连接
        if (config != null && config.isAutoConnectEnabled()) {
            Platform.runLater(() -> {
                Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
                    if (!lcuMonitor.isConnected()) {
                        appendStatus(languageManager.getString("status.connecting"));
                        attemptAutoConnection();
                    }
                }));
                timeline.play();
            });
        } else {
            appendStatus("等待手动连接到游戏客户端...");
        }
    }
    
    private void initializePositionComboBox() {
        if (positionComboBox != null) {
            positionComboBox.getItems().addAll(
                "top",      // 上路
                "jungle",   // 打野
                "middle",   // 中路
                "bottom",   // 下路ADC
                "utility"   // 辅助
            );
            
            // 设置显示转换器
            positionComboBox.setConverter(new javafx.util.StringConverter<String>() {
                @Override
                public String toString(String position) {
                    return translatePosition(position);
                }
                
                @Override
                public String fromString(String string) {
                    switch (string) {
                        case "上路": return "top";
                        case "打野": return "jungle";
                        case "中路": return "middle";
                        case "下路ADC": return "bottom";
                        case "辅助": return "utility";
                        default: return string;
                    }
                }
            });
        }
    }
    
    private void loadConfiguration() {
        config = AutoAcceptConfig.load();
        
        // 应用配置到UI
        autoAcceptCheckBox.setSelected(config.isAutoAcceptEnabled());
        checkIntervalSpinner.getValueFactory().setValue(config.getCheckIntervalMs());
        
        autoBanCheckBox.setSelected(config.getChampionSelect().isAutoBanEnabled());
        autoPickCheckBox.setSelected(config.getChampionSelect().isAutoPickEnabled());
        
        // System Tray and Auto Start settings
        if (systemTrayCheckBox != null) {
            systemTrayCheckBox.setSelected(config.isSystemTrayEnabled());
        }
        if (autoStartCheckBox != null) {
            autoStartCheckBox.setSelected(config.isAutoStartEnabled());
        }
        if (minimizeOnCloseCheckBox != null) {
            minimizeOnCloseCheckBox.setSelected(config.isMinimizeOnClose());
        }
        
        // Auto connect settings
        if (autoConnectCheckBox != null) {
            autoConnectCheckBox.setSelected(config.isAutoConnectEnabled());
        }
        if (autoReconnectCheckBox != null) {
            autoReconnectCheckBox.setSelected(config.isAutoReconnectEnabled());
        }
        
        // Popup suppression settings
        if (suppressReadyCheckCheckBox != null) {
            suppressReadyCheckCheckBox.setSelected(config.isSuppressReadyCheckPopup());
        }
        if (suppressBanPhaseCheckBox != null) {
            suppressBanPhaseCheckBox.setSelected(config.isSuppressBanPhasePopup());
        }
        if (suppressPickPhaseCheckBox != null) {
            suppressPickPhaseCheckBox.setSelected(config.isSuppressPickPhasePopup());
        }
        
        // Position presets settings
        if (usePositionPresetsCheckBox != null) {
            usePositionPresetsCheckBox.setSelected(config.getChampionSelect().isUsePositionBasedSelection());
        }
        
        updateChampionLabels();
        updateAutoStartStatus();
        updatePositionPresetsUI();
    }
    
    private void updateChampionLabels() {
        if (config.getChampionSelect().getBanChampion() != null) {
            banChampionLabel.setText(config.getChampionSelect().getBanChampion().toString());
            loadChampionAvatar(banChampionAvatar, config.getChampionSelect().getBanChampion().getKey());
        }
        
        if (config.getChampionSelect().getPickChampion() != null) {
            pickChampionLabel.setText(config.getChampionSelect().getPickChampion().toString());
            loadChampionAvatar(pickChampionAvatar, config.getChampionSelect().getPickChampion().getKey());
        }
    }
    
    private void loadChampionAvatar(ImageView imageView, String championKey) {
        if (imageView == null || championKey == null || championKey.isEmpty()) {
            return;
        }
        
        try {
            // 尝试加载英雄头像
            String avatarPath = "/champion/avatars/" + championKey + ".png";
            Image avatar = new Image(getClass().getResourceAsStream(avatarPath));
            
            if (avatar.isError()) {
                // 如果加载失败，使用默认头像
                Image defaultAvatar = new Image(getClass().getResourceAsStream("/default_champion.png"));
                imageView.setImage(defaultAvatar);
                logger.debug("Using default avatar for champion: {}", championKey);
            } else {
                imageView.setImage(avatar);
                logger.debug("Loaded avatar for champion: {}", championKey);
            }
        } catch (Exception e) {
            logger.warn("Failed to load avatar for champion: {}", championKey, e);
            // 尝试设置一个空的图像或默认图像
            try {
                Image defaultAvatar = new Image(getClass().getResourceAsStream("/default_champion.png"));
                imageView.setImage(defaultAvatar);
            } catch (Exception ex) {
                logger.debug("No default avatar available");
            }
        }
    }
    
    private void initializeAvatars() {
        // 为默认英雄加载头像
        loadChampionAvatar(banChampionAvatar, "Ekko");
        loadChampionAvatar(pickChampionAvatar, "Jinx");
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
        appendStatus("正在连接到游戏客户端...");
        
        lcuMonitor.connect()
            .thenAccept(connected -> Platform.runLater(() -> {
                if (connected) {
                    appendStatus("成功连接到游戏客户端");
                    lcuMonitor.startMonitoring();
                    
                    // 初始化弹窗抑制管理器
                    initializePopupSuppression();
                    
                    // 初始化智能时机管理器
                    initializeSmartTimingManager();
                    
                    disconnectButton.setDisable(false);
                } else {
                    appendStatus("连接失败：未找到游戏客户端或连接被拒绝");
                    connectButton.setDisable(false);
                }
            }))
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    appendStatus("连接错误：" + throwable.getMessage());
                    connectButton.setDisable(false);
                });
                return null;
            });
    }
    
    @FXML
    private void onDisconnectClicked() {
        lcuMonitor.stopMonitoring();
        updateConnectionStatus(false);
        updateGamePhase(GamePhase.NONE);
        
        // 停止自动重连
        if (autoReconnectTimeline != null) {
            autoReconnectTimeline.stop();
            autoReconnectTimeline = null;
        }
        
        connectButton.setDisable(false);
        disconnectButton.setDisable(true);
        
        appendStatus(languageManager.getString("status.disconnected"));
    }
    
    /**
     * 尝试自动连接
     */
    private void attemptAutoConnection() {
        if (lcuMonitor.isConnected()) {
            return;
        }
        
        lcuMonitor.connect()
            .thenAccept(connected -> Platform.runLater(() -> {
                if (connected) {
                    appendStatus(languageManager.getString("status.connected"));
                    lcuMonitor.startMonitoring();
                    initializePopupSuppression();
                    initializeSmartTimingManager();
                    disconnectButton.setDisable(false);
                    
                    // 停止重连任务
                    if (autoReconnectTimeline != null) {
                        autoReconnectTimeline.stop();
                        autoReconnectTimeline = null;
                    }
                } else {
                    // 如果启用了自动重连，开始重连任务
                    if (config != null && config.isAutoReconnectEnabled()) {
                        startAutoReconnect();
                    }
                }
            }))
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    // 如果启用了自动重连，开始重连任务
                    if (config != null && config.isAutoReconnectEnabled()) {
                        startAutoReconnect();
                    }
                });
                return null;
            });
    }
    
    /**
     * 开始自动重连机制
     */
    private void startAutoReconnect() {
        if (autoReconnectTimeline != null || lcuMonitor.isConnected()) {
            return;
        }
        
        int intervalSeconds = config != null ? config.getReconnectIntervalSeconds() : 10;
        
        autoReconnectTimeline = new Timeline(new KeyFrame(Duration.seconds(intervalSeconds), e -> {
            if (!lcuMonitor.isConnected()) {
                attemptAutoConnection();
            } else {
                // 已连接，停止重连任务
                if (autoReconnectTimeline != null) {
                    autoReconnectTimeline.stop();
                    autoReconnectTimeline = null;
                }
            }
        }));
        autoReconnectTimeline.setCycleCount(Timeline.INDEFINITE);
        autoReconnectTimeline.play();
        
        appendStatus("已启动自动重连，每" + intervalSeconds + "秒检测一次");
    }
    
    @FXML
    private void onAutoAcceptToggled() {
        config.setAutoAcceptEnabled(autoAcceptCheckBox.isSelected());
        saveConfiguration();
        
        String statusKey = autoAcceptCheckBox.isSelected() ? "status.autoAcceptEnabled" : "status.autoAcceptDisabled";
        appendStatus(languageManager.getString(statusKey));
    }
    
    @FXML
    private void onAutoBanToggled() {
        config.getChampionSelect().setAutoBanEnabled(autoBanCheckBox.isSelected());
        saveConfiguration();
        
        String statusKey = autoBanCheckBox.isSelected() ? "status.autoBanEnabled" : "status.autoBanDisabled";
        appendStatus(languageManager.getString(statusKey));
    }
    
    @FXML
    private void onAutoPickToggled() {
        config.getChampionSelect().setAutoPickEnabled(autoPickCheckBox.isSelected());
        saveConfiguration();
        
        String statusKey = autoPickCheckBox.isSelected() ? "status.autoPickEnabled" : "status.autoPickDisabled";
        appendStatus(languageManager.getString(statusKey));
    }
    
    @FXML
    private void onSelectBanChampionClicked() {
        selectChampion("Ban", (champion) -> {
            AutoAcceptConfig.ChampionInfo championInfo = new AutoAcceptConfig.ChampionInfo(champion);
            config.getChampionSelect().setBanChampion(championInfo);
            banChampionLabel.setText(championInfo.toString());
            loadChampionAvatar(banChampionAvatar, champion.getKey());
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
            loadChampionAvatar(pickChampionAvatar, champion.getKey());
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
            
            // 设置弹窗大小
            stage.setWidth(900);
            stage.setHeight(550);
            stage.setMinWidth(700);
            stage.setMinHeight(450);
            
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
            if (connectionStatusLabel != null) {
                String statusKey = connected ? "connection.status.connected" : "connection.status.disconnected";
                connectionStatusLabel.setText(languageManager.getString(statusKey));
                connectionStatusLabel.setStyle(connected ? 
                    "-fx-text-fill: #4CAF50; -fx-font-weight: bold;" : 
                    "-fx-text-fill: #F44336; -fx-font-weight: bold;");
            }
            
            // 处理连接状态变化
            if (!connected) {
                // 连接断开，停止任何正在进行的任务
                if (popupSuppressionManager != null) {
                    popupSuppressionManager.stopMonitoring();
                }
                
                // 如果启用了自动重连，开始重连任务
                if (config != null && config.isAutoReconnectEnabled() && autoReconnectTimeline == null) {
                    startAutoReconnect();
                }
            } else {
                // 连接成功，停止重连任务
                if (autoReconnectTimeline != null) {
                    autoReconnectTimeline.stop();
                    autoReconnectTimeline = null;
                }
                
                // 重新启用弹窗抑制
                if (popupSuppressionManager != null) {
                    popupSuppressionManager.startMonitoring();
                }
            }
        });
    }
    
    private void updateGamePhase(GamePhase phase) {
        Platform.runLater(() -> {
            String phaseText = translateGamePhase(phase);
            appendStatus("游戏状态: " + phaseText);
            
            // Clear processed actions when leaving champion select
            if (phase != GamePhase.CHAMP_SELECT && !processedActions.isEmpty()) {
                logger.debug("Clearing processed actions as we left champion select phase");
                processedActions.clear();
                lastSessionId = null;
            }
            
            // 更新弹窗抑制管理器的游戏阶段
            if (popupSuppressionManager != null) {
                popupSuppressionManager.updateGamePhase(phase);
            }
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
            
            // 获取session ID来跟踪会话变化
            String currentSessionId = session.path("gameId").asText("");
            if (currentSessionId.isEmpty()) {
                currentSessionId = session.path("myTeam").toString().hashCode() + "";
            }
            
            // 如果是新的会话，清空已处理的actions
            if (!currentSessionId.equals(lastSessionId)) {
                logger.debug("New champion select session detected, clearing processed actions");
                processedActions.clear();
                lastSessionId = currentSessionId;
                
                // 清空智能时机管理器的待处理actions
                if (smartTimingManager != null) {
                    smartTimingManager.clearPendingActionsForSession();
                }
                
                // 获取玩家位置
                updatePlayerPosition();
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
                            int championId = action.path("championId").asInt(0);
                            
                            // 只处理属于当前玩家且正在进行中且未完成的动作
                            // 还要确保championId为0（未选择英雄）且未处理过
                            if (actorCellId == localCellId && isInProgress && !completed 
                                && championId == 0 && !processedActions.contains(actionId)) {
                                
                                logger.debug("Processing action - ID: {}, Type: {}, ChampionId: {}", 
                                           actionId, type, championId);
                                
                                if ("ban".equals(type) && config.getChampionSelect().isAutoBanEnabled()) {
                                    processedActions.add(actionId); // 立即标记为已处理，防止重复
                                    handleAutoBan(actionId);
                                } else if ("pick".equals(type) && config.getChampionSelect().isAutoPickEnabled()) {
                                    processedActions.add(actionId); // 立即标记为已处理，防止重复
                                    handleAutoPick(actionId);
                                }
                            } else if (processedActions.contains(actionId)) {
                                logger.debug("Skipping already processed action - ID: {}, Type: {}", actionId, type);
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
            processedActions.remove(actionId); // 移除失败的action，允许重试
            return;
        }
        
        // 使用智能时机管理器处理Ban操作
        if (smartTimingManager != null && config.getChampionSelect().isSmartTimingEnabled()) {
            logger.info("Using smart timing for auto-ban action ID: {} with champion: {}", actionId, banChampion);
            appendStatus("智能Ban调度：" + banChampion.toString() + " (等待最佳时机)");
            
            smartTimingManager.handleSmartBan(actionId, banChampion, currentPlayerPosition);
        } else {
            // 传统的立即执行方式
            logger.info("Executing immediate auto-ban for action ID: {} with champion: {}", actionId, banChampion);
            appendStatus("正在自动Ban英雄：" + banChampion.toString() + " (Action ID: " + actionId + ")");
            
            lcuMonitor.banChampion(banChampion.getChampionId(), actionId)
                .thenAccept(success -> Platform.runLater(() -> {
                    if (success) {
                        appendStatus("✓ 成功Ban英雄：" + banChampion.toString());
                        logger.info("Successfully banned champion for action ID: {}", actionId);
                    } else {
                        appendStatus("✗ Ban英雄失败：" + banChampion.toString());
                        logger.warn("Failed to ban champion for action ID: {}", actionId);
                        processedActions.remove(actionId); // 移除失败的action，允许重试
                    }
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        appendStatus("✗ Ban英雄异常：" + throwable.getMessage());
                        logger.error("Exception during ban for action ID: " + actionId, throwable);
                        processedActions.remove(actionId); // 移除异常的action，允许重试
                    });
                    return null;
                });
        }
    }
    
    private void handleAutoPick(int actionId) {
        AutoAcceptConfig.ChampionInfo pickChampion = config.getChampionSelect().getPickChampion();
        if (pickChampion == null || pickChampion.getChampionId() == null) {
            appendStatus("自动Pick失败：未设置Pick英雄或英雄ID无效");
            processedActions.remove(actionId); // 移除失败的action，允许重试
            return;
        }
        
        // 使用智能时机管理器处理Pick操作
        if (smartTimingManager != null && config.getChampionSelect().isSmartTimingEnabled()) {
            logger.info("Using smart timing for auto-pick action ID: {} with champion: {}", actionId, pickChampion);
            appendStatus("智能Pick调度：" + pickChampion.toString() + " (等待最佳时机)");
            
            smartTimingManager.handleSmartPick(actionId, pickChampion, currentPlayerPosition);
        } else {
            // 传统的立即执行方式
            logger.info("Executing immediate auto-pick for action ID: {} with champion: {}", actionId, pickChampion);
            appendStatus("正在自动Pick英雄：" + pickChampion.toString() + " (Action ID: " + actionId + ")");
            
            lcuMonitor.pickChampion(pickChampion.getChampionId(), actionId)
                .thenAccept(success -> Platform.runLater(() -> {
                    if (success) {
                        appendStatus("✓ 成功Pick英雄：" + pickChampion.toString());
                        logger.info("Successfully picked champion for action ID: {}", actionId);
                    } else {
                        appendStatus("✗ Pick英雄失败：" + pickChampion.toString());
                        logger.warn("Failed to pick champion for action ID: {}", actionId);
                        processedActions.remove(actionId); // 移除失败的action，允许重试
                    }
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        appendStatus("✗ Pick英雄异常：" + throwable.getMessage());
                        logger.error("Exception during pick for action ID: " + actionId, throwable);
                        processedActions.remove(actionId); // 移除异常的action，允许重试
                    });
                    return null;
                });
        }
    }
    
    /**
     * 更新玩家位置信息
     */
    private void updatePlayerPosition() {
        if (lcuMonitor != null) {
            lcuMonitor.getPlayerPosition()
                .thenAccept(position -> {
                    currentPlayerPosition = position;
                    if (position != null) {
                        logger.info("Player position detected: {}", position);
                        Platform.runLater(() -> {
                            appendStatus("检测到分路位置: " + translatePosition(position));
                            updatePositionStatusUI(position);
                            
                            // 如果启用了分路预设，自动应用配置
                            if (config != null && config.getChampionSelect().isUsePositionBasedSelection()) {
                                applyPositionPresets(position);
                            }
                        });
                    } else {
                        logger.debug("Player position not available yet");
                        Platform.runLater(() -> updatePositionStatusUI(null));
                    }
                })
                .exceptionally(throwable -> {
                    logger.debug("Failed to get player position", throwable);
                    Platform.runLater(() -> updatePositionStatusUI(null));
                    return null;
                });
        }
    }
    
    /**
     * 翻译分路位置名称
     */
    private String translatePosition(String position) {
        if (position == null) return "未知";
        
        switch (position.toLowerCase()) {
            case "top": return "上路";
            case "jungle": return "打野";
            case "middle": return "中路";
            case "bottom": return "下路ADC";
            case "utility": return "辅助";
            default: return position;
        }
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
        // 允许在未连接时也可以设置英雄
        selectBanChampionButton.setDisable(false);
        selectPickChampionButton.setDisable(false);
    }
    
    // System Tray and Auto Start event handlers
    @FXML
    private void onSystemTrayToggled() {
        if (systemTrayCheckBox != null) {
            config.setSystemTrayEnabled(systemTrayCheckBox.isSelected());
            saveConfiguration();
            
            String status = systemTrayCheckBox.isSelected() ? "启用" : "禁用";
            appendStatus("系统托盘功能已" + status);
        }
    }
    
    
    @FXML
    private void onAutoConnectToggled() {
        if (autoConnectCheckBox != null) {
            boolean enabled = autoConnectCheckBox.isSelected();
            config.setAutoConnectEnabled(enabled);
            saveConfiguration();
            
            String statusKey = enabled ? "status.autoConnectEnabled" : "status.autoConnectDisabled";
            appendStatus(languageManager.getString(statusKey));
        }
    }
    
    @FXML
    private void onAutoReconnectToggled() {
        if (autoReconnectCheckBox != null) {
            boolean enabled = autoReconnectCheckBox.isSelected();
            config.setAutoReconnectEnabled(enabled);
            saveConfiguration();
            
            String statusKey = enabled ? "status.autoReconnectEnabled" : "status.autoReconnectDisabled";
            appendStatus(languageManager.getString(statusKey));
            
            // 如果禁用了重连功能，停止当前的重连任务
            if (!enabled && autoReconnectTimeline != null) {
                autoReconnectTimeline.stop();
                autoReconnectTimeline = null;
                appendStatus("已停止自动重连任务");
            }
        }
    }
    
    @FXML
    private void onAutoStartToggled() {
        if (autoStartCheckBox != null) {
            boolean enabled = autoStartCheckBox.isSelected();
            
            if (autoStartManager.isSupported()) {
                boolean success;
                if (enabled) {
                    success = autoStartManager.enableAutoStart();
                    if (success) {
                        config.setAutoStartEnabled(true);
                        appendStatus("开机自动启动已启用");
                    } else {
                        autoStartCheckBox.setSelected(false);
                        appendStatus("启用开机自动启动失败，请检查权限设置");
                    }
                } else {
                    success = autoStartManager.disableAutoStart();
                    if (success) {
                        config.setAutoStartEnabled(false);
                        appendStatus("开机自动启动已禁用");
                    } else {
                        autoStartCheckBox.setSelected(true);
                        appendStatus("禁用开机自动启动失败");
                    }
                }
            } else {
                autoStartCheckBox.setSelected(false);
                appendStatus("当前系统不支持开机自动启动功能");
            }
            
            saveConfiguration();
            updateAutoStartStatus();
        }
    }
    
    @FXML
    private void onTestAutoStartClicked() {
        if (autoStartManager.isSupported()) {
            String jarLocation = autoStartManager.getJarLocation();
            String command = autoStartManager.getRegistryCommand();
            
            appendStatus("JAR 位置: " + jarLocation);
            appendStatus("注册表命令: " + command);
            appendStatus("当前自动启动状态: " + (autoStartManager.isAutoStartEnabled() ? "已启用" : "已禁用"));
        } else {
            appendStatus("当前系统不支持开机自动启动功能");
        }
    }
    
    private void updateAutoStartStatus() {
        if (autoStartStatusLabel != null && autoStartManager != null) {
            Platform.runLater(() -> {
                if (autoStartManager.isSupported()) {
                    boolean isEnabled = autoStartManager.isAutoStartEnabled();
                    String statusText = isEnabled ? 
                        languageManager.getString("settings.enabled") : 
                        languageManager.getString("settings.disabled");
                    autoStartStatusLabel.setText(languageManager.getString("settings.status") + ": " + statusText);
                    autoStartStatusLabel.setStyle(isEnabled ? 
                        "-fx-text-fill: #4CAF50;" : 
                        "-fx-text-fill: #F44336;");
                } else {
                    autoStartStatusLabel.setText(languageManager.getString("settings.status") + ": " + languageManager.getString("settings.notSupported"));
                    autoStartStatusLabel.setStyle("-fx-text-fill: #FF9800;");
                }
            });
        }
    }

    public void shutdown() {
        if (smartTimingManager != null) {
            smartTimingManager.shutdown();
        }
        if (popupSuppressionManager != null) {
            popupSuppressionManager.shutdown();
        }
        if (lcuMonitor != null) {
            lcuMonitor.shutdown();
        }
        saveConfiguration();
    }
    
    @FunctionalInterface
    private interface ChampionSelectionCallback {
        void onChampionSelected(Champion champion);
    }
    
    @FXML
    private void onChineseSelected() {
        languageManager.setLanguage(LanguageManager.Language.CHINESE);
        appendStatus(languageManager.getString("common.language") + ": 中文");
        
        // 更新托盘菜单语言
        if (systemTrayManager != null) {
            systemTrayManager.setLanguage(true);
        }
    }
    
    @FXML
    private void onEnglishSelected() {
        languageManager.setLanguage(LanguageManager.Language.ENGLISH);
        appendStatus(languageManager.getString("common.language") + ": English");
        
        // 更新托盘菜单语言
        if (systemTrayManager != null) {
            systemTrayManager.setLanguage(false);
        }
    }
    
    private void updateTexts() {
        // Update window title (simplified approach)
        try {
            if (connectButton != null && connectButton.getScene() != null && connectButton.getScene().getWindow() != null) {
                Stage stage = (Stage) connectButton.getScene().getWindow();
                stage.setTitle(languageManager.getString("app.name"));
            }
        } catch (Exception e) {
            logger.debug("Could not update window title", e);
        }
        
        // Update labels (with null checks)
        if (connectionStatusLabel != null) {
            connectionStatusLabel.setText(lcuMonitor != null && lcuMonitor.isConnected() ? 
                languageManager.getString("connection.status.connected") : 
                languageManager.getString("connection.status.disconnected"));
        }
        
        // Update buttons (with null checks)
        if (connectButton != null) connectButton.setText(languageManager.getString("button.connect"));
        if (disconnectButton != null) disconnectButton.setText(languageManager.getString("button.disconnect"));
        
        // Update checkboxes (with null checks)
        if (autoAcceptCheckBox != null) autoAcceptCheckBox.setText(languageManager.getString("autoAccept.enable"));
        if (autoBanCheckBox != null) autoBanCheckBox.setText(languageManager.getString("championSelection.autoBan"));
        if (autoPickCheckBox != null) autoPickCheckBox.setText(languageManager.getString("championSelection.autoPick"));
        
        // Update auto connect checkboxes
        if (autoConnectCheckBox != null) autoConnectCheckBox.setText(languageManager.getString("autoConnect.autoConnect"));
        if (autoReconnectCheckBox != null) autoReconnectCheckBox.setText(languageManager.getString("autoConnect.autoReconnect"));
        
        // Update other buttons (with null checks)
        if (selectBanChampionButton != null) selectBanChampionButton.setText(languageManager.getString("button.select"));
        if (selectPickChampionButton != null) selectPickChampionButton.setText(languageManager.getString("button.select"));
        
        // Update settings checkboxes (with null checks)
        if (systemTrayCheckBox != null) systemTrayCheckBox.setText(languageManager.getString("settings.systemTray"));
        if (autoStartCheckBox != null) autoStartCheckBox.setText(languageManager.getString("settings.autoStart"));
        if (minimizeOnCloseCheckBox != null) minimizeOnCloseCheckBox.setText(languageManager.getString("settings.minimizeToTray"));
        
        // Update popup suppression checkboxes
        if (suppressReadyCheckCheckBox != null) suppressReadyCheckCheckBox.setText(languageManager.getString("popupSuppression.readyCheck"));
        if (suppressBanPhaseCheckBox != null) suppressBanPhaseCheckBox.setText(languageManager.getString("popupSuppression.banPhase"));
        if (suppressPickPhaseCheckBox != null) suppressPickPhaseCheckBox.setText(languageManager.getString("popupSuppression.pickPhase"));
        
        // Update language menu (with null checks)
        if (languageMenuButton != null) languageMenuButton.setText(languageManager.getString("settings.language"));
        
        // Update status text area prompt (with null checks)
        if (statusTextArea != null) statusTextArea.setPromptText(languageManager.getString("status.placeholder"));
        
        // Update auto start status label text
        updateAutoStartStatus();
        
        
        // 更新托盘状态标签
        if (trayStatusLabel != null) {
            String baseText = "托盘状态: ";
            if (systemTrayManager != null) {
                String status = systemTrayManager.getTrayIconStatus();
                trayStatusLabel.setText(baseText + status);
            } else {
                trayStatusLabel.setText(baseText + "未初始化");
            }
        }
        
        // Update tray status
        updateTrayStatus();
        
        // Update suppression status
        updateSuppressionStatus();
    }
    
    private String translateGamePhase(GamePhase phase) {
        if (phase == null) {
            return languageManager.getString("gamePhase.none");
        }
        
        switch (phase) {
            case NONE -> {
                return languageManager.getString("gamePhase.none");
            }
            case LOBBY -> {
                return languageManager.getString("gamePhase.lobby");
            }
            case MATCHMAKING -> {
                return languageManager.getString("gamePhase.matchmaking");
            }
            case READY_CHECK -> {
                return languageManager.getString("gamePhase.readyCheck");
            }
            case CHAMP_SELECT -> {
                return languageManager.getString("gamePhase.champSelect");
            }
            case IN_PROGRESS -> {
                return languageManager.getString("gamePhase.inGame");
            }
            case PRE_END_OF_GAME -> {
                return languageManager.getString("gamePhase.preEndOfGame");
            }
            case END_OF_GAME -> {
                return languageManager.getString("gamePhase.endOfGame");
            }
            default -> {
                return phase.getDisplayName();
            }
        }
    }
    
    // SystemTrayManager setter for dependency injection
    public void setSystemTrayManager(SystemTrayManager systemTrayManager) {
        this.systemTrayManager = systemTrayManager;
        // Update tray status when manager is set
        updateTrayStatus();
    }
    
    // Application setter for controlling close behavior
    public void setApplication(com.lol.championselector.ChampionSelectorApplication application) {
        this.application = application;
        // Sync minimize on close setting
        if (config != null) {
            application.setMinimizeOnClose(config.isMinimizeOnClose());
        }
    }
    
    
    private void showTrayIconGuidance() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("托盘图标使用指导");
            alert.setHeaderText("托盘图标不可见的可能原因和解决方案");
            
            String guidance = "如果您无法看到LOL Helper的托盘图标，请尝试以下解决方案：\n\n" +
                    "1. 点击“刷新托盘”按钮重新加载图标\n" +
                    "2. 检查Windows系统托盘设置：\n" +
                    "   - 右键点击任务栏，选择“任务栏设置”\n" +
                    "   - 在“通知区域”中选择“选择在任务栏上显示的图标”\n" +
                    "   - 找到LOL Helper并开启显示\n" +
                    "3. 如果问题仍然存在，请以管理员身份运行程序\n" +
                    "4. 重启应用程序或重启计算机\n\n" +
                    "注意：即使图标不可见，通知功能仍可能正常工作。";
            
            alert.setContentText(guidance);
            alert.showAndWait();
        });
    }
    
    private void updateTrayStatus() {
        if (trayStatusLabel != null && systemTrayManager != null) {
            Platform.runLater(() -> {
                String status = systemTrayManager.getTrayIconStatus();
                trayStatusLabel.setText("托盘状态: " + status);
                
                // 根据状态设置颜色
                if (status.contains("正常")) {
                    trayStatusLabel.setStyle("-fx-text-fill: green;");
                } else if (status.contains("丢失") || status.contains("未初始化")) {
                    trayStatusLabel.setStyle("-fx-text-fill: red;");
                } else {
                    trayStatusLabel.setStyle("-fx-text-fill: orange;");
                }
            });
        }
    }
    
    @FXML
    private void onMinimizeOnCloseToggled() {
        if (minimizeOnCloseCheckBox != null && config != null) {
            config.setMinimizeOnClose(minimizeOnCloseCheckBox.isSelected());
            saveConfiguration();
            
            // 更新应用程序设置
            if (application != null) {
                application.setMinimizeOnClose(config.isMinimizeOnClose());
            }
            
            String status = minimizeOnCloseCheckBox.isSelected() ? "启用" : "禁用";
            appendStatus("关闭窗口时最小化到托盘已" + status);
        }
    }
    
    // === 弹窗抑制相关方法 ===
    
    /**
     * 初始化智能时机管理器
     */
    private void initializeSmartTimingManager() {
        if (lcuMonitor != null && config != null) {
            try {
                smartTimingManager = new SmartTimingManager(lcuMonitor, config);
                smartTimingManager.start();
                appendStatus("✓ 智能时机控制已启用");
                logger.info("智能时机管理器初始化成功");
            } catch (Exception e) {
                logger.error("初始化智能时机管理器失败", e);
                appendStatus("⚠ 智能时机控制初始化失败");
            }
        }
    }
    
    /**
     * 初始化弹窗抑制管理器
     */
    private void initializePopupSuppression() {
        if (lcuMonitor != null && lcuMonitor.isConnected()) {
            // 创建弹窗抑制管理器，使用LCU连接
            try {
                // 通过反射或直接访问获取LCU连接
                java.lang.reflect.Field connectionField = lcuMonitor.getClass().getDeclaredField("connection");
                connectionField.setAccessible(true);
                Object connection = connectionField.get(lcuMonitor);
                
                if (connection != null) {
                    popupSuppressionManager = new PopupSuppressionManager(
                        (com.lol.championselector.lcu.LCUConnection) connection);
                    
                    // 配置初始状态
                    updatePopupSuppressionSettings();
                    
                    // 设置状态变化回调
                    popupSuppressionManager.setOnSuppressionStateChanged(this::updateSuppressionStatus);
                    
                    // 开始监控
                    popupSuppressionManager.startMonitoring();
                    
                    // 测试功能可用性
                    popupSuppressionManager.testSuppressionCapability()
                        .thenAccept(capable -> Platform.runLater(() -> {
                            if (capable) {
                                appendStatus("✓ 弹窗抑制功能已启用");
                            } else {
                                appendStatus("⚠ 弹窗抑制功能不可用");
                            }
                        }));
                        
                    logger.info("弹窗抑制管理器初始化成功");
                }
            } catch (Exception e) {
                logger.error("初始化弹窗抑制管理器失败", e);
                appendStatus("弹窗抑制功能初始化失败");
            }
        }
    }
    
    /**
     * 更新弹窗抑制设置
     */
    private void updatePopupSuppressionSettings() {
        if (popupSuppressionManager != null && config != null) {
            popupSuppressionManager.setSuppressReadyCheckPopup(config.isSuppressReadyCheckPopup());
            popupSuppressionManager.setSuppressBanPhasePopup(config.isSuppressBanPhasePopup());
            popupSuppressionManager.setSuppressPickPhasePopup(config.isSuppressPickPhasePopup());
        }
    }
    
    /**
     * 更新抑制状态显示
     */
    private void updateSuppressionStatus() {
        if (suppressionStatusLabel != null && popupSuppressionManager != null) {
            Platform.runLater(() -> {
                String status = popupSuppressionManager.getDetailedStatus();
                suppressionStatusLabel.setText(status);
                
                // 根据状态设置颜色
                if (status.contains("临时禁用") || status.contains("连续失败")) {
                    suppressionStatusLabel.setStyle("-fx-text-fill: red;");
                } else if (status.contains("✓")) {
                    suppressionStatusLabel.setStyle("-fx-text-fill: green;");
                } else {
                    suppressionStatusLabel.setStyle("-fx-text-fill: gray;");
                }
            });
        }
    }
    
    // === 弹窗抑制事件处理方法 ===
    
    @FXML
    private void onSuppressReadyCheckToggled() {
        if (suppressReadyCheckCheckBox != null && config != null) {
            config.setSuppressReadyCheckPopup(suppressReadyCheckCheckBox.isSelected());
            saveConfiguration();
            
            if (popupSuppressionManager != null) {
                popupSuppressionManager.setSuppressReadyCheckPopup(config.isSuppressReadyCheckPopup());
            }
            
            String status = suppressReadyCheckCheckBox.isSelected() ? "启用" : "禁用";
            appendStatus("准备检查弹窗抑制已" + status);
        }
    }
    
    @FXML
    private void onSuppressBanPhaseToggled() {
        if (suppressBanPhaseCheckBox != null && config != null) {
            config.setSuppressBanPhasePopup(suppressBanPhaseCheckBox.isSelected());
            saveConfiguration();
            
            if (popupSuppressionManager != null) {
                popupSuppressionManager.setSuppressBanPhasePopup(config.isSuppressBanPhasePopup());
            }
            
            String status = suppressBanPhaseCheckBox.isSelected() ? "启用" : "禁用";
            appendStatus("Ban阶段弹窗抑制已" + status);
        }
    }
    
    @FXML
    private void onSuppressPickPhaseToggled() {
        if (suppressPickPhaseCheckBox != null && config != null) {
            config.setSuppressPickPhasePopup(suppressPickPhaseCheckBox.isSelected());
            saveConfiguration();
            
            if (popupSuppressionManager != null) {
                popupSuppressionManager.setSuppressPickPhasePopup(config.isSuppressPickPhasePopup());
            }
            
            String status = suppressPickPhaseCheckBox.isSelected() ? "启用" : "禁用";
            appendStatus("Pick阶段弹窗抑制已" + status);
        }
    }
    
    // === 分路预设相关方法 ===
    
    /**
     * 分路预设开关切换事件处理
     */
    @FXML
    private void onUsePositionPresetsToggled() {
        if (usePositionPresetsCheckBox != null && config != null) {
            boolean enabled = usePositionPresetsCheckBox.isSelected();
            config.getChampionSelect().setUsePositionBasedSelection(enabled);
            saveConfiguration();
            
            String status = enabled ? "启用" : "禁用";
            appendStatus("分路预设功能已" + status);
            
            // 更新UI状态
            updatePositionPresetsUI();
            
            // 如果启用且有当前位置，立即应用配置
            if (enabled && currentPlayerPosition != null) {
                applyPositionPresets(currentPlayerPosition);
            }
        }
    }
    
    /**
     * 分路选择变化事件处理
     */
    @FXML
    private void onPositionSelectionChanged() {
        if (positionComboBox != null && config != null) {
            String selectedPosition = positionComboBox.getValue();
            if (selectedPosition != null && config.getChampionSelect().isUsePositionBasedSelection()) {
                applyPositionPresets(selectedPosition);
            } else if (selectedPosition != null) {
                // 仅更新预览，不应用配置
                AutoAcceptConfig.PositionConfig positionConfig = config.getChampionSelect().getPositionConfig(selectedPosition);
                updatePositionPreview(positionConfig);
            }
        }
    }
    
    /**
     * 编辑分路配置按钮点击事件
     */
    @FXML
    private void onEditPositionConfigClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PositionConfigDialog.fxml"));
            Stage stage = new Stage();
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
            stage.setTitle("分路配置管理");
            stage.initModality(Modality.APPLICATION_MODAL);
            
            // 设置对话框大小
            stage.setWidth(500);
            stage.setHeight(600);
            stage.setMinWidth(450);
            stage.setMinHeight(500);
            stage.setResizable(true);
            
            // 获取控制器并设置配置
            PositionConfigDialogController controller = loader.getController();
            controller.setConfig(config);
            controller.setOnConfigSaved(() -> {
                // 配置保存后刷新当前预设
                if (currentPlayerPosition != null && config.getChampionSelect().isUsePositionBasedSelection()) {
                    applyPositionPresets(currentPlayerPosition);
                }
                appendStatus("分路配置已更新");
            });
            
            stage.showAndWait();
        } catch (IOException e) {
            logger.error("Failed to open position config dialog", e);
            appendStatus("打开分路配置对话框失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新分路预设UI状态
     */
    private void updatePositionPresetsUI() {
        if (positionPresetsContainer != null && usePositionPresetsCheckBox != null) {
            boolean enabled = usePositionPresetsCheckBox.isSelected();
            positionPresetsContainer.setDisable(!enabled);
            
            if (enabled && currentPlayerPosition != null) {
                positionComboBox.setValue(currentPlayerPosition);
            }
        }
    }
    
    /**
     * 更新分路状态UI显示
     */
    private void updatePositionStatusUI(String position) {
        if (currentPositionStatusLabel != null) {
            if (position != null) {
                String translatedPosition = translatePosition(position);
                currentPositionStatusLabel.setText("当前检测到分路: " + translatedPosition);
                currentPositionStatusLabel.setStyle("-fx-text-fill: #4CAF50;");
                
                // 自动选择对应的分路
                if (positionComboBox != null) {
                    positionComboBox.setValue(position);
                }
            } else {
                currentPositionStatusLabel.setText("未检测到分路位置");
                currentPositionStatusLabel.setStyle("-fx-text-fill: #9E9E9E;");
            }
        }
    }
    
    /**
     * 应用分路预设配置
     */
    private void applyPositionPresets(String position) {
        if (config == null || position == null) {
            return;
        }
        
        AutoAcceptConfig.PositionConfig positionConfig = config.getChampionSelect().getPositionConfig(position);
        if (positionConfig != null) {
            // 应用Ban英雄预设
            AutoAcceptConfig.ChampionInfo preferredBan = positionConfig.getPreferredBanChampion();
            if (preferredBan == null && positionConfig.getBanChampions() != null && !positionConfig.getBanChampions().isEmpty()) {
                preferredBan = positionConfig.getBanChampions().get(0);
            }
            
            if (preferredBan != null) {
                // 确保championId有效
                preferredBan.ensureChampionId();
                config.getChampionSelect().setBanChampion(preferredBan);
                banChampionLabel.setText(preferredBan.toString());
                loadChampionAvatar(banChampionAvatar, preferredBan.getKey());
                logger.info("Applied position-based ban champion {} for position {}", preferredBan, position);
            }
            
            // 应用Pick英雄预设
            AutoAcceptConfig.ChampionInfo preferredPick = positionConfig.getPreferredPickChampion();
            if (preferredPick == null && positionConfig.getPickChampions() != null && !positionConfig.getPickChampions().isEmpty()) {
                preferredPick = positionConfig.getPickChampions().get(0);
            }
            
            if (preferredPick != null) {
                // 确保championId有效
                preferredPick.ensureChampionId();
                config.getChampionSelect().setPickChampion(preferredPick);
                pickChampionLabel.setText(preferredPick.toString());
                loadChampionAvatar(pickChampionAvatar, preferredPick.getKey());
                logger.info("Applied position-based pick champion {} for position {}", preferredPick, position);
            }
            
            saveConfiguration();
            appendStatus("已应用" + translatePosition(position) + "预设配置");
            logger.info("Applied position presets for: {}", position);
            
            // 更新分路预设预览
            updatePositionPreview(positionConfig);
        } else {
            logger.debug("No position config found for: {}", position);
            // 清空预览
            updatePositionPreview(null);
        }
    }
    
    /**
     * 更新分路预设预览显示
     */
    private void updatePositionPreview(AutoAcceptConfig.PositionConfig positionConfig) {
        if (positionPreviewContainer == null) {
            return;
        }
        
        Platform.runLater(() -> {
            positionPreviewContainer.getChildren().clear();
            
            if (positionConfig == null) {
                Label noPresetLabel = new Label("当前分路暂无预设");
                noPresetLabel.setStyle("-fx-text-fill: #999999;");
                positionPreviewContainer.getChildren().add(noPresetLabel);
                return;
            }
            
            // Ban英雄预览
            VBox banPreview = new VBox(3);
            banPreview.setAlignment(javafx.geometry.Pos.CENTER);
            Label banLabel = new Label("Ban");
            banLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666666;");
            
            HBox banChampions = new HBox(5);
            banChampions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            List<AutoAcceptConfig.ChampionInfo> banList = positionConfig.getBanChampions();
            if (banList != null && !banList.isEmpty()) {
                for (int i = 0; i < Math.min(3, banList.size()); i++) {
                    ImageView avatar = new ImageView();
                    avatar.setFitWidth(32);
                    avatar.setFitHeight(32);
                    avatar.setPreserveRatio(true);
                    loadChampionAvatar(avatar, banList.get(i).getKey());
                    banChampions.getChildren().add(avatar);
                }
                if (banList.size() > 3) {
                    Label moreLabel = new Label("+" + (banList.size() - 3));
                    moreLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999999;");
                    banChampions.getChildren().add(moreLabel);
                }
            } else {
                Label noBanLabel = new Label("无");
                noBanLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999999;");
                banChampions.getChildren().add(noBanLabel);
            }
            
            banPreview.getChildren().addAll(banLabel, banChampions);
            
            // Pick英雄预览
            VBox pickPreview = new VBox(3);
            pickPreview.setAlignment(javafx.geometry.Pos.CENTER);
            Label pickLabel = new Label("Pick");
            pickLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666666;");
            
            HBox pickChampions = new HBox(5);
            pickChampions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            List<AutoAcceptConfig.ChampionInfo> pickList = positionConfig.getPickChampions();
            if (pickList != null && !pickList.isEmpty()) {
                for (int i = 0; i < Math.min(3, pickList.size()); i++) {
                    ImageView avatar = new ImageView();
                    avatar.setFitWidth(32);
                    avatar.setFitHeight(32);
                    avatar.setPreserveRatio(true);
                    loadChampionAvatar(avatar, pickList.get(i).getKey());
                    pickChampions.getChildren().add(avatar);
                }
                if (pickList.size() > 3) {
                    Label moreLabel = new Label("+" + (pickList.size() - 3));
                    moreLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999999;");
                    pickChampions.getChildren().add(moreLabel);
                }
            } else {
                Label noPickLabel = new Label("无");
                noPickLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999999;");
                pickChampions.getChildren().add(noPickLabel);
            }
            
            pickPreview.getChildren().addAll(pickLabel, pickChampions);
            
            // 添加分隔线
            javafx.scene.control.Separator separator = new javafx.scene.control.Separator();
            separator.setOrientation(javafx.geometry.Orientation.VERTICAL);
            separator.setPrefHeight(40);
            
            positionPreviewContainer.getChildren().addAll(banPreview, separator, pickPreview);
        });
    }
}