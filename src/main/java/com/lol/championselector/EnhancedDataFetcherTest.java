package com.lol.championselector;

import com.lol.championselector.api.EnhancedRiotDataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class for Enhanced Riot Data Fetcher
 * Downloads complete champion data including stats and skills
 */
public class EnhancedDataFetcherTest {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedDataFetcherTest.class);
    
    public static void main(String[] args) {
        System.out.println("=== Enhanced Riot Data Fetcher Test ===");
        System.out.println("获取完整的英雄数据（包括三围属性和技能详情）...\n");
        
        EnhancedRiotDataFetcher fetcher = new EnhancedRiotDataFetcher();
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Fetch complete champion data
            boolean success = fetcher.fetchCompleteChampionData();
            
            long endTime = System.currentTimeMillis();
            long duration = (endTime - startTime) / 1000;
            
            if (success) {
                System.out.println("\n✅ 数据获取成功！");
                System.out.println("总耗时: " + duration + " 秒");
                System.out.println("\n保存的数据文件:");
                System.out.println("1. 英雄摘要（包含所有英雄三围）:");
                System.out.println("   - src/main/resources/champion/data/champions_summary.json");
                System.out.println("\n2. 每个英雄的完整数据:");
                System.out.println("   - src/main/resources/champion/data/full/[ChampionKey]_complete.json");
                System.out.println("\n3. 所有英雄汇总数据:");
                System.out.println("   - src/main/resources/champion/data/all_champions_data.json");
                System.out.println("\n4. 元数据信息:");
                System.out.println("   - src/main/resources/champion/data/metadata.json");
                System.out.println("   - src/main/resources/champion/data/version.txt");
                
                System.out.println("\n数据内容包括:");
                System.out.println("- 英雄基本信息（中英文名称、称号）");
                System.out.println("- 完整三围属性（生命值、攻击力、护甲、魔抗等）");
                System.out.println("- 所有技能详情（被动 + QWER）");
                System.out.println("- 技能数值（伤害、冷却、消耗、范围）");
                System.out.println("- 技能描述（中英双语）");
                System.out.println("- 使用技巧（友方/敌方）");
                
            } else {
                System.out.println("\n❌ 数据获取失败，请查看日志了解详情");
            }
            
        } catch (Exception e) {
            logger.error("测试过程中发生错误", e);
            System.out.println("\n❌ 测试失败: " + e.getMessage());
        } finally {
            fetcher.shutdown();
        }
        
        System.out.println("\n=== 测试完成 ===");
    }
}