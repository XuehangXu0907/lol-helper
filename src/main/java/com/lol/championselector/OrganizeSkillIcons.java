package com.lol.championselector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class OrganizeSkillIcons {
    
    private static final String CHAMPION_BASE_DIR = "src/main/resources/champion";
    private static final String SKILL_ICONS_DIR = "src/main/resources/champion/skill_icons";
    
    public static void main(String[] args) {
        OrganizeSkillIcons organizer = new OrganizeSkillIcons();
        organizer.organizeAllSkillIcons();
    }
    
    public void organizeAllSkillIcons() {
        System.out.println("开始整理技能图标...");
        
        try {
            // 创建目标目录
            Path skillIconsDir = Paths.get(SKILL_ICONS_DIR);
            Files.createDirectories(skillIconsDir);
            
            // 获取所有英雄目录
            File baseDir = new File(CHAMPION_BASE_DIR);
            File[] championDirs = baseDir.listFiles(File::isDirectory);
            
            if (championDirs == null) {
                System.err.println("未找到英雄目录: " + CHAMPION_BASE_DIR);
                return;
            }
            
            int totalFiles = 0;
            int movedFiles = 0;
            List<String> errors = new ArrayList<>();
            
            for (File championDir : championDirs) {
                String championName = championDir.getName();
                
                // 跳过特殊目录
                if (championName.equals("skill_icons") || championName.equals("data") || championName.equals("avatars")) {
                    continue;
                }
                
                System.out.println("处理英雄: " + championName);
                
                File[] iconFiles = championDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
                if (iconFiles != null) {
                    for (File iconFile : iconFiles) {
                        totalFiles++;
                        
                        try {
                            // 创建新的文件名，包含英雄名称以避免冲突
                            String newFileName = championName + "_" + iconFile.getName();
                            Path targetPath = skillIconsDir.resolve(newFileName);
                            
                            // 移动文件
                            Files.move(iconFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                            movedFiles++;
                            
                            System.out.println("  移动: " + iconFile.getName() + " -> " + newFileName);
                            
                        } catch (IOException e) {
                            String error = "移动文件失败: " + iconFile.getPath() + " - " + e.getMessage();
                            errors.add(error);
                            System.err.println(error);
                        }
                    }
                }
            }
            
            // 输出结果
            System.out.println("\n整理完成!");
            System.out.println("总文件数: " + totalFiles);
            System.out.println("成功移动: " + movedFiles);
            System.out.println("失败数: " + errors.size());
            
            if (!errors.isEmpty()) {
                System.err.println("\n错误列表:");
                for (String error : errors) {
                    System.err.println("  " + error);
                }
            }
            
            // 清理空目录
            cleanupEmptyDirectories(championDirs);
            
        } catch (Exception e) {
            System.err.println("整理过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void cleanupEmptyDirectories(File[] championDirs) {
        System.out.println("\n清理空目录...");
        
        for (File championDir : championDirs) {
            String championName = championDir.getName();
            
            // 跳过特殊目录
            if (championName.equals("skill_icons") || championName.equals("data") || championName.equals("avatars")) {
                continue;
            }
            
            try {
                // 检查目录是否为空
                File[] files = championDir.listFiles();
                if (files == null || files.length == 0) {
                    if (championDir.delete()) {
                        System.out.println("删除空目录: " + championName);
                    } else {
                        System.err.println("无法删除目录: " + championName);
                    }
                }
            } catch (Exception e) {
                System.err.println("清理目录时出错: " + championName + " - " + e.getMessage());
            }
        }
    }
}