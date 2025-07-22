package com.lol.championselector.manager;

import com.lol.championselector.api.SkillDataAnalyzer;
import com.lol.championselector.downloader.SkillDataDownloader;
import com.lol.championselector.model.Champion;
import com.lol.championselector.model.ChampionSkills;
import com.lol.championselector.model.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地技能数据管理器 - 管理本地存储的技能数据
 */
public class LocalSkillDataManager {
    private static final Logger logger = LoggerFactory.getLogger(LocalSkillDataManager.class);
    
    private final SkillDataDownloader downloader;
    private final Map<String, ChampionSkills> localCache;
    private final long CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000; // 24小时
    
    public LocalSkillDataManager() {
        this.downloader = new SkillDataDownloader();
        this.localCache = new ConcurrentHashMap<>();
        
        // 启动时检查本地数据状态
        checkLocalDataStatus();
    }
    
    /**
     * 检查本地数据状态
     */
    private void checkLocalDataStatus() {
        try {
            if (downloader.hasLocalData()) {
                SkillDataDownloader.DownloadMetadata metadata = downloader.loadMetadata();
                if (metadata != null) {
                    logger.info("Found local skill data: {} heroes, downloaded on {}", 
                            metadata.totalHeroes, metadata.downloadDate);
                    
                    // 检查数据是否过期
                    long age = System.currentTimeMillis() - metadata.lastDownload;
                    if (age > CACHE_EXPIRY_MS) {
                        logger.warn("Local skill data is {} hours old, consider updating", 
                                age / (1000 * 60 * 60));
                    }
                } else {
                    logger.warn("Local skill data found but no metadata available");
                }
            } else {
                logger.info("No local skill data found, will need to download first");
            }
        } catch (IOException e) {
            logger.error("Error checking local data status", e);
        }
    }
    
    /**
     * 获取英雄技能数据（优先使用本地数据）
     */
    public CompletableFuture<ChampionSkills> getChampionSkills(Champion champion) {
        return CompletableFuture.supplyAsync(() -> {
            if (champion == null) {
                return null;
            }
            
            String cacheKey = champion.getKey();
            
            // 先检查内存缓存
            if (localCache.containsKey(cacheKey)) {
                logger.debug("Returning cached skills for {}", champion.getNameCn());
                return localCache.get(cacheKey);
            }
            
            // 尝试从本地文件加载
            try {
                SkillDataDownloader.LocalSkillData localData = downloader.loadLocalSkillData(cacheKey);
                if (localData != null) {
                    ChampionSkills skills = convertToChampionSkills(champion, localData);
                    localCache.put(cacheKey, skills);
                    logger.debug("Loaded skills for {} from local storage", champion.getNameCn());
                    return skills;
                }
            } catch (IOException e) {
                logger.warn("Failed to load local skills for {}: {}", champion.getNameCn(), e.getMessage());
            }
            
            // 如果没有本地数据，返回空
            logger.debug("No local skills found for {}", champion.getNameCn());
            return ChampionSkills.createEmpty();
        });
    }
    
    /**
     * 转换本地技能数据为ChampionSkills格式
     */
    private ChampionSkills convertToChampionSkills(Champion champion, 
                                                  SkillDataDownloader.LocalSkillData localData) {
        List<Skill> skills = new ArrayList<>();
        
        if (localData.rawSkills != null && !localData.rawSkills.isEmpty()) {
            // First analyze the raw skills
            List<SkillDataAnalyzer.AnalyzedSkillData> analyzedSkills = SkillDataAnalyzer.analyzeAllSkills(localData.rawSkills);
            
            for (SkillDataAnalyzer.AnalyzedSkillData analyzed : analyzedSkills) {
                Skill skill = new Skill();
                skill.setId(analyzed.getSpellKey());
                skill.setName(analyzed.getName());
                skill.setDescription(analyzed.getDescription());
                skill.setTooltip(analyzed.formatDamageInfo());
                skill.setImageUrl(analyzed.getIconPath());
                skill.setVideoUrl(analyzed.getVideoPath());
                
                // 设置伤害数据 - 优先使用解析的伤害数据
                String formattedDamage = "";
                if (analyzed.getDamageData().containsKey("BaseDamage")) {
                    List<Double> baseDamage = analyzed.getDamageData().get("BaseDamage");
                    formattedDamage = formatDamageRange(baseDamage);
                    skill.setMaxRank(Math.min(baseDamage.size(), 5));
                } else if (analyzed.getDamageData().containsKey("PercentageDamage")) {
                    List<Double> percentageDamage = analyzed.getDamageData().get("PercentageDamage");
                    formattedDamage = formatPercentageRange(percentageDamage);
                    skill.setMaxRank(Math.min(percentageDamage.size(), 5));
                } else {
                    List<Double> primaryDamage = analyzed.getPrimaryDamage();
                    if (!primaryDamage.isEmpty()) {
                        formattedDamage = formatDamageRange(primaryDamage);
                        skill.setMaxRank(Math.min(primaryDamage.size(), 5));
                    }
                }
                
                // 设置系数信息
                String ratioInfo = "";
                Map<String, Double> ratios = analyzed.getRatios();
                if (!ratios.isEmpty()) {
                    List<String> ratioStrings = new ArrayList<>();
                    for (Map.Entry<String, Double> entry : ratios.entrySet()) {
                        ratioStrings.add(String.format("%.0f%% %s", entry.getValue() * 100, entry.getKey()));
                    }
                    ratioInfo = String.join(" | ", ratioStrings);
                }
                
                // 组合伤害和系数信息
                if (!formattedDamage.isEmpty() && !ratioInfo.isEmpty()) {
                    skill.setDamage(formattedDamage + " (+" + ratioInfo + ")");
                } else if (!formattedDamage.isEmpty()) {
                    skill.setDamage(formattedDamage);
                } else if (!ratioInfo.isEmpty()) {
                    skill.setDamage(ratioInfo);
                }
                
                if (!ratioInfo.isEmpty()) {
                    skill.setRatio(ratioInfo);
                }
                
                skills.add(skill);
            }
        }
        
        ChampionSkills championSkills = new ChampionSkills();
        championSkills.setChampionKey(champion.getKey());
        championSkills.setSkills(skills);
        championSkills.setDataSource("Local Storage");
        championSkills.setLastUpdated(localData.downloadTime);
        
        return championSkills;
    }
    
