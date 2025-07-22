package com.lol.championselector;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class ReorganizeSkillIcons {
    
    private static final String SKILL_ICONS_DIR = "src/main/resources/champion/skill_icons";
    
    public static void main(String[] args) {
        ReorganizeSkillIcons reorganizer = new ReorganizeSkillIcons();
        reorganizer.reorganizeSkillIcons();
    }
    
    public void reorganizeSkillIcons() {
        System.out.println("重新整理技能图标，创建英雄小文件夹...");
        
        try {
            Path skillIconsDir = Paths.get(SKILL_ICONS_DIR);
            if (!Files.exists(skillIconsDir)) {
                System.err.println("技能图标目录不存在: " + SKILL_ICONS_DIR);
                return;
            }
            
            // 获取所有图标文件
            File[] iconFiles = skillIconsDir.toFile().listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".png") && name.contains("_"));
            
            if (iconFiles == null) {
                System.err.println("未找到技能图标文件");
                return;
            }
            
            int totalFiles = iconFiles.length;
            int processedFiles = 0;
            List<String> errors = new ArrayList<>();
            
            System.out.println("找到 " + totalFiles + " 个技能图标文件");
            
            for (File iconFile : iconFiles) {
                try {
                    String fileName = iconFile.getName();
                    // 提取英雄名称 (文件名格式: ChampionName_SkillName.png)
                    String championName = fileName.substring(0, fileName.indexOf('_'));
                    String skillFileName = fileName.substring(fileName.indexOf('_') + 1);
                    
                    // 创建英雄目录
                    Path championDir = skillIconsDir.resolve(championName);
                    Files.createDirectories(championDir);
                    
                    // 移动文件
                    Path targetPath = championDir.resolve(skillFileName);
                    Files.move(iconFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                    
                    processedFiles++;
                    System.out.println("移动: " + fileName + " -> " + championName + "/" + skillFileName);
                    
                } catch (Exception e) {
                    String error = "处理文件失败: " + iconFile.getName() + " - " + e.getMessage();
                    errors.add(error);
                    System.err.println(error);
                }
            }
            
            // 输出结果
            System.out.println("\n重新整理完成!");
            System.out.println("总文件数: " + totalFiles);
            System.out.println("成功处理: " + processedFiles);
            System.out.println("失败数: " + errors.size());
            
            if (!errors.isEmpty()) {
                System.err.println("\n错误列表:");
                for (String error : errors) {
                    System.err.println("  " + error);
                }
            }
            
        } catch (Exception e) {
            System.err.println("重新整理过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}