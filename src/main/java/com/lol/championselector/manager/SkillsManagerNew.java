package com.lol.championselector.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lol.championselector.model.ChampionSkills;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

public class SkillsManagerNew {
    private static final Logger logger = LoggerFactory.getLogger(SkillsManagerNew.class);
    
    // Enhanced cache configuration with memory limits
    private static final int MAX_CACHE_SIZE = 120; // Reduced from 200
    private static final long MAX_MEMORY_USAGE_MB = 32; // 32MB memory limit
    private static final Duration CACHE_EXPIRE_TIME = Duration.ofHours(1); // Reduced from 2h
    
    private final Cache<String, ChampionSkills> skillsCache;
    private final LocalDataManager localDataManager;
    private volatile boolean isShuttingDown = false;
    
    // Cache statistics removed - not currently used
    
    public SkillsManagerNew() {
        this.skillsCache = Caffeine.newBuilder()
            .maximumSize(MAX_CACHE_SIZE)
            .maximumWeight(MAX_MEMORY_USAGE_MB * 1024 * 1024) // Convert MB to bytes
            .weigher((String key, ChampionSkills skills) -> {
                // Estimate memory usage for skills
                int skillCount = skills.getSkills() != null ? skills.getSkills().size() : 0;
                return Math.max(1000, skillCount * 2000); // Rough estimate per skill
            })
            .expireAfterWrite(CACHE_EXPIRE_TIME)
            .recordStats()
            .removalListener((key, value, cause) -> {
                logger.debug("Skills cache entry removed: {} (cause: {})", key, cause);
            })
            .build();
            
        this.localDataManager = new LocalDataManager();
    }
    
    public CompletableFuture<ChampionSkills> getChampionSkills(String championKey) {
        if (championKey == null || championKey.isEmpty()) {
            return CompletableFuture.completedFuture(ChampionSkills.createEmpty());
        }
        
        if (isShuttingDown) {
            return CompletableFuture.completedFuture(ChampionSkills.createEmpty());
        }
        
        // 检查缓存
        ChampionSkills cached = skillsCache.getIfPresent(championKey);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        // 异步加载技能数据
        return CompletableFuture.supplyAsync(() -> {
            if (isShuttingDown) {
                return ChampionSkills.createEmpty();
            }
            
            try {
                ChampionSkills skills = localDataManager.loadChampionSkills(championKey);
                if (skills != null && !skills.isEmpty()) {
                    // 使用Community Dragon数据增强技能信息
                    if (!isShuttingDown) {
                        localDataManager.enhanceSkillsWithCommunityDragonData(championKey, skills);
                    }
                    
                    if (!isShuttingDown) {
                        skillsCache.put(championKey, skills);
                    }
                    logger.debug("Loaded and enhanced skills for champion: {}", championKey);
                    return skills;
                } else {
                    ChampionSkills emptySkills = ChampionSkills.createEmpty();
                    if (!isShuttingDown) {
                        skillsCache.put(championKey, emptySkills);
                    }
                    return emptySkills;
                }
                
            } catch (Exception e) {
                if (!isShuttingDown) {
                    logger.warn("Error loading skills for champion: {}", championKey, e);
                }
                ChampionSkills emptySkills = ChampionSkills.createEmpty();
                if (!isShuttingDown) {
                    skillsCache.put(championKey, emptySkills);
                }
                return emptySkills;
            }
        }, ForkJoinPool.commonPool());
    }
    
    public void clearCache() {
        skillsCache.invalidateAll();
        logger.info("Skills cache cleared");
    }
    
    public void clearCacheForChampion(String championKey) {
        skillsCache.invalidate(championKey);
        logger.debug("Cleared cache for champion: {}", championKey);
    }
    
    public long getCacheSize() {
        return skillsCache.estimatedSize();
    }
    
    public CacheStats getCacheStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats stats = skillsCache.stats();
        return new CacheStats(
            skillsCache.estimatedSize(),
            stats.hitCount(),
            stats.missCount(),
            stats.hitRate(),
            estimateMemoryUsage()
        );
    }
    
    private long estimateMemoryUsage() {
        // Rough estimation: each skills entry uses about 10KB
        return skillsCache.estimatedSize() * 10 * 1024;
    }
    
    public static class CacheStats {
        public final long size;
        public final long hitCount;
        public final long missCount;
        public final double hitRate;
        public final long estimatedMemoryUsage;
        
        public CacheStats(long size, long hitCount, long missCount, double hitRate, long estimatedMemoryUsage) {
            this.size = size;
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.hitRate = hitRate;
            this.estimatedMemoryUsage = estimatedMemoryUsage;
        }
        
        @Override
        public String toString() {
            return String.format("SkillsCacheStats{size=%d, hitRate=%.2f%%, memoryUsage=%dKB}", 
                               size, hitRate * 100, estimatedMemoryUsage / 1024);
        }
    }
    
    public void preloadPopularChampions() {
        if (isShuttingDown) {
            return;
        }
        
        // 预加载一些热门英雄的技能数据 - 减少数量以控制内存使用
        String[] popularChampions = {
            "Ahri", "Yasuo", "Jinx", "Thresh", "LeeSin", "Zed", "Vayne", "Riven", 
            "Katarina", "Lucian", "Ezreal", "Jhin", "KaiSa"
        };
        
        logger.info("Preloading skills for {} popular champions", popularChampions.length);
        
        for (String championKey : popularChampions) {
            if (isShuttingDown) {
                break;
            }
            
            getChampionSkills(championKey).thenAccept(skills -> {
                if (!isShuttingDown) {
                    logger.debug("Preloaded skills for: {}", championKey);
                }
            }).exceptionally(throwable -> {
                if (!isShuttingDown) {
                    logger.debug("Failed to preload skills for: {}", championKey);
                }
                return null;
            });
        }
    }
    
    public void shutdown() {
        logger.info("Shutting down SkillsManagerNew...");
        isShuttingDown = true;
        
        try {
            // Log final cache statistics
            logger.info("Final skills cache stats: {}", getCacheStats());
            
            // Clear cache to free memory immediately
            skillsCache.invalidateAll();
            
            // LocalDataManager doesn't need explicit shutdown as it has no resources to clean up
            // It only contains ObjectMapper and List<Champion> which will be garbage collected
            
            logger.info("SkillsManagerNew shut down successfully");
        } catch (Exception e) {
            logger.warn("Error during SkillsManagerNew shutdown", e);
        }
    }
}