package com.lol.championselector;

import com.lol.championselector.manager.CommunityDragonSyncManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommunityDragonSyncApp {
    private static final Logger logger = LoggerFactory.getLogger(CommunityDragonSyncApp.class);
    
    public static void main(String[] args) {
        logger.info("Starting Community Dragon data synchronization...");
        
        CommunityDragonSyncManager syncManager = new CommunityDragonSyncManager();
        
        try {
            // 测试单个英雄
            logger.info("Testing sync with Aatrox...");
            boolean aatroxSuccess = syncManager.syncChampionSkillData("Aatrox");
            
            if (aatroxSuccess) {
                System.out.println("✅ Aatrox数据同步成功！");
                logger.info("Aatrox sync successful, proceeding with all champions...");
                
                // 同步所有英雄
                boolean allSuccess = syncManager.syncAllChampions();
                
                if (allSuccess) {
                    logger.info("All champions Community Dragon sync completed successfully!");
                    System.out.println("✅ 所有英雄Community Dragon数据同步成功完成！");
                } else {
                    logger.warn("Some champions failed to sync from Community Dragon");
                    System.out.println("⚠️ 部分英雄数据同步失败，请检查日志。");
                }
            } else {
                logger.error("Aatrox test sync failed, aborting full sync");
                System.out.println("❌ Aatrox测试同步失败，终止完整同步。");
                System.exit(1);
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error during Community Dragon synchronization", e);
            System.out.println("❌ Community Dragon数据同步发生意外错误: " + e.getMessage());
            System.exit(1);
        } finally {
            syncManager.shutdown();
        }
        
        System.out.println("🎉 Community Dragon数据同步完成！现在技能伤害数值应该更加准确。");
    }
}