package com.lol.championselector.api;

import com.lol.championselector.parser.DamageDataParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 技能数据分析器 - 解析和提取技能伤害数据
 */
public class SkillDataAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(SkillDataAnalyzer.class);
    
    /**
     * 解析技能数据
     */
    public static AnalyzedSkillData analyzeSkill(TencentChampionApi.SpellInfo spell) {
        if (spell == null) {
            return null;
        }
        
        AnalyzedSkillData skillData = new AnalyzedSkillData();
        skillData.setSpellKey(spell.getSpellKey());
        skillData.setName(spell.getName());
        skillData.setDescription(spell.getDescription());
        skillData.setIconPath(spell.getAbilityIconPath());
        skillData.setVideoPath(spell.getAbilityVideoPath());
        
        // 解析伤害数据 - 优先从描述文本中提取
        DamageDataParser.ParsedDamageData parsedDamage = DamageDataParser.parseDamageFromDescription(spell.getDescription());
        if (!parsedDamage.isEmpty()) {
            // 使用从描述文本中解析的伤害数据
            Map<String, List<Double>> damageData = new HashMap<>();
            if (!parsedDamage.baseDamage.isEmpty()) {
                damageData.put("BaseDamage", parsedDamage.baseDamage);
            }
            if (!parsedDamage.percentageDamage.isEmpty()) {
                damageData.put("PercentageDamage", parsedDamage.percentageDamage);
            }
            skillData.setDamageData(damageData);
            
            // 设置系数数据
            Map<String, Double> ratios = new HashMap<>();
            for (Map.Entry<String, Double> entry : parsedDamage.scalingRatios.entrySet()) {
                ratios.put(entry.getKey(), entry.getValue());
            }
            skillData.setRatios(ratios);
        } else {
            // 如果描述文本中没有找到伤害数据，则使用原有的方法
            if (spell.getEffectAmounts() != null) {
                skillData.setDamageData(extractDamageData(spell.getEffectAmounts()));
            }
            
            if (spell.getCoefficients() != null) {
                skillData.setRatios(extractRatios(spell.getCoefficients()));
            }
        }
        
        logger.debug("Analyzed skill: {} with {} damage effects and {} ratios", 
                spell.getName(), 
                skillData.getDamageData().size(), 
                skillData.getRatios().size());
        
        return skillData;
    }
    
    /**
     * 解析所有技能数据
     */
    public static List<AnalyzedSkillData> analyzeAllSkills(List<TencentChampionApi.SpellInfo> spells) {
        if (spells == null || spells.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<AnalyzedSkillData> analyzedSkills = new ArrayList<>();
        for (TencentChampionApi.SpellInfo spell : spells) {
            AnalyzedSkillData analyzed = analyzeSkill(spell);
            if (analyzed != null) {
                analyzedSkills.add(analyzed);
            }
        }
        
        logger.info("Analyzed {} skills successfully", analyzedSkills.size());
        return analyzedSkills;
    }
    
    /**
     * 提取伤害数据
     */
    private static Map<String, List<Double>> extractDamageData(TencentChampionApi.EffectAmounts effects) {
        Map<String, List<Double>> damageData = new HashMap<>();
        
        if (effects.getEffect1Amount() != null && !effects.getEffect1Amount().isEmpty()) {
            damageData.put("Effect1", new ArrayList<>(effects.getEffect1Amount()));
        }
        
        if (effects.getEffect2Amount() != null && !effects.getEffect2Amount().isEmpty()) {
            damageData.put("Effect2", new ArrayList<>(effects.getEffect2Amount()));
        }
        
        if (effects.getEffect3Amount() != null && !effects.getEffect3Amount().isEmpty()) {
            damageData.put("Effect3", new ArrayList<>(effects.getEffect3Amount()));
        }
        
        if (effects.getEffect4Amount() != null && !effects.getEffect4Amount().isEmpty()) {
            damageData.put("Effect4", new ArrayList<>(effects.getEffect4Amount()));
        }
        
        if (effects.getEffect5Amount() != null && !effects.getEffect5Amount().isEmpty()) {
            damageData.put("Effect5", new ArrayList<>(effects.getEffect5Amount()));
        }
        
        return damageData;
    }
    
    /**
     * 提取系数数据
     */
    private static Map<String, Double> extractRatios(TencentChampionApi.Coefficients coefficients) {
        Map<String, Double> ratios = new HashMap<>();
        
        if (coefficients.getCoefficient1() != null) {
            ratios.put("Coefficient1", coefficients.getCoefficient1());
        }
        
        if (coefficients.getCoefficient2() != null) {
            ratios.put("Coefficient2", coefficients.getCoefficient2());
        }
        
        if (coefficients.getCoefficient3() != null) {
            ratios.put("Coefficient3", coefficients.getCoefficient3());
        }
        
        if (coefficients.getCoefficient4() != null) {
            ratios.put("Coefficient4", coefficients.getCoefficient4());
        }
        
        if (coefficients.getCoefficient5() != null) {
            ratios.put("Coefficient5", coefficients.getCoefficient5());
        }
        
        return ratios;
    }
    
    /**
     * 获取指定等级的伤害值
     */
    public static Double getDamageAtRank(AnalyzedSkillData skill, String effectKey, int rank) {
        if (skill == null || effectKey == null || rank < 1 || rank > 5) {
            return null;
        }
        
        List<Double> damages = skill.getDamageData().get(effectKey);
        if (damages == null || damages.size() < rank) {
            return null;
        }
        
        return damages.get(rank - 1); // rank 1-5 对应 index 0-4
    }
    
    /**
     * 获取系数值
     */
    public static Double getRatio(AnalyzedSkillData skill, String coefficientKey) {
        if (skill == null || coefficientKey == null) {
            return null;
        }
        
        return skill.getRatios().get(coefficientKey);
    }
    
    /**
     * 分析后的技能数据
     */
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnalyzedSkillData {
        private String spellKey;
        private String name;
        private String description;
        private String iconPath;
        private String videoPath;
        private Map<String, List<Double>> damageData = new HashMap<>();
        private Map<String, Double> ratios = new HashMap<>();
        
        // Constructors
        public AnalyzedSkillData() {}
        
        public AnalyzedSkillData(String spellKey, String name, String description) {
            this.spellKey = spellKey;
            this.name = name;
            this.description = description;
        }
        
        // Getters and setters
        public String getSpellKey() {
            return spellKey;
        }
        
        public void setSpellKey(String spellKey) {
            this.spellKey = spellKey;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getIconPath() {
            return iconPath;
        }
        
        public void setIconPath(String iconPath) {
            this.iconPath = iconPath;
        }
        
        public String getVideoPath() {
            return videoPath;
        }
        
        public void setVideoPath(String videoPath) {
            this.videoPath = videoPath;
        }
        
        public Map<String, List<Double>> getDamageData() {
            return damageData;
        }
        
        public void setDamageData(Map<String, List<Double>> damageData) {
            this.damageData = damageData;
        }
        
        public Map<String, Double> getRatios() {
            return ratios;
        }
        
        public void setRatios(Map<String, Double> ratios) {
            this.ratios = ratios;
        }
        
        /**
         * 获取主要伤害数据（通常是Effect1）
         */
        public List<Double> getPrimaryDamage() {
            if (damageData.containsKey("Effect1")) {
                return damageData.get("Effect1");
            }
            // 如果没有Effect1，返回第一个可用的效果
            return damageData.values().stream().findFirst().orElse(new ArrayList<>());
        }
        
        /**
         * 设置主要伤害数据
         */
        public void setPrimaryDamage(List<Double> primaryDamage) {
            // 这个方法用于JSON反序列化
        }
        
        /**
         * 获取主要系数（通常是Coefficient1）
         */
        public Double getPrimaryRatio() {
            if (ratios.containsKey("Coefficient1")) {
                return ratios.get("Coefficient1");
            }
            // 如果没有Coefficient1，返回第一个可用的系数
            return ratios.values().stream().findFirst().orElse(0.0);
        }
        
        /**
         * 设置主要系数
         */
        public void setPrimaryRatio(Double primaryRatio) {
            // 这个方法用于JSON反序列化
        }
        
        /**
         * 格式化显示伤害信息
         */
        public String formatDamageInfo() {
            StringBuilder sb = new StringBuilder();
            
            List<Double> primaryDamage = getPrimaryDamage();
            if (!primaryDamage.isEmpty()) {
                sb.append("基础伤害: ");
                for (int i = 0; i < primaryDamage.size(); i++) {
                    if (i > 0) sb.append("/");
                    sb.append(String.format("%.0f", primaryDamage.get(i)));
                }
                
                Double primaryRatio = getPrimaryRatio();
                if (primaryRatio != null && primaryRatio > 0) {
                    sb.append(String.format(" (+%.1f%% AP/AD)", primaryRatio * 100));
                }
            }
            
            return sb.toString();
        }
        
        @Override
        public String toString() {
            return String.format("AnalyzedSkillData{spellKey='%s', name='%s', damages=%d, ratios=%d}", 
                    spellKey, name, damageData.size(), ratios.size());
        }
    }
}