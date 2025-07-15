package com.lol.championselector.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.championselector.model.Champion;
import com.lol.championselector.model.ChampionSkills;
import com.lol.championselector.model.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalDataManager {
    private static final Logger logger = LoggerFactory.getLogger(LocalDataManager.class);
    
    private static final String DATA_DIR = "champion_data";
    private static final String CHAMPIONS_FILE = "champions.json";
    private static final String SKILLS_DIR = "skills";
    
    private final ObjectMapper objectMapper;
    private List<Champion> champions;
    
    public LocalDataManager() {
        this.objectMapper = new ObjectMapper();
        this.champions = new ArrayList<>();
    }
    
    public boolean loadChampions() {
        try {
            File championsFile = new File(DATA_DIR, CHAMPIONS_FILE);
            if (!championsFile.exists()) {
                logger.warn("Champions data file not found: {}", championsFile.getAbsolutePath());
                return false;
            }
            
            JsonNode rootNode = objectMapper.readTree(championsFile);
            JsonNode dataNode = rootNode.path("data");
            
            if (!dataNode.isObject()) {
                logger.error("Invalid champions data structure");
                return false;
            }
            
            champions.clear();
            Iterator<String> fieldNames = dataNode.fieldNames();
            
            while (fieldNames.hasNext()) {
                String championKey = fieldNames.next();
                JsonNode championNode = dataNode.get(championKey);
                
                Champion champion = parseChampion(championNode);
                if (champion != null) {
                    champions.add(champion);
                }
            }
            
            logger.info("Loaded {} champions from local data", champions.size());
            return true;
            
        } catch (Exception e) {
            logger.error("Error loading champions from local data", e);
            return false;
        }
    }
    
    private Champion parseChampion(JsonNode championNode) {
        try {
            String key = championNode.path("id").asText();
            String id = championNode.path("key").asText();
            String nameEn = championNode.path("name").asText();
            String nameCn = championNode.path("name").asText(); // 中文数据
            String title = championNode.path("title").asText();
            
            // 解析标签
            List<String> tags = new ArrayList<>();
            JsonNode tagsNode = championNode.path("tags");
            if (tagsNode.isArray()) {
                for (JsonNode tagNode : tagsNode) {
                    tags.add(tagNode.asText());
                }
            }
            
            // 生成关键词
            List<String> keywords = generateKeywords(key, nameEn, nameCn, title, tags);
            
            return new Champion(key, id, nameEn, nameCn, keywords, title, tags);
            
        } catch (Exception e) {
            logger.warn("Error parsing champion data", e);
            return null;
        }
    }
    
    private List<String> generateKeywords(String key, String nameEn, String nameCn, String title, List<String> tags) {
        List<String> keywords = new ArrayList<>();
        
        // 添加基本信息
        if (key != null) keywords.add(key);
        if (nameEn != null) keywords.add(nameEn);
        if (nameCn != null) keywords.add(nameCn);
        if (title != null) keywords.add(title);
        
        // 添加中文标签映射
        for (String tag : tags) {
            switch (tag) {
                case "Fighter": keywords.add("战士"); break;
                case "Tank": keywords.add("坦克"); break;
                case "Assassin": keywords.add("刺客"); break;
                case "Mage": keywords.add("法师"); break;
                case "Marksman": keywords.add("射手"); break;
                case "Support": keywords.add("辅助"); break;
            }
            keywords.add(tag);
        }
        
        return keywords;
    }
    
    public ChampionSkills loadChampionSkills(String championKey) {
        try {
            File skillFile = new File(DATA_DIR, SKILLS_DIR + "/" + championKey + ".json");
            if (!skillFile.exists()) {
                logger.debug("Skills file not found for champion: {}", championKey);
                return ChampionSkills.createEmpty();
            }
            
            JsonNode rootNode = objectMapper.readTree(skillFile);
            JsonNode dataNode = rootNode.path("data").path(championKey);
            
            if (dataNode.isMissingNode()) {
                logger.debug("No skills data found for champion: {}", championKey);
                return ChampionSkills.createEmpty();
            }
            
            return parseSkillsData(dataNode);
            
        } catch (Exception e) {
            logger.warn("Error loading skills for champion: {}", championKey, e);
            return ChampionSkills.createEmpty();
        }
    }
    
    private ChampionSkills parseSkillsData(JsonNode championData) {
        ChampionSkills skills = new ChampionSkills();
        
        try {
            // 解析被动技能
            JsonNode passiveNode = championData.path("passive");
            if (!passiveNode.isMissingNode()) {
                skills.setPassive(parseSkill(passiveNode, true));
            }
            
            // 解析主动技能
            JsonNode spellsArray = championData.path("spells");
            if (spellsArray.isArray()) {
                List<Skill> spells = new ArrayList<>();
                for (JsonNode spellNode : spellsArray) {
                    spells.add(parseSkill(spellNode, false));
                }
                skills.setSpells(spells);
            }
        } catch (Exception e) {
            logger.warn("Error parsing skills data", e);
        }
        
        return skills;
    }
    
    private Skill parseSkill(JsonNode skillNode, boolean isPassive) {
        Skill skill = new Skill();
        
        try {
            skill.setName(getTextOrDefault(skillNode, "name", "未知技能"));
            skill.setDescription(getTextOrDefault(skillNode, "description", ""));
            
            if (!isPassive) {
                skill.setTooltip(getTextOrDefault(skillNode, "tooltip", ""));
                skill.setCooldown(formatCooldown(getTextOrDefault(skillNode, "cooldownBurn", "")));
                skill.setCost(formatCost(getTextOrDefault(skillNode, "costBurn", "")));
                skill.setRange(formatRange(getTextOrDefault(skillNode, "rangeBurn", "")));
                skill.setDamage(extractDamageInfo(skillNode));
                skill.setEffect(extractEffectInfo(skillNode));
                skill.setScaling(extractScalingInfo(skillNode));
                skill.setDamageType(extractDamageType(skillNode));
            } else {
                // 被动技能的效果描述
                String desc = skill.getDescription();
                if (desc.length() > 80) {
                    skill.setEffect(desc.substring(0, 77) + "...");
                } else {
                    skill.setEffect(desc);
                }
            }
        } catch (Exception e) {
            logger.warn("Error parsing individual skill", e);
        }
        
        return skill;
    }
    
    private String getTextOrDefault(JsonNode node, String fieldName, String defaultValue) {
        JsonNode field = node.path(fieldName);
        return field.isMissingNode() ? defaultValue : field.asText(defaultValue);
    }
    
    private String formatCooldown(String cooldownBurn) {
        if (cooldownBurn.isEmpty() || "0".equals(cooldownBurn)) {
            return "";
        }
        return cooldownBurn + "秒";
    }
    
    private String formatCost(String costBurn) {
        if (costBurn.isEmpty() || "0".equals(costBurn)) {
            return "";
        }
        return costBurn + "蓝";
    }
    
    private String formatRange(String rangeBurn) {
        if (rangeBurn.isEmpty() || "0".equals(rangeBurn)) {
            return "";
        }
        return rangeBurn;
    }
    
    private String extractDamageInfo(JsonNode skillNode) {
        try {
            // 方法1: 从effectBurn提取实际数值
            JsonNode effectBurn = skillNode.path("effectBurn");
            if (effectBurn.isArray()) {
                for (int i = 1; i < effectBurn.size(); i++) { // 跳过索引0
                    String value = effectBurn.get(i).asText();
                    if (!value.equals("0") && !value.isEmpty() && value.matches(".*\\d.*")) {
                        // 检查是否包含伤害相关的数值
                        if (value.contains("/") && value.split("/").length >= 3) {
                            return value;
                        }
                    }
                }
            }
            
            // 方法2: 从effect数组提取
            JsonNode effect = skillNode.path("effect");
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
            
            // 方法3: 从tooltip提取数值
            String tooltip = getTextOrDefault(skillNode, "tooltip", "");
            if (!tooltip.isEmpty()) {
                String damage = extractDamageFromTooltip(tooltip);
                if (!damage.isEmpty()) {
                    return damage;
                }
            }
            
            // 方法4: 如果包含变量但没有实际数值，给出提示
            if (tooltip.contains("damage") && tooltip.contains("{{")) {
                return "查看游戏内详情";
            }
            
        } catch (Exception e) {
            logger.debug("Could not extract damage info", e);
        }
        
        return "";
    }
    
    private String extractDamageFromTooltip(String tooltip) {
        try {
            // 匹配 "60/90/120/150/180" 格式
            Pattern damagePattern = Pattern.compile("(\\d+(?:/\\d+){4})");
            Matcher matcher = damagePattern.matcher(tooltip);
            if (matcher.find()) {
                return matcher.group(1);
            }
            
            // 匹配 "60-180" 格式
            Pattern rangePattern = Pattern.compile("(\\d+)(?:-|至)(\\d+)");
            matcher = rangePattern.matcher(tooltip);
            if (matcher.find()) {
                return matcher.group(1) + "-" + matcher.group(2);
            }
            
            // 匹配百分比
            Pattern percentPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)%(?:\\s*-\\s*(\\d+(?:\\.\\d+)?)%)?");
            matcher = percentPattern.matcher(tooltip);
            if (matcher.find()) {
                if (matcher.group(2) != null) {
                    return matcher.group(1) + "%-" + matcher.group(2) + "%";
                } else {
                    return matcher.group(1) + "%";
                }
            }
            
            // 匹配基础伤害
            Pattern basicPattern = Pattern.compile("造成(\\d+)");
            matcher = basicPattern.matcher(tooltip);
            if (matcher.find()) {
                return matcher.group(1);
            }
            
        } catch (Exception e) {
            logger.debug("Error extracting damage from tooltip", e);
        }
        
        return "";
    }
    
    private String extractEffectInfo(JsonNode skillNode) {
        try {
            String tooltip = getTextOrDefault(skillNode, "tooltip", "");
            if (tooltip.contains("眩晕")) return "眩晕";
            if (tooltip.contains("减速")) return "减速";
            if (tooltip.contains("击飞")) return "击飞";
            if (tooltip.contains("沉默")) return "沉默";
            if (tooltip.contains("禁锢")) return "禁锢";
            if (tooltip.contains("护盾")) return "护盾";
            if (tooltip.contains("治疗")) return "治疗";
            
        } catch (Exception e) {
            logger.debug("Could not extract effect info", e);
        }
        return "";
    }
    
    private String extractScalingInfo(JsonNode skillNode) {
        try {
            String tooltip = getTextOrDefault(skillNode, "tooltip", "");
            if (tooltip.contains("法术强度") && tooltip.contains("%")) {
                return "AP缩放";
            }
            if (tooltip.contains("攻击力") && tooltip.contains("%")) {
                return "AD缩放";
            }
            if (tooltip.contains("最大生命值")) {
                return "生命值缩放";
            }
        } catch (Exception e) {
            logger.debug("Could not extract scaling info", e);
        }
        return "";
    }
    
    private String extractDamageType(JsonNode skillNode) {
        try {
            String tooltip = getTextOrDefault(skillNode, "tooltip", "");
            if (tooltip.contains("魔法伤害")) return "魔法";
            if (tooltip.contains("物理伤害")) return "物理";
            if (tooltip.contains("真实伤害")) return "真实";
        } catch (Exception e) {
            logger.debug("Could not extract damage type", e);
        }
        return "混合";
    }
    
    public List<Champion> getAllChampions() {
        return new ArrayList<>(champions);
    }
    
    public Champion getChampion(String key) {
        return champions.stream()
            .filter(champion -> champion.getKey().equals(key))
            .findFirst()
            .orElse(null);
    }
    
    public List<Champion> searchChampions(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllChampions();
        }
        
        String searchQuery = query.toLowerCase().trim();
        return champions.stream()
            .filter(champion -> matchesQuery(champion, searchQuery))
            .collect(java.util.stream.Collectors.toList());
    }
    
    private boolean matchesQuery(Champion champion, String query) {
        return champion.getKeywords().stream()
            .anyMatch(keyword -> keyword.toLowerCase().contains(query));
    }
    
    /**
     * 整合Community Dragon数据以增强技能伤害信息
     */
    public void enhanceSkillsWithCommunityDragonData(String championKey, ChampionSkills skills) {
        try {
            File communityFile = new File("champion_data/community_dragon/" + championKey + "_community.json");
            if (!communityFile.exists()) {
                return;
            }
            
            JsonNode communityData = objectMapper.readTree(communityFile);
            JsonNode skillDataNode = communityData.path("skillData");
            
            if (skillDataNode.isMissingNode()) {
                return;
            }
            
            // 增强技能伤害数据
            enhanceSkillDamage(skills.getPassive(), skillDataNode, "passive");
            
            List<Skill> spells = skills.getSpells();
            if (spells != null && !spells.isEmpty()) {
                String[] skillTypes = {"q", "w", "e", "r"};
                for (int i = 0; i < Math.min(spells.size(), skillTypes.length); i++) {
                    enhanceSkillDamage(spells.get(i), skillDataNode, skillTypes[i]);
                }
            }
            
            logger.debug("Enhanced skills for {} with Community Dragon data", championKey);
            
        } catch (Exception e) {
            logger.debug("Could not enhance skills with Community Dragon data for {}: {}", championKey, e.getMessage());
        }
    }
    
    private void enhanceSkillDamage(Skill skill, JsonNode communityData, String skillType) {
        if (skill == null || skill.getName().isEmpty()) {
            return;
        }
        
        try {
            JsonNode damageData = communityData.path("damageData");
            
            // 查找匹配的伤害数据
            damageData.fields().forEachRemaining(entry -> {
                String key = entry.getKey().toLowerCase();
                JsonNode value = entry.getValue();
                
                if (key.contains(skillType) && key.contains("damage")) {
                    String enhancedDamage = extractCommunityDragonDamage(value);
                    if (!enhancedDamage.isEmpty() && (skill.getDamage().isEmpty() || skill.getDamage().equals("0"))) {
                        skill.setDamage(enhancedDamage);
                        logger.debug("Enhanced {} skill damage: {}", skillType, enhancedDamage);
                    }
                }
            });
            
            // 增强冷却时间数据
            JsonNode cooldownData = communityData.path("cooldownData");
            cooldownData.fields().forEachRemaining(entry -> {
                String key = entry.getKey().toLowerCase();
                JsonNode value = entry.getValue();
                
                if (key.contains(skillType) && key.contains("cooldown")) {
                    String enhancedCooldown = extractCommunityDragonArray(value);
                    if (!enhancedCooldown.isEmpty() && skill.getCooldown().isEmpty()) {
                        skill.setCooldown(enhancedCooldown + "秒");
                    }
                }
            });
            
        } catch (Exception e) {
            logger.debug("Error enhancing skill {}: {}", skillType, e.getMessage());
        }
    }
    
    private String extractCommunityDragonDamage(JsonNode damageNode) {
        if (damageNode.isArray()) {
            List<String> values = new ArrayList<>();
            for (JsonNode value : damageNode) {
                if (value.isNumber()) {
                    int intValue = value.asInt();
                    if (intValue > 0) {
                        values.add(String.valueOf(intValue));
                    }
                }
            }
            if (!values.isEmpty()) {
                return String.join("/", values);
            }
        } else if (damageNode.isNumber()) {
            int value = damageNode.asInt();
            if (value > 0) {
                return String.valueOf(value);
            }
        }
        return "";
    }
    
    private String extractCommunityDragonArray(JsonNode arrayNode) {
        if (arrayNode.isArray()) {
            List<String> values = new ArrayList<>();
            for (JsonNode value : arrayNode) {
                if (value.isNumber()) {
                    double doubleValue = value.asDouble();
                    if (doubleValue > 0) {
                        values.add(String.valueOf(doubleValue));
                    }
                }
            }
            if (!values.isEmpty()) {
                return String.join("/", values);
            }
        }
        return "";
    }
}