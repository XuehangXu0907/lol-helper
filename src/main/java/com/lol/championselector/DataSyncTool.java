package com.lol.championselector;

import com.lol.championselector.downloader.SkillDataDownloader;
import com.lol.championselector.manager.LocalSkillDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

/**
 * 数据同步工具 - 独立的命令行工具用于下载和管理技能数据
 */
public class DataSyncTool {
    private static final Logger logger = LoggerFactory.getLogger(DataSyncTool.class);
    
    public static void main(String[] args) {
        DataSyncTool tool = new DataSyncTool();
        
        if (args.length > 0) {
            // 命令行模式
            tool.runCommand(args[0]);
        } else {
            // 交互模式
            tool.runInteractiveMode();
        }
    }
    
    /**
     * 运行指定命令
     */
    private void runCommand(String command) {
        logger.info("Running command: {}", command);
        
        try {
            switch (command.toLowerCase()) {
                case "download":
                case "sync":
                    downloadAllData();
                    break;
                case "status":
                case "info":
                    showDataInfo();
                    break;
                case "help":
                    showHelp();
                    break;
                default:
                    logger.error("Unknown command: {}. Use 'help' to see available commands.", command);
            }
        } catch (Exception e) {
            logger.error("Command execution failed", e);
        }
    }
    
    /**
     * 运行交互模式
     */
    private void runInteractiveMode() {
        logger.info("=== LOL Helper - 技能数据同步工具 ===");
        logger.info("输入命令或 'help' 查看帮助，'quit' 退出");
        
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.print("\n> ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                continue;
            }
            
            if ("quit".equalsIgnoreCase(input) || "exit".equalsIgnoreCase(input)) {
                logger.info("再见！");
                break;
            }
            
            try {
                handleInteractiveCommand(input);
            } catch (Exception e) {
                logger.error("命令执行失败: {}", e.getMessage());
            }
        }
        
        scanner.close();
    }
    
    /**
     * 处理交互式命令
     */
    private void handleInteractiveCommand(String input) {
        String[] parts = input.split("\\s+");
        String command = parts[0].toLowerCase();
        
        switch (command) {
            case "download":
            case "sync":
                downloadAllData();
                break;
            case "status":
            case "info":
                showDataInfo();
                break;
            case "check":
                checkUpdateStatus();
                break;
            case "clear":
                clearCache();
                break;
            case "help":
                showHelp();
                break;
            default:
                logger.warn("未知命令: {}. 输入 'help' 查看可用命令.", command);
        }
    }
    
    /**
     * 下载所有数据
     */
    private void downloadAllData() {
        logger.info("开始下载所有英雄技能数据...");
        
        SkillDataDownloader downloader = new SkillDataDownloader();
        
        try {
            CompletableFuture<SkillDataDownloader.DownloadResult> future = downloader.downloadAllChampionData();
            
            // 显示进度（简单的等待动画）
            showProgressAnimation(future);
            
            SkillDataDownloader.DownloadResult result = future.get();
            
            logger.info("\n下载完成！");
            logger.info("总计英雄: {}", result.totalHeroes);
            logger.info("成功下载: {}", result.successCount);
            logger.info("下载失败: {}", result.failureCount);
            logger.info("成功率: {:.1f}%", result.getSuccessRate());
            logger.info("耗时: {}ms", result.getDuration());
            
            if (result.failureCount > 0) {
                logger.warn("有 {} 个英雄下载失败，可以稍后重试", result.failureCount);
            }
            
        } catch (Exception e) {
            logger.error("下载失败", e);
        } finally {
            downloader.shutdown();
        }
    }
    
    /**
     * 显示进度动画
     */
    private void showProgressAnimation(CompletableFuture<?> future) {
        String[] spinner = {"|", "/", "-", "\\"};
        int i = 0;
        
        while (!future.isDone()) {
            System.out.print("\r下载中 " + spinner[i % spinner.length]);
            i++;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.print("\r");
    }
    
    /**
     * 显示数据信息
     */
    private void showDataInfo() {
        logger.info("=== 本地数据状态 ===");
        
        LocalSkillDataManager manager = new LocalSkillDataManager();
        
        try {
            LocalSkillDataManager.LocalDataInfo info = manager.getLocalDataInfo();
            
            if (info.error != null) {
                logger.error("获取数据信息失败: {}", info.error);
                return;
            }
            
            logger.info("是否有数据: {}", info.hasData ? "是" : "否");
            
            if (info.hasData) {
                logger.info("技能文件数: {}", info.skillFileCount);
                logger.info("英雄总数: {}", info.totalHeroes);
                logger.info("成功下载: {}", info.successCount);
                logger.info("下载失败: {}", info.failureCount);
                logger.info("成功率: {:.1f}%", info.getSuccessRate());
                logger.info("上次下载: {}", info.getFormattedLastDownload());
                logger.info("数据年龄: {} 小时", info.ageHours);
                logger.info("版本: {}", info.version);
                logger.info("需要更新: {}", info.needsUpdate ? "是" : "否");
                logger.info("内存缓存: {} 个英雄", info.cacheSize);
            } else {
                logger.info("没有找到本地数据，建议运行 'download' 命令下载数据");
            }
            
        } catch (Exception e) {
            logger.error("获取数据信息失败", e);
        } finally {
            manager.shutdown();
        }
    }
    
    /**
     * 检查更新状态
     */
    private void checkUpdateStatus() {
        logger.info("检查更新状态...");
        
        LocalSkillDataManager manager = new LocalSkillDataManager();
        
        try {
            boolean needsUpdate = manager.needsUpdate();
            
            if (needsUpdate) {
                logger.info("本地数据需要更新！建议运行 'download' 命令");
            } else {
                logger.info("本地数据是最新的");
            }
            
        } catch (Exception e) {
            logger.error("检查更新状态失败", e);
        } finally {
            manager.shutdown();
        }
    }
    
    /**
     * 清除缓存
     */
    private void clearCache() {
        logger.info("清除内存缓存...");
        
        LocalSkillDataManager manager = new LocalSkillDataManager();
        
        try {
            manager.clearCache();
            logger.info("缓存已清除");
        } catch (Exception e) {
            logger.error("清除缓存失败", e);
        } finally {
            manager.shutdown();
        }
    }
    
    /**
     * 显示帮助信息
     */
    private void showHelp() {
        logger.info("=== 可用命令 ===");
        logger.info("download, sync    - 下载所有英雄技能数据");
        logger.info("status, info      - 显示本地数据状态");
        logger.info("check             - 检查是否需要更新");
        logger.info("clear             - 清除内存缓存");
        logger.info("help              - 显示此帮助信息");
        logger.info("quit, exit        - 退出程序");
        logger.info("");
        logger.info("=== 命令行用法 ===");
        logger.info("java -cp ... com.lol.championselector.DataSyncTool <command>");
        logger.info("例如: java -cp ... com.lol.championselector.DataSyncTool download");
    }
}