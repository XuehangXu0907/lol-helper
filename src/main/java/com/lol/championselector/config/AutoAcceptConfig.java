package com.lol.championselector.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.championselector.model.Champion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AutoAcceptConfig {
    private static final Logger logger = LoggerFactory.getLogger(AutoAcceptConfig.class);
    private static final String CONFIG_FILE = "auto-accept-config.json";
    
    private boolean autoAcceptEnabled = true;
    private boolean autoDeclineEnabled = false;
    private int checkIntervalMs = 1000;
    
    private ChampionSelectConfig championSelect = new ChampionSelectConfig();
    
    public static class ChampionSelectConfig {
        private boolean autoBanEnabled = false;
        private boolean autoPickEnabled = false;
        private ChampionInfo banChampion;
        private ChampionInfo pickChampion;
        
        public ChampionSelectConfig() {
            // 默认Ban英雄：艾克
            this.banChampion = new ChampionInfo("Ekko", "艾克", "时间刺客");
            // 默认Pick英雄：金克丝
            this.pickChampion = new ChampionInfo("Jinx", "金克丝", "暴走萝莉");
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
            this.championId = champion.getId() != null ? Integer.parseInt(champion.getId()) : null;
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
    
    @Override
    public String toString() {
        return "AutoAcceptConfig{" +
                "autoAcceptEnabled=" + autoAcceptEnabled +
                ", autoBanEnabled=" + championSelect.autoBanEnabled +
                ", autoPickEnabled=" + championSelect.autoPickEnabled +
                ", banChampion=" + championSelect.banChampion +
                ", pickChampion=" + championSelect.pickChampion +
                '}';
    }
}