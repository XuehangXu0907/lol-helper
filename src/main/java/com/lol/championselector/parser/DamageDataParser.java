package com.lol.championselector.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 伤害数据解析器 - 从技能描述文本中提取伤害数值
 */
public class DamageDataParser {
    private static final Logger logger = LoggerFactory.getLogger(DamageDataParser.class);
    
    // 匹配伤害数值的正则表达式模式
    private static final Pattern DAMAGE_PATTERN = Pattern.compile("【([0-9/+.%]+)】");
    private static final Pattern SCALING_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)%【([^】]+)】");
    
    /**
     * 解析技能描述中的伤害数据
     */
    public static ParsedDamageData parseDamageFromDescription(String description) {
        if (description == null || description.isEmpty()) {
            return new ParsedDamageData();
        }
        
        ParsedDamageData result = new ParsedDamageData();
        
        try {
            // 1. 提取所有【】中的数值
            Matcher damageMatcher = DAMAGE_PATTERN.matcher(description);
            while (damageMatcher.find()) {
                String damageText = damageMatcher.group(1);
                
                // 检查是否为数值序列（如: 80/110/140/170/200）
                if (damageText.contains("/") && !damageText.contains("%")) {
                    List<Double> damageValues = parseNumberSequence(damageText);
                    if (!damageValues.isEmpty()) {
                        result.baseDamage = damageValues;
                        logger.debug("Parsed base damage: {}", damageValues);
                    }
                }
                // 检查是否为百分比数值序列（如: 55%/65%/75%）
                else if (damageText.contains("/") && damageText.contains("%")) {
                    List<Double> percentageValues = parsePercentageSequence(damageText);
                    if (!percentageValues.isEmpty()) {
                        result.percentageDamage = percentageValues;
                        logger.debug("Parsed percentage damage: {}", percentageValues);
                    }
                }
            }
            
            // 2. 提取技能系数（如: 70%【法术强度】）
            Matcher scalingMatcher = SCALING_PATTERN.matcher(description);
            while (scalingMatcher.find()) {
                double ratio = Double.parseDouble(scalingMatcher.group(1)) / 100.0;
                String scalingType = scalingMatcher.group(2);
                
                result.scalingRatios.put(scalingType, ratio);
                logger.debug("Parsed scaling: {}% {}", ratio * 100, scalingType);
            }
            
            // 3. 提取固定数值（如随等级提升的伤害）
            if (description.contains("随等级提升")) {
                result.hasLevelScaling = true;
            }
            
        } catch (Exception e) {
            logger.warn("Error parsing damage from description: {}", description, e);
        }
        
        return result;
    }
    
    /**
     * 解析数值序列（如: "80/110/140/170/200"）
     */
    private static List<Double> parseNumberSequence(String sequence) {
        List<Double> values = new ArrayList<>();
        
        try {
            String[] parts = sequence.split("/");
            for (String part : parts) {
                if (!part.trim().isEmpty()) {
                    values.add(Double.parseDouble(part.trim()));
                }
            }
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse number sequence: {}", sequence);
        }
        
        return values;
    }
    
    /**
     * 解析百分比序列（如: "55%/65%/75%"）
     */
    private static List<Double> parsePercentageSequence(String sequence) {
        List<Double> values = new ArrayList<>();
        
        try {
            String[] parts = sequence.split("/");
            for (String part : parts) {
                String cleanPart = part.trim().replace("%", "");
                if (!cleanPart.isEmpty()) {
                    values.add(Double.parseDouble(cleanPart));
                }
            }
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse percentage sequence: {}", sequence);
        }
        
        return values;
    }
    
    /**
     * 格式化伤害信息显示
     */
    public static String formatDamageInfo(ParsedDamageData damageData) {
        if (damageData == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // 基础伤害
        if (damageData.baseDamage != null && !damageData.baseDamage.isEmpty()) {
            sb.append("基础伤害: ");
            for (int i = 0; i < damageData.baseDamage.size(); i++) {
                if (i > 0) sb.append("/");
                sb.append(String.format("%.0f", damageData.baseDamage.get(i)));
            }
        }
        
        // 百分比伤害
        if (damageData.percentageDamage != null && !damageData.percentageDamage.isEmpty()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append("百分比: ");
            for (int i = 0; i < damageData.percentageDamage.size(); i++) {
                if (i > 0) sb.append("/");
                sb.append(String.format("%.0f%%", damageData.percentageDamage.get(i)));
            }
        }
        
        // 技能系数
        if (damageData.scalingRatios != null && !damageData.scalingRatios.isEmpty()) {
            for (Map.Entry<String, Double> entry : damageData.scalingRatios.entrySet()) {
                if (sb.length() > 0) sb.append(" | ");
                sb.append(String.format("%.0f%% %s", entry.getValue() * 100, entry.getKey()));
            }
        }
        
        // 等级缩放
        if (damageData.hasLevelScaling) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append("随等级提升");
        }
        
        return sb.toString();
    }
    
    /**
     * 解析后的伤害数据
     */
    public static class ParsedDamageData {
        public List<Double> baseDamage = new ArrayList<>();
        public List<Double> percentageDamage = new ArrayList<>();
        public Map<String, Double> scalingRatios = new HashMap<>();
        public boolean hasLevelScaling = false;
        
        public boolean isEmpty() {
            return baseDamage.isEmpty() && 
                   percentageDamage.isEmpty() && 
                   scalingRatios.isEmpty() && 
                   !hasLevelScaling;
        }
        
        @Override
        public String toString() {
            return String.format("ParsedDamageData{base=%s, percentage=%s, scaling=%s, levelScaling=%s}",
                    baseDamage, percentageDamage, scalingRatios, hasLevelScaling);
        }
    }
}