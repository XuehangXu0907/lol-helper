package com.lol.championselector;

import com.lol.championselector.api.RiotDataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class for RiotDataFetcher
 * Fetches the latest champion data from Riot API with bilingual support
 */
public class RiotDataFetcherTest {
    private static final Logger logger = LoggerFactory.getLogger(RiotDataFetcherTest.class);
    
    public static void main(String[] args) {
        System.out.println("=== Riot Data Fetcher Test ===");
        System.out.println("正在从 Riot API 获取最新的英雄数据（中英双语）...\n");
        
        RiotDataFetcher fetcher = new RiotDataFetcher();
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Fetch latest champion data
            boolean success = fetcher.fetchLatestChampionData();
            
            long endTime = System.currentTimeMillis();
            long duration = (endTime - startTime) / 1000;
            
            if (success) {
                System.out.println("\n✅ 数据获取成功！");
                System.out.println("总耗时: " + duration + " 秒");
                System.out.println("\n数据保存位置:");
                System.out.println("- 英雄列表: src/main/resources/champion/data/champions_bilingual.json");
                System.out.println("- 详细数据: src/main/resources/champion/data/skills/");
                System.out.println("- 版本信息: src/main/resources/champion/data/version.txt");
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