package com.lol.championselector;

import com.lol.championselector.api.TencentChampionApi;
import com.lol.championselector.api.SkillDataAnalyzer;
import com.lol.championselector.manager.EnhancedSkillsManager;
import com.lol.championselector.model.Champion;
import com.lol.championselector.model.ChampionSkills;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 腾讯API功能测试类
 */
public class TencentApiTest {
    private static final Logger logger = LoggerFactory.getLogger(TencentApiTest.class);
    
    public static void main(String[] args) {
        TencentApiTest test = new TencentApiTest();
        
        try {
            logger.info("Starting Tencent API tests...");
            
            // 测试1: 获取英雄列表
            test.testHeroList();
            
            // 测试2: 获取特定英雄数据
            test.testHeroDetail();
            
            // 测试3: 测试增强技能管理器
            test.testEnhancedSkillsManager();
            
            logger.info("All tests completed successfully!");
            
        } catch (Exception e) {
            logger.error("Test failed", e);
        }
    }
    
    /**
     * 测试获取英雄列表
     */
    private void testHeroList() {
        logger.info("=== Testing Hero List ===");
        
        TencentChampionApi api = new TencentChampionApi();
        
        try {
            CompletableFuture<TencentChampionApi.HeroListResponse> future = api.getHeroList();
            TencentChampionApi.HeroListResponse response = future.get();
            
            if (response != null && response.getHero() != null) {
                logger.info("Successfully fetched {} heroes", response.getHero().size());
                
                // 显示前5个英雄的信息
                for (int i = 0; i < Math.min(5, response.getHero().size()); i++) {
                    TencentChampionApi.HeroInfo hero = response.getHero().get(i);
                    logger.info("Hero {}: ID={}, Name={}, Alias={}, Title={}", 
                            i + 1, hero.getHeroId(), hero.getName(), 
                            hero.getAlias(), hero.getTitle());
                }
            } else {
                logger.warn("No hero data received");
            }
            
        } catch (Exception e) {
            logger.error("Failed to test hero list", e);
        } finally {
            api.shutdown();
        }
    }
    
    /**
     * 测试获取特定英雄详细数据
     */
    private void testHeroDetail() {
        logger.info("=== Testing Hero Detail ===");
        
        TencentChampionApi api = new TencentChampionApi();
        
        try {
            // 测试安妮（ID=1）的数据
            int annieId = 1;
            CompletableFuture<TencentChampionApi.HeroDetailResponse> future = api.getHeroDetail(annieId);
            TencentChampionApi.HeroDetailResponse response = future.get();
            
            if (response != null) {
                logger.info("Successfully fetched detail for hero ID: {}", annieId);
                
                if (response.getHero() != null) {
                    TencentChampionApi.DetailedHeroInfo hero = response.getHero();
                    logger.info("Hero Info: Name={}, Alias={}, Title={}", 
                            hero.getName(), hero.getAlias(), hero.getTitle());
                }
                
                if (response.getSpells() != null) {
                    logger.info("Found {} spells", response.getSpells().size());
                    
                    for (int i = 0; i < response.getSpells().size(); i++) {
                        TencentChampionApi.SpellInfo spell = response.getSpells().get(i);
                        logger.info("Spell {}: Key={}, Name={}", 
                                i + 1, spell.getSpellKey(), spell.getName());
                        
                        // 分析技能数据
                        SkillDataAnalyzer.AnalyzedSkillData analyzed = 
                                SkillDataAnalyzer.analyzeSkill(spell);
                        if (analyzed != null) {
                            logger.info("  Damage Info: {}", analyzed.formatDamageInfo());
                        }
                    }
                }
            } else {
                logger.warn("No detail data received for hero ID: {}", annieId);
            }
            
        } catch (Exception e) {
            logger.error("Failed to test hero detail", e);
        } finally {
            api.shutdown();
        }
    }
    
    /**
     * 测试增强技能管理器
     */
    private void testEnhancedSkillsManager() {
        logger.info("=== Testing Enhanced Skills Manager ===");
        
        EnhancedSkillsManager manager = new EnhancedSkillsManager();
        
        try {
            // 创建测试英雄
            Champion testChampion = new Champion();
            testChampion.setKey("Annie");
            testChampion.setNameEn("Annie");
            testChampion.setNameCn("安妮");
            testChampion.setTitle("黑暗之女");
            
            // 获取技能数据
            CompletableFuture<ChampionSkills> future = manager.getChampionSkills(testChampion);
            ChampionSkills skills = future.get();
            
            if (skills != null) {
                logger.info("Successfully fetched skills for {}", testChampion.getNameCn());
                logger.info("Data source: {}", skills.getDataSource());
                logger.info("Last updated: {}", skills.getLastUpdated());
                
                List<com.lol.championselector.model.Skill> allSkills = skills.getAllSkills();
                logger.info("Total skills: {}", allSkills.size());
                
                for (int i = 0; i < allSkills.size(); i++) {
                    com.lol.championselector.model.Skill skill = allSkills.get(i);
                    logger.info("Skill {}: ID={}, Name={}, Damage={}, Ratio={}", 
                            i + 1, skill.getId(), skill.getName(), 
                            skill.getDamage(), skill.getRatio());
                }
                
                // 测试缓存
                logger.info("Cache stats: {}", manager.getCacheStats());
                
            } else {
                logger.warn("No skills data received for {}", testChampion.getNameCn());
            }
            
        } catch (Exception e) {
            logger.error("Failed to test enhanced skills manager", e);
        } finally {
            manager.shutdown();
        }
    }
}