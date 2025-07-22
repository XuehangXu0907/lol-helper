package com.lol.championselector.downloader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.championselector.api.TencentChampionApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 技能数据下载器 - 从腾讯API下载技能数据到本地
 */
public class SkillDataDownloader {
    private static final Logger logger = LoggerFactory.getLogger(SkillDataDownloader.class);
    
    private static final String DATA_DIR = "skill_data";
    private static final String CHAMPIONS_FILE = "champions.json";
    private static final String SKILLS_DIR = "skills";
    private static final String METADATA_FILE = "metadata.json";
    
    private final TencentChampionApi tencentApi;
    private final ObjectMapper objectMapper;
    private final Path dataDirectory;
    private final Path skillsDirectory;
    private final Map<String, Integer> championIdMapping;
    
    public SkillDataDownloader() {
        this.tencentApi = new TencentChampionApi();
        this.objectMapper = new ObjectMapper();
        this.dataDirectory = Paths.get(DATA_DIR);
        this.skillsDirectory = dataDirectory.resolve(SKILLS_DIR);
        this.championIdMapping = new ConcurrentHashMap<>();
        
        // 创建目录
        createDirectories();
    }
    
    /**
     * 创建必要的目录
     */
    private void createDirectories() {
        try {
            Files.createDirectories(dataDirectory);
            Files.createDirectories(skillsDirectory);
            logger.info("Created data directories: {}", dataDirectory.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to create data directories", e);
            throw new RuntimeException("Failed to create data directories", e);
        }
    }
    
    /**
     * 下载所有英雄数据
     */
    public CompletableFuture<DownloadResult> downloadAllChampionData() {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Starting download of all champion data...");
            
            DownloadResult result = new DownloadResult();
            result.startTime = System.currentTimeMillis();
            
            try {
                // 1. 获取英雄列表
                logger.info("Fetching hero list...");
                TencentChampionApi.HeroListResponse heroList = tencentApi.getHeroList().get();
                
                if (heroList == null || heroList.getHero() == null) {
                    throw new RuntimeException("Failed to fetch hero list");
                }
                
                List<TencentChampionApi.HeroInfo> heroes = heroList.getHero();
                logger.info("Found {} heroes to download", heroes.size());
                
                // 2. 保存英雄列表
                saveHeroList(heroes);
                result.totalHeroes = heroes.size();
                
                // 3. 构建ID映射
                buildChampionIdMapping(heroes);
                
                // 4. 下载每个英雄的技能数据
                AtomicInteger successCount = new AtomicInteger(0);
                AtomicInteger failureCount = new AtomicInteger(0);
                
                List<CompletableFuture<Void>> downloadTasks = new ArrayList<>();
                
                for (TencentChampionApi.HeroInfo hero : heroes) {
                    CompletableFuture<Void> task = downloadHeroSkills(hero)
                            .thenAccept(success -> {
                                if (success) {
                                    int current = successCount.incrementAndGet();
                                    if (current % 10 == 0) {
                                        logger.info("Downloaded skills for {}/{} heroes", current, heroes.size());
                                    }
                                } else {
                                    failureCount.incrementAndGet();
                                }
                            })
                            .exceptionally(throwable -> {
                                logger.warn("Failed to download skills for hero {}: {}", 
                                        hero.getAlias(), throwable.getMessage());
                                failureCount.incrementAndGet();
                                return null;
                            });
                    
                    downloadTasks.add(task);
                    
                    // 控制并发数，避免过多请求
                    if (downloadTasks.size() >= 5) {
                        CompletableFuture.allOf(downloadTasks.toArray(new CompletableFuture[0])).get();
                        downloadTasks.clear();
                        
                        // 短暂休息避免请求过快
                        Thread.sleep(500);
                    }
                }
                
                // 等待剩余任务完成
                if (!downloadTasks.isEmpty()) {
                    CompletableFuture.allOf(downloadTasks.toArray(new CompletableFuture[0])).get();
                }
                
                result.successCount = successCount.get();
                result.failureCount = failureCount.get();
                result.endTime = System.currentTimeMillis();
                
                // 5. 保存元数据
                saveMetadata(result);
                
                logger.info("Download completed! Success: {}, Failed: {}, Duration: {}ms", 
                        result.successCount, result.failureCount, result.getDuration());
                
                return result;
                
            } catch (Exception e) {
                logger.error("Failed to download champion data", e);
                result.endTime = System.currentTimeMillis();
                result.error = e.getMessage();
                throw new RuntimeException("Download failed", e);
            }
        });
    }
    
    /**
     * 下载单个英雄的技能数据
     */
    private CompletableFuture<Boolean> downloadHeroSkills(TencentChampionApi.HeroInfo hero) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int heroId = Integer.parseInt(hero.getHeroId());
                
                // 获取英雄详细数据
                TencentChampionApi.HeroDetailResponse detail = tencentApi.getHeroDetail(heroId).get();
                
                if (detail == null || detail.getSpells() == null) {
                    logger.warn("No skill data found for hero: {} ({})", hero.getAlias(), heroId);
                    return false;
                }
                
                // 直接保存原始技能数据，不进行任何处理
                
                // 创建本地技能数据结构
                LocalSkillData localSkillData = new LocalSkillData();
                localSkillData.heroId = heroId;
                localSkillData.heroKey = hero.getAlias();
                localSkillData.heroName = hero.getName();
                localSkillData.heroTitle = hero.getTitle();
                localSkillData.downloadTime = System.currentTimeMillis();
                localSkillData.rawSkills = detail.getSpells(); // 保存原始技能数据
                
                // 保存到文件
                saveSkillData(hero.getAlias(), localSkillData);
                
                logger.debug("Downloaded skills for hero: {} ({})", hero.getAlias(), heroId);
                return true;
                
            } catch (Exception e) {
                logger.error("Failed to download skills for hero: {} ({})", hero.getAlias(), hero.getHeroId(), e);
                return false;
            }
        });
    }
    
    /**
     * 保存英雄列表到本地
     */
    private void saveHeroList(List<TencentChampionApi.HeroInfo> heroes) throws IOException {
        Path heroListFile = dataDirectory.resolve(CHAMPIONS_FILE);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(heroListFile.toFile(), heroes);
        logger.info("Saved hero list to: {}", heroListFile.toAbsolutePath());
    }
    
    /**
     * 构建英雄ID映射
     */
    private void buildChampionIdMapping(List<TencentChampionApi.HeroInfo> heroes) {
        championIdMapping.clear();
        for (TencentChampionApi.HeroInfo hero : heroes) {
            try {
                int heroId = Integer.parseInt(hero.getHeroId());
                championIdMapping.put(hero.getAlias(), heroId);
            } catch (NumberFormatException e) {
                logger.warn("Invalid hero ID: {} for hero: {}", hero.getHeroId(), hero.getAlias());
            }
        }
        logger.info("Built champion ID mapping for {} heroes", championIdMapping.size());
    }
    
    /**
     * 保存技能数据到本地文件
     */
    private void saveSkillData(String heroKey, LocalSkillData skillData) throws IOException {
        Path skillFile = skillsDirectory.resolve(heroKey + ".json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(skillFile.toFile(), skillData);
    }
    
    /**
     * 保存下载元数据
     */
    private void saveMetadata(DownloadResult result) throws IOException {
        DownloadMetadata metadata = new DownloadMetadata();
        metadata.lastDownload = result.startTime;
        metadata.totalHeroes = result.totalHeroes;
        metadata.successCount = result.successCount;
        metadata.failureCount = result.failureCount;
        metadata.duration = result.getDuration();
        metadata.version = "1.0.0";
        metadata.downloadDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        Path metadataFile = dataDirectory.resolve(METADATA_FILE);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataFile.toFile(), metadata);
        logger.info("Saved download metadata to: {}", metadataFile.toAbsolutePath());
    }
    
    /**
     * 加载本地英雄列表
     */
    public List<TencentChampionApi.HeroInfo> loadLocalHeroList() throws IOException {
        Path heroListFile = dataDirectory.resolve(CHAMPIONS_FILE);
        if (!Files.exists(heroListFile)) {
            return Collections.emptyList();
        }
        
        TencentChampionApi.HeroInfo[] heroes = objectMapper.readValue(
                heroListFile.toFile(), TencentChampionApi.HeroInfo[].class);
        return Arrays.asList(heroes);
    }
    
    /**
     * 加载本地技能数据
     */
    public LocalSkillData loadLocalSkillData(String heroKey) throws IOException {
        Path skillFile = skillsDirectory.resolve(heroKey + ".json");
        if (!Files.exists(skillFile)) {
            return null;
        }
        
        return objectMapper.readValue(skillFile.toFile(), LocalSkillData.class);
    }
    
    /**
     * 加载下载元数据
     */
    public DownloadMetadata loadMetadata() throws IOException {
        Path metadataFile = dataDirectory.resolve(METADATA_FILE);
        if (!Files.exists(metadataFile)) {
            return null;
        }
        
        return objectMapper.readValue(metadataFile.toFile(), DownloadMetadata.class);
    }
    
    /**
     * 检查本地数据是否存在
     */
    public boolean hasLocalData() {
        return Files.exists(dataDirectory.resolve(CHAMPIONS_FILE)) && 
               Files.exists(skillsDirectory) &&
               skillsDirectory.toFile().list().length > 0;
    }
    
    /**
     * 获取本地技能文件数量
     */
    public int getLocalSkillFileCount() {
        File[] files = skillsDirectory.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        return files != null ? files.length : 0;
    }
    
    /**
     * 关闭资源
     */
    public void shutdown() {
        if (tencentApi != null) {
            tencentApi.shutdown();
        }
    }
    
    /**
     * 本地技能数据结构
     */
    public static class LocalSkillData {
        public int heroId;
        public String heroKey;
        public String heroName;
        public String heroTitle;
        public long downloadTime;
        public List<TencentChampionApi.SpellInfo> rawSkills; // 原始技能数据
        
        // 默认构造函数
        public LocalSkillData() {}
    }
    
    /**
     * 下载结果
     */
    public static class DownloadResult {
        public long startTime;
        public long endTime;
        public int totalHeroes;
        public int successCount;
        public int failureCount;
        public String error;
        
        public long getDuration() {
            return endTime - startTime;
        }
        
        public double getSuccessRate() {
            return totalHeroes > 0 ? (double) successCount / totalHeroes * 100 : 0;
        }
        
        @Override
        public String toString() {
            return String.format("DownloadResult{total=%d, success=%d, failed=%d, duration=%dms, successRate=%.1f%%}",
                    totalHeroes, successCount, failureCount, getDuration(), getSuccessRate());
        }
    }
    
    /**
     * 下载元数据
     */
    public static class DownloadMetadata {
        public long lastDownload;
        public int totalHeroes;
        public int successCount;
        public int failureCount;
        public long duration;
        public String version;
        public String downloadDate;
        
        // 默认构造函数
        public DownloadMetadata() {}
    }
}