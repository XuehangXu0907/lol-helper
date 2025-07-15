package com.lol.championselector.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class DataSyncManager {
    private static final Logger logger = LoggerFactory.getLogger(DataSyncManager.class);
    
    private static final String VERSION_API = "https://ddragon.leagueoflegends.com/api/versions.json";
    private static final String CHAMPIONS_API = "https://ddragon.leagueoflegends.com/cdn/%s/data/zh_CN/champion.json";
    private static final String CHAMPION_DETAIL_API = "https://ddragon.leagueoflegends.com/cdn/%s/data/zh_CN/champion/%s.json";
    
    private static final String DATA_DIR = "champion_data";
    private static final String CHAMPIONS_FILE = "champions.json";
    private static final String VERSION_FILE = "version.txt";
    private static final String LAST_UPDATE_FILE = "last_update.txt";
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public DataSyncManager() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
        this.objectMapper = new ObjectMapper();
        
        // 创建数据目录
        createDataDirectory();
    }
    
    private void createDataDirectory() {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            boolean created = dataDir.mkdirs();
            if (created) {
                logger.info("Created data directory: {}", DATA_DIR);
            } else {
                logger.warn("Failed to create data directory: {}", DATA_DIR);
            }
        }
    }
    
    public boolean syncChampionData() {
        try {
            logger.info("Starting champion data synchronization...");
            
            // 1. 获取最新版本
            String latestVersion = getLatestVersion();
            if (latestVersion == null) {
                logger.error("Failed to get latest version");
                return false;
            }
            
            logger.info("Latest version: {}", latestVersion);
            
            // 2. 检查是否需要更新
            String currentVersion = getCurrentVersion();
            if (latestVersion.equals(currentVersion)) {
                logger.info("Data is already up to date (version: {})", currentVersion);
                return true;
            }
            
            // 3. 获取英雄列表
            JsonNode championsData = getChampionsData(latestVersion);
            if (championsData == null) {
                logger.error("Failed to get champions data");
                return false;
            }
            
            // 4. 保存基础数据
            if (!saveChampionsData(championsData, latestVersion)) {
                logger.error("Failed to save champions data");
                return false;
            }
            
            // 5. 获取详细技能数据
            if (!syncDetailedSkillsData(latestVersion, championsData)) {
                logger.error("Failed to sync detailed skills data");
                return false;
            }
            
            // 6. 更新版本信息
            updateVersionInfo(latestVersion);
            
            logger.info("Champion data synchronization completed successfully");
            return true;
            
        } catch (Exception e) {
            logger.error("Error during data synchronization", e);
            return false;
        }
    }
    
    private String getLatestVersion() {
        try {
            Request request = new Request.Builder()
                .url(VERSION_API)
                .build();
                
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("Failed to get version info, HTTP: {}", response.code());
                    return null;
                }
                
                ResponseBody body = response.body();
                if (body == null) {
                    logger.error("Empty response body for version info");
                    return null;
                }
                
                JsonNode versions = objectMapper.readTree(body.string());
                if (versions.isArray() && versions.size() > 0) {
                    return versions.get(0).asText();
                }
            }
        } catch (Exception e) {
            logger.error("Error getting latest version", e);
        }
        
        return null;
    }
    
    private JsonNode getChampionsData(String version) {
        try {
            String url = String.format(CHAMPIONS_API, version);
            Request request = new Request.Builder()
                .url(url)
                .build();
                
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("Failed to get champions data, HTTP: {}", response.code());
                    return null;
                }
                
                ResponseBody body = response.body();
                if (body == null) {
                    logger.error("Empty response body for champions data");
                    return null;
                }
                
                return objectMapper.readTree(body.string());
            }
        } catch (Exception e) {
            logger.error("Error getting champions data", e);
        }
        
        return null;
    }
    
    private boolean syncDetailedSkillsData(String version, JsonNode championsData) {
        try {
            JsonNode data = championsData.path("data");
            if (!data.isObject()) {
                logger.error("Invalid champions data structure");
                return false;
            }
            
            int totalChampions = data.size();
            int successCount = 0;
            int errorCount = 0;
            
            logger.info("Starting to sync detailed data for {} champions", totalChampions);
            
            // 创建技能数据目录
            File skillsDir = new File(DATA_DIR, "skills");
            if (!skillsDir.exists()) {
                skillsDir.mkdirs();
            }
            
            for (JsonNode championNode : data) {
                String championKey = championNode.path("id").asText();
                
                try {
                    JsonNode detailedData = getChampionDetailedData(version, championKey);
                    if (detailedData != null) {
                        File skillFile = new File(skillsDir, championKey + ".json");
                        objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValue(skillFile, detailedData);
                        successCount++;
                        
                        if (successCount % 10 == 0) {
                            logger.info("Synced {}/{} champions", successCount, totalChampions);
                        }
                    } else {
                        errorCount++;
                        logger.warn("Failed to get detailed data for champion: {}", championKey);
                    }
                    
                    // 添加延迟避免请求过于频繁
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    errorCount++;
                    logger.error("Error syncing champion: {}", championKey, e);
                }
            }
            
            logger.info("Detailed data sync completed: {} success, {} errors", successCount, errorCount);
            return errorCount == 0 || (double) successCount / totalChampions > 0.8; // 80%以上成功率认为可接受
            
        } catch (Exception e) {
            logger.error("Error during detailed skills data sync", e);
            return false;
        }
    }
    
    private JsonNode getChampionDetailedData(String version, String championKey) {
        try {
            String url = String.format(CHAMPION_DETAIL_API, version, championKey);
            Request request = new Request.Builder()
                .url(url)
                .build();
                
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return null;
                }
                
                ResponseBody body = response.body();
                if (body == null) {
                    return null;
                }
                
                return objectMapper.readTree(body.string());
            }
        } catch (Exception e) {
            logger.debug("Error getting detailed data for champion: {}", championKey, e);
        }
        
        return null;
    }
    
    private boolean saveChampionsData(JsonNode championsData, String version) {
        try {
            File championsFile = new File(DATA_DIR, CHAMPIONS_FILE);
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(championsFile, championsData);
            
            logger.info("Saved champions data to: {}", championsFile.getAbsolutePath());
            return true;
            
        } catch (Exception e) {
            logger.error("Error saving champions data", e);
            return false;
        }
    }
    
    private String getCurrentVersion() {
        try {
            File versionFile = new File(DATA_DIR, VERSION_FILE);
            if (versionFile.exists()) {
                return new String(java.nio.file.Files.readAllBytes(versionFile.toPath())).trim();
            }
        } catch (Exception e) {
            logger.debug("Error reading current version", e);
        }
        
        return "";
    }
    
    private void updateVersionInfo(String version) {
        try {
            // 保存版本信息
            File versionFile = new File(DATA_DIR, VERSION_FILE);
            java.nio.file.Files.write(versionFile.toPath(), version.getBytes());
            
            // 保存更新时间
            File updateFile = new File(DATA_DIR, LAST_UPDATE_FILE);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            java.nio.file.Files.write(updateFile.toPath(), timestamp.getBytes());
            
            logger.info("Updated version info to: {}", version);
            
        } catch (Exception e) {
            logger.error("Error updating version info", e);
        }
    }
    
    public boolean isDataAvailable() {
        File championsFile = new File(DATA_DIR, CHAMPIONS_FILE);
        return championsFile.exists();
    }
    
    public String getDataVersion() {
        return getCurrentVersion();
    }
    
    public String getLastUpdateTime() {
        try {
            File updateFile = new File(DATA_DIR, LAST_UPDATE_FILE);
            if (updateFile.exists()) {
                return new String(java.nio.file.Files.readAllBytes(updateFile.toPath())).trim();
            }
        } catch (Exception e) {
            logger.debug("Error reading last update time", e);
        }
        
        return "未知";
    }
    
    public void shutdown() {
        try {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
            logger.info("DataSyncManager shut down successfully");
        } catch (Exception e) {
            logger.warn("Error during DataSyncManager shutdown", e);
        }
    }
}