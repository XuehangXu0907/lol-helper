package com.lol.championselector.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.championselector.model.Champion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AutoAcceptConfig {
    private static final Logger logger = LoggerFactory.getLogger(AutoAcceptConfig.class);
    private static final String CONFIG_FILE = "auto-accept-config.json";
    
    private boolean autoAcceptEnabled = true;
    private boolean autoDeclineEnabled = false;
    private int checkIntervalMs = 1000;
    
    private ChampionSelectConfig championSelect = new ChampionSelectConfig();
    
    // System Tray and Auto Start settings
    private boolean systemTrayEnabled = true;
    private boolean autoStartEnabled = false;
    
    // Auto connect settings
    private boolean autoConnectEnabled = true; // 启动时自动连接
    private boolean autoReconnectEnabled = true; // 断线自动重连
    private int reconnectIntervalSeconds = 10; // 重连检测间隔
    
    // Window close behavior settings
    private boolean minimizeOnClose = true; // 关闭窗口时最小化到托盘
    private boolean confirmOnExit = false; // 退出时显示确认对话框
    
    // Popup suppression settings
    private boolean suppressReadyCheckPopup = false; // 抑制准备检查弹窗
    private boolean suppressBanPhasePopup = false; // 抑制Ban阶段弹窗
    private boolean suppressPickPhasePopup = false; // 抑制Pick阶段弹窗
    
    public static class ChampionSelectConfig {
        private boolean autoBanEnabled = false;
        private boolean autoPickEnabled = false;
        private ChampionInfo banChampion;
        private ChampionInfo pickChampion;
        
        // 新增：智能时机控制
        private boolean smartTimingEnabled = true; // 启用智能时机控制
        private int banExecutionDelaySeconds = 2; // Ban阶段剩余秒数时执行
        private int pickExecutionDelaySeconds = 2; // Pick阶段剩余秒数时执行
        private boolean enableHover = true; // 启用hover预选
        
        // 新增：分路配置
        private boolean usePositionBasedSelection = false; // 启用基于分路的选择
        private Map<String, PositionConfig> positionConfigs = new HashMap<>();
        
        public ChampionSelectConfig() {
            // 默认Ban英雄：艾克
            this.banChampion = new ChampionInfo("Ekko", "艾克", "时间刺客");
            // 默认Pick英雄：金克丝
            this.pickChampion = new ChampionInfo("Jinx", "金克丝", "暴走萝莉");
            
            // 初始化默认分路配置
            initializeDefaultPositionConfigs();
        }
        
        private void initializeDefaultPositionConfigs() {
            // Top lane
            PositionConfig topConfig = new PositionConfig("top");
            topConfig.addBanChampion(new ChampionInfo("Darius", "德莱厄斯", "诺克萨斯之手"));
            topConfig.addBanChampion(new ChampionInfo("Garen", "盖伦", "德玛西亚之力"));
            topConfig.addPickChampion(new ChampionInfo("Garen", "盖伦", "德玛西亚之力"));
            topConfig.addPickChampion(new ChampionInfo("Malphite", "墨菲特", "熔岩巨兽"));
            positionConfigs.put("top", topConfig);
            
            // Jungle
            PositionConfig jungleConfig = new PositionConfig("jungle");
            jungleConfig.addBanChampion(new ChampionInfo("Graves", "格雷福斯", "法外狂徒"));
            jungleConfig.addBanChampion(new ChampionInfo("Ekko", "艾克", "时间刺客"));
            jungleConfig.addPickChampion(new ChampionInfo("Graves", "格雷福斯", "法外狂徒"));
            jungleConfig.addPickChampion(new ChampionInfo("Warwick", "沃里克", "祖安怒兽"));
            positionConfigs.put("jungle", jungleConfig);
            
            // Middle
            PositionConfig midConfig = new PositionConfig("middle");
            midConfig.addBanChampion(new ChampionInfo("Yasuo", "亚索", "疾风剑豪"));
            midConfig.addBanChampion(new ChampionInfo("Zed", "劫", "影流之主"));
            midConfig.addPickChampion(new ChampionInfo("Annie", "安妮", "黑暗之女"));
            midConfig.addPickChampion(new ChampionInfo("Malzahar", "玛尔扎哈", "虚空先知"));
            positionConfigs.put("middle", midConfig);
            
            // Bottom ADC
            PositionConfig botConfig = new PositionConfig("bottom");
            botConfig.addBanChampion(new ChampionInfo("Draven", "德莱文", "荣耀行刑官"));
            botConfig.addBanChampion(new ChampionInfo("Vayne", "薇恩", "暗夜猎手"));
            botConfig.addPickChampion(new ChampionInfo("Jinx", "金克丝", "暴走萝莉"));
            botConfig.addPickChampion(new ChampionInfo("Ashe", "艾希", "寒冰射手"));
            positionConfigs.put("bottom", botConfig);
            
            // Support
            PositionConfig supportConfig = new PositionConfig("utility");
            supportConfig.addBanChampion(new ChampionInfo("Thresh", "锤石", "魂锁典狱长"));
            supportConfig.addBanChampion(new ChampionInfo("Blitzcrank", "布里茨", "蒸汽机器人"));
            supportConfig.addPickChampion(new ChampionInfo("Soraka", "索拉卡", "众星之子"));
            supportConfig.addPickChampion(new ChampionInfo("Janna", "迦娜", "风暴之怒"));
            positionConfigs.put("utility", supportConfig);
        }
        
        // Getters and Setters
        public boolean isAutoBanEnabled() { return autoBanEnabled; }
        public void setAutoBanEnabled(boolean autoBanEnabled) { this.autoBanEnabled = autoBanEnabled; }
        
        public boolean isAutoPickEnabled() { return autoPickEnabled; }
        public void setAutoPickEnabled(boolean autoPickEnabled) { this.autoPickEnabled = autoPickEnabled; }
        
        public ChampionInfo getBanChampion() { return banChampion; }
        public void setBanChampion(ChampionInfo banChampion) { this.banChampion = banChampion; }
        
        public ChampionInfo getPickChampion() { return pickChampion; }
        public void setPickChampion(ChampionInfo pickChampion) { this.pickChampion = pickChampion; }
        
        // 智能时机控制相关
        public boolean isSmartTimingEnabled() { return smartTimingEnabled; }
        public void setSmartTimingEnabled(boolean smartTimingEnabled) { this.smartTimingEnabled = smartTimingEnabled; }
        
        public int getBanExecutionDelaySeconds() { return banExecutionDelaySeconds; }
        public void setBanExecutionDelaySeconds(int banExecutionDelaySeconds) { 
            this.banExecutionDelaySeconds = Math.max(1, Math.min(10, banExecutionDelaySeconds)); 
        }
        
        public int getPickExecutionDelaySeconds() { return pickExecutionDelaySeconds; }
        public void setPickExecutionDelaySeconds(int pickExecutionDelaySeconds) { 
            this.pickExecutionDelaySeconds = Math.max(1, Math.min(10, pickExecutionDelaySeconds)); 
        }
        
        public boolean isEnableHover() { return enableHover; }
        public void setEnableHover(boolean enableHover) { this.enableHover = enableHover; }
        
        // 分路配置相关
        public boolean isUsePositionBasedSelection() { return usePositionBasedSelection; }
        public void setUsePositionBasedSelection(boolean usePositionBasedSelection) { this.usePositionBasedSelection = usePositionBasedSelection; }
        
        public Map<String, PositionConfig> getPositionConfigs() { return positionConfigs; }
        public void setPositionConfigs(Map<String, PositionConfig> positionConfigs) { this.positionConfigs = positionConfigs; }
        
        public PositionConfig getPositionConfig(String position) { 
            return positionConfigs.getOrDefault(position, null); 
        }
    }
    
    /**
     * 分路配置类
     */
    public static class PositionConfig {
        private String position;
        private List<ChampionInfo> banChampions = new ArrayList<>();
        private List<ChampionInfo> pickChampions = new ArrayList<>();
        
        public PositionConfig() {}
        
        public PositionConfig(String position) {
            this.position = position;
        }
        
        public void addBanChampion(ChampionInfo champion) {
            banChampions.add(champion);
        }
        
        public void addPickChampion(ChampionInfo champion) {
            pickChampions.add(champion);
        }
        
        public ChampionInfo getPreferredBanChampion() {
            return banChampions.isEmpty() ? null : banChampions.get(0);
        }
        
        public ChampionInfo getPreferredPickChampion() {
            return pickChampions.isEmpty() ? null : pickChampions.get(0);
        }
        
        public ChampionInfo getAlternateBanChampion(Set<Integer> bannedChampionIds) {
            for (ChampionInfo champion : banChampions) {
                if (champion.getChampionId() != null && 
                    !bannedChampionIds.contains(champion.getChampionId())) {
                    return champion;
                }
            }
            return null;
        }
        
        public ChampionInfo getAlternatePickChampion(Set<Integer> bannedChampionIds, Set<Integer> pickedChampionIds) {
            for (ChampionInfo champion : pickChampions) {
                if (champion.getChampionId() != null && 
                    !bannedChampionIds.contains(champion.getChampionId()) &&
                    !pickedChampionIds.contains(champion.getChampionId())) {
                    return champion;
                }
            }
            return null;
        }
        
        // Getters and Setters
        public String getPosition() { return position; }
        public void setPosition(String position) { this.position = position; }
        
        public List<ChampionInfo> getBanChampions() { return banChampions; }
        public void setBanChampions(List<ChampionInfo> banChampions) { this.banChampions = banChampions; }
        
        public List<ChampionInfo> getPickChampions() { return pickChampions; }
        public void setPickChampions(List<ChampionInfo> pickChampions) { this.pickChampions = pickChampions; }
    }
    
    public static class ChampionInfo {
        private String key;
        private String nameCn;
        private String nameEn;
        private String title;
        private Integer championId;
        
        public ChampionInfo() {}
        
        public ChampionInfo(String key, String nameCn, String title) {
            this.key = key;
            this.nameCn = nameCn;
            this.nameEn = key; // 默认使用key作为英文名
            this.title = title;
        }
        
        public ChampionInfo(Champion champion) {
            this.key = champion.getKey();
            this.nameCn = champion.getNameCn();
            this.nameEn = champion.getNameEn();
            this.title = champion.getTitle();
            
            // 安全地解析 championId
            try {
                if (champion.getId() != null && !champion.getId().trim().isEmpty()) {
                    this.championId = Integer.parseInt(champion.getId().trim());
                } else {
                    logger.warn("Champion {} has null or empty ID", champion.getKey());
                    this.championId = null;
                }
            } catch (NumberFormatException e) {
                logger.error("Failed to parse champion ID for {}: {}", champion.getKey(), champion.getId(), e);
                this.championId = null;
            }
        }
        
        // Getters and Setters
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        
        public String getNameCn() { return nameCn; }
        public void setNameCn(String nameCn) { this.nameCn = nameCn; }
        
        public String getNameEn() { return nameEn; }
        public void setNameEn(String nameEn) { this.nameEn = nameEn; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public Integer getChampionId() { return championId; }
        public void setChampionId(Integer championId) { this.championId = championId; }
        
        @Override
        public String toString() {
            return nameCn + " (" + nameEn + ")";
        }
    }
    
    // 静态方法：加载配置
    public static AutoAcceptConfig load() {
        ObjectMapper mapper = new ObjectMapper();
        File configFile = new File(CONFIG_FILE);
        
        if (configFile.exists()) {
            try {
                AutoAcceptConfig config = mapper.readValue(configFile, AutoAcceptConfig.class);
                logger.info("Loaded configuration from {}", CONFIG_FILE);
                return config;
            } catch (IOException e) {
                logger.error("Failed to load configuration from {}, using defaults", CONFIG_FILE, e);
            }
        } else {
            logger.info("Configuration file {} not found, using defaults", CONFIG_FILE);
        }
        
        return new AutoAcceptConfig();
    }
    
    // 保存配置
    public void save() {
        ObjectMapper mapper = new ObjectMapper();
        File configFile = new File(CONFIG_FILE);
        
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(configFile, this);
            logger.info("Saved configuration to {}", CONFIG_FILE);
        } catch (IOException e) {
            logger.error("Failed to save configuration to {}", CONFIG_FILE, e);
        }
    }
    
    // Getters and Setters
    public boolean isAutoAcceptEnabled() { return autoAcceptEnabled; }
    public void setAutoAcceptEnabled(boolean autoAcceptEnabled) { this.autoAcceptEnabled = autoAcceptEnabled; }
    
    public boolean isAutoDeclineEnabled() { return autoDeclineEnabled; }
    public void setAutoDeclineEnabled(boolean autoDeclineEnabled) { this.autoDeclineEnabled = autoDeclineEnabled; }
    
    public int getCheckIntervalMs() { return checkIntervalMs; }
    public void setCheckIntervalMs(int checkIntervalMs) { this.checkIntervalMs = Math.max(100, Math.min(5000, checkIntervalMs)); }
    
    public ChampionSelectConfig getChampionSelect() { return championSelect; }
    public void setChampionSelect(ChampionSelectConfig championSelect) { this.championSelect = championSelect; }
    
    // System Tray and Auto Start getters and setters
    public boolean isSystemTrayEnabled() { return systemTrayEnabled; }
    public void setSystemTrayEnabled(boolean systemTrayEnabled) { this.systemTrayEnabled = systemTrayEnabled; }
    
    
    public boolean isAutoStartEnabled() { return autoStartEnabled; }
    public void setAutoStartEnabled(boolean autoStartEnabled) { this.autoStartEnabled = autoStartEnabled; }
    
    // Window close behavior getters and setters
    public boolean isMinimizeOnClose() { return minimizeOnClose; }
    public void setMinimizeOnClose(boolean minimizeOnClose) { this.minimizeOnClose = minimizeOnClose; }
    
    public boolean isConfirmOnExit() { return confirmOnExit; }
    public void setConfirmOnExit(boolean confirmOnExit) { this.confirmOnExit = confirmOnExit; }
    
    // Popup suppression getters and setters
    public boolean isSuppressReadyCheckPopup() { return suppressReadyCheckPopup; }
    public void setSuppressReadyCheckPopup(boolean suppressReadyCheckPopup) { this.suppressReadyCheckPopup = suppressReadyCheckPopup; }
    
    public boolean isSuppressBanPhasePopup() { return suppressBanPhasePopup; }
    public void setSuppressBanPhasePopup(boolean suppressBanPhasePopup) { this.suppressBanPhasePopup = suppressBanPhasePopup; }
    
    public boolean isSuppressPickPhasePopup() { return suppressPickPhasePopup; }
    public void setSuppressPickPhasePopup(boolean suppressPickPhasePopup) { this.suppressPickPhasePopup = suppressPickPhasePopup; }
    
    // Auto connect getters and setters
    public boolean isAutoConnectEnabled() { return autoConnectEnabled; }
    public void setAutoConnectEnabled(boolean autoConnectEnabled) { this.autoConnectEnabled = autoConnectEnabled; }
    
    public boolean isAutoReconnectEnabled() { return autoReconnectEnabled; }
    public void setAutoReconnectEnabled(boolean autoReconnectEnabled) { this.autoReconnectEnabled = autoReconnectEnabled; }
    
    public int getReconnectIntervalSeconds() { return reconnectIntervalSeconds; }
    public void setReconnectIntervalSeconds(int reconnectIntervalSeconds) { 
        this.reconnectIntervalSeconds = Math.max(5, Math.min(60, reconnectIntervalSeconds)); 
    }
    
    @Override
    public String toString() {
        return "AutoAcceptConfig{" +
                "autoAcceptEnabled=" + autoAcceptEnabled +
                ", autoBanEnabled=" + championSelect.autoBanEnabled +
                ", autoPickEnabled=" + championSelect.autoPickEnabled +
                ", banChampion=" + championSelect.banChampion +
                ", pickChampion=" + championSelect.pickChampion +
                ", systemTrayEnabled=" + systemTrayEnabled +
                ", autoStartEnabled=" + autoStartEnabled +
                ", minimizeOnClose=" + minimizeOnClose +
                ", suppressReadyCheckPopup=" + suppressReadyCheckPopup +
                ", suppressBanPhasePopup=" + suppressBanPhasePopup +
                ", suppressPickPhasePopup=" + suppressPickPhasePopup +
                '}';
    }
}