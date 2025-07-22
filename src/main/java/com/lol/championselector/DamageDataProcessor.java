package com.lol.championselector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.championselector.parser.DamageDataParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 伤害数据处理器 - 重新处理已下载的技能数据，提取伤害信息
 */
public class DamageDataProcessor {
    private static final Logger logger = LoggerFactory.getLogger(DamageDataProcessor.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public static void main(String[] args) {
        DamageDataProcessor processor = new DamageDataProcessor();
        
        try {
            logger.info("开始处理本地技能数据，提取伤害信息...");
            
            // 处理单个英雄作为测试
            if (args.length > 0) {
                String heroKey = args[0];
                processor.processHeroData(heroKey);
            } else {
                // 处理Alistar作为测试
                processor.processHeroData("Alistar");
            }
            
        } catch (Exception e) {
            logger.error("处理失败", e);
        }
    }
    
    /**
     * 处理单个英雄的技能数据
     */
    public void processHeroData(String heroKey) {
        logger.info("处理英雄: {}", heroKey);
        
        try {
            Path skillFilePath = Paths.get("skill_data", "skills", heroKey + ".json");
            
            if (!Files.exists(skillFilePath)) {
                logger.warn("技能文件不存在: {}", skillFilePath);
                return;
            }
            
            // 读取现有数据
            HeroSkillData heroData = objectMapper.readValue(skillFilePath.toFile(), HeroSkillData.class);
            
            logger.info("英雄: {} ({})", heroData.heroName, heroData.heroTitle);
            logger.info("技能数量: {}", heroData.skills.size());
            
            boolean hasUpdates = false;
            
            // 处理每个技能
            for (SkillInfo skill : heroData.skills) {
                logger.info("\n处理技能: {} ({})", skill.name, skill.spellKey);
                logger.info("描述: {}", skill.description);
                
                // 使用伤害解析器分析描述
                DamageDataParser.ParsedDamageData parsedDamage = 
                    DamageDataParser.parseDamageFromDescription(skill.description);
                
                if (!parsedDamage.isEmpty()) {
                    logger.info("解析结果: {}", parsedDamage);
                    
                    // 更新技能数据
                    updateSkillDamageData(skill, parsedDamage);
                    hasUpdates = true;
                    
                    // 显示格式化的伤害信息
                    String damageInfo = DamageDataParser.formatDamageInfo(parsedDamage);
                    logger.info("格式化伤害: {}", damageInfo);
                } else {
                    logger.info("未找到伤害数据");
                }
            }
            
            // 如果有更新，保存文件
            if (hasUpdates) {
                logger.info("\n保存更新后的数据到: {}", skillFilePath);
                objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(skillFilePath.toFile(), heroData);
            }
            
        } catch (Exception e) {
            logger.error("处理英雄 {} 失败", heroKey, e);
        }
    }
    
    /**
     * 更新技能的伤害数据
     */
    private void updateSkillDamageData(SkillInfo skill, DamageDataParser.ParsedDamageData parsedDamage) {
        // 更新基础伤害
        if (!parsedDamage.baseDamage.isEmpty()) {
            skill.primaryDamage = new ArrayList<>(parsedDamage.baseDamage);
            skill.damageData.put("BaseDamage", new ArrayList<>(parsedDamage.baseDamage));
        }
        
        // 更新百分比伤害
        if (!parsedDamage.percentageDamage.isEmpty()) {
            skill.damageData.put("PercentageDamage", new ArrayList<>(parsedDamage.percentageDamage));
        }
        
        // 更新系数
        if (!parsedDamage.scalingRatios.isEmpty()) {
            for (Map.Entry<String, Double> entry : parsedDamage.scalingRatios.entrySet()) {
                skill.ratios.put(entry.getKey(), entry.getValue());
            }
            
            // 设置主要系数（第一个找到的）
            skill.primaryRatio = parsedDamage.scalingRatios.values().iterator().next();
        }
        
        logger.info("  更新伤害数据: {} 个数值, {} 个系数", 
                skill.damageData.size(), skill.ratios.size());
    }
    
    /**
     * 英雄技能数据结构
     */
    public static class HeroSkillData {
        public int heroId;
        public String heroKey;
        public String heroName;
        public String heroTitle;
        public long downloadTime;
        public List<SkillInfo> skills = new ArrayList<>();
    }
    
    /**
     * 技能信息结构
     */
    public static class SkillInfo {
        public String spellKey;
        public String name;
        public String description;
        public String iconPath;
        public String videoPath;
        public Map<String, List<Double>> damageData = new HashMap<>();
        public Map<String, Double> ratios = new HashMap<>();
        public double primaryRatio = 0.0;
        public List<Double> primaryDamage = new ArrayList<>();
    }
}