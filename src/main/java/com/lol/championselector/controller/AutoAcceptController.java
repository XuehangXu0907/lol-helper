package com.lol.championselector.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.lol.championselector.config.AutoAcceptConfig;
import com.lol.championselector.lcu.GamePhase;
import com.lol.championselector.lcu.LCUMonitor;
import com.lol.championselector.model.Champion;
import com.lol.championselector.manager.LanguageManager;
import com.lol.championselector.manager.PopupSuppressionManager;
import com.lol.championselector.manager.SystemTrayManager;
import com.lol.championselector.manager.WindowsAutoStartManager;
import com.lol.championselector.manager.DraftPickEngine;
import com.lol.championselector.manager.SmartChampionSelector;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import com.lol.championselector.manager.ResourceManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ButtonBar;
import java.util.List;
import javafx.stage.Modality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.HashMap;

public class AutoAcceptController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(AutoAcceptController.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    /**
     * 队列选择结果类
     */
    private static class QueueSelectionResult {
        private final AutoAcceptConfig.ChampionInfo champion;
        private final int queuePosition; // 0-based position in queue
        private final int totalQueueSize;
        private final String source; // "position_queue", "global_default", etc.
        
        public QueueSelectionResult(AutoAcceptConfig.ChampionInfo champion, int queuePosition, int totalQueueSize, String source) {
            this.champion = champion;
            this.queuePosition = queuePosition;
            this.totalQueueSize = totalQueueSize;
            this.source = source;
        }
        
        public AutoAcceptConfig.ChampionInfo getChampion() { return champion; }
        public int getQueuePosition() { return queuePosition; }
        public int getTotalQueueSize() { return totalQueueSize; }
        public String getSource() { return source; }
        
        public String getDisplayText() {
            if ("global_default".equals(source)) {
                return champion.toString() + " (全局默认)";
            } else if ("position_queue".equals(source)) {
                return champion.toString() + " (" + (queuePosition + 1) + "/" + totalQueueSize + ")";
            } else {
                return champion.toString();
            }
        }
    }
    
    @FXML private Label connectionStatusLabel;
    
    @FXML private Button connectButton;
    @FXML private Button disconnectButton;
    
    @FXML private CheckBox autoAcceptCheckBox;
    @FXML private Spinner<Integer> checkIntervalSpinner;
    
    @FXML private CheckBox autoBanCheckBox;
    @FXML private CheckBox autoPickCheckBox;
    
    // Smart features settings
    @FXML private CheckBox autoHoverCheckBox;
    @FXML private CheckBox smartBanCheckBox;
    @FXML private Label smartFeaturesLabel;
    
    // Simple delay ban settings
    @FXML private Spinner<Integer> simpleBanDelaySpinner;
    
    // Simple delay pick settings
    @FXML private Spinner<Integer> simplePickDelaySpinner;
    
    
    // Queue status display components
    @FXML private VBox banQueueStatus;
    @FXML private Label banQueueLabel;
    @FXML private HBox banQueuePreview;
    @FXML private VBox pickQueueStatus;
    @FXML private Label pickQueueLabel;
    @FXML private HBox pickQueuePreview;
    
    // Additional labels that need translation
    @FXML private Label checkIntervalLabel;
    @FXML private Label millisecondsLabel;
    @FXML private Label currentPositionLabel;
    @FXML private Label banExecutionModeLabel;
    @FXML private Label delayTimeLabel;
    @FXML private Label secondsLabel;
    @FXML private Label pickDelayTimeLabel;
    @FXML private Label pickSecondsLabel;
    @FXML private Label generalSettingsLabel;
    @FXML private Label connectionSettingsLabel;
    @FXML private Label popupSuppressionLabel;
    @FXML private Label suppressionDescriptionLabel;
    
    @FXML private TextArea statusTextArea;
    // 日志弹窗相关
    private Stage logViewerStage;
    private LogViewerDialogController logViewerController;
    
    @FXML private Button languageToggleButton;
    
    // System Tray and Auto Start settings
    @FXML private CheckBox systemTrayCheckBox;
    @FXML private CheckBox autoStartCheckBox;
    @FXML private CheckBox minimizeOnCloseCheckBox;
    
    // Auto connect settings
    @FXML private CheckBox autoConnectCheckBox;
    @FXML private CheckBox autoReconnectCheckBox;
    @FXML private Label autoStartStatusLabel;
    @FXML private Label trayStatusLabel;
    
    // Popup suppression settings
    @FXML private CheckBox suppressReadyCheckCheckBox;
    @FXML private CheckBox suppressBanPhaseCheckBox;
    @FXML private CheckBox suppressPickPhaseCheckBox;
    @FXML private CheckBox forceEnglishTrayMenuCheckBox;
    @FXML private Label suppressionStatusLabel;
    
    // Position presets settings
    @FXML private ComboBox<String> positionComboBox;
    @FXML private HBox positionPresetsContainer;
    @FXML private Button editPositionConfigButton;
    
    // UI Layout elements
    @FXML private ScrollPane contentScrollPane;
    
    
    // Status bar elements
    @FXML private Button toggleStatusButton;
    
    
    // Card titles
    @FXML private Label autoAcceptCardTitle;
    @FXML private Label positionPresetsCardTitle;
    @FXML private Label banPickSettingsCardTitle;
    
    // Status badges
    @FXML private Label autoAcceptStatusBadge;
    @FXML private Label autoBanStatusBadge;
    @FXML private Label autoPickStatusBadge;
    
    
    private LCUMonitor lcuMonitor;
    private AutoAcceptConfig config;
    private LanguageManager languageManager;
    private WindowsAutoStartManager autoStartManager;
    private SystemTrayManager systemTrayManager;
    
    // Auto reconnect mechanism
    private Timeline autoReconnectTimeline;
    private PopupSuppressionManager popupSuppressionManager;
    private DraftPickEngine draftPickEngine;
    private SmartChampionSelector smartChampionSelector;
    private com.lol.championselector.ChampionSelectorApplication application;
    
    // Resource management
    private final ResourceManager resourceManager = ResourceManager.getInstance();
    
    // Action tracking to prevent duplicate operations
    private final Set<Integer> processedActions = new HashSet<>();
    private final Map<Integer, ActionStatus> actionStatusMap = new HashMap<>();
    private String lastSessionId = null;
    
    // Action status enum for better tracking
    private enum ActionStatus {
        PROCESSING,  // 正在处理中
        SUCCESS,     // 成功完成
        FAILED,      // 失败，可以重试
        RETRY_LIMIT  // 达到重试次数限制
    }
    
    // Action retry tracking
    private final Map<Integer, Integer> actionRetryCount = new HashMap<>();
    private static final int MAX_RETRY_COUNT = 3;
    
    // Player position tracking
    private String currentPlayerPosition = null;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        languageManager = LanguageManager.getInstance();
        autoStartManager = new WindowsAutoStartManager();
        
        // 初始化新的pick逻辑组件
        draftPickEngine = new DraftPickEngine();
        smartChampionSelector = new SmartChampionSelector();
        
        initializeComponents();
        loadConfiguration();
        setupLCUMonitor();
        updateUI();
        updateTexts();
        
        // 初始化状态徽章
        updateStatusBadges();
        
        // Listen for language changes
        languageManager.currentLocaleProperty().addListener((obs, oldVal, newVal) -> {
            updateTexts();
            updateStatusBadges(); // 语言改变时也要更新状态徽章
        });
        
        // Initialize UI
        initializeUI();
    }
    
    private void initializeUI() {
        // Optimize scroll speed
        optimizeScrollSpeed();
    }
    
    private void optimizeScrollSpeed() {
        if (contentScrollPane != null) {
            logger.debug("Setting up scroll speed optimization for main UI");
            
            // Set much faster scroll speed with fixed pixel increment
            contentScrollPane.setOnScroll(event -> {
                if (event.getDeltaY() != 0) {
                    // Use fixed pixel increment for more predictable scrolling
                    double scrollPixels = event.getDeltaY() > 0 ? -120 : 120; // 120 pixels per scroll
                    double contentHeight = contentScrollPane.getContent().getBoundsInLocal().getHeight();
                    double viewportHeight = contentScrollPane.getViewportBounds().getHeight();
                    
                    if (contentHeight > viewportHeight) {
                        double currentVvalue = contentScrollPane.getVvalue();
                        double scrollableHeight = contentHeight - viewportHeight;
                        double scrollAmount = scrollPixels / scrollableHeight;
                        double newVvalue = currentVvalue + scrollAmount;
                        
                        // Clamp to valid range [0, 1]
                        newVvalue = Math.max(0, Math.min(1, newVvalue));
                        
                        logger.trace("Scroll event: deltaY={}, newVvalue={}", event.getDeltaY(), newVvalue);
                        contentScrollPane.setVvalue(newVvalue);
                    }
                    
                    event.consume();
                }
            });
            
            logger.info("Scroll speed optimization configured for main UI");
        } else {
            logger.warn("contentScrollPane is null, cannot optimize scroll speed");
        }
    }
    
    private void initializeComponents() {
        // 设置检查间隔spinner (秒为单位，范围1-60秒，默认1秒，步长1秒)
        checkIntervalSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, 1, 1));
        
        // 设置Ban延迟的spinner (1-30秒，默认25秒)
        if (simpleBanDelaySpinner != null) {
            simpleBanDelaySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 25, 1));
        }
        
        // 设置Pick延迟的spinner (1-30秒，默认25秒)
        if (simplePickDelaySpinner != null) {
            simplePickDelaySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 25, 1));
        }
        
        // 初始化分路下拉框
        initializePositionComboBox();
        
        // 初始状态
        updateConnectionStatus(false);
        updateGamePhase(GamePhase.NONE);
        
        
        appendStatus("应用程序已启动...");
        
        // 设置自动连接
        if (config != null && config.isAutoConnectEnabled()) {
            Platform.runLater(() -> {
                Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
                    if (!lcuMonitor.isConnected()) {
                        appendStatus(languageManager.getString("status.connecting"));
                        attemptAutoConnection();
                    }
                }));
                resourceManager.registerTimeline(timeline);
                timeline.play();
            });
        } else {
            appendStatus("等待手动连接到游戏客户端...");
        }
    }
    
    private void initializePositionComboBox() {
        if (positionComboBox != null) {
            positionComboBox.getItems().addAll(
                "default",  // 默认设置
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
                    // 检查中文翻译
                    if (string.equals(languageManager.getString("position.global"))) return "default";
                    if (string.equals(languageManager.getString("position.top"))) return "top";
                    if (string.equals(languageManager.getString("position.jungle"))) return "jungle";
                    if (string.equals(languageManager.getString("position.middle"))) return "middle";
                    if (string.equals(languageManager.getString("position.bottom"))) return "bottom";
                    if (string.equals(languageManager.getString("position.utility"))) return "utility";
                    return string;
                }
            });
            
            // 默认选择默认设置
            positionComboBox.setValue("default");
        }
    }
    
    private void loadConfiguration() {
        config = AutoAcceptConfig.load();
        
        // 验证和修复延迟配置
        if (config.getChampionSelect() != null) {
            if (!config.getChampionSelect().validateDelayConfiguration()) {
                logger.warn("Invalid delay configuration detected, applying fixes...");
                config.getChampionSelect().fixDelayConfiguration();
                saveConfiguration(); // 保存修复后的配置
            }
            
            logger.info("Delay configuration validated - Ban: {}s, Pick: {}s", 
                       config.getChampionSelect().getSimpleBanDelaySeconds(),
                       config.getChampionSelect().getSimplePickDelaySeconds());
        }
        
        // 应用配置到UI
        autoAcceptCheckBox.setSelected(config.isAutoAcceptEnabled());
        // 将毫秒转换为秒显示给用户
        checkIntervalSpinner.getValueFactory().setValue(config.getCheckIntervalMs() / 1000);
        
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
        if (forceEnglishTrayMenuCheckBox != null) {
            forceEnglishTrayMenuCheckBox.setSelected(config.isForceEnglishTrayMenu());
        }
        
        // Position presets settings - auto-enabled by default
        
        // Delay settings
        if (simpleBanDelaySpinner != null) {
            simpleBanDelaySpinner.getValueFactory().setValue(config.getChampionSelect().getSimpleBanDelaySeconds());
        }
        if (simplePickDelaySpinner != null) {
            simplePickDelaySpinner.getValueFactory().setValue(config.getChampionSelect().getSimplePickDelaySeconds());
        }
        
        // Smart features settings
        if (autoHoverCheckBox != null) {
            autoHoverCheckBox.setSelected(config.getChampionSelect().isAutoHoverEnabled());
        }
        if (smartBanCheckBox != null) {
            smartBanCheckBox.setSelected(config.getChampionSelect().isSmartBanEnabled());
        }
        
        updateAutoStartStatus();
        updatePositionPresetsUI();
        // Spinners are always enabled now
        
        // 更新队列状态显示
        updateQueueStatusDisplay();
    }
    
    /**
     * 更新队列状态显示
     */
    private void updateQueueStatusDisplay() {
        String userSelectedPosition = getUserSelectedPosition();
        
        if (config.getChampionSelect().isUsePositionBasedSelection() && userSelectedPosition != null) {
            AutoAcceptConfig.PositionConfig positionConfig = config.getChampionSelect().getPositionConfig(userSelectedPosition);
            if (positionConfig != null) {
                // 显示ban队列状态
                updateBanQueueDisplay(positionConfig);
                // 显示pick队列状态
                updatePickQueueDisplay(positionConfig);
                return;
            }
        }
        
        // 如果没有分路配置，隐藏队列状态显示
        banQueueStatus.setVisible(false);
        pickQueueStatus.setVisible(false);
    }
    
    /**
     * 更新ban队列显示
     */
    private void updateBanQueueDisplay(AutoAcceptConfig.PositionConfig positionConfig) {
        if (banQueuePreview == null || banQueueLabel == null || banQueueStatus == null) {
            return;
        }
        
        List<AutoAcceptConfig.ChampionInfo> banChampions = positionConfig.getBanChampions();
        if (banChampions != null && !banChampions.isEmpty()) {
            banQueueStatus.setVisible(true);
            banQueueLabel.setText(languageManager.getString("queue.ban") + " (" + banChampions.size() + "/5): ");
            
            // 清除旧的预览项
            banQueuePreview.getChildren().clear();
            
            // 获取当前可用的英雄位置（模拟已ban状态）
            Set<Integer> simulatedBannedChampions = new HashSet<>();
            // 这里可以添加实际的已ban英雄，但为了演示我们暂时使用空集合
            
            int currentActivePosition = -1;
            for (int i = 0; i < banChampions.size(); i++) {
                AutoAcceptConfig.ChampionInfo champion = banChampions.get(i);
                if (champion.getChampionId() != null && !simulatedBannedChampions.contains(champion.getChampionId()) && currentActivePosition == -1) {
                    currentActivePosition = i;
                }
            }
            
            // 添加队列预览项（使用英雄头像）
            for (int i = 0; i < banChampions.size(); i++) {
                AutoAcceptConfig.ChampionInfo champion = banChampions.get(i);
                
                // 创建英雄头像
                ImageView championAvatar = new ImageView();
                championAvatar.setFitWidth(24);
                championAvatar.setFitHeight(24);
                championAvatar.setPreserveRatio(true);
                championAvatar.getStyleClass().add("queue-preview-avatar");
                
                // 加载英雄头像
                loadChampionAvatar(championAvatar, champion.getKey());
                
                // 设置Tooltip显示英雄信息
                Tooltip tooltip = new Tooltip((i + 1) + ". " + champion.getNameCn());
                Tooltip.install(championAvatar, tooltip);
                
                if (i == currentActivePosition) {
                    // 当前将被选择的英雄高亮显示（添加边框）
                    championAvatar.setStyle("-fx-effect: dropshadow(gaussian, #4CAF50, 2, 0.8, 0, 0); -fx-border-color: #4CAF50; -fx-border-width: 2px; -fx-border-radius: 3px;");
                } else {
                    championAvatar.setStyle("-fx-opacity: 0.7;");
                }
                
                banQueuePreview.getChildren().add(championAvatar);
            }
        } else {
            banQueueStatus.setVisible(false);
        }
    }
    
    /**
     * 更新pick队列显示
     */
    private void updatePickQueueDisplay(AutoAcceptConfig.PositionConfig positionConfig) {
        if (pickQueuePreview == null || pickQueueLabel == null || pickQueueStatus == null) {
            return;
        }
        
        List<AutoAcceptConfig.ChampionInfo> pickChampions = positionConfig.getPickChampions();
        if (pickChampions != null && !pickChampions.isEmpty()) {
            pickQueueStatus.setVisible(true);
            pickQueueLabel.setText(languageManager.getString("queue.pick") + " (" + pickChampions.size() + "/5): ");
            
            // 清除旧的预览项
            pickQueuePreview.getChildren().clear();
            
            // 获取当前可用的英雄位置（模拟已ban/pick状态）
            Set<Integer> simulatedBannedChampions = new HashSet<>();
            Set<Integer> simulatedPickedChampions = new HashSet<>();
            // 这里可以添加实际的已ban/pick英雄，但为了演示我们暂时使用空集合
            
            int currentActivePosition = -1;
            for (int i = 0; i < pickChampions.size(); i++) {
                AutoAcceptConfig.ChampionInfo champion = pickChampions.get(i);
                if (champion.getChampionId() != null && 
                    !simulatedBannedChampions.contains(champion.getChampionId()) &&
                    !simulatedPickedChampions.contains(champion.getChampionId()) && 
                    currentActivePosition == -1) {
                    currentActivePosition = i;
                }
            }
            
            // 添加队列预览项（使用英雄头像）
            for (int i = 0; i < pickChampions.size(); i++) {
                AutoAcceptConfig.ChampionInfo champion = pickChampions.get(i);
                
                // 创建英雄头像
                ImageView championAvatar = new ImageView();
                championAvatar.setFitWidth(24);
                championAvatar.setFitHeight(24);
                championAvatar.setPreserveRatio(true);
                championAvatar.getStyleClass().add("queue-preview-avatar");
                
                // 加载英雄头像
                loadChampionAvatar(championAvatar, champion.getKey());
                
                // 设置Tooltip显示英雄信息
                Tooltip tooltip = new Tooltip((i + 1) + ". " + champion.getNameCn());
                Tooltip.install(championAvatar, tooltip);
                
                if (i == currentActivePosition) {
                    // 当前将被选择的英雄高亮显示（添加蓝色边框）
                    championAvatar.setStyle("-fx-effect: dropshadow(gaussian, #2196F3, 2, 0.8, 0, 0); -fx-border-color: #2196F3; -fx-border-width: 2px; -fx-border-radius: 3px;");
                } else {
                    championAvatar.setStyle("-fx-opacity: 0.7;");
                }
                
                pickQueuePreview.getChildren().add(championAvatar);
            }
        } else {
            pickQueueStatus.setVisible(false);
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
    
    
    private void setupLCUMonitor() {
        lcuMonitor = new LCUMonitor();
        
        // 设置回调
        lcuMonitor.setOnConnectionChanged(this::updateConnectionStatus);
        lcuMonitor.setOnPhaseChanged(this::updateGamePhase);
        lcuMonitor.setOnReadyCheckChanged(this::handleReadyCheckChanged);
        lcuMonitor.setOnChampSelectSessionChanged(this::handleChampSelectSessionChanged);
    }
    
    
    @FXML
    private void onToggleStatusClicked() {
        showLogViewerDialog();
    }
    
    /**
     * 显示日志查看器弹窗
     */
    private void showLogViewerDialog() {
        try {
            // 如果弹窗已存在且显示中，直接激活
            if (logViewerStage != null && logViewerStage.isShowing()) {
                logViewerStage.toFront();
                logViewerStage.requestFocus();
                return;
            }
            
            // 加载FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LogViewerDialog.fxml"));
            loader.setResources(languageManager.getResourceBundle());
            VBox content = loader.load();
            
            // 创建新的Stage
            logViewerStage = new Stage();
            logViewerStage.setTitle(languageManager.getString("dialog.logViewer.title") + " - LOL Helper");
            logViewerStage.setScene(new Scene(content));
            logViewerStage.setWidth(900);
            logViewerStage.setHeight(600);
            logViewerStage.setMinWidth(600);
            logViewerStage.setMinHeight(400);
            
            // 设置图标
            try {
                logViewerStage.getIcons().add(new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/icon/app-icon.png")));
            } catch (Exception e) {
                logger.debug("Could not load window icon", e);
            }
            
            // 设置弹窗位置（相对于主窗口）
            Stage primaryStage = (Stage) toggleStatusButton.getScene().getWindow();
            if (primaryStage != null) {
                logViewerStage.setX(primaryStage.getX() + 50);
                logViewerStage.setY(primaryStage.getY() + 50);
            }
            
            // 获取控制器并设置引用
            logViewerController = loader.getController();
            logViewerController.setExternalLogTextArea(statusTextArea);
            logViewerController.setLanguageManager(languageManager);
            
            // 显示弹窗
            logViewerStage.show();
            
            logger.debug("Log viewer dialog opened successfully");
            
        } catch (IOException | RuntimeException e) {
            logger.error("Failed to open log viewer dialog", e);
            showAlert(languageManager.getString("error.title"), languageManager.getString("error.openLogViewer") + ": " + e.getMessage());
        }
    }
    
    /**
     * 向日志弹窗发送新的日志消息
     */
    private void notifyLogViewerDialog(String message) {
        if (logViewerController != null) {
            logViewerController.appendLogMessage(message);
        }
    }
    
    /**
     * 显示错误对话框
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
            resourceManager.unregisterTimeline(autoReconnectTimeline);
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
                    disconnectButton.setDisable(false);
                    
                    // 停止重连任务
                    if (autoReconnectTimeline != null) {
                        autoReconnectTimeline.stop();
                        resourceManager.unregisterTimeline(autoReconnectTimeline);
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
        
        autoReconnectTimeline = new Timeline(new KeyFrame(Duration.seconds(intervalSeconds), event -> {
            if (!lcuMonitor.isConnected()) {
                attemptAutoConnection();
            } else {
                // 已连接，停止重连任务
                if (autoReconnectTimeline != null) {
                    autoReconnectTimeline.stop();
                    resourceManager.unregisterTimeline(autoReconnectTimeline);
                    autoReconnectTimeline = null;
                }
            }
        }));
        autoReconnectTimeline.setCycleCount(Timeline.INDEFINITE);
        resourceManager.registerTimeline(autoReconnectTimeline);
        autoReconnectTimeline.play();
        
        appendStatus("已启动自动重连，每" + intervalSeconds + "秒检测一次");
    }
    
    @FXML
    private void onAutoAcceptToggled() {
        config.setAutoAcceptEnabled(autoAcceptCheckBox.isSelected());
        saveConfiguration();
        
        String statusKey = autoAcceptCheckBox.isSelected() ? "status.autoAcceptEnabled" : "status.autoAcceptDisabled";
        appendStatus(languageManager.getString(statusKey));
        
        // 更新状态徽章
        updateStatusBadges();
    }
    
    @FXML
    private void onAutoBanToggled() {
        config.getChampionSelect().setAutoBanEnabled(autoBanCheckBox.isSelected());
        saveConfiguration();
        
        String statusKey = autoBanCheckBox.isSelected() ? "status.autoBanEnabled" : "status.autoBanDisabled";
        appendStatus(languageManager.getString(statusKey));
        
        // 更新状态徽章
        updateStatusBadges();
    }
    
    @FXML
    private void onAutoPickToggled() {
        config.getChampionSelect().setAutoPickEnabled(autoPickCheckBox.isSelected());
        saveConfiguration();
        
        String statusKey = autoPickCheckBox.isSelected() ? "status.autoPickEnabled" : "status.autoPickDisabled";
        appendStatus(languageManager.getString(statusKey));
        
        // 更新状态徽章
        updateStatusBadges();
    }
    
    /**
     * 更新所有状态徽章的显示
     */
    private void updateStatusBadges() {
        if (config == null || languageManager == null) {
            return;
        }
        
        // 更新自动接受状态徽章
        updateStatusBadge(autoAcceptStatusBadge, config.isAutoAcceptEnabled());
        
        // 更新自动Ban状态徽章
        updateStatusBadge(autoBanStatusBadge, config.getChampionSelect().isAutoBanEnabled());
        
        // 更新自动Pick状态徽章
        updateStatusBadge(autoPickStatusBadge, config.getChampionSelect().isAutoPickEnabled());
    }
    
    /**
     * 更新单个状态徽章
     */
    private void updateStatusBadge(Label badge, boolean isEnabled) {
        if (badge == null) {
            return;
        }
        
        // 更新文本
        String statusKey = isEnabled ? "settings.enabled" : "settings.disabled";
        badge.setText(languageManager.getString(statusKey));
        
        // 更新样式类
        badge.getStyleClass().removeAll("success", "error", "warning", "info");
        badge.getStyleClass().add(isEnabled ? "success" : "error");
    }
    
    
    @FXML
    private void onAutoHoverToggled() {
        if (autoHoverCheckBox != null && config != null) {
            boolean enabled = autoHoverCheckBox.isSelected();
            config.getChampionSelect().setAutoHoverEnabled(enabled);
            saveConfiguration();
            
            String status = enabled ? "启用" : "禁用";
            appendStatus("自动预选英雄已" + status);
        }
    }
    
    @FXML
    private void onSmartBanToggled() {
        if (smartBanCheckBox != null && config != null) {
            boolean enabled = smartBanCheckBox.isSelected();
            config.getChampionSelect().setSmartBanEnabled(enabled);
            saveConfiguration();
            
            String status = enabled ? "启用" : "禁用";
            appendStatus("智能禁用功能已" + status);
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
                actionStatusMap.clear();
                actionRetryCount.clear();
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
                            // 显示成功通知
                            if (systemTrayManager != null) {
                                systemTrayManager.showInfo("LOL助手", "成功自动接受对局");
                            }
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
                actionStatusMap.clear();
                actionRetryCount.clear();
                lastSessionId = currentSessionId;
                
                // 获取玩家位置，在位置确认后再处理hover，设置3秒超时
                updatePlayerPosition()
                    .orTimeout(3, TimeUnit.SECONDS)
                    .thenRun(() -> {
                        // 在分路信息确认后再处理自动预选功能
                        if (config.getChampionSelect().isAutoHoverEnabled()) {
                            logger.info("Position confirmed, now handling auto hover for position: {}", currentPlayerPosition);
                            handleAutoHover(session);
                        }
                    })
                    .exceptionally(throwable -> {
                        // 超时或其他错误时的处理
                        logger.warn("Failed to get position within timeout, proceeding with hover using default settings", throwable);
                        if (config.getChampionSelect().isAutoHoverEnabled()) {
                            Platform.runLater(() -> {
                                appendStatus("分路获取超时，使用默认设置进行预选");
                                handleAutoHover(session);
                            });
                        }
                        return null;
                    });
            }
            
            // 获取当前召唤师ID
            JsonNode localPlayerCell = session.path("localPlayerCellId");
            if (localPlayerCell.isMissingNode()) {
                return;
            }
            int localCellId = localPlayerCell.asInt();
            
            // 使用新的Draft Pick引擎分析session
            DraftPickEngine.DraftAnalysis draftAnalysis = draftPickEngine.analyzeDraftSession(session, localCellId);
            if (draftAnalysis != null) {
                logger.debug("Draft分析结果: {}", draftAnalysis);
                
                // 如果有当前玩家的action，使用增强的处理逻辑
                if (draftAnalysis.getCurrentPlayerAction() != null) {
                    DraftPickEngine.DraftAction playerAction = draftAnalysis.getCurrentPlayerAction();
                    int actionId = playerAction.getActionId();
                    
                    if (canProcessAction(actionId)) {
                        markActionProcessing(actionId); // 立即标记为处理中，防止重复
                        
                        if ("ban".equals(playerAction.getType()) && config.getChampionSelect().isAutoBanEnabled()) {
                            handleEnhancedAutoBan(actionId, draftAnalysis);
                        } else if ("pick".equals(playerAction.getType()) && config.getChampionSelect().isAutoPickEnabled()) {
                            handleEnhancedAutoPick(actionId, draftAnalysis);
                        }
                    }
                    return; // 使用了增强逻辑，直接返回
                }
            }
            
            // 回退到原有逻辑（兼容性保证）
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
                            
                            // 处理ban和pick操作的条件不同
                            if (actorCellId == localCellId && isInProgress && !completed && !processedActions.contains(actionId)) {
                                
                                logger.debug("Processing action using legacy logic - ID: {}, Type: {}, ChampionId: {}", 
                                           actionId, type, championId);
                                
                                if ("ban".equals(type) && config.getChampionSelect().isAutoBanEnabled()) {
                                    processedActions.add(actionId);
                                    handleAutoBan(actionId);
                                } else if ("pick".equals(type) && config.getChampionSelect().isAutoPickEnabled() && championId == 0) {
                                    processedActions.add(actionId);
                                    handleAutoPick(actionId);
                                }
                            }
                        }
                    }
                }
            }
        });
    }
    
    /**
     * 获取用户手动选择的分路位置
     */
    private String getUserSelectedPosition() {
        if (positionComboBox != null && positionComboBox.getValue() != null) {
            String selectedPosition = positionComboBox.getValue();
            logger.debug("User selected position from UI: {}", selectedPosition);
            return selectedPosition;
        }
        return null;
    }
    
    /**
     * 检查action是否可以处理
     */
    private boolean canProcessAction(int actionId) {
        ActionStatus status = actionStatusMap.get(actionId);
        if (status == null) {
            return true; // 新的action可以处理
        }
        
        return switch (status) {
            case PROCESSING -> {
                logger.debug("Action {} is already being processed", actionId);
                yield false;
            }
            case SUCCESS -> {
                logger.debug("Action {} was already completed successfully", actionId);
                yield false;
            }
            case RETRY_LIMIT -> {
                logger.warn("Action {} has reached retry limit", actionId);
                yield false;
            }
            case FAILED -> {
                int retryCount = actionRetryCount.getOrDefault(actionId, 0);
                if (retryCount >= MAX_RETRY_COUNT) {
                    actionStatusMap.put(actionId, ActionStatus.RETRY_LIMIT);
                    logger.warn("Action {} reached maximum retry count: {}", actionId, retryCount);
                    yield false;
                } else {
                    logger.info("Action {} can be retried (attempt {} of {})", actionId, retryCount + 1, MAX_RETRY_COUNT);
                    yield true;
                }
            }
            default -> true;
        };
    }
    
    /**
     * 标记action开始处理
     */
    private void markActionProcessing(int actionId) {
        actionStatusMap.put(actionId, ActionStatus.PROCESSING);
        processedActions.add(actionId);
        logger.debug("Marked action {} as PROCESSING", actionId);
    }
    
    /**
     * 标记action成功完成
     */
    private void markActionSuccess(int actionId) {
        actionStatusMap.put(actionId, ActionStatus.SUCCESS);
        actionRetryCount.remove(actionId); // 清除重试计数
        logger.debug("Marked action {} as SUCCESS", actionId);
    }
    
    /**
     * 标记action失败，允许重试
     */
    private void markActionFailed(int actionId, String reason) {
        int currentRetryCount = actionRetryCount.getOrDefault(actionId, 0);
        actionRetryCount.put(actionId, currentRetryCount + 1);
        
        if (currentRetryCount + 1 >= MAX_RETRY_COUNT) {
            actionStatusMap.put(actionId, ActionStatus.RETRY_LIMIT);
            logger.warn("Action {} failed and reached retry limit. Reason: {}", actionId, reason);
        } else {
            actionStatusMap.put(actionId, ActionStatus.FAILED);
            markActionFailed(actionId, "Action execution failed");
            logger.warn("Action {} failed (attempt {} of {}). Reason: {}", actionId, currentRetryCount + 1, MAX_RETRY_COUNT, reason);
        }
    }

    

    /**
     * 记录详细的pick决策过程
     */
    private void logPickDecisionProcess(String step, String details, int actionId) {
        String logMessage = String.format("[PICK_DECISION][Action:%d][%s] %s", actionId, step, details);
        logger.info(logMessage);
        
        // 同时输出到用户界面，方便调试
        Platform.runLater(() -> {
            appendStatus("🔍 " + step + ": " + details);
        });
    }
    
    /**
     * 记录pick配置状态
     */
    private void logPickConfigurationStatus(int actionId) {
        logPickDecisionProcess("CONFIG_CHECK", "检查pick配置状态", actionId);
        
        if (config == null) {
            logPickDecisionProcess("CONFIG_ERROR", "配置对象为null", actionId);
            return;
        }
        
        AutoAcceptConfig.ChampionSelectConfig championSelect = config.getChampionSelect();
        if (championSelect == null) {
            logPickDecisionProcess("CONFIG_ERROR", "英雄选择配置为null", actionId);
            return;
        }
        
        logPickDecisionProcess("CONFIG_STATUS", 
            String.format("AutoPick: %s, PositionBased: %s, SmartTiming: %s", 
                championSelect.isAutoPickEnabled(),
                championSelect.isUsePositionBasedSelection(),
                championSelect.isSmartTimingEnabled()), actionId);
                
        logPickDecisionProcess("POSITION_INFO", 
            String.format("Current: %s, User: %s", 
                currentPlayerPosition != null ? currentPlayerPosition : "null",
                getUserSelectedPosition() != null ? getUserSelectedPosition() : "null"), actionId);
                
        logPickDecisionProcess("TIMING_CONFIG", 
            String.format("SimpleDelay: %s (%ds), SmartTiming: %s", 
                championSelect.isUseSimpleDelayPick(),
                championSelect.getSimplePickDelaySeconds(),
                championSelect.isSmartTimingEnabled()), actionId);
    }

    /**
     * 根据已ban和已pick英雄列表选择可用的pick英雄
     */
    private AutoAcceptConfig.ChampionInfo selectAvailablePickChampion(AutoAcceptConfig.ChampionInfo defaultPickChampion, Set<Integer> bannedChampions, Set<Integer> pickedChampions) {
        logger.info("[PICK_SELECT] Starting champion selection - defaultPickChampion: {}, bannedChampions: {}, pickedChampions: {}", 
                   defaultPickChampion != null ? defaultPickChampion.toString() : "null", 
                   bannedChampions != null ? bannedChampions : "null", 
                   pickedChampions != null ? pickedChampions : "null");
        
        // 基本验证
        if (config == null || config.getChampionSelect() == null) {
            logger.error("[PICK_SELECT] Config or champion select config is null");
            return null;
        }
        
        boolean usePositionBased = config.getChampionSelect().isUsePositionBasedSelection();
        logger.info("[PICK_SELECT] Configuration - usePositionBasedSelection: {}, currentPlayerPosition: {}", 
                   usePositionBased, currentPlayerPosition);
        
        // 确保集合不为null
        if (bannedChampions == null) bannedChampions = new HashSet<>();
        if (pickedChampions == null) pickedChampions = new HashSet<>();
        
        // 获取用户手动选择的分路作为备用
        String userSelectedPosition = null;
        try {
            userSelectedPosition = getUserSelectedPosition();
            logger.info("[PICK_SELECT] userSelectedPosition: {}", userSelectedPosition);
        } catch (Exception e) {
            logger.warn("[PICK_SELECT] Failed to get user selected position", e);
        }
        
        // 优先级1：如果启用了分路预设，优先从LCU API检测的位置选择英雄
        if (usePositionBased && currentPlayerPosition != null && !currentPlayerPosition.trim().isEmpty()) {
            logger.debug("[PICK_SELECT] Priority 1: Trying LCU detected position: {}", currentPlayerPosition);
            try {
                AutoAcceptConfig.PositionConfig positionConfig = config.getChampionSelect().getPositionConfig(currentPlayerPosition);
                logger.debug("[PICK_SELECT] Position config for {}: {}", currentPlayerPosition, positionConfig != null ? "found" : "null");
                
                if (positionConfig != null && positionConfig.getPickChampions() != null) {
                    logger.debug("[PICK_SELECT] Available pick champions in queue: {}", positionConfig.getPickChampions().size());
                    AutoAcceptConfig.ChampionInfo alternateChampion = positionConfig.getAlternatePickChampion(bannedChampions, pickedChampions);
                    logger.debug("[PICK_SELECT] getAlternatePickChampion returned: {}", alternateChampion);
                    
                    if (alternateChampion != null) {
                        try {
                            alternateChampion.ensureChampionId();
                            if (alternateChampion.getChampionId() != null) {
                                logger.info("[PICK_SELECT] Selected pick champion {} from LCU detected position {} (priority 1, skipping {} banned + {} picked)", 
                                           alternateChampion, currentPlayerPosition, bannedChampions.size(), pickedChampions.size());
                                return alternateChampion;
                            } else {
                                logger.warn("[PICK_SELECT] Champion {} has null ID after ensureChampionId", alternateChampion);
                            }
                        } catch (Exception e) {
                            logger.warn("[PICK_SELECT] Error ensuring champion ID for {}", alternateChampion, e);
                        }
                    }
                    logger.debug("[PICK_SELECT] No available champions in LCU detected position {} pick queue", currentPlayerPosition);
                } else {
                    logger.warn("[PICK_SELECT] No position config or empty pick champions for LCU detected position {}", currentPlayerPosition);
                }
            } catch (Exception e) {
                logger.error("[PICK_SELECT] Error processing LCU detected position {}", currentPlayerPosition, e);
            }
        } else {
            logger.debug("[PICK_SELECT] LCU position-based selection disabled or no current position from API");
        }
        
        // 优先级2：如果LCU API位置不可用，使用用户手动选择的分路
        if (usePositionBased && userSelectedPosition != null && !userSelectedPosition.trim().isEmpty()) {
            logger.debug("[PICK_SELECT] Priority 2: Trying user selected position: {}", userSelectedPosition);
            try {
                AutoAcceptConfig.PositionConfig positionConfig = config.getChampionSelect().getPositionConfig(userSelectedPosition);
                logger.debug("[PICK_SELECT] User selected position config for {}: {}", userSelectedPosition, positionConfig != null ? "found" : "null");
                
                if (positionConfig != null && positionConfig.getPickChampions() != null) {
                    logger.debug("[PICK_SELECT] Available pick champions in user selected queue: {}", positionConfig.getPickChampions().size());
                    AutoAcceptConfig.ChampionInfo alternateChampion = positionConfig.getAlternatePickChampion(bannedChampions, pickedChampions);
                    logger.debug("[PICK_SELECT] getAlternatePickChampion from user selected returned: {}", alternateChampion);
                    
                    if (alternateChampion != null) {
                        try {
                            alternateChampion.ensureChampionId();
                            if (alternateChampion.getChampionId() != null) {
                                logger.info("[PICK_SELECT] Selected pick champion {} from user selected position {} (priority 2, skipping {} banned + {} picked)", 
                                           alternateChampion, userSelectedPosition, bannedChampions.size(), pickedChampions.size());
                                return alternateChampion;
                            } else {
                                logger.warn("[PICK_SELECT] User selected champion {} has null ID after ensureChampionId", alternateChampion);
                            }
                        } catch (Exception e) {
                            logger.warn("[PICK_SELECT] Error ensuring champion ID for user selected {}", alternateChampion, e);
                        }
                    }
                    logger.debug("[PICK_SELECT] No available champions in user selected position {} pick queue", userSelectedPosition);
                } else {
                    logger.warn("[PICK_SELECT] No position config or empty pick champions for user selected position {}", userSelectedPosition);
                }
            } catch (Exception e) {
                logger.error("[PICK_SELECT] Error processing user selected position {}", userSelectedPosition, e);
            }
        } else {
            logger.debug("[PICK_SELECT] No user selected position available or position-based selection disabled");
        }
        
        // 优先级3：当前两个优先级都不可用时，尝试从所有分路配置中寻找可用英雄
        if (usePositionBased && (currentPlayerPosition == null || currentPlayerPosition.trim().isEmpty()) && 
            (userSelectedPosition == null || userSelectedPosition.trim().isEmpty())) {
            logger.debug("[PICK_SELECT] Priority 3: No specific position available, searching all position configs");
            
            try {
                Map<String, AutoAcceptConfig.PositionConfig> positionConfigs = config.getChampionSelect().getPositionConfigs();
                if (positionConfigs != null) {
                    // 遍历所有分路配置，寻找未被ban/pick的英雄
                    for (String position : positionConfigs.keySet()) {
                        if (position == null || position.trim().isEmpty()) continue;
                        
                        try {
                            AutoAcceptConfig.PositionConfig positionConfig = config.getChampionSelect().getPositionConfig(position);
                            if (positionConfig != null && positionConfig.getPickChampions() != null) {
                                AutoAcceptConfig.ChampionInfo alternateChampion = positionConfig.getAlternatePickChampion(bannedChampions, pickedChampions);
                                if (alternateChampion != null) {
                                    try {
                                        alternateChampion.ensureChampionId();
                                        if (alternateChampion.getChampionId() != null) {
                                            logger.info("[PICK_SELECT] Selected pick champion {} from {} position as fallback (priority 3, skipping {} banned + {} picked)", 
                                                       alternateChampion, position, bannedChampions.size(), pickedChampions.size());
                                            return alternateChampion;
                                        } else {
                                            logger.warn("[PICK_SELECT] Fallback champion {} has null ID after ensureChampionId", alternateChampion);
                                        }
                                    } catch (Exception e) {
                                        logger.warn("[PICK_SELECT] Error ensuring champion ID for fallback {}", alternateChampion, e);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("[PICK_SELECT] Error processing position config for {}", position, e);
                        }
                    }
                }
                logger.debug("[PICK_SELECT] No available champions found in any position config, trying global default");
            } catch (Exception e) {
                logger.error("[PICK_SELECT] Error searching all position configs", e);
            }
        }
        
        // 优先级4：回退到默认英雄（如果未被ban/pick）
        logger.debug("[PICK_SELECT] Priority 4: Checking default pick champion: {}", defaultPickChampion);
        if (defaultPickChampion != null) {
            try {
                // 确保默认英雄的championId有效
                defaultPickChampion.ensureChampionId();
                
                if (defaultPickChampion.getChampionId() != null &&
                    !bannedChampions.contains(defaultPickChampion.getChampionId()) &&
                    !pickedChampions.contains(defaultPickChampion.getChampionId())) {
                    logger.info("[PICK_SELECT] Using fallback default pick champion {} (priority 4)", defaultPickChampion);
                    return defaultPickChampion;
                } else {
                    logger.debug("[PICK_SELECT] Default pick champion {} is banned, picked, or has null ID (ID: {}, banned: {}, picked: {})", 
                               defaultPickChampion, defaultPickChampion.getChampionId(),
                               defaultPickChampion.getChampionId() != null && bannedChampions.contains(defaultPickChampion.getChampionId()),
                               defaultPickChampion.getChampionId() != null && pickedChampions.contains(defaultPickChampion.getChampionId()));
                }
            } catch (Exception e) {
                logger.warn("[PICK_SELECT] Error checking default pick champion", e);
            }
        } else {
            logger.debug("[PICK_SELECT] No default pick champion available");
        }
        
        // 优先级5：所有选项都不可用
        logger.warn("[PICK_SELECT] No available pick champion found - all options exhausted (banned: {}, picked: {})", 
                   bannedChampions.size(), pickedChampions.size());
        logger.debug("[PICK_SELECT] Banned champions: {}", bannedChampions);
        logger.debug("[PICK_SELECT] Picked champions: {}", pickedChampions);
        return null;
    }
    
    /**
     * 根据已ban英雄列表选择可用的ban英雄
     */
    private AutoAcceptConfig.ChampionInfo selectAvailableBanChampion(AutoAcceptConfig.ChampionInfo defaultBanChampion, Set<Integer> bannedChampions) {
        logger.info("selectAvailableBanChampion: defaultBanChampion = {}, bannedChampions = {}", defaultBanChampion, bannedChampions);
        logger.info("selectAvailableBanChampion: usePositionBasedSelection = {}, currentPlayerPosition = {}", 
                   config.getChampionSelect().isUsePositionBasedSelection(), currentPlayerPosition);
        
        // 智能禁用功能：获取队友预选的英雄，避免禁用它们
        Set<Integer> excludedChampions = new HashSet<>(bannedChampions);
        if (config.getChampionSelect().isSmartBanEnabled()) {
            try {
                Set<Integer> teammateHoveredChampions = lcuMonitor.getTeammateHoveredChampions().get();
                if (!teammateHoveredChampions.isEmpty()) {
                    excludedChampions.addAll(teammateHoveredChampions);
                    logger.info("Smart ban enabled - excluding teammate hovered champions: {}", teammateHoveredChampions);
                    // 更新状态信息
                    Platform.runLater(() -> {
                        if (teammateHoveredChampions.size() == 1) {
                            appendStatus("智能禁用：避免禁用队友预选的英雄");
                        } else if (teammateHoveredChampions.size() > 1) {
                            appendStatus("智能禁用：避免禁用" + teammateHoveredChampions.size() + "个队友预选的英雄");
                        }
                    });
                }
            } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                logger.warn("Failed to get teammate hovered champions for smart ban", e);
            }
        } else {
            logger.debug("Smart ban disabled - not checking teammate hovered champions");
        }
        
        // 获取用户手动选择的分路作为备用
        String userSelectedPosition = getUserSelectedPosition();
        logger.info("selectAvailableBanChampion: userSelectedPosition = {}", userSelectedPosition);
        
        // 优先级1：如果启用了分路预设，优先从LCU API检测的位置选择英雄
        if (config.getChampionSelect().isUsePositionBasedSelection() && currentPlayerPosition != null) {
            AutoAcceptConfig.PositionConfig positionConfig = config.getChampionSelect().getPositionConfig(currentPlayerPosition);
            logger.info("selectAvailableBanChampion: positionConfig for {} = {}", currentPlayerPosition, positionConfig);
            
            if (positionConfig != null) {
                logger.info("selectAvailableBanChampion: available ban champions in queue = {}", positionConfig.getBanChampions());
                AutoAcceptConfig.ChampionInfo alternateChampion = positionConfig.getAlternateBanChampion(excludedChampions);
                logger.info("selectAvailableBanChampion: getAlternateBanChampion returned = {}", alternateChampion);
                
                if (alternateChampion != null) {
                    alternateChampion.ensureChampionId();
                    logger.info("Selected ban champion {} from LCU detected position {} queue (priority 1, skipping {} excluded champions)", 
                               alternateChampion, currentPlayerPosition, excludedChampions.size());
                    return alternateChampion;
                }
                logger.debug("No available champions in LCU detected position {} ban queue, trying user selected position", currentPlayerPosition);
            } else {
                logger.warn("selectAvailableBanChampion: no position config found for LCU detected position {}", currentPlayerPosition);
            }
        } else {
            logger.info("selectAvailableBanChampion: LCU position-based selection disabled or no current position from API");
        }
        
        // 优先级2：如果LCU API位置不可用，使用用户手动选择的分路
        if (config.getChampionSelect().isUsePositionBasedSelection() && userSelectedPosition != null) {
            AutoAcceptConfig.PositionConfig positionConfig = config.getChampionSelect().getPositionConfig(userSelectedPosition);
            logger.info("selectAvailableBanChampion: user selected positionConfig for {} = {}", userSelectedPosition, positionConfig);
            
            if (positionConfig != null) {
                logger.info("selectAvailableBanChampion: available ban champions in user selected queue = {}", positionConfig.getBanChampions());
                AutoAcceptConfig.ChampionInfo alternateChampion = positionConfig.getAlternateBanChampion(excludedChampions);
                logger.info("selectAvailableBanChampion: getAlternateBanChampion from user selected returned = {}", alternateChampion);
                
                if (alternateChampion != null) {
                    alternateChampion.ensureChampionId();
                    logger.info("Selected ban champion {} from user selected position {} queue (priority 2, skipping {} excluded champions)", 
                               alternateChampion, userSelectedPosition, excludedChampions.size());
                    return alternateChampion;
                }
                logger.debug("No available champions in user selected position {} ban queue, trying fallback options", userSelectedPosition);
            } else {
                logger.warn("selectAvailableBanChampion: no position config found for user selected position {}", userSelectedPosition);
            }
        } else {
            logger.info("selectAvailableBanChampion: no user selected position available or position-based selection disabled");
        }
        
        // 优先级3：当前两个优先级都不可用时，尝试从所有分路配置中寻找可用英雄
        if (config.getChampionSelect().isUsePositionBasedSelection() && currentPlayerPosition == null && userSelectedPosition == null) {
            logger.info("No specific position available but position-based selection enabled, searching all position configs for available champion");
            
            // 遍历所有分路配置，寻找未被ban的英雄
            for (String position : config.getChampionSelect().getPositionConfigs().keySet()) {
                AutoAcceptConfig.PositionConfig positionConfig = config.getChampionSelect().getPositionConfig(position);
                if (positionConfig != null) {
                    AutoAcceptConfig.ChampionInfo alternateChampion = positionConfig.getAlternateBanChampion(excludedChampions);
                    if (alternateChampion != null) {
                        alternateChampion.ensureChampionId();
                        logger.info("Selected ban champion {} from {} position queue as fallback (priority 3, skipping {} excluded champions)", 
                                   alternateChampion, position, excludedChampions.size());
                        return alternateChampion;
                    }
                }
            }
            logger.debug("No available champions found in any position config, trying global default");
        }
        
        // 优先级4：回退到默认英雄（如果未被排除）
        if (defaultBanChampion != null && defaultBanChampion.getChampionId() != null &&
            !excludedChampions.contains(defaultBanChampion.getChampionId())) {
            logger.info("Using fallback default ban champion {} (priority 4)", defaultBanChampion);
            return defaultBanChampion;
        }
        
        // 优先级5：所有选项都不可用
        logger.warn("No available ban champion found - all position queues exhausted and default champion excluded (excluded champions: {})", excludedChampions);
        return null;
    }
    
    
    /**
     * 在队列中查找可用英雄
     */
    private QueueSelectionResult findAvailableChampionInQueue(List<AutoAcceptConfig.ChampionInfo> championQueue, Set<Integer> bannedChampions, String source) {
        if (championQueue == null || championQueue.isEmpty()) {
            return null;
        }
        
        for (int i = 0; i < championQueue.size(); i++) {
            AutoAcceptConfig.ChampionInfo champion = championQueue.get(i);
            if (champion.getChampionId() != null && !bannedChampions.contains(champion.getChampionId())) {
                return new QueueSelectionResult(champion, i, championQueue.size(), source);
            }
        }
        return null;
    }
    
    private void handleAutoBan(int actionId) {
        AutoAcceptConfig.ChampionInfo banChampion = config.getChampionSelect().getBanChampion();
        logger.info("handleAutoBan called - Action ID: {}, Ban champion from config: {}, Position-based selection enabled: {}", 
                   actionId, banChampion, config.getChampionSelect().isUsePositionBasedSelection());
        
        if (banChampion == null || banChampion.getChampionId() == null) {
            appendStatus("自动Ban失败：未设置Ban英雄或英雄ID无效");
            markActionFailed(actionId, "Ban champion not set or invalid champion ID");
            return;
        }
        
        // 如果启用了分路预设但当前位置为null，先尝试获取位置信息
        if (config.getChampionSelect().isUsePositionBasedSelection() && currentPlayerPosition == null) {
            logger.info("Position-based selection enabled but currentPlayerPosition is null, updating position first");
            // 尝试多次获取位置信息，因为在英雄选择初期位置可能还未分配
            tryGetPlayerPositionWithRetry(actionId, banChampion, 0);
        } else {
            // 直接执行ban逻辑
            proceedWithAutoBan(actionId, banChampion);
        }
    }
    
    /**
     * 带重试机制的位置获取
     */
    private void tryGetPlayerPositionWithRetry(int actionId, AutoAcceptConfig.ChampionInfo banChampion, int attempt) {
        if (attempt >= 3) {
            logger.warn("Failed to get player position after 3 attempts, proceeding with global config");
            proceedWithAutoBan(actionId, banChampion);
            return;
        }
        
        lcuMonitor.getPlayerPosition()
            .thenAccept(position -> {
                if (position != null && !position.trim().isEmpty()) {
                    currentPlayerPosition = position;
                    logger.info("Updated currentPlayerPosition to: {} (attempt {})", position, attempt + 1);
                    Platform.runLater(() -> {
                        appendStatus(languageManager.getString("queue.current") + ": " + translatePosition(position));
                        updatePositionStatusUI(position);
                    });
                    // 成功获取位置，继续执行ban逻辑
                    proceedWithAutoBan(actionId, banChampion);
                } else {
                    logger.debug("Position still empty/null, attempt {}/3", attempt + 1);
                    // 延迟后重试
                    if (attempt < 2) {
                        Platform.runLater(() -> {
                            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
                                tryGetPlayerPositionWithRetry(actionId, banChampion, attempt + 1);
                            }));
                            timeline.play();
                        });
                    } else {
                        logger.warn("Position still empty after {} attempts, proceeding with global config", attempt + 1);
                        proceedWithAutoBan(actionId, banChampion);
                    }
                }
            })
            .exceptionally(throwable -> {
                logger.warn("Failed to get player position (attempt {}), retrying...", attempt + 1, throwable);
                if (attempt < 2) {
                    Platform.runLater(() -> {
                        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
                            tryGetPlayerPositionWithRetry(actionId, banChampion, attempt + 1);
                        }));
                        timeline.play();
                    });
                } else {
                    logger.warn("Failed to get player position after {} attempts, proceeding with global config", attempt + 1);
                    proceedWithAutoBan(actionId, banChampion);
                }
                return null;
            });
    }
    
    /**
     * 继续执行自动ban逻辑
     */
    private void proceedWithAutoBan(int actionId, AutoAcceptConfig.ChampionInfo banChampion) {
        // 获取已ban英雄列表，用于智能选择可用英雄
        lcuMonitor.getBannedChampions()
            .thenAccept(bannedChampions -> {
                logger.info("Currently banned champions: {}", bannedChampions);
                
                // 使用延迟执行
                handleDelayBan(actionId, banChampion, bannedChampions);
            })
            .exceptionally(throwable -> {
                logger.error("Failed to get banned champions, proceeding with default ban", throwable);
                // 如果获取失败，仍然使用原有逻辑执行
                handleDelayBan(actionId, banChampion, new HashSet<>());
                return null;
            });
    }
    
    /**
     * 延迟执行Ban
     */
    private void handleDelayBan(int actionId, AutoAcceptConfig.ChampionInfo banChampion, Set<Integer> bannedChampions) {
        int delaySeconds = config.getChampionSelect().getSimpleBanDelaySeconds();
        
        // 先选择可用的ban英雄
        AutoAcceptConfig.ChampionInfo selectedBanChampion = selectAvailableBanChampion(banChampion, bannedChampions);
        if (selectedBanChampion == null) {
            appendStatus("✗ 自动Ban失败：没有可用的英雄（所有英雄已被ban）");
            markActionFailed(actionId, "Action validation failed");
            return;
        }
        
        logger.info("Using simple delay ban for action ID: {} with champion: {} (delay: {}s)", 
                   actionId, selectedBanChampion, delaySeconds);
        appendStatus("简单延迟Ban：" + selectedBanChampion.toString() + " (" + delaySeconds + "秒后执行)");
        
        // 使用JavaFX Timeline实现延迟执行
        Timeline delayTimeline = new Timeline(new KeyFrame(Duration.seconds(delaySeconds), event -> {
            // 延迟执行前再次获取最新的已ban英雄列表
            lcuMonitor.getBannedChampions()
                .thenAccept(currentBannedChampions -> {
                    // 再次选择可用英雄，确保延迟期间没有被其他人ban掉
                    AutoAcceptConfig.ChampionInfo finalBanChampion = selectAvailableBanChampion(selectedBanChampion, currentBannedChampions);
                    if (finalBanChampion == null) {
                        Platform.runLater(() -> {
                            appendStatus("✗ 延迟Ban失败：所有候选英雄已被ban");
                            logger.warn("All candidate champions have been banned during delay for action {}", actionId);
                        });
                        return;
                    }
                    
                    logger.info("Executing simple delay ban - Action ID: {}, Final champion: {}", actionId, finalBanChampion);
                    
                    lcuMonitor.banChampion(finalBanChampion.getChampionId(), actionId)
                        .thenAccept(success -> Platform.runLater(() -> {
                            if (success) {
                                appendStatus("✓ 延迟Ban成功：" + finalBanChampion.toString());
                                logger.info("Simple delay ban successful for action {}", actionId);
                                markActionSuccess(actionId);
                                // 显示成功通知
                                if (systemTrayManager != null) {
                                    systemTrayManager.showInfo("LOL助手", "成功Ban英雄：" + finalBanChampion.toString());
                                }
                            } else {
                                appendStatus("✗ 延迟Ban失败：" + finalBanChampion.toString());
                                logger.warn("Simple delay ban failed for action {}", actionId);
                            }
                        }))
                        .exceptionally(throwable -> {
                            Platform.runLater(() -> {
                                appendStatus("✗ 延迟Ban异常：" + throwable.getMessage());
                                logger.error("Exception during simple delay ban for action ID: " + actionId, throwable);
                            });
                            return null;
                        });
                })
                .exceptionally(throwable -> {
                    // 如果获取已ban英雄失败，仍然尝试ban原来选择的英雄
                    logger.error("Failed to get current banned champions, using previously selected champion", throwable);
                    lcuMonitor.banChampion(selectedBanChampion.getChampionId(), actionId)
                        .thenAccept(success -> Platform.runLater(() -> {
                            if (success) {
                                appendStatus("✓ 延迟Ban成功：" + selectedBanChampion.toString());
                                markActionSuccess(actionId);
                                // 显示成功通知
                                if (systemTrayManager != null) {
                                    systemTrayManager.showInfo("LOL助手", "成功Ban英雄：" + selectedBanChampion.toString());
                                }
                            } else {
                                appendStatus("✗ 延迟Ban失败：" + selectedBanChampion.toString());
                            }
                        }));
                    return null;
                });
        }));
        
        resourceManager.registerTimeline(delayTimeline);
        delayTimeline.play();
    }
    
    /**
     * 延迟执行Pick
     */
    private void handleDelayPick(int actionId, AutoAcceptConfig.ChampionInfo pickChampion) {
        int delaySeconds = config.getChampionSelect().getSimplePickDelaySeconds();
        
        // 验证延迟时间配置
        if (delaySeconds <= 0) {
            logger.warn("[AUTO_PICK] Invalid delay seconds: {}, using immediate pick", delaySeconds);
            performImmediatePick(actionId, pickChampion);
            return;
        }
        
        logger.info("[AUTO_PICK] Starting delay pick for action ID: {} with champion: {} (delay: {}s)", 
                   actionId, pickChampion, delaySeconds);
        
        Platform.runLater(() -> {
            appendStatus("⏰ 延迟Pick启动：" + pickChampion.toString() + " (将在" + delaySeconds + "秒后执行)");
        });
        
        // 创建倒计时Timeline，每秒更新一次显示
        Timeline countdownTimeline = createPickCountdownTimeline(actionId, pickChampion, delaySeconds);
        
        // 创建最终执行的Timeline
        Timeline delayTimeline = new Timeline(new KeyFrame(Duration.seconds(delaySeconds), event -> {
            logger.debug("[AUTO_PICK] Delay timer expired, executing pick for action ID: {}", actionId);
            
            // 检查LCU连接状态
            if (lcuMonitor == null || !lcuMonitor.isConnected()) {
                logger.error("[AUTO_PICK] LCU connection lost during delay for action ID: {}", actionId);
                Platform.runLater(() -> {
                    appendStatus("✗ 延迟Pick失败：LCU连接中断");
                });
                markActionFailed(actionId, "Action processing failed");
                return;
            }
            
            // 延迟执行前再次获取最新的已ban和已pick英雄列表
            logger.debug("[AUTO_PICK] Fetching updated champion status before delayed pick...");
            CompletableFuture<Set<Integer>> bannedChampionsFuture = lcuMonitor.getBannedChampions();
            CompletableFuture<Set<Integer>> pickedChampionsFuture = lcuMonitor.getPickedChampions();
            
            CompletableFuture.allOf(bannedChampionsFuture, pickedChampionsFuture)
                .thenAccept(v -> {
                    try {
                        Set<Integer> currentBannedChampions = bannedChampionsFuture.join();
                        Set<Integer> currentPickedChampions = pickedChampionsFuture.join();
                        
                        logger.debug("[AUTO_PICK] Updated champion status - Banned: {}, Picked: {}", 
                                   currentBannedChampions != null ? currentBannedChampions : "null",
                                   currentPickedChampions != null ? currentPickedChampions : "null");
                        
                        // 确保集合不为null
                        if (currentBannedChampions == null) currentBannedChampions = new HashSet<>();
                        if (currentPickedChampions == null) currentPickedChampions = new HashSet<>();
                        
                        // 再次选择可用英雄，确保延迟期间没有被其他人ban或pick掉
                        AutoAcceptConfig.ChampionInfo finalPickChampion = selectAvailablePickChampion(pickChampion, currentBannedChampions, currentPickedChampions);
                        if (finalPickChampion == null) {
                            logger.warn("[AUTO_PICK] All candidate champions have been banned or picked during delay for action {}", actionId);
                            Platform.runLater(() -> {
                                appendStatus("✗ 延迟Pick失败：所有候选英雄已被ban或pick");
                            });
                            markActionFailed(actionId, "Action processing failed");
                            return;
                        }
                        
                        logger.info("[AUTO_PICK] Final champion selected after delay: {} (ID: {})", 
                                   finalPickChampion, finalPickChampion.getChampionId());
                    
                        // 执行实际的pick操作
                        Platform.runLater(() -> {
                            appendStatus("⚡ 正在执行延迟Pick：" + finalPickChampion.toString());
                        });
                        
                        lcuMonitor.pickChampion(finalPickChampion.getChampionId(), actionId)
                            .thenAccept(success -> Platform.runLater(() -> {
                                if (success) {
                                    appendStatus("✓ 延迟Pick成功：" + finalPickChampion.toString());
                                    logger.info("[AUTO_PICK] Successfully picked champion after delay for action ID: {}", actionId);
                                    markActionSuccess(actionId);
                                    // 显示成功通知
                                    if (systemTrayManager != null) {
                                        systemTrayManager.showInfo("LOL助手", "成功Pick英雄：" + finalPickChampion.toString());
                                    }
                                } else {
                                    appendStatus("✗ 延迟Pick失败：" + finalPickChampion.toString());
                                    logger.warn("[AUTO_PICK] Failed to pick champion after delay for action ID: {}", actionId);
                                    markActionFailed(actionId, "Action execution failed");
                                }
                            }))
                            .exceptionally(throwable -> {
                                Platform.runLater(() -> {
                                    appendStatus("✗ 延迟Pick异常：" + throwable.getMessage());
                                    logger.error("[AUTO_PICK] Exception during delayed pick for action ID: " + actionId, throwable);
                                    markActionFailed(actionId, "Action execution exception: " + throwable.getMessage());
                                });
                                return null;
                            });
                    } catch (Exception e) {
                        logger.error("[AUTO_PICK] Error during delayed pick processing for action ID: " + actionId, e);
                        Platform.runLater(() -> {
                            appendStatus("✗ 延迟Pick处理错误：" + e.getMessage());
                        });
                        markActionFailed(actionId, "Action processing failed");
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("[AUTO_PICK] Failed to get champion status for delayed pick", throwable);
                    Platform.runLater(() -> {
                        appendStatus("✗ 获取英雄状态失败：" + throwable.getMessage());
                    });
                    markActionFailed(actionId, "Action processing failed");
                    return null;
                });
        }));
        
        // 启动倒计时显示和最终执行timeline
        resourceManager.registerTimeline(countdownTimeline);
        resourceManager.registerTimeline(delayTimeline);
        countdownTimeline.play();
        delayTimeline.play();
    }
    
    /**
     * 创建pick倒计时Timeline，每秒更新显示
     */
    private Timeline createPickCountdownTimeline(int actionId, AutoAcceptConfig.ChampionInfo pickChampion, int totalSeconds) {
        Timeline timeline = new Timeline();
        
        for (int i = 1; i <= totalSeconds; i++) {
            final int secondsLeft = totalSeconds - i;
            KeyFrame frame = new KeyFrame(Duration.seconds(i), event -> {
                if (secondsLeft > 0) {
                    Platform.runLater(() -> {
                        appendStatus("⏳ 等待Pick " + pickChampion.getNameCn() + " - 还有 " + secondsLeft + " 秒");
                    });
                    logger.debug("[AUTO_PICK] Pick countdown for action {}: {} seconds remaining", actionId, secondsLeft);
                } else {
                    Platform.runLater(() -> {
                        appendStatus("⚡ 延迟时间到，即将执行Pick: " + pickChampion.getNameCn());
                    });
                    logger.info("[AUTO_PICK] Pick countdown completed for action {}", actionId);
                }
            });
            timeline.getKeyFrames().add(frame);
        }
        
        return timeline;
    }
    
    /**
     * 执行立即Pick（当延迟时间无效时使用）
     */
    private void performImmediatePick(int actionId, AutoAcceptConfig.ChampionInfo pickChampion) {
        logger.info("[AUTO_PICK] Executing immediate pick for action ID: {} with champion: {}", actionId, pickChampion);
        
        Platform.runLater(() -> {
            appendStatus("⚡ 立即执行Pick：" + pickChampion.toString());
        });
        
        // 获取当前banned和picked状态
        CompletableFuture<Set<Integer>> bannedChampionsFuture = lcuMonitor.getBannedChampions();
        CompletableFuture<Set<Integer>> pickedChampionsFuture = lcuMonitor.getPickedChampions();
        
        CompletableFuture.allOf(bannedChampionsFuture, pickedChampionsFuture)
            .thenAccept(v -> {
                try {
                    Set<Integer> currentBannedChampions = bannedChampionsFuture.join();
                    Set<Integer> currentPickedChampions = pickedChampionsFuture.join();
                    
                    if (currentBannedChampions == null) currentBannedChampions = new HashSet<>();
                    if (currentPickedChampions == null) currentPickedChampions = new HashSet<>();
                    
                    AutoAcceptConfig.ChampionInfo finalPickChampion = selectAvailablePickChampion(pickChampion, currentBannedChampions, currentPickedChampions);
                    if (finalPickChampion == null) {
                        logger.warn("[AUTO_PICK] All candidate champions have been banned or picked for immediate pick action {}", actionId);
                        Platform.runLater(() -> {
                            appendStatus("✗ 立即Pick失败：所有候选英雄已被ban或pick");
                        });
                        markActionFailed(actionId, "Action processing failed");
                        return;
                    }
                    
                    // 执行pick操作
                    lcuMonitor.pickChampion(finalPickChampion.getChampionId(), actionId)
                        .thenAccept(success -> Platform.runLater(() -> {
                            if (success) {
                                appendStatus("✓ 立即Pick成功：" + finalPickChampion.toString());
                                logger.info("[AUTO_PICK] Successfully picked champion immediately for action ID: {}", actionId);
                                markActionSuccess(actionId);
                            } else {
                                appendStatus("✗ 立即Pick失败：" + finalPickChampion.toString());
                                logger.error("[AUTO_PICK] Failed to pick champion immediately for action ID: {}", actionId);
                                markActionFailed(actionId, "Action execution failed");
                            }
                        }))
                        .exceptionally(throwable -> {
                            Platform.runLater(() -> {
                                appendStatus("✗ 立即Pick异常：" + throwable.getMessage());
                                logger.error("[AUTO_PICK] Exception during immediate pick for action ID: " + actionId, throwable);
                                markActionFailed(actionId, "Action execution exception: " + throwable.getMessage());
                            });
                            return null;
                        });
                } catch (Exception e) {
                    logger.error("[AUTO_PICK] Error during immediate pick processing for action ID: " + actionId, e);
                    Platform.runLater(() -> {
                        appendStatus("✗ 立即Pick处理错误：" + e.getMessage());
                    });
                    markActionFailed(actionId, "Action processing failed");
                }
            })
            .exceptionally(throwable -> {
                logger.error("[AUTO_PICK] Failed to get champion status for immediate pick", throwable);
                Platform.runLater(() -> {
                    appendStatus("✗ 获取英雄状态失败：" + throwable.getMessage());
                });
                markActionFailed(actionId, "Action processing failed");
                return null;
            });
    }
    
    /**
     * Handle auto hover functionality when entering champion select
     */
    private void handleAutoHover(JsonNode session) {
        logger.info("handleAutoHover called - Auto hover enabled: {}", 
                   config.getChampionSelect().isAutoHoverEnabled());
        
        if (!config.getChampionSelect().isAutoHoverEnabled()) {
            return;
        }
        
        // Get local player's cell ID
        JsonNode localPlayerCell = session.path("localPlayerCellId");
        if (localPlayerCell.isMissingNode()) {
            logger.warn("Cannot get local player cell ID for auto hover");
            return;
        }
        int localCellId = localPlayerCell.asInt();
        
        // Find current player's pick action that is available for hovering
        JsonNode actions = session.path("actions");
        if (actions.isArray()) {
            for (JsonNode actionGroup : actions) {
                if (actionGroup.isArray()) {
                    for (JsonNode action : actionGroup) {
                        int actorCellId = action.path("actorCellId").asInt();
                        String type = action.path("type").asText("");
                        boolean completed = action.path("completed").asBoolean(false);
                        int championId = action.path("championId").asInt(0);
                        int actionId = action.path("id").asInt();
                        
                        // Look for pick actions that belong to the current player and haven't been completed
                        if (actorCellId == localCellId && "pick".equals(type) && !completed && championId == 0) {
                            logger.info("Found pick action for auto hover - Action ID: {}", actionId);
                            performAutoHover(actionId);
                            return; // Only hover for the first available pick action
                        }
                    }
                }
            }
        }
        
        logger.debug("No suitable pick action found for auto hover");
    }
    
    /**
     * Perform the actual auto hover operation
     */
    private void performAutoHover(int actionId) {
        // Get the champion to hover based on configuration
        AutoAcceptConfig.ChampionInfo hoverChampion = getHoverChampion();
        
        if (hoverChampion == null || hoverChampion.getChampionId() == null) {
            logger.warn("No hover champion configured or champion ID is null");
            appendStatus("自动预选失败：未设置预选英雄或英雄ID无效");
            return;
        }
        
        logger.info("Auto hovering champion: {} (ID: {})", hoverChampion.getNameCn(), hoverChampion.getChampionId());
        appendStatus("自动预选英雄：" + hoverChampion.getNameCn());
        
        // Perform the hover operation
        lcuMonitor.hoverChampion(hoverChampion.getChampionId(), actionId)
            .thenAccept(success -> {
                Platform.runLater(() -> {
                    if (success) {
                        appendStatus("成功预选英雄：" + hoverChampion.getNameCn());
                        logger.info("Successfully hovered champion: {}", hoverChampion.getNameCn());
                    } else {
                        appendStatus("预选英雄失败：" + hoverChampion.getNameCn());
                        logger.error("Failed to hover champion: {}", hoverChampion.getNameCn());
                    }
                });
            })
            .exceptionally(throwable -> {
                Platform.runLater(() -> appendStatus("预选英雄时发生错误：" + throwable.getMessage()));
                logger.error("Error during auto hover", throwable);
                return null;
            });
    }
    
    /**
     * Get the champion to hover based on current configuration
     */
    private AutoAcceptConfig.ChampionInfo getHoverChampion() {
        logger.debug("Getting hover champion - position based selection: {}, current position: {}", 
                    config.getChampionSelect().isUsePositionBasedSelection(), currentPlayerPosition);
        
        // If position-based selection is enabled, try to get position-specific champion
        if (config.getChampionSelect().isUsePositionBasedSelection()) {
            // First try to use the detected current position
            if (currentPlayerPosition != null && !currentPlayerPosition.equals("default")) {
                AutoAcceptConfig.PositionConfig positionConfig = config.getChampionSelect().getPositionConfig(currentPlayerPosition);
                if (positionConfig != null && !positionConfig.getPickChampions().isEmpty()) {
                    logger.info("Using position-specific hover champion for position: {}", currentPlayerPosition);
                    return positionConfig.getPickChampions().get(0);
                }
            }
            
            // Fallback to user selected position
            String userSelectedPosition = getUserSelectedPosition();
            if (userSelectedPosition != null && !userSelectedPosition.equals("default")) {
                AutoAcceptConfig.PositionConfig positionConfig = config.getChampionSelect().getPositionConfig(userSelectedPosition);
                if (positionConfig != null && !positionConfig.getPickChampions().isEmpty()) {
                    logger.info("Using user-selected position hover champion for position: {}", userSelectedPosition);
                    return positionConfig.getPickChampions().get(0);
                }
            }
        }
        
        // Use global pick champion as fallback
        AutoAcceptConfig.ChampionInfo globalPickChampion = config.getChampionSelect().getPickChampion();
        logger.info("Using global hover champion: {}", globalPickChampion != null ? globalPickChampion.getNameCn() : "null");
        return globalPickChampion;
    }
    
    private void handleAutoPick(int actionId) {
        logger.info("[AUTO_PICK] ===============================================");
        logger.info("[AUTO_PICK] Starting handleAutoPick - Action ID: {}", actionId);
        logger.info("[AUTO_PICK] Current time: {}", System.currentTimeMillis());
        logger.info("[AUTO_PICK] Thread: {}", Thread.currentThread().getName());
        
        // 记录详细的配置状态用于调试
        logPickConfigurationStatus(actionId);
        
        // 记录游戏状态
        logger.info("[AUTO_PICK] Current game phase: {}", lcuMonitor != null ? lcuMonitor.getCurrentPhase() : "null");
        logger.info("[AUTO_PICK] LCU connection status: {}", lcuMonitor != null ? lcuMonitor.isConnected() : "disconnected");
        
        // 检查基本状态
        if (config == null) {
            logger.error("[AUTO_PICK] Config is null, cannot proceed with auto pick");
            appendStatus("✗ 自动Pick失败：配置未加载");
            markActionFailed(actionId, "Action validation failed");
            return;
        }
        
        if (!config.getChampionSelect().isAutoPickEnabled()) {
            logger.debug("[AUTO_PICK] Auto pick is disabled in config");
            appendStatus("✗ 自动Pick失败：功能未启用");
            markActionFailed(actionId, "Action validation failed");
            return;
        }
        
        AutoAcceptConfig.ChampionInfo pickChampion = config.getChampionSelect().getPickChampion();
        boolean usePositionBased = config.getChampionSelect().isUsePositionBasedSelection();
        
        logger.info("[AUTO_PICK] Config check - Pick champion: {}, Position-based: {}, Current position: {}", 
                   pickChampion != null ? pickChampion.toString() : "null", 
                   usePositionBased, currentPlayerPosition);
        
        // 增强的配置验证
        if (pickChampion == null) {
            logger.warn("[AUTO_PICK] Pick champion config is null");
            appendStatus("✗ 自动Pick失败：未设置Pick英雄");
            markActionFailed(actionId, "Action validation failed");
            return;
        }
        
        if (pickChampion.getChampionId() == null) {
            logger.warn("[AUTO_PICK] Pick champion ID is null for champion: {}", pickChampion);
            // 尝试修复championId
            pickChampion.ensureChampionId();
            if (pickChampion.getChampionId() == null) {
                logger.error("[AUTO_PICK] Failed to resolve champion ID for: {}", pickChampion);
                appendStatus("✗ 自动Pick失败：英雄ID无效 (" + pickChampion.getKey() + ")");
                markActionFailed(actionId, "Action processing failed");
                return;
            } else {
                logger.info("[AUTO_PICK] Successfully resolved champion ID: {} for {}", 
                           pickChampion.getChampionId(), pickChampion.getKey());
            }
        }
        
        // 如果启用了分路预设但当前位置为null，先尝试获取位置信息
        if (usePositionBased && (currentPlayerPosition == null || currentPlayerPosition.trim().isEmpty())) {
            logger.info("[AUTO_PICK] Position-based selection enabled but currentPlayerPosition is null/empty, updating position first");
            // 尝试多次获取位置信息，因为在英雄选择初期位置可能还未分配
            tryGetPlayerPositionForPick(actionId, pickChampion, 0);
        } else {
            logger.info("[AUTO_PICK] Proceeding with pick logic directly - Position-based: {}, Current position: {}", 
                       usePositionBased, currentPlayerPosition);
            // 直接执行pick逻辑
            proceedWithAutoPick(actionId, pickChampion);
        }
    }
    
    /**
     * 带重试机制的位置获取（针对pick）
     */
    private void tryGetPlayerPositionForPick(int actionId, AutoAcceptConfig.ChampionInfo pickChampion, int attempt) {
        logger.debug("[AUTO_PICK] Attempting to get player position for pick - Attempt: {}/3", attempt + 1);
        
        if (attempt >= 3) {
            logger.warn("[AUTO_PICK] Failed to get player position after 3 attempts, proceeding with global config for pick");
            appendStatus("⚠ 无法获取分路信息，使用全局配置");
            proceedWithAutoPick(actionId, pickChampion);
            return;
        }
        
        if (lcuMonitor == null) {
            logger.error("[AUTO_PICK] LCU Monitor is null, cannot get player position");
            appendStatus("✗ 自动Pick失败：LCU连接不可用");
            markActionFailed(actionId, "Action validation failed");
            return;
        }
        
        lcuMonitor.getPlayerPosition()
            .thenAccept(position -> {
                logger.debug("[AUTO_PICK] Got position result: '{}' (attempt {})", position, attempt + 1);
                
                if (position != null && !position.trim().isEmpty()) {
                    currentPlayerPosition = position;
                    logger.info("[AUTO_PICK] Successfully updated currentPlayerPosition to: {} (attempt {})", position, attempt + 1);
                    Platform.runLater(() -> {
                        appendStatus(languageManager.getString("queue.current") + ": " + translatePosition(position));
                        updatePositionStatusUI(position);
                    });
                    // 成功获取位置，继续执行pick逻辑
                    proceedWithAutoPick(actionId, pickChampion);
                } else {
                    logger.debug("[AUTO_PICK] Position still empty/null for pick, attempt {}/3", attempt + 1);
                    // 延迟后重试
                    if (attempt < 2) {
                        Platform.runLater(() -> {
                            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
                                tryGetPlayerPositionForPick(actionId, pickChampion, attempt + 1);
                            }));
                            timeline.play();
                        });
                    } else {
                        logger.warn("[AUTO_PICK] Position still empty after {} attempts for pick, proceeding with global config", attempt + 1);
                        appendStatus("⚠ 无法获取分路信息，使用全局配置");
                        proceedWithAutoPick(actionId, pickChampion);
                    }
                }
            })
            .exceptionally(throwable -> {
                logger.warn("[AUTO_PICK] Failed to get player position for pick (attempt {}), error: {}", 
                           attempt + 1, throwable.getMessage(), throwable);
                if (attempt < 2) {
                    Platform.runLater(() -> {
                        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
                            tryGetPlayerPositionForPick(actionId, pickChampion, attempt + 1);
                        }));
                        timeline.play();
                    });
                } else {
                    logger.warn("[AUTO_PICK] Failed to get player position for pick after {} attempts, proceeding with global config", attempt + 1);
                    Platform.runLater(() -> {
                        appendStatus("⚠ 获取分路信息失败，使用全局配置");
                    });
                    proceedWithAutoPick(actionId, pickChampion);
                }
                return null;
            });
    }
    
    /**
     * 继续执行自动pick逻辑
     */
    private void proceedWithAutoPick(int actionId, AutoAcceptConfig.ChampionInfo pickChampion) {
        logger.info("[AUTO_PICK] Starting proceedWithAutoPick - Action ID: {}, Champion: {}", actionId, pickChampion);
        
        // 验证LCU连接状态
        if (!validateLCUConnection("自动Pick")) {
            markActionFailed(actionId, "Action validation failed");
            return;
        }
        
        // 获取已ban和已pick英雄列表，用于智能选择可用英雄
        logger.debug("[AUTO_PICK] Fetching banned and picked champions...");
        CompletableFuture<Set<Integer>> bannedChampionsFuture = lcuMonitor.getBannedChampions();
        CompletableFuture<Set<Integer>> pickedChampionsFuture = lcuMonitor.getPickedChampions();
        
        CompletableFuture.allOf(bannedChampionsFuture, pickedChampionsFuture)
            .thenAccept(v -> {
                try {
                    Set<Integer> bannedChampions = bannedChampionsFuture.join();
                    Set<Integer> pickedChampions = pickedChampionsFuture.join();
                    
                    logger.info("[AUTO_PICK] Currently banned champions: {}, picked champions: {}", 
                               bannedChampions != null ? bannedChampions : "null", 
                               pickedChampions != null ? pickedChampions : "null");
                    
                    // 确保集合不为null
                    if (bannedChampions == null) bannedChampions = new HashSet<>();
                    if (pickedChampions == null) pickedChampions = new HashSet<>();
                    
                    // 选择可用的pick英雄
                    logger.debug("[AUTO_PICK] Selecting available pick champion...");
                    AutoAcceptConfig.ChampionInfo selectedPickChampion = selectAvailablePickChampion(pickChampion, bannedChampions, pickedChampions);
                    
                    if (selectedPickChampion == null) {
                        logger.warn("[AUTO_PICK] No available champion found for pick");
                        Platform.runLater(() -> {
                            appendStatus("✗ 自动Pick失败：没有可用的英雄（所有英雄已被ban或pick）");
                        });
                        markActionFailed(actionId, "Action processing failed");
                        return;
                    }
                    
                    logger.info("[AUTO_PICK] Selected champion for pick: {} (ID: {})", 
                               selectedPickChampion, selectedPickChampion.getChampionId());
                    
                    // 检查延迟配置并决定执行方式
                    int delaySeconds = config.getChampionSelect().getSimplePickDelaySeconds();
                    logger.info("[AUTO_PICK] Delay configuration: {} seconds", delaySeconds);
                    
                    if (delaySeconds > 0) {
                        logger.info("[AUTO_PICK] Using delay pick mode with {} seconds delay", delaySeconds);
                        Platform.runLater(() -> {
                            appendStatus("🕐 启动延迟Pick模式：" + selectedPickChampion.getNameCn() + " (延迟" + delaySeconds + "秒)");
                        });
                        handleDelayPick(actionId, selectedPickChampion);
                    } else {
                        logger.info("[AUTO_PICK] Delay is 0 or negative, using immediate pick");
                        Platform.runLater(() -> {
                            appendStatus("⚡ 延迟为0，立即执行Pick：" + selectedPickChampion.getNameCn());
                        });
                        performImmediatePick(actionId, selectedPickChampion);
                    }
                } catch (Exception e) {
                    logger.error("[AUTO_PICK] Error processing banned/picked champions", e);
                    Platform.runLater(() -> {
                        appendStatus("✗ 自动Pick失败：处理英雄数据错误");
                    });
                    markActionFailed(actionId, "Action processing failed");
                    return;
                }
            })
            .exceptionally(throwable -> {
                logger.error("[AUTO_PICK] Failed to get banned/picked champions for pick", throwable);
                Platform.runLater(() -> {
                    appendStatus("⚠ 获取英雄状态失败，使用默认配置");
                });
                
                // 如果获取失败，仍然使用原有逻辑执行，但要检查championId
                if (pickChampion.getChampionId() == null) {
                    logger.error("[AUTO_PICK] Cannot proceed with fallback pick - champion ID is null");
                    Platform.runLater(() -> {
                        appendStatus("✗ 自动Pick失败：英雄ID无效");
                    });
                    markActionFailed(actionId, "Action processing failed");
                    return null;
                }
                
                logger.info("[AUTO_PICK] Using fallback pick with original champion: {}", pickChampion);
                
                // 同样检查延迟配置
                int delaySeconds = config.getChampionSelect().getSimplePickDelaySeconds();
                logger.info("[AUTO_PICK] Fallback delay configuration: {} seconds", delaySeconds);
                
                if (delaySeconds > 0) {
                    logger.info("[AUTO_PICK] Using fallback delay pick with {} seconds", delaySeconds);
                    Platform.runLater(() -> {
                        appendStatus("🕐 回退到延迟Pick：" + pickChampion.getNameCn() + " (延迟" + delaySeconds + "秒)");
                    });
                    handleDelayPick(actionId, pickChampion);
                } else {
                    logger.info("[AUTO_PICK] Fallback delay is 0, using immediate pick");
                    Platform.runLater(() -> {
                        appendStatus("⚡ 回退到立即Pick：" + pickChampion.getNameCn());
                    });
                    performImmediatePick(actionId, pickChampion);
                }
                return null;
            });
    }
    
    
    /**
     * 验证LCU连接状态的方法
     */
    private boolean validateLCUConnection(String operation) {
        if (lcuMonitor == null) {
            logger.error("[LCU_CHECK] LCU Monitor is null for operation: {}", operation);
            Platform.runLater(() -> {
                appendStatus("✗ " + operation + "失败：LCU监控器未初始化");
            });
            return false;
        }
        
        if (!lcuMonitor.isConnected()) {
            logger.warn("[LCU_CHECK] LCU is not connected for operation: {}", operation);
            Platform.runLater(() -> {
                appendStatus("✗ " + operation + "失败：未连接到英雄联盟客户端");
            });
            return false;
        }
        
        if (lcuMonitor.getCurrentPhase() == null) {
            logger.warn("[LCU_CHECK] Current game phase is null for operation: {}", operation);
            Platform.runLater(() -> {
                appendStatus("⚠ " + operation + "警告：无法获取游戏阶段信息");
            });
            // 这种情况下继续执行，因为可能是临时性问题
        }
        
        return true;
    }
    
    /**
     * 更新玩家位置信息
     * @return CompletableFuture that completes when position update is finished
     */
    private CompletableFuture<Void> updatePlayerPosition() {
        if (lcuMonitor != null) {
            return lcuMonitor.getPlayerPosition()
                .thenAccept(position -> {
                    if (position != null && !position.trim().isEmpty()) {
                        currentPlayerPosition = position;
                        logger.info("Player position detected: {}", position);
                        Platform.runLater(() -> {
                            appendStatus(languageManager.getString("queue.current") + ": " + translatePosition(position));
                            updatePositionStatusUI(position);
                            
                            // 如果启用了分路预设，自动应用配置
                            if (config != null && config.getChampionSelect().isUsePositionBasedSelection()) {
                                applyPositionPresets(position);
                            }
                            
                            // 更新队列状态显示
                            updateQueueStatusDisplay();
                        });
                    } else {
                        // 未检测到分路信息，使用全局设置
                        currentPlayerPosition = "default";
                        logger.debug("Player position not available, using global settings");
                        Platform.runLater(() -> {
                            appendStatus("未检测到分路信息，使用全局设置");
                            updatePositionStatusUI(null);
                            
                            // 如果启用了分路预设，应用全局配置
                            if (config != null && config.getChampionSelect().isUsePositionBasedSelection()) {
                                applyPositionPresets("default");
                            }
                            
                            // 更新队列状态显示
                            updateQueueStatusDisplay();
                        });
                    }
                })
                .exceptionally(throwable -> {
                    // 连接失败时也使用全局设置
                    currentPlayerPosition = "default";
                    logger.debug("Failed to get player position, using global settings", throwable);
                    Platform.runLater(() -> {
                        appendStatus("未检测到分路信息，使用全局设置");
                        updatePositionStatusUI(null);
                        
                        // 更新队列状态显示
                        updateQueueStatusDisplay();
                    });
                    return null;
                });
        } else {
            // 如果lcuMonitor为null，返回已完成的CompletableFuture
            currentPlayerPosition = "default";
            logger.warn("LCU Monitor is null, using default position");
            Platform.runLater(() -> {
                appendStatus("未连接到LCU，使用全局设置");
                updatePositionStatusUI(null);
                updateQueueStatusDisplay();
            });
            return CompletableFuture.completedFuture(null);
        }
    }
    
    /**
     * 翻译分路位置名称
     */
    private String translatePosition(String position) {
        if (position == null) return languageManager.getString("common.unknown");
        
        return switch (position.toLowerCase()) {
            case "default" -> languageManager.getString("position.global");
            case "top" -> languageManager.getString("position.top");
            case "jungle" -> languageManager.getString("position.jungle");
            case "middle" -> languageManager.getString("position.middle");
            case "bottom" -> languageManager.getString("position.bottom");
            case "utility" -> languageManager.getString("position.utility");
            default -> position;
        };
    }
    
    private void appendStatus(String message) {
        Platform.runLater(() -> {
            String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
            String formattedMessage = String.format("[%s] %s\n", timestamp, message);
            statusTextArea.appendText(formattedMessage);
            
            // 滚动到底部
            statusTextArea.setScrollTop(Double.MAX_VALUE);
            
            // 通知日志弹窗
            notifyLogViewerDialog(message);
        });
    }
    
    private void saveConfiguration() {
        // 保存spinner的值，将秒转换为毫秒
        config.setCheckIntervalMs(checkIntervalSpinner.getValue() * 1000);
        
        // 保存简单延迟Ban的spinner值
        if (simpleBanDelaySpinner != null && simpleBanDelaySpinner.getValue() != null) {
            config.getChampionSelect().setSimpleBanDelaySeconds(simpleBanDelaySpinner.getValue());
        }
        
        // 保存简单延迟Pick的spinner值
        if (simplePickDelaySpinner != null && simplePickDelaySpinner.getValue() != null) {
            config.getChampionSelect().setSimplePickDelaySeconds(simplePickDelaySpinner.getValue());
        }
        
        config.save();
    }
    
    private void updateUI() {
        // UI更新逻辑（如果需要的话）
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
                resourceManager.unregisterTimeline(autoReconnectTimeline);
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
                    // First check permissions before attempting to enable
                    List<WindowsAutoStartManager.PermissionDiagnostic> diagnostics = autoStartManager.diagnosePermissions();
                    boolean hasPermissionIssues = diagnostics.stream().anyMatch(d -> !d.hasPermission());
                    
                    if (hasPermissionIssues) {
                        autoStartCheckBox.setSelected(false);
                        appendStatus("权限检查失败，无法启用自动启动:");
                        for (WindowsAutoStartManager.PermissionDiagnostic diagnostic : diagnostics) {
                            if (!diagnostic.hasPermission()) {
                                appendStatus("• " + diagnostic.getIssue());
                                appendStatus("  解决方案: " + diagnostic.getSolution());
                            }
                        }
                        showPermissionHelpDialog();
                        return;
                    }
                    
                    success = autoStartManager.enableAutoStart();
                    if (success) {
                        config.setAutoStartEnabled(true);
                        appendStatus("开机自动启动已启用");
                    } else {
                        autoStartCheckBox.setSelected(false);
                        appendStatus("启用开机自动启动失败，请检查权限设置");
                        showPermissionHelpDialog();
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
            
            // Also show permission diagnostic
            appendStatus("权限诊断:");
            List<WindowsAutoStartManager.PermissionDiagnostic> diagnostics = autoStartManager.diagnosePermissions();
            for (WindowsAutoStartManager.PermissionDiagnostic diagnostic : diagnostics) {
                String status = diagnostic.hasPermission() ? "✓" : "✗";
                appendStatus(status + " " + diagnostic.getType() + 
                    (diagnostic.hasPermission() ? "" : " - " + diagnostic.getIssue()));
            }
        } else {
            appendStatus("当前系统不支持开机自动启动功能");
        }
    }
    
    private void showPermissionHelpDialog() {
        Platform.runLater(() -> {
            try {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Auto-Start Permission Issues");
                alert.setHeaderText("Unable to enable auto-start due to permission issues");
                
                String report = autoStartManager.getManualFixInstructions();
                
                TextArea textArea = new TextArea(report);
                textArea.setEditable(false);
                textArea.setWrapText(true);
                textArea.setMaxWidth(Double.MAX_VALUE);
                textArea.setMaxHeight(Double.MAX_VALUE);
                textArea.setPrefSize(600, 400);
                
                ScrollPane scrollPane = new ScrollPane(textArea);
                scrollPane.setFitToWidth(true);
                scrollPane.setFitToHeight(true);
                
                alert.getDialogPane().setContent(scrollPane);
                alert.getDialogPane().setPrefSize(650, 500);
                
                ButtonType tryFixButton = new ButtonType("Try Auto Fix");
                ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
                alert.getButtonTypes().setAll(tryFixButton, okButton);
                
                alert.showAndWait().ifPresent(buttonType -> {
                    if (buttonType == tryFixButton) {
                        performAutoFix();
                    }
                });
                
            } catch (Exception e) {
                logger.error("Failed to show permission help dialog", e);
                appendStatus("Failed to show permission help dialog: " + e.getMessage());
            }
        });
    }
    
    private void performAutoFix() {
        CompletableFuture.supplyAsync(() -> {
            return autoStartManager.attemptPermissionFixes();
        }).thenAccept(fixResults -> {
            Platform.runLater(() -> {
                appendStatus("Auto-fix results:");
                for (String result : fixResults) {
                    appendStatus("• " + result);
                }
                
                // Re-check permissions after fix attempt
                List<WindowsAutoStartManager.PermissionDiagnostic> diagnostics = autoStartManager.diagnosePermissions();
                boolean stillHasIssues = diagnostics.stream().anyMatch(d -> !d.hasPermission());
                
                if (!stillHasIssues) {
                    appendStatus("✓ All permission issues resolved! You can now try enabling auto-start again.");
                } else {
                    appendStatus("⚠ Some permission issues remain. You may need to run as Administrator.");
                }
                
                updateAutoStartStatus();
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                logger.error("Auto-fix failed", ex);
                appendStatus("Auto-fix failed: " + ex.getMessage());
            });
            return null;
        });
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
    private void onLanguageToggle() {
        LanguageManager.Language nextLanguage = languageManager.switchToNextLanguage();
        
        // 更新按钮文本显示当前语言状态
        updateLanguageButtonText(nextLanguage);
        
        // 更新界面文本
        updateTexts();
        
        // 记录语言切换
        appendStatus(languageManager.getString("common.language") + ": " + nextLanguage.getDisplayName());
        
        // 更新托盘菜单语言
        if (systemTrayManager != null) {
            boolean isChinese = nextLanguage == LanguageManager.Language.CHINESE;
            systemTrayManager.setLanguage(isChinese);
        }
    }
    
    /**
     * 更新语言切换按钮的文本
     */
    private void updateLanguageButtonText(LanguageManager.Language currentLanguage) {
        if (languageToggleButton != null) {
            String buttonText = switch (currentLanguage) {
                case CHINESE -> "中→EN"; // 当前中文，提示可切换到英文
                case ENGLISH -> "EN→中"; // 当前英文，提示可切换到中文
                default -> "中→EN";
            };
            languageToggleButton.setText(buttonText);
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
        if (autoAcceptCheckBox != null) autoAcceptCheckBox.setText(languageManager.getString("checkbox.enableAutoAccept"));
        if (autoBanCheckBox != null) autoBanCheckBox.setText(languageManager.getString("championSelection.autoBan"));
        if (autoPickCheckBox != null) autoPickCheckBox.setText(languageManager.getString("championSelection.autoPick"));
        
        // Update smart features checkboxes
        if (autoHoverCheckBox != null) autoHoverCheckBox.setText(languageManager.getString("smartFeatures.autoHover"));
        if (smartBanCheckBox != null) smartBanCheckBox.setText(languageManager.getString("smartFeatures.smartBan"));
        if (smartFeaturesLabel != null) smartFeaturesLabel.setText(languageManager.getString("smartFeatures.title"));
        
        // Update auto connect checkboxes
        if (autoConnectCheckBox != null) autoConnectCheckBox.setText(languageManager.getString("checkbox.autoConnect"));
        if (autoReconnectCheckBox != null) autoReconnectCheckBox.setText(languageManager.getString("checkbox.autoReconnect"));
        
        // Update settings checkboxes (with null checks)
        if (systemTrayCheckBox != null) systemTrayCheckBox.setText(languageManager.getString("checkbox.enableSystemTray"));
        if (autoStartCheckBox != null) autoStartCheckBox.setText(languageManager.getString("checkbox.autoStart"));
        if (minimizeOnCloseCheckBox != null) minimizeOnCloseCheckBox.setText(languageManager.getString("checkbox.minimizeOnClose"));
        
        // Update popup suppression checkboxes
        if (suppressReadyCheckCheckBox != null) suppressReadyCheckCheckBox.setText(languageManager.getString("checkbox.suppressReadyCheck"));
        if (suppressBanPhaseCheckBox != null) suppressBanPhaseCheckBox.setText(languageManager.getString("checkbox.suppressBanPhase"));
        if (suppressPickPhaseCheckBox != null) suppressPickPhaseCheckBox.setText(languageManager.getString("checkbox.suppressPickPhase"));
        if (forceEnglishTrayMenuCheckBox != null) forceEnglishTrayMenuCheckBox.setText(languageManager.getString("tray.forceEnglish"));
        
        // Update language toggle button (with null checks)
        updateLanguageButtonText(languageManager.getCurrentLanguage());
        
        // Update status text area prompt (with null checks)
        if (statusTextArea != null) statusTextArea.setPromptText(languageManager.getString("status.placeholder"));
        // Status split pane doesn't need title update since it's in the header label
        if (suppressionStatusLabel != null) suppressionStatusLabel.setText(languageManager.getString("status.popupSuppressionDisabled"));
        
        // Update additional labels (with null checks)
        if (checkIntervalLabel != null) checkIntervalLabel.setText(languageManager.getString("label.checkInterval"));
        if (millisecondsLabel != null) millisecondsLabel.setText(languageManager.getString("label.milliseconds"));
        if (currentPositionLabel != null) currentPositionLabel.setText(languageManager.getString("label.currentPosition"));
        if (banExecutionModeLabel != null) banExecutionModeLabel.setText(languageManager.getString("label.banExecutionMode"));
        if (delayTimeLabel != null) delayTimeLabel.setText(languageManager.getString("label.delayTime"));
        if (secondsLabel != null) secondsLabel.setText(languageManager.getString("label.seconds"));
        if (pickDelayTimeLabel != null) pickDelayTimeLabel.setText(languageManager.getString("label.delayTime"));
        if (pickSecondsLabel != null) pickSecondsLabel.setText(languageManager.getString("label.seconds"));
        if (generalSettingsLabel != null) generalSettingsLabel.setText(languageManager.getString("card.generalSettings"));
        if (connectionSettingsLabel != null) connectionSettingsLabel.setText(languageManager.getString("card.connectionSettings"));
        if (popupSuppressionLabel != null) popupSuppressionLabel.setText(languageManager.getString("card.popupSuppression"));
        if (suppressionDescriptionLabel != null) suppressionDescriptionLabel.setText(languageManager.getString("label.suppressionDescription"));
        
        
        // Update card titles (with null checks)
        if (autoAcceptCardTitle != null) autoAcceptCardTitle.setText(languageManager.getString("card.autoAcceptTitle"));
        if (positionPresetsCardTitle != null) positionPresetsCardTitle.setText(languageManager.getString("card.positionPresets"));
        if (banPickSettingsCardTitle != null) banPickSettingsCardTitle.setText(languageManager.getString("card.banPickSettings"));
        
        // Update buttons (with null checks)
        if (editPositionConfigButton != null) editPositionConfigButton.setText(languageManager.getString("button.editConfig"));
        
        
        // Update auto start status label text
        updateAutoStartStatus();
        
        
        // 更新托盘状态标签
        if (trayStatusLabel != null) {
            String baseText = languageManager.getString("label.trayStatus") + ": ";
            if (systemTrayManager != null) {
                String status = systemTrayManager.getTrayIconStatus();
                trayStatusLabel.setText(baseText + status);
            } else {
                trayStatusLabel.setText(baseText + languageManager.getString("status.notInitialized"));
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
        
        // 应用配置中的托盘菜单设置
        if (config != null) {
            systemTrayManager.setForceEnglishMenu(config.isForceEnglishTrayMenu());
        }
        
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
    
    @FXML
    private void onForceEnglishTrayMenuToggled() {
        if (forceEnglishTrayMenuCheckBox != null && config != null) {
            config.setForceEnglishTrayMenu(forceEnglishTrayMenuCheckBox.isSelected());
            saveConfiguration();
            
            // 更新系统托盘菜单
            if (systemTrayManager != null) {
                systemTrayManager.setForceEnglishMenu(config.isForceEnglishTrayMenu());
            }
            
            String status = forceEnglishTrayMenuCheckBox.isSelected() ? "启用" : "禁用";
            appendStatus("强制英文托盘菜单已" + status);
        }
    }
    
    // === 分路预设相关方法 ===
    
    
    /**
     * 分路选择变化事件处理
     */
    @FXML
    private void onPositionSelectionChanged() {
        if (positionComboBox != null && config != null) {
            String selectedPosition = positionComboBox.getValue();
            if (selectedPosition != null && config.getChampionSelect().isUsePositionBasedSelection()) {
                applyPositionPresets(selectedPosition);
            }
            
            // 更新队列状态显示
            updateQueueStatusDisplay();
        }
    }
    
    /**
     * 编辑分路配置按钮点击事件
     */
    @FXML
    private void onEditPositionConfigClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PositionConfigDialog.fxml"));
            loader.setResources(languageManager.getResourceBundle());
            Stage stage = new Stage();
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
            stage.setTitle(languageManager.getString("dialog.positionConfigTitle"));
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
                // 更新队列状态显示，确保主界面显示最新的配置
                updateQueueStatusDisplay();
                appendStatus(languageManager.getString("status.positionConfigUpdated"));
            });
            
            stage.showAndWait();
        } catch (IOException e) {
            logger.error("Failed to open position config dialog", e);
            appendStatus(languageManager.getString("error.openPositionConfigFailed") + ": " + e.getMessage());
        }
    }
    
    /**
     * 更新分路预设UI状态
     */
    private void updatePositionPresetsUI() {
        // Position presets are always enabled
        if (positionComboBox != null) {
            positionComboBox.setDisable(false);
            if (currentPlayerPosition != null) {
                positionComboBox.setValue(currentPlayerPosition);
            }
        }
        if (editPositionConfigButton != null) {
            editPositionConfigButton.setDisable(false);
        }
    }
    
    /**
     * 更新分路状态UI显示
     */
    private void updatePositionStatusUI(String position) {
        if (position != null) {
            // 自动选择对应的分路
            if (positionComboBox != null) {
                positionComboBox.setValue(position);
            }
        } else {
            // 当未检测到分路时，自动回退到全局设置
            if (positionComboBox != null) {
                positionComboBox.setValue("default");
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
                // 记录应用前的状态
                AutoAcceptConfig.ChampionInfo previousBan = config.getChampionSelect().getBanChampion();
                logger.info("Applying position presets - Previous ban: {}, New ban for position {}: {}", 
                           previousBan, position, preferredBan);
                
                // 确保championId有效
                preferredBan.ensureChampionId();
                config.getChampionSelect().setBanChampion(preferredBan);
                
                // 验证设置是否成功
                AutoAcceptConfig.ChampionInfo verifyBan = config.getChampionSelect().getBanChampion();
                logger.info("Position preset applied - Verification: ban champion is now {}", verifyBan);
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
                logger.info("Applied position-based pick champion {} for position {}", preferredPick, position);
            }
            
            saveConfiguration();
            appendStatus(java.text.MessageFormat.format(languageManager.getString("queue.applied"), translatePosition(position)));
            logger.info("Applied position presets for: {}", position);
        } else {
            logger.debug("No position config found for: {}", position);
        }
    }
    
    
    /**
     * 增强的自动Ban处理 - 集成Draft Pick分析
     */
    private void handleEnhancedAutoBan(int actionId, DraftPickEngine.DraftAnalysis draftAnalysis) {
        logger.info("增强自动Ban开始 - Action ID: {}, Draft分析: {}", actionId, draftAnalysis);
        
        // 获取智能Ban建议
        List<String> recommendations = draftPickEngine.getStrategicRecommendations(draftAnalysis);
        for (String recommendation : recommendations) {
            appendStatus("策略建议: " + recommendation);
        }
        
        // 获取已ban的英雄（避免重复）
        Set<Integer> bannedChampions = draftAnalysis.getBannedChampions();
        Set<Integer> pickedChampions = draftAnalysis.getPickedChampions();
        
        // 调用原有的ban逻辑，但传递更多信息
        handleAutoBanWithContext(actionId);
    }
    
    /**
     * 增强的自动Pick处理 - 集成智能选择策略
     */
    private void handleEnhancedAutoPick(int actionId, DraftPickEngine.DraftAnalysis draftAnalysis) {
        logger.info("增强自动Pick开始 - Action ID: {}, Draft分析: {}", actionId, draftAnalysis);
        
        // 检查是否应该延迟pick等待更多信息
        boolean shouldDelay = draftPickEngine.shouldDelayPick(draftAnalysis);
        if (shouldDelay) {
            logger.info("建议延迟pick以获取更多敌方信息");
            appendStatus("智能延迟pick，等待敌方选择...");
            
            // 延迟3-5秒后再执行
            Timeline delayTimeline = new Timeline(new KeyFrame(Duration.seconds(3), event -> {
                executeEnhancedPick(actionId, draftAnalysis);
            }));
            resourceManager.registerTimeline(delayTimeline);
        delayTimeline.play();
        } else {
            executeEnhancedPick(actionId, draftAnalysis);
        }
    }
    
    /**
     * 执行增强的pick逻辑
     */
    private void executeEnhancedPick(int actionId, DraftPickEngine.DraftAnalysis draftAnalysis) {
        // 获取当前配置的pick英雄
        AutoAcceptConfig.ChampionInfo defaultPickChampion = config.getChampionSelect().getPickChampion();
        
        // 获取当前位置的英雄队列
        List<AutoAcceptConfig.ChampionInfo> championQueue = getChampionQueueForCurrentPosition();
        
        // 获取剩余时间信息
        if (lcuMonitor != null) {
            lcuMonitor.getRemainingTimeInPhase()
                .thenAccept(timeSeconds -> {
                    // 使用智能选择器分析最佳选择（含时间信息）
                    SmartChampionSelector.SelectionStrategy strategy = smartChampionSelector.selectOptimalChampion(
                            draftAnalysis, defaultPickChampion, championQueue, currentPlayerPosition, timeSeconds);
                    
                    Platform.runLater(() -> processPickStrategy(actionId, strategy, timeSeconds));
                })
                .exceptionally(throwable -> {
                    logger.warn("无法获取剩余时间，使用默认策略", throwable);
                    // 回退到不含时间信息的策略
                    SmartChampionSelector.SelectionStrategy strategy = smartChampionSelector.selectOptimalChampion(
                            draftAnalysis, defaultPickChampion, championQueue, currentPlayerPosition);
                    Platform.runLater(() -> processPickStrategy(actionId, strategy, 30));
                    return null;
                });
        } else {
            // 没有LCU连接时的回退逻辑
            SmartChampionSelector.SelectionStrategy strategy = smartChampionSelector.selectOptimalChampion(
                    draftAnalysis, defaultPickChampion, championQueue, currentPlayerPosition);
            processPickStrategy(actionId, strategy, 30);
        }
    }
    
    /**
     * 处理pick策略结果
     */
    private void processPickStrategy(int actionId, SmartChampionSelector.SelectionStrategy strategy, int remainingTimeSeconds) {
        
        logger.info("智能选择策略: {}, 剩余时间: {}秒", strategy, remainingTimeSeconds);
        appendStatus(String.format("智能选择策略: %s (剩余时间: %d秒)", strategy.getReason(), remainingTimeSeconds));
        
        if (strategy.getRecommendedChampion() != null) {
            // 根据剩余时间调整延迟策略
            boolean shouldDelayForTime = strategy.shouldDelay() && remainingTimeSeconds > 10;
            
            if (shouldDelayForTime) {
                appendStatus("基于时间和策略，延迟pick获取更多信息...");
                // 智能延迟：剩余时间越多，延迟越久（最多5秒）
                int delaySeconds = Math.min(5, remainingTimeSeconds / 6);
                
                Timeline delayTimeline = new Timeline(new KeyFrame(Duration.seconds(delaySeconds), event -> {
                    executePickWithStrategy(actionId, strategy);
                }));
                resourceManager.registerTimeline(delayTimeline);
        delayTimeline.play();
            } else {
                executePickWithStrategy(actionId, strategy);
            }
        } else {
            logger.warn("智能选择器未找到可用英雄");
            appendStatus("❌ 智能选择失败：无可用英雄");
            markActionFailed(actionId, "Action execution failed");
        }
    }
    
    /**
     * 使用策略执行pick
     */
    private void executePickWithStrategy(int actionId, SmartChampionSelector.SelectionStrategy strategy) {
        // 显示备选英雄信息
        if (!strategy.getAlternatives().isEmpty()) {
            appendStatus("备选英雄: " + String.join(", ", strategy.getAlternatives()));
        }
        
        // 执行pick
        AutoAcceptConfig.ChampionInfo selectedChampion = strategy.getRecommendedChampion();
        selectedChampion.ensureChampionId();
        
        appendStatus(String.format("智能选择英雄: %s (优先级: %d)", 
                    selectedChampion.getNameCn() != null ? selectedChampion.getNameCn() : selectedChampion.getKey(),
                    strategy.getPriority()));
        
        // 调用实际的pick操作
        performChampionPick(actionId, selectedChampion);
    }
    
    /**
     * 获取当前位置的英雄队列
     */
    private List<AutoAcceptConfig.ChampionInfo> getChampionQueueForCurrentPosition() {
        List<AutoAcceptConfig.ChampionInfo> championQueue = new ArrayList<>();
        
        // 优先级1：API检测的位置
        if (config.getChampionSelect().isUsePositionBasedSelection() && currentPlayerPosition != null) {
            AutoAcceptConfig.PositionConfig positionConfig = config.getChampionSelect().getPositionConfig(currentPlayerPosition);
            if (positionConfig != null && positionConfig.getPickChampions() != null) {
                championQueue.addAll(positionConfig.getPickChampions());
            }
        }
        
        // 优先级2：用户手动选择的位置
        String userSelectedPosition = getUserSelectedPosition();
        if (championQueue.isEmpty() && config.getChampionSelect().isUsePositionBasedSelection() && userSelectedPosition != null) {
            AutoAcceptConfig.PositionConfig positionConfig = config.getChampionSelect().getPositionConfig(userSelectedPosition);
            if (positionConfig != null && positionConfig.getPickChampions() != null) {
                championQueue.addAll(positionConfig.getPickChampions());
            }
        }
        
        // 优先级3：全局设置
        if (championQueue.isEmpty()) {
            AutoAcceptConfig.ChampionInfo globalPick = config.getChampionSelect().getPickChampion();
            if (globalPick != null) {
                championQueue.add(globalPick);
            }
        }
        
        logger.debug("获取到{}个可选英雄用于智能选择", championQueue.size());
        return championQueue;
    }
    
    /**
     * 带上下文的Ban处理
     */
    private void handleAutoBanWithContext(int actionId) {
        // 调用现有的ban逻辑
        handleAutoBan(actionId);
    }
    
    /**
     * 执行英雄pick操作
     */
    private void performChampionPick(int actionId, AutoAcceptConfig.ChampionInfo champion) {
        if (lcuMonitor != null) {
            lcuMonitor.pickChampion(champion.getChampionId(), actionId)
                .thenAccept(success -> Platform.runLater(() -> {
                    if (success) {
                        String championName = champion.getNameCn() != null ? champion.getNameCn() : champion.getKey();
                        appendStatus("✓ 成功pick英雄: " + championName);
                        markActionSuccess(actionId);
                    } else {
                        appendStatus("✗ Pick英雄失败");
                        markActionFailed(actionId, "Action execution failed");
                    }
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        logger.error("Pick英雄操作异常", throwable);
                        appendStatus("✗ Pick英雄异常: " + throwable.getMessage());
                        markActionFailed(actionId, "Action execution failed");
                    });
                    return null;
                });
        }
    }
}