    /**
     * 格式化伤害范围
     */
    private String formatDamageRange(List<Double> damages) {
        if (damages.isEmpty()) return "";
        if (damages.size() == 1) return String.format("%.0f", damages.get(0));
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < damages.size(); i++) {
            if (i > 0) sb.append(" / ");
            sb.append(String.format("%.0f", damages.get(i)));
        }
        return sb.toString();
    }
    
    /**
     * 格式化百分比范围
     */
    private String formatPercentageRange(List<Double> percentages) {
        if (percentages.isEmpty()) return "";
        if (percentages.size() == 1) return String.format("%.0f%%", percentages.get(0));
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < percentages.size(); i++) {
            if (i > 0) sb.append(" / ");
            sb.append(String.format("%.0f%%", percentages.get(i)));
        }
        return sb.toString();
    }
    
    /**
     * 下载所有技能数据到本地
     */
    public CompletableFuture<SkillDataDownloader.DownloadResult> downloadAllSkills() {
        logger.info("Starting download of all skill data...");
        
        return downloader.downloadAllChampionData()
                .thenApply(result -> {
                    // 清除内存缓存，强制重新加载
                    localCache.clear();
                    logger.info("Download completed: {}", result);
                    return result;
                });
    }
    
    /**
     * 检查是否需要更新本地数据
     */
    public boolean needsUpdate() {
        try {
            if (!downloader.hasLocalData()) {
                return true; // 没有本地数据，需要下载
            }
            
            SkillDataDownloader.DownloadMetadata metadata = downloader.loadMetadata();
            if (metadata == null) {
                return true; // 没有元数据，需要重新下载
            }
            
            long age = System.currentTimeMillis() - metadata.lastDownload;
            return age > CACHE_EXPIRY_MS; // 数据过期，需要更新
            
        } catch (IOException e) {
            logger.error("Error checking update status", e);
            return true; // 发生错误，建议更新
        }
    }
    
    /**
     * 获取本地数据信息
     */
    public LocalDataInfo getLocalDataInfo() {
        LocalDataInfo info = new LocalDataInfo();
        
        try {
            info.hasData = downloader.hasLocalData();
            info.skillFileCount = downloader.getLocalSkillFileCount();
            
            SkillDataDownloader.DownloadMetadata metadata = downloader.loadMetadata();
            if (metadata != null) {
                info.totalHeroes = metadata.totalHeroes;
                info.successCount = metadata.successCount;
                info.failureCount = metadata.failureCount;
                info.lastDownload = metadata.lastDownload;
                info.downloadDate = metadata.downloadDate;
                info.version = metadata.version;
                
                long age = System.currentTimeMillis() - metadata.lastDownload;
                info.ageHours = age / (1000 * 60 * 60);
                info.needsUpdate = age > CACHE_EXPIRY_MS;
            }
            
            info.cacheSize = localCache.size();
            
        } catch (IOException e) {
            logger.error("Error getting local data info", e);
            info.error = e.getMessage();
        }
        
        return info;
    }
    
    /**
     * 清除本地缓存
     */
    public void clearCache() {
        localCache.clear();
        logger.info("Local skill data cache cleared");
    }
    
    /**
     * 预加载热门英雄的技能数据
     */
    public void preloadPopularChampions(List<Champion> popularChampions) {
        if (popularChampions == null || popularChampions.isEmpty()) {
            return;
        }
        
        logger.info("Preloading skills for {} popular champions from local storage", popularChampions.size());
        
        List<CompletableFuture<ChampionSkills>> futures = new ArrayList<>();
        for (Champion champion : popularChampions) {
            futures.add(getChampionSkills(champion));
        }
        
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenRun(() -> logger.info("Preloading completed for {} champions", popularChampions.size()))
                .exceptionally(throwable -> {
                    logger.warn("Some preloading operations failed", throwable);
                    return null;
                });
    }
    
    /**
     * 关闭资源
     */
    public void shutdown() {
        if (downloader != null) {
            downloader.shutdown();
        }
        localCache.clear();
        logger.info("LocalSkillDataManager shutdown completed");
    }
    
    /**
     * 本地数据信息
     */
    public static class LocalDataInfo {
        public boolean hasData = false;
        public int skillFileCount = 0;
        public int totalHeroes = 0;
        public int successCount = 0;
        public int failureCount = 0;
        public long lastDownload = 0;
        public String downloadDate = "";
        public String version = "";
        public long ageHours = 0;
        public boolean needsUpdate = true;
        public int cacheSize = 0;
        public String error = null;
        
        public String getFormattedLastDownload() {
            if (lastDownload == 0) return "Never";
            
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(lastDownload), ZoneId.systemDefault());
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        
        public double getSuccessRate() {
            return totalHeroes > 0 ? (double) successCount / totalHeroes * 100 : 0;
        }
        
        @Override
        public String toString() {
            return String.format("LocalDataInfo{hasData=%s, files=%d, heroes=%d, success=%d, age=%dh, needsUpdate=%s}",
                    hasData, skillFileCount, totalHeroes, successCount, ageHours, needsUpdate);
        }
    }
}