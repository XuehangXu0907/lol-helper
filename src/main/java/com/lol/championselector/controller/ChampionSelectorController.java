package com.lol.championselector.controller;

import com.lol.championselector.manager.AvatarManager;
import com.lol.championselector.manager.ChampionDataManager;
import com.lol.championselector.manager.ResponsiveLayoutManager;
import com.lol.championselector.manager.SkillsManager;
import com.lol.championselector.manager.LanguageManager;
import com.lol.championselector.model.Champion;
import com.lol.championselector.model.ChampionSkills;
import com.lol.championselector.model.Skill;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import com.lol.championselector.manager.ResourceManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ChampionSelectorController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(ChampionSelectorController.class);
    
    private static final int SEARCH_DEBOUNCE_MS = 200;
    private static final int RESIZE_DEBOUNCE_MS = 300;
    private static final int BUTTON_SIZE = 100;
    
    @FXML private GridPane championGrid;
    @FXML private TextField searchField;
    @FXML private VBox skillsContainer;
    @FXML private VBox championInfoPanel;
    @FXML private Label selectedChampionLabel;
    @FXML private Label layoutInfoLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private ScrollPane championScrollPane;
    
    // 类型过滤按钮
    @FXML private Button allTypesButton;
    @FXML private Button fighterButton;
    @FXML private Button assassinButton;
    @FXML private Button mageButton;
    @FXML private Button markmanButton;
    @FXML private Button supportButton;
    @FXML private Button tankButton;
    @FXML private Button clearFilterButton;
    
    private final ChampionDataManager dataManager;
    private final AvatarManager avatarManager;
    private final SkillsManager skillsManager;
    private final ResponsiveLayoutManager layoutManager;
    private final LanguageManager languageManager;
    
    private Timeline searchTimeline;
    private Timeline resizeTimeline;
    private final ResourceManager resourceManager = ResourceManager.getInstance();
    private int currentColumns;
    private Champion selectedChampion;
    private List<Button> championButtons;
    
    // 选择模式相关
    private boolean selectionMode = false;
    private ChampionSelectionCallback onChampionSelected;
    
    // 类型过滤相关
    private String currentFilter = null;
    
    public ChampionSelectorController() {
        this.dataManager = new ChampionDataManager();
        this.avatarManager = new AvatarManager();
        this.skillsManager = new SkillsManager();
        this.layoutManager = new ResponsiveLayoutManager();
        this.languageManager = LanguageManager.getInstance();
        this.championButtons = new ArrayList<>();
    }
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.info("Initializing ChampionSelectorController");
        
        setupSearchDebounce();
        setupLoadingIndicator();
        setupFilterButtons();
        updateTexts();
        optimizeScrollSpeed();
        
        // Listen for language changes
        languageManager.currentLocaleProperty().addListener((obs, oldVal, newVal) -> {
            updateTexts();
        });
        
        Platform.runLater(() -> {
            setupResponsiveLayout();
            loadChampionsWithSkeleton();
        });
    }
    
    private void setupFilterButtons() {
        // 初始化过滤器按钮样式
        updateFilterButtonStyles();
    }
    
    private void optimizeScrollSpeed() {
        if (championScrollPane != null) {
            logger.debug("Setting up scroll speed optimization for champion selector");
            
            // Set faster scroll speed with fixed pixel increment
            championScrollPane.setOnScroll(event -> {
                if (event.getDeltaY() != 0) {
                    // Use fixed pixel increment for more predictable scrolling
                    double scrollPixels = event.getDeltaY() > 0 ? -120 : 120; // 120 pixels per scroll
                    double contentHeight = championScrollPane.getContent().getBoundsInLocal().getHeight();
                    double viewportHeight = championScrollPane.getViewportBounds().getHeight();
                    
                    if (contentHeight > viewportHeight) {
                        double currentVvalue = championScrollPane.getVvalue();
                        double scrollableHeight = contentHeight - viewportHeight;
                        double scrollAmount = scrollPixels / scrollableHeight;
                        double newVvalue = currentVvalue + scrollAmount;
                        
                        // Clamp to valid range [0, 1]
                        newVvalue = Math.max(0, Math.min(1, newVvalue));
                        
                        championScrollPane.setVvalue(newVvalue);
                    }
                    
                    event.consume();
                }
            });
            
            logger.debug("Scroll speed optimization configured for champion selector");
        }
    }
    
    private void setupSearchDebounce() {
        searchTimeline = new Timeline();
        resourceManager.registerTimeline(searchTimeline);
        
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            searchTimeline.stop();
            searchTimeline.getKeyFrames().clear();
            searchTimeline.getKeyFrames().add(
                new KeyFrame(Duration.millis(SEARCH_DEBOUNCE_MS), 
                    e -> performSearch(newText))
            );
            searchTimeline.play();
        });
        
        logger.debug("Search debounce configured with {}ms delay", SEARCH_DEBOUNCE_MS);
    }
    
    private void setupResponsiveLayout() {
        Scene scene = championGrid.getScene();
        if (scene != null) {
            resizeTimeline = new Timeline();
            resourceManager.registerTimeline(resizeTimeline);
            
            scene.widthProperty().addListener((obs, oldWidth, newWidth) -> {
                resizeTimeline.stop();
                resizeTimeline.getKeyFrames().clear();
                resizeTimeline.getKeyFrames().add(
                    new KeyFrame(Duration.millis(RESIZE_DEBOUNCE_MS),
                        e -> handleWindowResize(newWidth.doubleValue()))
                );
                resizeTimeline.play();
            });
            
            // 初始布局计算
            handleWindowResize(scene.getWidth());
        }
    }
    
    private void setupLoadingIndicator() {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
        }
    }
    
    private void loadChampionsWithSkeleton() {
        showLoading(true);
        
        Platform.runLater(() -> {
            try {
                List<Champion> champions = dataManager.getAllChampions();
                createChampionButtons(champions);
                updateLayoutInfo();
                showLoading(false);
                
                logger.info("Loaded {} champions successfully", champions.size());
            } catch (Exception e) {
                logger.error("Failed to load champions", e);
                showError("加载英雄数据失败: " + e.getMessage());
                showLoading(false);
            }
        });
    }
    
    private void createChampionButtons(List<Champion> champions) {
        championButtons.clear();
        championGrid.getChildren().clear();
        
        for (Champion champion : champions) {
            Button championButton = createChampionButton(champion);
            championButtons.add(championButton);
        }
        
        // 计算当前窗口的最佳列数
        Scene scene = championGrid.getScene();
        if (scene != null && scene.getWidth() > 0 && !Double.isNaN(scene.getWidth())) {
            currentColumns = layoutManager.calculateOptimalColumns(scene.getWidth());
            logger.debug("Using scene width {} for initial layout calculation: {} columns", 
                        scene.getWidth(), currentColumns);
        } else {
            // 对话框预设宽度为900px，使用这个值进行初始计算
            double defaultDialogWidth = 900.0;
            currentColumns = layoutManager.calculateOptimalColumns(defaultDialogWidth);
            logger.debug("Scene width unavailable, using default dialog width {} for initial layout: {} columns", 
                        defaultDialogWidth, currentColumns);
        }
        
        layoutManager.rearrangeChampionGrid(championGrid, 
                                          new ArrayList<>(championButtons), 
                                          currentColumns);
    }
    
    private Button createChampionButton(Champion champion) {
        Button button = new Button();
        button.setPrefSize(BUTTON_SIZE, BUTTON_SIZE);
        button.setMinSize(BUTTON_SIZE, BUTTON_SIZE);
        button.setMaxSize(BUTTON_SIZE, BUTTON_SIZE);
        
        // 设置样式
        button.getStyleClass().add("champion-button");
        button.setStyle("-fx-background-color: #2c2c2c; -fx-border-color: #463714; -fx-border-width: 2px;");
        
        // 设置默认图标
        ImageView defaultImageView = new ImageView(avatarManager.getDefaultImage());
        defaultImageView.setFitWidth(90);
        defaultImageView.setFitHeight(90);
        defaultImageView.setPreserveRatio(true);
        button.setGraphic(defaultImageView);
        
        // 异步加载头像
        loadAvatarAsync(champion, button);
        
        // 设置点击事件
        button.setOnAction(e -> selectChampion(champion, button));
        
        // 设置提示文本
        updateChampionTooltip(button, champion);
        
        // 设置用户数据
        button.setUserData(champion);
        
        return button;
    }
    
    private void loadAvatarAsync(Champion champion, Button button) {
        avatarManager.getAvatarAsync(champion.getKey())
            .thenAccept(image -> Platform.runLater(() -> {
                if (button.getGraphic() instanceof ImageView imageView) {
                    imageView.setImage(image);
                }
            }))
            .exceptionally(throwable -> {
                logger.warn("Failed to load avatar for {}: {}", champion.getKey(), throwable.getMessage());
                return null;
            });
    }
    
    private void selectChampion(Champion champion, Button button) {
        // 清除之前选中的按钮样式
        championButtons.forEach(btn -> btn.getStyleClass().remove("selected"));
        
        // 添加选中样式
        button.getStyleClass().add("selected");
        button.setStyle("-fx-background-color: #c89b3c; -fx-border-color: #463714; -fx-border-width: 2px;");
        
        this.selectedChampion = champion;
        
        // 如果是选择模式，执行回调并关闭窗口
        if (selectionMode && onChampionSelected != null) {
            onChampionSelected.onChampionSelected(champion);
            // 关闭当前窗口
            Platform.runLater(() -> {
                if (button.getScene() != null && button.getScene().getWindow() != null) {
                    button.getScene().getWindow().hide();
                }
            });
            return;
        }
        
        // 更新选中英雄信息
        if (selectedChampionLabel != null) {
            selectedChampionLabel.setText(getChampionDisplayName(champion) + " - " + champion.getTitle());
        }
        
        // 加载技能信息
        loadChampionSkills(champion);
        
        // 显示技能详情对话框
        showSkillDetailsDialog(champion);
        
        logger.info("Selected champion: {} ({})", champion.getNameCn(), champion.getKey());
    }
    
    private void loadChampionSkills(Champion champion) {
        if (skillsContainer != null) {
            skillsContainer.getChildren().clear();
            skillsContainer.getChildren().add(new Label("正在加载技能信息..."));
        }
        
        skillsManager.getSkillsAsync(champion.getKey())
            .thenAccept(skills -> Platform.runLater(() -> displaySkills(skills)))
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    logger.warn("Failed to load skills for {}: {}", champion.getKey(), throwable.getMessage());
                    if (skillsContainer != null) {
                        skillsContainer.getChildren().clear();
                        skillsContainer.getChildren().add(new Label("技能信息加载失败"));
                    }
                });
                return null;
            });
    }
    
    private void displaySkills(ChampionSkills skills) {
        if (skillsContainer == null) {
            return;
        }
        
        skillsContainer.getChildren().clear();
        
        if (skills.isEmpty()) {
            skillsContainer.getChildren().add(new Label("暂无技能信息"));
            return;
        }
        
        // 显示被动技能
        if (skills.getPassive() != null) {
            VBox passiveBox = createSkillBox("被动技能", skills.getPassive());
            skillsContainer.getChildren().add(passiveBox);
        }
        
        // 显示主动技能
        if (skills.getSpells() != null && !skills.getSpells().isEmpty()) {
            String[] skillKeys = {"Q", "W", "E", "R"};
            for (int i = 0; i < Math.min(skills.getSpells().size(), skillKeys.length); i++) {
                VBox skillBox = createSkillBox(skillKeys[i] + " 技能", skills.getSpells().get(i));
                skillsContainer.getChildren().add(skillBox);
            }
        }
    }
    
    private VBox createSkillBox(String skillType, Skill skill) {
        VBox skillBox = new VBox(8);
        skillBox.setPadding(new Insets(12));
        skillBox.setStyle("-fx-border-color: #463714; -fx-border-width: 1px; -fx-background-color: #1e1e1e; -fx-border-radius: 5px; -fx-background-radius: 5px;");
        
        // 技能标题（使用图标）
        String skillIcon = getSkillIcon(skillType);
        Label titleLabel = new Label(skillIcon + " " + skill.getName());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #c9aa71; -fx-font-size: 15px;");
        skillBox.getChildren().add(titleLabel);
        
        // 技能描述
        if (skill.getDescription() != null && !skill.getDescription().isEmpty()) {
            Label descLabel = new Label(skill.getDescription());
            descLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 12px; -fx-wrap-text: true;");
            descLabel.setWrapText(true);
            descLabel.setMaxWidth(280);
            skillBox.getChildren().add(descLabel);
        }
        
        // 伤害信息（更突出显示）
        if (skill.getDamage() != null && !skill.getDamage().isEmpty()) {
            Label damageLabel = new Label("⚔️ 伤害: " + skill.getDamage());
            damageLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 13px; -fx-font-weight: bold;");
            skillBox.getChildren().add(damageLabel);
        }
        
        // 效果信息
        if (skill.getEffect() != null && !skill.getEffect().isEmpty()) {
            Label effectLabel = new Label("✨ 效果: " + skill.getEffect());
            effectLabel.setStyle("-fx-text-fill: #4ecdc4; -fx-font-size: 12px;");
            skillBox.getChildren().add(effectLabel);
        }
        
        // 缩放信息
        if (skill.getScaling() != null && !skill.getScaling().isEmpty()) {
            Label scalingLabel = new Label("📊 缩放: " + skill.getScaling());
            scalingLabel.setStyle("-fx-text-fill: #ffd93d; -fx-font-size: 12px;");
            skillBox.getChildren().add(scalingLabel);
        }
        
        // 数值信息容器（水平布局）
        HBox statsContainer = new HBox(10);
        
        // 冷却时间
        if (skill.getCooldown() != null && !skill.getCooldown().isEmpty()) {
            Label cooldownLabel = new Label("⏱ " + skill.getCooldown());
            cooldownLabel.setStyle("-fx-text-fill: #87ceeb; -fx-font-size: 11px; -fx-font-weight: bold;");
            statsContainer.getChildren().add(cooldownLabel);
        }
        
        // 消耗
        if (skill.getCost() != null && !skill.getCost().isEmpty()) {
            Label costLabel = new Label("💧 " + skill.getCost());
            costLabel.setStyle("-fx-text-fill: #4fc3f7; -fx-font-size: 11px; -fx-font-weight: bold;");
            statsContainer.getChildren().add(costLabel);
        }
        
        // 施放距离
        if (skill.getRange() != null && !skill.getRange().isEmpty()) {
            Label rangeLabel = new Label("📏 " + skill.getRange());
            rangeLabel.setStyle("-fx-text-fill: #ffb74d; -fx-font-size: 11px; -fx-font-weight: bold;");
            statsContainer.getChildren().add(rangeLabel);
        }
        
        // 伤害类型
        if (skill.getDamageType() != null && !skill.getDamageType().isEmpty()) {
            Label damageTypeLabel = new Label("🎯 " + skill.getDamageType());
            damageTypeLabel.setStyle("-fx-text-fill: #ff8a65; -fx-font-size: 11px; -fx-font-weight: bold;");
            statsContainer.getChildren().add(damageTypeLabel);
        }
        
        if (!statsContainer.getChildren().isEmpty()) {
            skillBox.getChildren().add(statsContainer);
        }
        
        return skillBox;
    }
    
    private String getSkillIcon(String skillType) {
        switch (skillType) {
            case "被动技能": return "🔮";
            case "Q 技能": return "🅠";
            case "W 技能": return "🅦";
            case "E 技能": return "🅔";
            case "R 技能": return "🅡";
            default: return "⚡";
        }
    }
    
    private void performSearch(String query) {
        try {
            List<Champion> filteredChampions = dataManager.searchChampions(query);
            
            // 应用类型过滤
            if (currentFilter != null) {
                filteredChampions = filteredChampions.stream()
                    .filter(champion -> champion.getTags().contains(currentFilter))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            createChampionButtons(filteredChampions);
            updateLayoutInfo();
            
            logger.debug("Search completed for '{}' with filter '{}': {} results", 
                        query, currentFilter, filteredChampions.size());
        } catch (Exception e) {
            logger.error("Search failed for query: " + query, e);
            showError("搜索失败: " + e.getMessage());
        }
    }
    
    private void handleWindowResize(double windowWidth) {
        int newColumns = layoutManager.calculateOptimalColumns(windowWidth);
        
        if (layoutManager.shouldRearrange(currentColumns, newColumns)) {
            currentColumns = newColumns;
            layoutManager.rearrangeChampionGrid(championGrid, 
                                              new ArrayList<>(championButtons), 
                                              currentColumns);
            updateLayoutInfo();
            
            logger.debug("Layout rearranged to {} columns for window width {}", 
                        newColumns, windowWidth);
        }
    }
    
    private void updateLayoutInfo() {
        if (layoutInfoLabel != null) {
            Scene scene = championGrid.getScene();
            double windowWidth = scene != null ? scene.getWidth() : 800;
            String info = layoutManager.getLayoutInfo(windowWidth, currentColumns, championButtons.size());
            layoutInfoLabel.setText(info);
        }
    }
    
    private void showLoading(boolean show) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(show);
        }
    }
    
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("应用程序遇到错误");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    public Champion getSelectedChampion() {
        return selectedChampion;
    }
    
    // 选择模式相关方法
    public void setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
        
        // 在选择模式下隐藏整个右侧英雄信息面板
        if (selectionMode && championInfoPanel != null) {
            championInfoPanel.setVisible(false);
            championInfoPanel.setManaged(false);
        }
    }
    
    public void setOnChampionSelected(ChampionSelectionCallback callback) {
        this.onChampionSelected = callback;
    }
    
    public boolean isInitialized() {
        return championButtons != null && !championButtons.isEmpty() && championGrid != null;
    }
    
    // 类型过滤事件处理方法
    @FXML
    private void onAllTypesClicked() {
        setFilter(null);
        updateFilterButtonStyles();
    }
    
    @FXML
    private void onFighterClicked() {
        setFilter("Fighter");
        updateFilterButtonStyles();
    }
    
    @FXML
    private void onAssassinClicked() {
        setFilter("Assassin");
        updateFilterButtonStyles();
    }
    
    @FXML
    private void onMageClicked() {
        setFilter("Mage");
        updateFilterButtonStyles();
    }
    
    @FXML
    private void onMarkmanClicked() {
        setFilter("Marksman");
        updateFilterButtonStyles();
    }
    
    @FXML
    private void onSupportClicked() {
        setFilter("Support");
        updateFilterButtonStyles();
    }
    
    @FXML
    private void onTankClicked() {
        setFilter("Tank");
        updateFilterButtonStyles();
    }
    
    @FXML
    private void onClearFilterClicked() {
        setFilter(null);
        updateFilterButtonStyles();
    }
    
    private void setFilter(String filter) {
        this.currentFilter = filter;
        performSearch(searchField.getText());
    }
    
    private void updateFilterButtonStyles() {
        // 重置所有按钮样式
        allTypesButton.getStyleClass().removeAll("active-filter");
        fighterButton.getStyleClass().removeAll("active-filter");
        assassinButton.getStyleClass().removeAll("active-filter");
        mageButton.getStyleClass().removeAll("active-filter");
        markmanButton.getStyleClass().removeAll("active-filter");
        supportButton.getStyleClass().removeAll("active-filter");
        tankButton.getStyleClass().removeAll("active-filter");
        
        // 为当前活动的过滤器添加样式
        if (currentFilter == null) {
            allTypesButton.getStyleClass().add("active-filter");
        } else {
            switch (currentFilter) {
                case "Fighter": fighterButton.getStyleClass().add("active-filter"); break;
                case "Assassin": assassinButton.getStyleClass().add("active-filter"); break;
                case "Mage": mageButton.getStyleClass().add("active-filter"); break;
                case "Marksman": markmanButton.getStyleClass().add("active-filter"); break;
                case "Support": supportButton.getStyleClass().add("active-filter"); break;
                case "Tank": tankButton.getStyleClass().add("active-filter"); break;
            }
        }
    }

    public void shutdown() {
        try {
            if (searchTimeline != null) {
                searchTimeline.stop();
            }
            if (resizeTimeline != null) {
                resizeTimeline.stop();
            }
            
            avatarManager.shutdown();
            skillsManager.shutdown();
            
            logger.info("ChampionSelectorController shut down successfully");
        } catch (Exception e) {
            logger.warn("Error during controller shutdown", e);
        }
    }
    
    @FunctionalInterface
    public interface ChampionSelectionCallback {
        void onChampionSelected(Champion champion);
    }
    
    private void updateTexts() {
        // Update search field
        searchField.setPromptText(languageManager.getString("common.searchPlaceholder"));
        
        // Update filter buttons
        allTypesButton.setText(languageManager.getString("common.all"));
        fighterButton.setText(languageManager.getString("type.fighter"));
        assassinButton.setText(languageManager.getString("type.assassin"));
        mageButton.setText(languageManager.getString("type.mage"));
        markmanButton.setText(languageManager.getString("type.marksman"));
        supportButton.setText(languageManager.getString("type.support"));
        tankButton.setText(languageManager.getString("type.tank"));
        clearFilterButton.setText(languageManager.getString("common.clear"));
        
        // Update selected champion label
        if (selectedChampion == null) {
            selectedChampionLabel.setText(languageManager.getString("common.selectChampion"));
        } else {
            selectedChampionLabel.setText(getChampionDisplayName(selectedChampion) + " - " + selectedChampion.getTitle());
        }
        
        // Update all champion button tooltips
        updateAllChampionTooltips();
    }
    
    private void updateAllChampionTooltips() {
        for (Button button : championButtons) {
            Champion champion = (Champion) button.getUserData();
            if (champion != null) {
                updateChampionTooltip(button, champion);
            }
        }
    }
    
    private void updateChampionTooltip(Button button, Champion champion) {
        String name = languageManager.getCurrentLanguage() == LanguageManager.Language.CHINESE ? 
            champion.getNameCn() : champion.getNameEn();
        Tooltip tooltip = new Tooltip(name + " - " + champion.getTitle());
        button.setTooltip(tooltip);
    }
    
    private String getChampionDisplayName(Champion champion) {
        return languageManager.getCurrentLanguage() == LanguageManager.Language.CHINESE ? 
            champion.getNameCn() : champion.getNameEn();
    }
    
    private void showSkillDetailsDialog(Champion champion) {
        // 如果是选择模式，不显示详情对话框
        if (selectionMode) {
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SkillDetailsDialog.fxml"));
            Scene scene = new Scene(loader.load());
            
            SkillDetailsController controller = loader.getController();
            controller.setChampion(champion);
            
            Stage stage = new Stage();
            stage.setTitle("英雄技能详情 - " + getChampionDisplayName(champion));
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(true);
            stage.setMinWidth(500);
            stage.setMinHeight(600);
            stage.setWidth(500);
            stage.setHeight(600);
            
            // 加载技能数据并显示
            skillsManager.getSkillsAsync(champion.getKey())
                .thenAccept(skills -> Platform.runLater(() -> {
                    controller.setSkills(skills);
                    stage.show();
                    
                    // 当对话框关闭时，清理资源
                    stage.setOnCloseRequest(e -> controller.shutdown());
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        logger.warn("Failed to load skills for dialog: {}", throwable.getMessage());
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("错误");
                        alert.setHeaderText("技能信息加载失败");
                        alert.setContentText("无法加载 " + getChampionDisplayName(champion) + " 的技能信息。");
                        alert.showAndWait();
                    });
                    return null;
                });
                
        } catch (Exception e) {
            logger.error("Failed to show skill details dialog", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("无法显示技能详情");
            alert.setContentText("打开技能详情对话框时发生错误。");
            alert.showAndWait();
        }
    }
}