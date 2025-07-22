package com.lol.championselector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EnhancedSkillDataExtractor {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedSkillDataExtractor.class);
    
    public static void main(String[] args) {
        System.out.println("增强技能数据提取器");
        System.out.println("正在分析英雄技能数据...");
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            
            // 读取几个测试英雄的技能数据
            String[] testChampions = {"Aatrox", "Ahri", "Brand", "Jinx", "Lux"};
            
            for (String champion : testChampions) {
                System.out.println("\n=== " + champion + " ===");
                
                File skillFile = new File("src/main/resources/champion/data/skills/" + champion + ".json");
                if (skillFile.exists()) {
                    JsonNode championData = mapper.readTree(skillFile);
                    JsonNode spells = championData.path("data").path(champion).path("spells");
                    
                    if (spells.isArray()) {
                        for (int i = 0; i < spells.size(); i++) {
                            JsonNode spell = spells.get(i);
                            String skillName = spell.path("name").asText();
                            String skillId = spell.path("id").asText();
                            
                            System.out.println("技能 " + (i + 1) + ": " + skillName + " (" + skillId + ")");
                            
                            // 提取实际伤害数值
                            String damage = extractRealDamageValues(spell);
                            if (!damage.isEmpty()) {
                                System.out.println("  伤害: " + damage);
                            }
                            
                            // 提取冷却时间
                            String cooldown = extractCooldown(spell);
                            if (!cooldown.isEmpty()) {
                                System.out.println("  冷却: " + cooldown);
                            }
                            
                            // 提取消耗
                            String cost = extractCost(spell);
                            if (!cost.isEmpty()) {
                                System.out.println("  消耗: " + cost);
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            System.out.println("分析失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String extractRealDamageValues(JsonNode spell) {
        try {
            // 方法1: 从effectBurn提取
            JsonNode effectBurn = spell.path("effectBurn");
            if (effectBurn.isArray()) {
                List<String> damageValues = new ArrayList<>();
                for (int i = 1; i < effectBurn.size(); i++) { // 跳过索引0
                    String value = effectBurn.get(i).asText();
                    if (!value.equals("0") && !value.isEmpty() && value.matches(".*\\d.*")) {
                        // 检查是否包含伤害相关的数值
                        if (value.contains("/") && value.split("/").length >= 3) {
                            damageValues.add("等级" + i + ": " + value);
                        }
                    }
                }
                if (!damageValues.isEmpty()) {
                    return String.join("; ", damageValues);
                }
            }
            
            // 方法2: 从effect数组提取
            JsonNode effect = spell.path("effect");
            if (effect.isArray()) {
                for (int i = 1; i < effect.size(); i++) {
                    JsonNode effectArray = effect.get(i);
                    if (effectArray.isArray() && effectArray.size() >= 5) {
                        List<String> values = new ArrayList<>();
                        for (JsonNode value : effectArray) {
                            if (value.isNumber() && value.asDouble() > 0) {
                                values.add(String.valueOf(value.asInt()));
                            }
                        }
                        if (values.size() >= 3) {
                            return String.join("/", values);
                        }
                    }
                }
            }
            
            // 方法3: 从tooltip变量推断
            String tooltip = spell.path("tooltip").asText();
            if (tooltip.contains("damage") && tooltip.contains("{{")) {
                return "变量伤害 (查看游戏内详情)";
            }
            
        } catch (Exception e) {
            logger.debug("Error extracting damage for spell", e);
        }
        
        return "";
    }
    
    private static String extractCooldown(JsonNode spell) {
        JsonNode cooldown = spell.path("cooldown");
        if (cooldown.isArray() && cooldown.size() >= 5) {
            List<String> values = new ArrayList<>();
            for (JsonNode value : cooldown) {
                if (value.isNumber()) {
                    values.add(String.valueOf(value.asInt()));
                }
            }
            if (!values.isEmpty()) {
                return String.join("/", values) + "秒";
            }
        }
        return spell.path("cooldownBurn").asText();
    }
    
    private static String extractCost(JsonNode spell) {
        JsonNode cost = spell.path("cost");
        if (cost.isArray() && cost.size() >= 5) {
            List<String> values = new ArrayList<>();
            for (JsonNode value : cost) {
                if (value.isNumber() && value.asInt() > 0) {
                    values.add(String.valueOf(value.asInt()));
                }
            }
            if (!values.isEmpty()) {
                return String.join("/", values) + " " + spell.path("costType").asText("蓝");
            }
        }
        String costBurn = spell.path("costBurn").asText();
        if (!costBurn.equals("0") && !costBurn.isEmpty()) {
            return costBurn + " " + spell.path("costType").asText("蓝");
        }
        return "";
    }
}