package com.lol.championselector;

import com.lol.championselector.manager.DataSyncManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataSyncApp {
    private static final Logger logger = LoggerFactory.getLogger(DataSyncApp.class);
    
    public static void main(String[] args) {
        logger.info("Starting champion data synchronization...");
        
        DataSyncManager syncManager = new DataSyncManager();
        
        try {
            boolean success = syncManager.syncChampionData();
            
            if (success) {
                logger.info("Data synchronization completed successfully!");
                logger.info("Data version: {}", syncManager.getDataVersion());
                logger.info("Last update: {}", syncManager.getLastUpdateTime());
                System.out.println("✅ 数据同步成功完成！");
                System.out.println("📦 数据版本: " + syncManager.getDataVersion());
                System.out.println("🕒 更新时间: " + syncManager.getLastUpdateTime());
            } else {
                logger.error("Data synchronization failed!");
                System.out.println("❌ 数据同步失败！请检查网络连接和日志。");
                System.exit(1);
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error during data synchronization", e);
            System.out.println("❌ 数据同步发生意外错误: " + e.getMessage());
            System.exit(1);
        } finally {
            syncManager.shutdown();
        }
        
        System.out.println("🎉 可以重新启动应用程序使用最新数据！");
    }
}