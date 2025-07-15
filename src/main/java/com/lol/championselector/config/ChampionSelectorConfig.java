package com.lol.championselector.config;

import java.time.Duration;

public class ChampionSelectorConfig {
    // Search configuration
    private int searchDebounceMs = 200;
    private int resizeDebounceMs = 300;
    
    // Network configuration
    private int maxConcurrentDownloads = 5;
    private int connectTimeoutSeconds = 10;
    private int readTimeoutSeconds = 15;
    private int maxRetryAttempts = 3;
    
    // Cache configuration
    private String cacheDirectory = "champion_cache";
    private Duration cacheExpireTime = Duration.ofHours(24);
    private int memoryCacheSize = 200;
    private int skillsCacheSize = 100;
    
    // UI configuration
    private int buttonSize = 100;
    private int minColumns = 3;
    private int maxColumns = 12;
    private int buttonSpacing = 5;
    private int windowMargin = 60;
    
    // Application configuration
    private String applicationTitle = "英雄联盟 - 英雄选择器";
    private String version = "1.0.0";
    private boolean enableLogging = true;
    private String logLevel = "INFO";
    
    public ChampionSelectorConfig() {}
    
    // Search configuration getters and setters
    public int getSearchDebounceMs() {
        return searchDebounceMs;
    }
    
    public void setSearchDebounceMs(int searchDebounceMs) {
        this.searchDebounceMs = Math.max(50, Math.min(1000, searchDebounceMs));
    }
    
    public int getResizeDebounceMs() {
        return resizeDebounceMs;
    }
    
    public void setResizeDebounceMs(int resizeDebounceMs) {
        this.resizeDebounceMs = Math.max(100, Math.min(1000, resizeDebounceMs));
    }
    
    // Network configuration getters and setters
    public int getMaxConcurrentDownloads() {
        return maxConcurrentDownloads;
    }
    
    public void setMaxConcurrentDownloads(int maxConcurrentDownloads) {
        this.maxConcurrentDownloads = Math.max(1, Math.min(20, maxConcurrentDownloads));
    }
    
    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }
    
    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = Math.max(5, Math.min(60, connectTimeoutSeconds));
    }
    
    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }
    
    public void setReadTimeoutSeconds(int readTimeoutSeconds) {
        this.readTimeoutSeconds = Math.max(5, Math.min(120, readTimeoutSeconds));
    }
    
    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }
    
    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = Math.max(0, Math.min(10, maxRetryAttempts));
    }
    
    // Cache configuration getters and setters
    public String getCacheDirectory() {
        return cacheDirectory;
    }
    
    public void setCacheDirectory(String cacheDirectory) {
        this.cacheDirectory = cacheDirectory != null ? cacheDirectory : "champion_cache";
    }
    
    public Duration getCacheExpireTime() {
        return cacheExpireTime;
    }
    
    public void setCacheExpireTime(Duration cacheExpireTime) {
        this.cacheExpireTime = cacheExpireTime != null ? cacheExpireTime : Duration.ofHours(24);
    }
    
    public int getMemoryCacheSize() {
        return memoryCacheSize;
    }
    
    public void setMemoryCacheSize(int memoryCacheSize) {
        this.memoryCacheSize = Math.max(10, Math.min(1000, memoryCacheSize));
    }
    
    public int getSkillsCacheSize() {
        return skillsCacheSize;
    }
    
    public void setSkillsCacheSize(int skillsCacheSize) {
        this.skillsCacheSize = Math.max(10, Math.min(500, skillsCacheSize));
    }
    
    // UI configuration getters and setters
    public int getButtonSize() {
        return buttonSize;
    }
    
    public void setButtonSize(int buttonSize) {
        this.buttonSize = Math.max(50, Math.min(200, buttonSize));
    }
    
    public int getMinColumns() {
        return minColumns;
    }
    
    public void setMinColumns(int minColumns) {
        this.minColumns = Math.max(1, Math.min(10, minColumns));
    }
    
    public int getMaxColumns() {
        return maxColumns;
    }
    
    public void setMaxColumns(int maxColumns) {
        this.maxColumns = Math.max(getMinColumns(), Math.min(20, maxColumns));
    }
    
    public int getButtonSpacing() {
        return buttonSpacing;
    }
    
    public void setButtonSpacing(int buttonSpacing) {
        this.buttonSpacing = Math.max(0, Math.min(20, buttonSpacing));
    }
    
    public int getWindowMargin() {
        return windowMargin;
    }
    
    public void setWindowMargin(int windowMargin) {
        this.windowMargin = Math.max(20, Math.min(200, windowMargin));
    }
    
    // Application configuration getters and setters
    public String getApplicationTitle() {
        return applicationTitle;
    }
    
    public void setApplicationTitle(String applicationTitle) {
        this.applicationTitle = applicationTitle != null ? applicationTitle : "英雄联盟 - 英雄选择器";
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version != null ? version : "1.0.0";
    }
    
    public boolean isEnableLogging() {
        return enableLogging;
    }
    
    public void setEnableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
    }
    
    public String getLogLevel() {
        return logLevel;
    }
    
    public void setLogLevel(String logLevel) {
        if (logLevel != null && 
            (logLevel.equals("TRACE") || logLevel.equals("DEBUG") || 
             logLevel.equals("INFO") || logLevel.equals("WARN") || 
             logLevel.equals("ERROR"))) {
            this.logLevel = logLevel;
        }
    }
    
    // Utility methods
    public int getButtonWidthWithSpacing() {
        return buttonSize + (buttonSpacing * 2);
    }
    
    public boolean isValid() {
        return searchDebounceMs > 0 && 
               resizeDebounceMs > 0 &&
               maxConcurrentDownloads > 0 &&
               connectTimeoutSeconds > 0 &&
               readTimeoutSeconds > 0 &&
               memoryCacheSize > 0 &&
               skillsCacheSize > 0 &&
               buttonSize > 0 &&
               minColumns > 0 &&
               maxColumns >= minColumns &&
               cacheDirectory != null &&
               !cacheDirectory.trim().isEmpty() &&
               applicationTitle != null &&
               version != null;
    }
    
    @Override
    public String toString() {
        return "ChampionSelectorConfig{" +
                "searchDebounceMs=" + searchDebounceMs +
                ", resizeDebounceMs=" + resizeDebounceMs +
                ", maxConcurrentDownloads=" + maxConcurrentDownloads +
                ", cacheDirectory='" + cacheDirectory + '\'' +
                ", memoryCacheSize=" + memoryCacheSize +
                ", buttonSize=" + buttonSize +
                ", minColumns=" + minColumns +
                ", maxColumns=" + maxColumns +
                ", applicationTitle='" + applicationTitle + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}