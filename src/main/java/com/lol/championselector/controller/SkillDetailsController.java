package com.lol.championselector.controller;

import com.lol.championselector.manager.LanguageManager;
import com.lol.championselector.manager.SkillIconManager;
import com.lol.championselector.model.Champion;
import com.lol.championselector.model.ChampionSkills;
import com.lol.championselector.model.Skill;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class SkillDetailsController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(SkillDetailsController.class);
    
    @FXML private Label championNameLabel;
    @FXML private VBox skillsContainer;
    @FXML private Button closeButton;
    @FXML private Button okButton;
    @FXML private ScrollPane skillsScrollPane;
    
    private Champion champion;
    private ChampionSkills skills;
    private final LanguageManager languageManager;
    private final SkillIconManager skillIconManager;
    
    public SkillDetailsController() {
        this.languageManager = LanguageManager.getInstance();
        this.skillIconManager = new SkillIconManager();
    }
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.debug("Initializing SkillDetailsController");
        
        // Listen for language changes
        languageManager.currentLocaleProperty().addListener((obs, oldVal, newVal) -> {
            updateTexts();
        });
        
        updateTexts();
        optimizeScrollSpeed();
    }
    
    private void optimizeScrollSpeed() {
        if (skillsScrollPane != null) {
            logger.debug("Setting up scroll speed optimization for skill details");
            
            // Set faster scroll speed with fixed pixel increment
            skillsScrollPane.setOnScroll(event -> {
                if (event.getDeltaY() != 0) {
                    // Use fixed pixel increment for more predictable scrolling
                    double scrollPixels = event.getDeltaY() > 0 ? -120 : 120; // 120 pixels per scroll
                    double contentHeight = skillsScrollPane.getContent().getBoundsInLocal().getHeight();
                    double viewportHeight = skillsScrollPane.getViewportBounds().getHeight();
                    
                    if (contentHeight > viewportHeight) {
                        double currentVvalue = skillsScrollPane.getVvalue();
                        double scrollableHeight = contentHeight - viewportHeight;
                        double scrollAmount = scrollPixels / scrollableHeight;
                        double newVvalue = currentVvalue + scrollAmount;
                        
                        // Clamp to valid range [0, 1]
                        newVvalue = Math.max(0, Math.min(1, newVvalue));
                        
                        skillsScrollPane.setVvalue(newVvalue);
                    }
                    
                    event.consume();
                }
            });
            
            logger.debug("Scroll speed optimization configured for skill details");
        }
    }
    
    public void setChampion(Champion champion) {
        this.champion = champion;
        updateChampionInfo();
    }
    
    public void setSkills(ChampionSkills skills) {
        this.skills = skills;
        displaySkills();
    }
    
    private void updateChampionInfo() {
        if (champion != null && championNameLabel != null) {
            String displayName = getChampionDisplayName(champion);
            championNameLabel.setText(displayName + " - " + champion.getTitle());
        }
    }
    
    private String getChampionDisplayName(Champion champion) {
        return languageManager.getCurrentLanguage() == LanguageManager.Language.CHINESE ? 
            champion.getNameCn() : champion.getNameEn();
    }
    
    private void displaySkills() {
        if (skills == null || skillsContainer == null) {
            return;
        }
        
        skillsContainer.getChildren().clear();
        
        // Display passive skill
        if (skills.getPassive() != null) {
            VBox passiveBox = createSkillBox("被动技能", skills.getPassive(), true);
            skillsContainer.getChildren().add(passiveBox);
        }
        
        // Display active skills (Q, W, E, R)
        if (skills.getSpells() != null && !skills.getSpells().isEmpty()) {
            String[] skillKeys = {"Q", "W", "E", "R"};
            for (int i = 0; i < skills.getSpells().size() && i < skillKeys.length; i++) {
                Skill spell = skills.getSpells().get(i);
                VBox skillBox = createSkillBox(skillKeys[i] + " 技能", spell, false);
                skillsContainer.getChildren().add(skillBox);
            }
        }
        
        logger.debug("Displayed skills for champion: {}", champion != null ? champion.getKey() : "unknown");
    }
    
    private VBox createSkillBox(String skillType, Skill skill, boolean isPassive) {
        VBox skillBox = new VBox(6);
        skillBox.getStyleClass().add("skill-box");
        skillBox.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e9ecef; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 12px;");
        
        // Skill header with icon, name and type
        HBox headerBox = new HBox(8);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        // Skill icon
        ImageView skillIcon = new ImageView();
        skillIcon.setFitWidth(32);
        skillIcon.setFitHeight(32);
        skillIcon.setPreserveRatio(true);
        skillIcon.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 3, 0, 0, 1);");
        
        // Load skill icon asynchronously
        loadSkillIcon(skill, skillIcon, isPassive);
        
        Label skillTypeLabel = new Label(skillType);
        skillTypeLabel.getStyleClass().add("skill-type");
        skillTypeLabel.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-padding: 2px 8px; -fx-background-radius: 12px; -fx-font-weight: bold; -fx-font-size: 11px;");
        
        Label skillNameLabel = new Label(skill.getName());
        skillNameLabel.getStyleClass().add("skill-name");
        skillNameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #212529;");
        
        headerBox.getChildren().addAll(skillIcon, skillTypeLabel, skillNameLabel);
        skillBox.getChildren().add(headerBox);
        
        // Skill description
        if (skill.getDescription() != null && !skill.getDescription().isEmpty()) {
            Label descriptionLabel = new Label(skill.getDescription());
            descriptionLabel.getStyleClass().add("skill-description");
            descriptionLabel.setWrapText(true);
            descriptionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #495057; -fx-padding: 4px 0;");
            skillBox.getChildren().add(descriptionLabel);
        }
        
        // Skill stats in a compact grid
        HBox statsBox = new HBox(15);
        statsBox.setStyle("-fx-padding: 8px 0 0 0;");
        
        if (skill.getCooldown() != null && !skill.getCooldown().isEmpty() && !skill.getCooldown().equals("0")) {
            VBox cooldownBox = createStatBox("冷却", skill.getCooldown(), "#17a2b8");
            statsBox.getChildren().add(cooldownBox);
        }
        
        if (skill.getCost() != null && !skill.getCost().isEmpty() && !skill.getCost().equals("0")) {
            VBox costBox = createStatBox("消耗", skill.getCost(), "#6f42c1");
            statsBox.getChildren().add(costBox);
        }
        
        if (skill.getRange() != null && !skill.getRange().isEmpty() && !skill.getRange().equals("25000")) {
            VBox rangeBox = createStatBox("范围", skill.getRange(), "#28a745");
            statsBox.getChildren().add(rangeBox);
        }
        
        if (skill.getDamage() != null && !skill.getDamage().isEmpty()) {
            VBox damageBox = createStatBox("伤害", skill.getDamage(), "#dc3545");
            statsBox.getChildren().add(damageBox);
        }
        
        if (!statsBox.getChildren().isEmpty()) {
            skillBox.getChildren().add(statsBox);
        }
        
        // Tooltip (detailed description) - collapsible
        if (skill.getTooltip() != null && !skill.getTooltip().isEmpty() && !skill.getTooltip().equals(skill.getDescription())) {
            Button detailsButton = new Button("查看详情");
            detailsButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 4px 8px; -fx-background-radius: 4px;");
            
            Label tooltipLabel = new Label(skill.getTooltip());
            tooltipLabel.getStyleClass().add("skill-tooltip");
            tooltipLabel.setWrapText(true);
            tooltipLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d; -fx-padding: 8px 0; -fx-background-color: #f8f9fa; -fx-border-color: #e9ecef; -fx-border-width: 1px 0 0 0;");
            tooltipLabel.setVisible(false);
            tooltipLabel.setManaged(false);
            
            detailsButton.setOnAction(e -> {
                boolean isVisible = tooltipLabel.isVisible();
                tooltipLabel.setVisible(!isVisible);
                tooltipLabel.setManaged(!isVisible);
                detailsButton.setText(isVisible ? "查看详情" : "隐藏详情");
            });
            
            skillBox.getChildren().addAll(detailsButton, tooltipLabel);
        }
        
        return skillBox;
    }
    
    private VBox createStatBox(String label, String value, String color) {
        VBox statBox = new VBox(2);
        statBox.setAlignment(javafx.geometry.Pos.CENTER);
        statBox.setStyle("-fx-min-width: 60px;");
        
        Label labelLabel = new Label(label);
        labelLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + color + "; -fx-font-weight: bold;");
        
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #212529; -fx-font-weight: bold;");
        
        statBox.getChildren().addAll(labelLabel, valueLabel);
        return statBox;
    }
    
    private void updateTexts() {
        if (okButton != null) {
            okButton.setText(languageManager.getString("common.ok"));
        }
        
        // Update champion info if already set
        updateChampionInfo();
    }
    
    @FXML
    private void onCloseClicked() {
        closeDialog();
    }
    
    @FXML
    private void onOkClicked() {
        closeDialog();
    }
    
    private void closeDialog() {
        Stage stage = (Stage) okButton.getScene().getWindow();
        stage.close();
    }
    
    private void loadSkillIcon(Skill skill, ImageView imageView, boolean isPassive) {
        String iconFileName = extractIconFileName(skill.getImageUrl());
        String championKey = champion != null ? champion.getKey() : null;
        
        if (iconFileName != null) {
            if (isPassive) {
                skillIconManager.getPassiveIconAsync(iconFileName, championKey)
                    .thenAccept(image -> Platform.runLater(() -> imageView.setImage(image)))
                    .exceptionally(throwable -> {
                        logger.warn("Failed to load passive icon for {}: {}", skill.getName(), throwable.getMessage());
                        return null;
                    });
            } else {
                skillIconManager.getSkillIconAsync(iconFileName, championKey)
                    .thenAccept(image -> Platform.runLater(() -> imageView.setImage(image)))
                    .exceptionally(throwable -> {
                        logger.warn("Failed to load skill icon for {}: {}", skill.getName(), throwable.getMessage());
                        return null;
                    });
            }
        }
    }
    
    private String extractIconFileName(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        
        // If it's already just a filename
        if (!imageUrl.contains("/")) {
            return imageUrl;
        }
        
        // Extract filename from URL
        String[] parts = imageUrl.split("/");
        return parts[parts.length - 1];
    }
    
    public void shutdown() {
        if (skillIconManager != null) {
            skillIconManager.shutdown();
        }
    }
}