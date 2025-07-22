package com.lol.championselector.manager;

import com.lol.championselector.api.SkillDataAnalyzer;
import com.lol.championselector.api.TencentChampionApi;
import com.lol.championselector.model.Champion;
import com.lol.championselector.model.ChampionSkills;
import com.lol.championselector.model.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 增强的技能管理器 - 集成腾讯API数据
 */
public class EnhancedSkillsManager {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedSkillsManager.class);
    
    private final TencentChampionApi tencentApi;
    private final SkillsManager fallbackSkillsManager;
    private final Map<String, ChampionSkills> skillsCache;
    private final Map<Integer, String> heroIdMapping; // 腾讯ID到英雄Key的映射
    
    public EnhancedSkillsManager() {
        this.tencentApi = new TencentChampionApi();
        this.fallbackSkillsManager = new SkillsManager();
        this.skillsCache = new ConcurrentHashMap<>();
        this.heroIdMapping = new ConcurrentHashMap<>();
        
        // 初始化时构建ID映射
        initializeHeroIdMapping();
    }
    
    /**
     * 初始化英雄ID映射
     */
    private void initializeHeroIdMapping() {
        tencentApi.getHeroList().thenAccept(heroListResponse -> {
            if (heroListResponse != null && heroListResponse.getHero() != null) {
                for (TencentChampionApi.HeroInfo hero : heroListResponse.getHero()) {
                    try {
                        int heroId = Integer.parseInt(hero.getHeroId());
                        String heroKey = convertNameToKey(hero.getAlias() != null ? hero.getAlias() : hero.getName());
                        heroIdMapping.put(heroId, heroKey);
                        logger.debug("Mapped hero ID {} to key {}", heroId, heroKey);
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid hero ID format: {}", hero.getHeroId());
                    }
                }
                logger.info("Initialized hero ID mapping for {} heroes", heroIdMapping.size());
            }
        }).exceptionally(throwable -> {
            logger.error("Failed to initialize hero ID mapping", throwable);
            return null;
        });
    }
    
    /**
     * 获取英雄技能数据（优先使用腾讯API）
     */
    public CompletableFuture<ChampionSkills> getChampionSkills(Champion champion) {
        if (champion == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        // 先检查缓存
        String cacheKey = champion.getKey();
        if (skillsCache.containsKey(cacheKey)) {
            logger.debug("Returning cached skills for {}", champion.getNameCn());
            return CompletableFuture.completedFuture(skillsCache.get(cacheKey));
        }
        
        // 尝试从腾讯API获取
        Integer tencentHeroId = findTencentHeroId(champion);
        if (tencentHeroId != null) {
            return fetchFromTencentApi(champion, tencentHeroId)
                    .exceptionally(throwable -> {
                        logger.warn("Failed to fetch from Tencent API for {}, falling back to local data", 
                                champion.getNameCn(), throwable);
                        return getFallbackSkills(champion);
                    });
        } else {
            logger.debug("No Tencent hero ID found for {}, using fallback", champion.getNameCn());
            return CompletableFuture.completedFuture(getFallbackSkills(champion));
        }
    }
    
    /**
     * 从腾讯API获取技能数据
     */
    private CompletableFuture<ChampionSkills> fetchFromTencentApi(Champion champion, Integer heroId) {
        return tencentApi.getHeroDetail(heroId).thenApply(response -> {
            if (response == null || response.getSpells() == null) {
                throw new RuntimeException("Invalid response from Tencent API");
            }
            
            List<SkillDataAnalyzer.AnalyzedSkillData> analyzedSkills = 
                    SkillDataAnalyzer.analyzeAllSkills(response.getSpells());
            
            ChampionSkills championSkills = convertToChampionSkills(champion, analyzedSkills);
            
            // 缓存结果
            skillsCache.put(champion.getKey(), championSkills);
            
            logger.info("Successfully fetched and cached skills for {} from Tencent API", champion.getNameCn());
            return championSkills;
        });
    }
    
    /**
     * 查找腾讯英雄ID
     */
    private Integer findTencentHeroId(Champion champion) {
        // 通过英雄Key反向查找
        for (Map.Entry<Integer, String> entry : heroIdMapping.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(champion.getKey()) ||
                entry.getValue().equalsIgnoreCase(champion.getNameEn()) ||
                entry.getValue().equalsIgnoreCase(champion.getNameCn())) {
                return entry.getKey();
            }
        }
        
        // 尝试通过名称匹配
        String heroKey = convertNameToKey(champion.getNameEn());
        for (Map.Entry<Integer, String> entry : heroIdMapping.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(heroKey)) {
                return entry.getKey();
            }
        }
        
        return null;
    }
    
    /**
     * 转换名称为Key格式
     */
    private String convertNameToKey(String name) {
        if (name == null) return "";
        return name.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }
    
    /**
     * 转换分析后的技能数据为ChampionSkills
     */
    private ChampionSkills convertToChampionSkills(Champion champion, 
                                                  List<SkillDataAnalyzer.AnalyzedSkillData> analyzedSkills) {
        List<Skill> skills = new ArrayList<>();
        
        for (SkillDataAnalyzer.AnalyzedSkillData analyzed : analyzedSkills) {
            Skill skill = new Skill();
            skill.setId(analyzed.getSpellKey());
            skill.setName(analyzed.getName());
            skill.setDescription(analyzed.getDescription());
            skill.setTooltip(analyzed.formatDamageInfo());
            skill.setImageUrl(analyzed.getIconPath());
            skill.setVideoUrl(analyzed.getVideoPath());
            
            // 设置伤害数据
            List<Double> primaryDamage = analyzed.getPrimaryDamage();
            if (!primaryDamage.isEmpty()) {
                skill.setDamage(formatDamageRange(primaryDamage));
                skill.setMaxRank(Math.min(primaryDamage.size(), 5));
            }
            
            // 设置系数
            Double primaryRatio = analyzed.getPrimaryRatio();
            if (primaryRatio != null && primaryRatio > 0) {
                skill.setRatio(String.format("%.1f%% AP/AD", primaryRatio * 100));
            }
            
            skills.add(skill);
        }
        
        ChampionSkills championSkills = new ChampionSkills();
        championSkills.setChampionKey(champion.getKey());
        championSkills.setSkills(skills);
        championSkills.setDataSource("Tencent API");
        championSkills.setLastUpdated(System.currentTimeMillis());
        
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
     * 获取后备技能数据
     */
    private ChampionSkills getFallbackSkills(Champion champion) {
        try {
            ChampionSkills fallbackSkills = fallbackSkillsManager.getSkillsAsync(champion.getKey()).get();
            if (fallbackSkills != null) {
                fallbackSkills.setDataSource("Local Data");
                skillsCache.put(champion.getKey(), fallbackSkills);
            }
            return fallbackSkills;
        } catch (Exception e) {
            logger.warn("Failed to get fallback skills for {}", champion.getKey(), e);
            return ChampionSkills.createEmpty();
        }
    }
    
    /**
     * 清除技能缓存
     */
    public void clearCache() {
        skillsCache.clear();
        logger.info("Skills cache cleared");
    }
    
    /**
     * 清除指定英雄的技能缓存
     */
    public void clearCache(String championKey) {
        skillsCache.remove(championKey);
        logger.debug("Cleared cache for champion: {}", championKey);
    }
    
    /**
     * 预加载热门英雄的技能数据
     */
    public void preloadPopularChampions(List<Champion> popularChampions) {
        if (popularChampions == null || popularChampions.isEmpty()) {
            return;
        }
        
        logger.info("Preloading skills for {} popular champions", popularChampions.size());
        
        List<CompletableFuture<ChampionSkills>> futures = new ArrayList<>();
        for (Champion champion : popularChampions) {
            futures.add(getChampionSkills(champion));
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> logger.info("Preloading completed for {} champions", popularChampions.size()))
                .exceptionally(throwable -> {
                    logger.warn("Some preloading operations failed", throwable);
                    return null;
                });
    }
    
    /**
     * 获取缓存状态
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("cachedChampions", skillsCache.size());
        stats.put("heroIdMappings", heroIdMapping.size());
        stats.put("cacheKeys", new ArrayList<>(skillsCache.keySet()));
        return stats;
    }
    
    /**
     * 关闭资源
     */
    public void shutdown() {
        if (tencentApi != null) {
            tencentApi.shutdown();
        }
        skillsCache.clear();
        heroIdMapping.clear();
        logger.info("EnhancedSkillsManager shutdown completed");
    }
}