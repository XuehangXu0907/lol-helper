package com.lol.championselector;

import com.lol.championselector.manager.SkillDamageDataManager;

public class TestSkillDamageIntegration {
    
    public static void main(String[] args) {
        System.out.println("测试技能伤害数据集成...");
        
        SkillDamageDataManager damageManager = new SkillDamageDataManager();
        
        // 测试安妮的技能伤害数据
        try {
            System.out.println("\n=== 测试安妮技能伤害数据 ===");
            
            var damageData = damageManager.getChampionDamageData("Annie");
            if (damageData.isEmpty()) {
                System.out.println("未找到安妮的伤害数据");
                return;
            }
            
            System.out.println("找到 " + damageData.size() + " 个技能的伤害数据");
            
            for (var entry : damageData.entrySet()) {
                String spellKey = entry.getKey();
                SkillDamageDataManager.SkillDamageData skillData = entry.getValue();
                
                System.out.println("\n技能键: " + spellKey);
                System.out.println("技能名: " + skillData.getName());
                
                if (skillData.getBaseDamage() != null && skillData.getBaseDamage().length > 0) {
                    System.out.print("基础伤害: ");
                    for (int i = 0; i < skillData.getBaseDamage().length; i++) {
                        if (i > 0) System.out.print("/");
                        System.out.print((int)skillData.getBaseDamage()[i]);
                    }
                    System.out.println();
                }
                
                if (skillData.getRatios() != null && !skillData.getRatios().isEmpty()) {
                    System.out.println("缩放: " + skillData.getRatios());
                }
                
                if (skillData.getDescription() != null && !skillData.getDescription().isEmpty()) {
                    String desc = skillData.getDescription();
                    if (desc.length() > 80) {
                        desc = desc.substring(0, 77) + "...";
                    }
                    System.out.println("描述: " + desc);
                }
            }
            
            System.out.println("\n技能伤害数据读取测试完成！");
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}