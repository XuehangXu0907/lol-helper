package com.lol.championselector;

import com.lol.championselector.manager.LocalSkillDataManager;
import com.lol.championselector.model.Champion;
import com.lol.championselector.model.ChampionSkills;
import com.lol.championselector.model.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 本地数据测试 - 测试技能数据下载和本地存储功能
 */
public class LocalDataTest {
    private static final Logger logger = LoggerFactory.getLogger(LocalDataTest.class);
    
    public static void main(String[] args) {
        LocalDataTest test = new LocalDataTest();
        
        try {
            logger.info("Starting local data tests...");
            
            // 测试1: 显示当前数据状态
            test.testDataStatus();
            
            // 测试2: 下载少量数据测试（安全测试）
            test.testSmallDownload();
            
            // 测试3: 测试本地数据加载
            test.testLocalDataLoading();
            
            logger.info("All local data tests completed!");
            
        } catch (Exception e) {
            logger.error("Test failed", e);
        }
    }
    
    /**
     * 测试数据状态
     */
    private void testDataStatus() {
        logger.info("=== Testing Data Status ===");
        
        LocalSkillDataManager manager = new LocalSkillDataManager();
        
        try {
            LocalSkillDataManager.LocalDataInfo info = manager.getLocalDataInfo();
            
            logger.info("Local data info: {}", info);
            
            if (info.hasData) {
                logger.info("Found existing local data:");
                logger.info("  - Skill files: {}", info.skillFileCount);
                logger.info("  - Total heroes: {}", info.totalHeroes);
                logger.info("  - Success rate: {:.1f}%", info.getSuccessRate());
                logger.info("  - Last download: {}", info.getFormattedLastDownload());
                logger.info("  - Age: {} hours", info.ageHours);
                logger.info("  - Needs update: {}", info.needsUpdate);
            } else {
                logger.info("No local data found");
            }
            
        } catch (Exception e) {
            logger.error("Failed to test data status", e);
        } finally {
            manager.shutdown();
        }
    }
    
    /**
     * 测试小量下载（仅下载前5个英雄作为测试）
     */
    private void testSmallDownload() {
        logger.info("=== Testing Small Download ===");
        
        // 注意：这是一个安全的测试，只下载前几个英雄
        // 如果要下载所有数据，请使用 DataSyncTool
        
        logger.info("This test would download hero data, but is disabled for safety.");
        logger.info("To download all data, use: java -cp ... com.lol.championselector.DataSyncTool download");
        
        // 如果要启用测试下载，取消注释以下代码：
        /*
        SkillDataDownloader downloader = new SkillDataDownloader();
        
        try {
            // 这里可以实现一个测试用的小量下载
            logger.info("Small download test would run here");
            
        } catch (Exception e) {
            logger.error("Failed to test small download", e);
        } finally {
            downloader.shutdown();
        }
        */
    }
    
    /**
     * 测试本地数据加载
     */
    private void testLocalDataLoading() {
        logger.info("=== Testing Local Data Loading ===");
        
        LocalSkillDataManager manager = new LocalSkillDataManager();
        
        try {
            // 创建测试英雄
            Champion[] testChampions = {
                createTestChampion("Annie", "安妮", "黑暗之女"),
                createTestChampion("Ahri", "阿狸", "九尾妖狐"),
                createTestChampion("Yasuo", "亚索", "疾风剑豪")
            };
            
            for (Champion champion : testChampions) {
                logger.info("Testing champion: {}", champion.getNameCn());
                
                ChampionSkills skills = manager.getChampionSkills(champion).get();
                
                if (skills != null && !skills.isEmpty()) {
                    logger.info("  Found skills for {}: {} skills from {}", 
                            champion.getNameCn(), skills.getAllSkills().size(), skills.getDataSource());
                    
                    // 显示技能详情
                    List<Skill> allSkills = skills.getAllSkills();
                    for (int i = 0; i < Math.min(2, allSkills.size()); i++) {
                        Skill skill = allSkills.get(i);
                        logger.info("    Skill {}: {} - {}", 
                                i + 1, skill.getName(), skill.getDamage());
                    }
                } else {
                    logger.info("  No skills found for {}", champion.getNameCn());
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to test local data loading", e);
        } finally {
            manager.shutdown();
        }
    }
    
    /**
     * 创建测试用英雄
     */
    private Champion createTestChampion(String key, String nameCn, String title) {
        Champion champion = new Champion();
        champion.setKey(key);
        champion.setNameEn(key);
        champion.setNameCn(nameCn);
        champion.setTitle(title);
        return champion;
    }
}