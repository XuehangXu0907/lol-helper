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
    
    private final Cache<String, ChampionSkills> skillsCache;
    private final LocalDataManager localDataManager;
    
    public SkillsManagerNew() {
        this.skillsCache = Caffeine.newBuilder()
            .maximumSize(200)
            .expireAfterWrite(Duration.ofHours(2))
            .build();
            
        this.localDataManager = new LocalDataManager();
    }
    
    public CompletableFuture<ChampionSkills> getChampionSkills(String championKey) {
        if (championKey == null || championKey.isEmpty()) {
            return CompletableFuture.completedFuture(ChampionSkills.createEmpty());
        }
        
        // 检查缓存
        ChampionSkills cached = skillsCache.getIfPresent(championKey);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        // 异步加载技能数据
        return CompletableFuture.supplyAsync(() -> {
            try {
                ChampionSkills skills = localDataManager.loadChampionSkills(championKey);
                if (skills != null && !skills.isEmpty()) {
                    // 使用Community Dragon数据增强技能信息
                    localDataManager.enhanceSkillsWithCommunityDragonData(championKey, skills);
                    
                    skillsCache.put(championKey, skills);
                    logger.debug("Loaded and enhanced skills for champion: {}", championKey);
                    return skills;
                } else {
                    ChampionSkills emptySkills = ChampionSkills.createEmpty();
                    skillsCache.put(championKey, emptySkills);
                    return emptySkills;
                }
                
            } catch (Exception e) {
                logger.warn("Error loading skills for champion: {}", championKey, e);
                ChampionSkills emptySkills = ChampionSkills.createEmpty();
                skillsCache.put(championKey, emptySkills);
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
    
    public void preloadPopularChampions() {
        // 预加载一些热门英雄的技能数据
        String[] popularChampions = {
            "Ahri", "Yasuo", "Jinx", "Thresh", "LeeSin", "Zed", "Vayne", "Riven", 
            "Katarina", "Draven", "Lucian", "Ezreal", "Jhin", "Graves", "KaiSa"
        };
        
        logger.info("Preloading skills for {} popular champions", popularChampions.length);
        
        for (String championKey : popularChampions) {
            getChampionSkills(championKey).thenAccept(skills -> {
                logger.debug("Preloaded skills for: {}", championKey);
            }).exceptionally(throwable -> {
                logger.debug("Failed to preload skills for: {}", championKey);
                return null;
            });
        }
    }
    
    public void shutdown() {
        try {
            skillsCache.invalidateAll();
            logger.info("SkillsManagerNew shut down successfully");
        } catch (Exception e) {
            logger.warn("Error during SkillsManagerNew shutdown", e);
        }
    }
}