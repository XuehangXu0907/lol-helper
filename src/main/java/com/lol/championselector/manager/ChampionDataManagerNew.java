package com.lol.championselector.manager;

import com.lol.championselector.model.Champion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class ChampionDataManagerNew {
    private static final Logger logger = LoggerFactory.getLogger(ChampionDataManagerNew.class);
    
    private final List<Champion> allChampions;
    private final Map<String, Champion> championMap;
    private final LocalDataManager localDataManager;
    private final DataSyncManager dataSyncManager;
    
    public ChampionDataManagerNew() {
        this.allChampions = new ArrayList<>();
        this.championMap = new HashMap<>();
        this.localDataManager = new LocalDataManager();
        this.dataSyncManager = new DataSyncManager();
        
        initializeChampionData();
    }
    
    private void initializeChampionData() {
        try {
            // 检查本地数据是否可用
            if (!dataSyncManager.isDataAvailable()) {
                logger.info("No local data found, attempting to sync from official API...");
                
                boolean syncSuccess = dataSyncManager.syncChampionData();
                if (!syncSuccess) {
                    logger.error("Failed to sync data from API, falling back to hardcoded data");
                    initializeHardcodedData();
                    return;
                }
            }
            
            // 加载本地数据
            boolean loadSuccess = localDataManager.loadChampions();
            if (loadSuccess) {
                List<Champion> loadedChampions = localDataManager.getAllChampions();
                allChampions.addAll(loadedChampions);
                
                // 构建索引
                for (Champion champion : allChampions) {
                    championMap.put(champion.getKey(), champion);
                }
                
                logger.info("Initialized {} champions from local data (version: {})", 
                           allChampions.size(), dataSyncManager.getDataVersion());
            } else {
                logger.error("Failed to load local data, falling back to hardcoded data");
                initializeHardcodedData();
            }
            
        } catch (Exception e) {
            logger.error("Error initializing champion data, falling back to hardcoded data", e);
            initializeHardcodedData();
        }
    }
    
    private void initializeHardcodedData() {
        // 清空现有数据
        allChampions.clear();
        championMap.clear();
        
        // 添加一些基础英雄作为后备数据
        addChampion("Aatrox", "266", "Aatrox", "亚托克斯", 
                   Arrays.asList("上单", "战士", "暗裔剑魔", "亚托克斯"), "暗裔剑魔", 
                   Arrays.asList("Fighter", "Tank"));
        
        addChampion("Ahri", "103", "Ahri", "阿狸", 
                   Arrays.asList("中单", "法师", "九尾妖狐", "阿狸"), "九尾妖狐", 
                   Arrays.asList("Mage", "Assassin"));
        
        addChampion("Akali", "84", "Akali", "阿卡丽", 
                   Arrays.asList("中单", "上单", "刺客", "离群之刺", "阿卡丽"), "离群之刺", 
                   Arrays.asList("Assassin"));
        
        // 添加更多基础英雄...
        logger.warn("Using hardcoded fallback data with {} champions", allChampions.size());
    }
    
    private void addChampion(String key, String id, String nameEn, String nameCn, 
                           List<String> keywords, String title, List<String> tags) {
        Champion champion = new Champion(key, id, nameEn, nameCn, keywords, title, tags);
        allChampions.add(champion);
        championMap.put(key, champion);
    }
    
    public List<Champion> getAllChampions() {
        return new ArrayList<>(allChampions);
    }
    
    public Champion getChampion(String key) {
        return championMap.get(key);
    }
    
    public List<Champion> searchChampions(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllChampions();
        }
        
        return localDataManager != null ? 
            localDataManager.searchChampions(query) : 
            searchChampionsInMemory(query);
    }
    
    private List<Champion> searchChampionsInMemory(String query) {
        String searchQuery = query.toLowerCase().trim();
        return allChampions.stream()
            .filter(champion -> matchesQuery(champion, searchQuery))
            .collect(Collectors.toList());
    }
    
    private boolean matchesQuery(Champion champion, String query) {
        return champion.getKeywords().stream()
            .anyMatch(keyword -> keyword.toLowerCase().contains(query));
    }
    
    public Champion getRandomChampion() {
        if (allChampions.isEmpty()) {
            return null;
        }
        Random random = new Random();
        return allChampions.get(random.nextInt(allChampions.size()));
    }
    
    public List<Champion> getChampionsByTag(String tag) {
        return allChampions.stream()
            .filter(champion -> champion.getTags().contains(tag))
            .collect(Collectors.toList());
    }
    
    public int getChampionCount() {
        return allChampions.size();
    }
    
    public String getDataVersion() {
        return dataSyncManager.getDataVersion();
    }
    
    public String getLastUpdateTime() {
        return dataSyncManager.getLastUpdateTime();
    }
    
    public boolean refreshData() {
        try {
            logger.info("Refreshing champion data...");
            boolean success = dataSyncManager.syncChampionData();
            
            if (success) {
                // 重新加载数据
                allChampions.clear();
                championMap.clear();
                
                boolean loadSuccess = localDataManager.loadChampions();
                if (loadSuccess) {
                    List<Champion> loadedChampions = localDataManager.getAllChampions();
                    allChampions.addAll(loadedChampions);
                    
                    for (Champion champion : allChampions) {
                        championMap.put(champion.getKey(), champion);
                    }
                    
                    logger.info("Data refresh completed: {} champions loaded", allChampions.size());
                    return true;
                }
            }
            
            logger.error("Failed to refresh champion data");
            return false;
            
        } catch (Exception e) {
            logger.error("Error during data refresh", e);
            return false;
        }
    }
    
    public void shutdown() {
        if (dataSyncManager != null) {
            dataSyncManager.shutdown();
        }
    }
}