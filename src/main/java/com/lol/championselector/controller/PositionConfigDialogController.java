package com.lol.championselector.controller;

import com.lol.championselector.config.AutoAcceptConfig;
import com.lol.championselector.model.Champion;
import com.lol.championselector.manager.LanguageManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class PositionConfigDialogController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(PositionConfigDialogController.class);
    
    @FXML private ComboBox<String> positionSelector;
    @FXML private ListView<AutoAcceptConfig.ChampionInfo> banChampionsListView;
    @FXML private ListView<AutoAcceptConfig.ChampionInfo> pickChampionsListView;
    
    @FXML private Button addBanButton;
    @FXML private Button removeBanButton;
    @FXML private Button moveBanUpButton;
    @FXML private Button moveBanDownButton;
    
    @FXML private Button addPickButton;
    @FXML private Button removePickButton;
    @FXML private Button movePickUpButton;
    @FXML private Button movePickDownButton;
    
    @FXML private Button resetButton;
    @FXML private Button cancelButton;
    @FXML private Button saveButton;
    
    private AutoAcceptConfig config;
    private LanguageManager languageManager;
    private String currentPosition;
    private boolean isModified = false;
    
    // 回调接口
    @FunctionalInterface
    public interface ConfigSaveCallback {
        void onConfigSaved();
    }
    
    private ConfigSaveCallback onConfigSaved;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        languageManager = LanguageManager.getInstance();
        initializePositionSelector();
        setupListViews();
        updateButtonStates();
    }
    
    private void initializePositionSelector() {
        positionSelector.getItems().addAll("top", "jungle", "middle", "bottom", "utility");
        
        // 设置显示转换器
        positionSelector.setConverter(new javafx.util.StringConverter<String>() {
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
        
        // 默认选择第一个
        if (!positionSelector.getItems().isEmpty()) {
            positionSelector.setValue(positionSelector.getItems().get(0));
            onPositionChanged();
        }
    }
    
    private void setupListViews() {
        // 设置ListView的显示格式
        banChampionsListView.setCellFactory(listView -> new ChampionListCell());
        pickChampionsListView.setCellFactory(listView -> new ChampionListCell());
        
        // 添加选择监听器
        banChampionsListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> updateButtonStates());
        pickChampionsListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> updateButtonStates());
    }
    
    public void setConfig(AutoAcceptConfig config) {
        this.config = config;
        if (currentPosition != null) {
            loadPositionConfig();
        }
    }
    
    public void setOnConfigSaved(ConfigSaveCallback callback) {
        this.onConfigSaved = callback;
    }
    
    @FXML
    private void onPositionChanged() {
        currentPosition = positionSelector.getValue();
        if (config != null && currentPosition != null) {
            loadPositionConfig();
        }
    }
    
    private void loadPositionConfig() {
        if (config == null || currentPosition == null) return;
        
        AutoAcceptConfig.PositionConfig posConfig = config.getChampionSelect().getPositionConfig(currentPosition);
        if (posConfig != null) {
            // 加载Ban英雄列表
            ObservableList<AutoAcceptConfig.ChampionInfo> banList = FXCollections.observableArrayList(
                new ArrayList<>(posConfig.getBanChampions()));
            banChampionsListView.setItems(banList);
            
            // 加载Pick英雄列表
            ObservableList<AutoAcceptConfig.ChampionInfo> pickList = FXCollections.observableArrayList(
                new ArrayList<>(posConfig.getPickChampions()));
            pickChampionsListView.setItems(pickList);
        } else {
            // 如果没有配置，创建空列表
            banChampionsListView.setItems(FXCollections.observableArrayList());
            pickChampionsListView.setItems(FXCollections.observableArrayList());
        }
        
        updateButtonStates();
    }
    
    @FXML
    private void onAddBanChampionClicked() {
        selectChampion(champion -> {
            AutoAcceptConfig.ChampionInfo championInfo = new AutoAcceptConfig.ChampionInfo(champion);
            banChampionsListView.getItems().add(championInfo);
            markAsModified();
        });
    }
    
    @FXML
    private void onRemoveBanChampionClicked() {
        AutoAcceptConfig.ChampionInfo selected = banChampionsListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            banChampionsListView.getItems().remove(selected);
            markAsModified();
        }
    }
    
    @FXML
    private void onMoveBanUpClicked() {
        moveItem(banChampionsListView, true);
    }
    
    @FXML
    private void onMoveBanDownClicked() {
        moveItem(banChampionsListView, false);
    }
    
    @FXML
    private void onAddPickChampionClicked() {
        selectChampion(champion -> {
            AutoAcceptConfig.ChampionInfo championInfo = new AutoAcceptConfig.ChampionInfo(champion);
            pickChampionsListView.getItems().add(championInfo);
            markAsModified();
        });
    }
    
    @FXML
    private void onRemovePickChampionClicked() {
        AutoAcceptConfig.ChampionInfo selected = pickChampionsListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            pickChampionsListView.getItems().remove(selected);
            markAsModified();
        }
    }
    
    @FXML
    private void onMovePickUpClicked() {
        moveItem(pickChampionsListView, true);
    }
    
    @FXML
    private void onMovePickDownClicked() {
        moveItem(pickChampionsListView, false);
    }
    
    @FXML
    private void onResetClicked() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认重置");
        alert.setHeaderText("重置分路配置");
        alert.setContentText("确定要将" + translatePosition(currentPosition) + "的配置重置为默认值吗？");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                resetCurrentPositionToDefault();
            }
        });
    }
    
    @FXML
    private void onCancelClicked() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
    
    @FXML
    private void onSaveClicked() {
        if (config != null && currentPosition != null) {
            saveCurrentPositionConfig();
            config.save();
            
            if (onConfigSaved != null) {
                onConfigSaved.onConfigSaved();
            }
            
            Stage stage = (Stage) saveButton.getScene().getWindow();
            stage.close();
        }
    }
    
    private void selectChampion(ChampionSelectionCallback callback) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ChampionSelectorView.fxml"));
            Stage stage = new Stage();
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
            stage.setTitle("选择英雄");
            stage.initModality(Modality.APPLICATION_MODAL);
            
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
        }
    }
    
    private void moveItem(ListView<AutoAcceptConfig.ChampionInfo> listView, boolean moveUp) {
        int selectedIndex = listView.getSelectionModel().getSelectedIndex();
        if (selectedIndex == -1) return;
        
        ObservableList<AutoAcceptConfig.ChampionInfo> items = listView.getItems();
        int newIndex;
        
        if (moveUp) {
            if (selectedIndex == 0) return;
            newIndex = selectedIndex - 1;
        } else {
            if (selectedIndex == items.size() - 1) return;
            newIndex = selectedIndex + 1;
        }
        
        AutoAcceptConfig.ChampionInfo item = items.remove(selectedIndex);
        items.add(newIndex, item);
        listView.getSelectionModel().select(newIndex);
        
        markAsModified();
    }
    
    private void saveCurrentPositionConfig() {
        if (config == null || currentPosition == null) return;
        
        AutoAcceptConfig.PositionConfig posConfig = config.getChampionSelect().getPositionConfig(currentPosition);
        if (posConfig == null) {
            posConfig = new AutoAcceptConfig.PositionConfig(currentPosition);
            config.getChampionSelect().getPositionConfigs().put(currentPosition, posConfig);
        }
        
        // 保存Ban英雄列表
        posConfig.getBanChampions().clear();
        posConfig.getBanChampions().addAll(banChampionsListView.getItems());
        
        // 保存Pick英雄列表
        posConfig.getPickChampions().clear();
        posConfig.getPickChampions().addAll(pickChampionsListView.getItems());
        
        logger.info("Saved configuration for position: {}", currentPosition);
    }
    
    private void resetCurrentPositionToDefault() {
        if (currentPosition == null) return;
        
        // 创建新的默认配置
        AutoAcceptConfig.PositionConfig defaultConfig = createDefaultPositionConfig(currentPosition);
        
        // 更新UI
        if (defaultConfig != null) {
            banChampionsListView.getItems().setAll(defaultConfig.getBanChampions());
            pickChampionsListView.getItems().setAll(defaultConfig.getPickChampions());
            markAsModified();
        }
    }
    
    private AutoAcceptConfig.PositionConfig createDefaultPositionConfig(String position) {
        AutoAcceptConfig.PositionConfig config = new AutoAcceptConfig.PositionConfig(position);
        
        switch (position) {
            case "top":
                config.addBanChampion(new AutoAcceptConfig.ChampionInfo("Darius", "德莱厄斯", "诺克萨斯之手"));
                config.addBanChampion(new AutoAcceptConfig.ChampionInfo("Garen", "盖伦", "德玛西亚之力"));
                config.addPickChampion(new AutoAcceptConfig.ChampionInfo("Garen", "盖伦", "德玛西亚之力"));
                config.addPickChampion(new AutoAcceptConfig.ChampionInfo("Malphite", "墨菲特", "熔岩巨兽"));
                break;
            case "jungle":
                config.addBanChampion(new AutoAcceptConfig.ChampionInfo("Graves", "格雷福斯", "法外狂徒"));
                config.addBanChampion(new AutoAcceptConfig.ChampionInfo("Ekko", "艾克", "时间刺客"));
                config.addPickChampion(new AutoAcceptConfig.ChampionInfo("Graves", "格雷福斯", "法外狂徒"));
                config.addPickChampion(new AutoAcceptConfig.ChampionInfo("Warwick", "沃里克", "祖安怒兽"));
                break;
            case "middle":
                config.addBanChampion(new AutoAcceptConfig.ChampionInfo("Yasuo", "亚索", "疾风剑豪"));
                config.addBanChampion(new AutoAcceptConfig.ChampionInfo("Zed", "劫", "影流之主"));
                config.addPickChampion(new AutoAcceptConfig.ChampionInfo("Annie", "安妮", "黑暗之女"));
                config.addPickChampion(new AutoAcceptConfig.ChampionInfo("Malzahar", "玛尔扎哈", "虚空先知"));
                break;
            case "bottom":
                config.addBanChampion(new AutoAcceptConfig.ChampionInfo("Draven", "德莱文", "荣耀行刑官"));
                config.addBanChampion(new AutoAcceptConfig.ChampionInfo("Vayne", "薇恩", "暗夜猎手"));
                config.addPickChampion(new AutoAcceptConfig.ChampionInfo("Jinx", "金克丝", "暴走萝莉"));
                config.addPickChampion(new AutoAcceptConfig.ChampionInfo("Ashe", "艾希", "寒冰射手"));
                break;
            case "utility":
                config.addBanChampion(new AutoAcceptConfig.ChampionInfo("Thresh", "锤石", "魂锁典狱长"));
                config.addBanChampion(new AutoAcceptConfig.ChampionInfo("Blitzcrank", "布里茨", "蒸汽机器人"));
                config.addPickChampion(new AutoAcceptConfig.ChampionInfo("Soraka", "索拉卡", "众星之子"));
                config.addPickChampion(new AutoAcceptConfig.ChampionInfo("Janna", "迦娜", "风暴之怒"));
                break;
        }
        
        return config;
    }
    
    private void updateButtonStates() {
        // Ban英雄按钮状态
        boolean banSelected = banChampionsListView.getSelectionModel().getSelectedItem() != null;
        int banIndex = banChampionsListView.getSelectionModel().getSelectedIndex();
        int banSize = banChampionsListView.getItems().size();
        
        removeBanButton.setDisable(!banSelected);
        moveBanUpButton.setDisable(!banSelected || banIndex == 0);
        moveBanDownButton.setDisable(!banSelected || banIndex == banSize - 1);
        
        // Pick英雄按钮状态
        boolean pickSelected = pickChampionsListView.getSelectionModel().getSelectedItem() != null;
        int pickIndex = pickChampionsListView.getSelectionModel().getSelectedIndex();
        int pickSize = pickChampionsListView.getItems().size();
        
        removePickButton.setDisable(!pickSelected);
        movePickUpButton.setDisable(!pickSelected || pickIndex == 0);
        movePickDownButton.setDisable(!pickSelected || pickIndex == pickSize - 1);
    }
    
    private void markAsModified() {
        isModified = true;
        updateButtonStates();
    }
    
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
    
    // 自定义ListCell用于显示ChampionInfo
    private static class ChampionListCell extends ListCell<AutoAcceptConfig.ChampionInfo> {
        @Override
        protected void updateItem(AutoAcceptConfig.ChampionInfo item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setText(null);
            } else {
                setText(item.toString());
            }
        }
    }
    
    @FunctionalInterface
    private interface ChampionSelectionCallback {
        void onChampionSelected(Champion champion);
    }
}