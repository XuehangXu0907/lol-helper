package com.lol.championselector.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lol.championselector.config.ChampionVersionMapping;
import com.lol.championselector.model.ChampionSkills;
import com.lol.championselector.model.Skill;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class SkillsManager {
    private static final Logger logger = LoggerFactory.getLogger(SkillsManager.class);
    
    private static final String SKILLS_API_TEMPLATE = 
        "https://ddragon.leagueoflegends.com/cdn/%s/data/zh_CN/champion/%s.json";
    
    private static final Map<String, String> KEY_CORRECTIONS = Map.of(
        "Wukong", "MonkeyKing",
        "RekSai", "RekSai",
        "KaiSa", "Kaisa",
        "KhaZix", "Khazix"
    );
    
    private final Cache<String, ChampionSkills> skillsCache;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SkillDamageDataManager damageDataManager;
    
    public SkillsManager() {
        this.skillsCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(Duration.ofHours(1))
            .build();
            
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
            
        this.objectMapper = new ObjectMapper();
        this.damageDataManager = new SkillDamageDataManager();
    }
    
    public CompletableFuture<ChampionSkills> getSkillsAsync(String championKey) {
        ChampionSkills cached = skillsCache.getIfPresent(championKey);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            // First try to load from local complete data
            ChampionSkills localSkills = loadFromLocalCompleteData(championKey);
            if (localSkills != null) {
                skillsCache.put(championKey, localSkills);
                return localSkills;
            }
            
            // Fallback to network API
            try {
                String version = ChampionVersionMapping.getVersion(championKey);
                String correctedKey = getCorrectedKey(championKey);
                String url = String.format(SKILLS_API_TEMPLATE, version, correctedKey);
                
                Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "LoL Champion Selector")
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        logger.warn("Failed to get skills for {}: HTTP {}", championKey, response.code());
                        return createEmptySkills();
                    }
                    
                    ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        logger.warn("Empty response body for skills: {}", championKey);
                        return createEmptySkills();
                    }
                    
                    JsonNode root = objectMapper.readTree(responseBody.string());
                    JsonNode dataNode = root.path("data");
                    
                    if (dataNode.isMissingNode() || !dataNode.isObject()) {
                        logger.warn("Invalid data structure for skills: {}", championKey);
                        return createEmptySkills();
                    }
                    
                    JsonNode championData = dataNode.elements().next();
                    if (championData == null) {
                        logger.warn("No champion data found for: {}", championKey);
                        return createEmptySkills();
                    }
                    
                    ChampionSkills skills = parseSkillsData(championData, championKey);
                    skillsCache.put(championKey, skills);
                    
                    return skills;
                }
            } catch (Exception e) {
                logger.warn("Failed to get skills for: {}", championKey, e);
                return createEmptySkills();
            }
        }, ForkJoinPool.commonPool());
    }
    
    private ChampionSkills parseSkillsData(JsonNode championData, String championKey) {
        ChampionSkills skills = new ChampionSkills();
        
        try {
            JsonNode passiveNode = championData.path("passive");
            if (!passiveNode.isMissingNode()) {
                Skill passive = parseSkill(passiveNode, true);
                // Enhance passive with damage data
                damageDataManager.enhanceSkillWithDamageData(passive, championKey, "passive");
                skills.setPassive(passive);
            }
            
            JsonNode spellsArray = championData.path("spells");
            if (spellsArray.isArray()) {
                List<Skill> spells = new ArrayList<>();
                String[] spellKeys = {"q", "w", "e", "r"};
                int spellIndex = 0;
                
                for (JsonNode spellNode : spellsArray) {
                    Skill spell = parseSkill(spellNode, false);
                    
                    // Enhance spell with damage data
                    if (spellIndex < spellKeys.length) {
                        damageDataManager.enhanceSkillWithDamageData(spell, championKey, spellKeys[spellIndex]);
                    }
                    
                    spells.add(spell);
                    spellIndex++;
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
            
            // Extract image URL for both active and passive skills
            JsonNode imageNode = skillNode.path("image");
            if (!imageNode.isMissingNode()) {
                String imageFileName = getTextOrDefault(imageNode, "full", "");
                skill.setImageUrl(imageFileName);
            }
            
            if (!isPassive) {
                skill.setTooltip(getTextOrDefault(skillNode, "tooltip", ""));
                skill.setCooldown(getArrayOrDefault(skillNode, "cooldownBurn", ""));
                skill.setCost(getArrayOrDefault(skillNode, "costBurn", ""));
                skill.setRange(getArrayOrDefault(skillNode, "rangeBurn", ""));
                skill.setDamage(extractDamageInfo(skillNode));
                skill.setEffect(extractEffectInfo(skillNode));
                skill.setScaling(extractScalingInfo(skillNode));
                skill.setDamageType(extractDamageType(skillNode));
            } else {
                // 被动技能也可以有一些信息
                skill.setEffect(extractPassiveEffect(skillNode));
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
    
    private String getArrayOrDefault(JsonNode node, String fieldName, String defaultValue) {
        JsonNode field = node.path(fieldName);
        if (field.isMissingNode()) {
            return defaultValue;
        }
        
        String value = field.asText(defaultValue);
        if (value.isEmpty() || "0".equals(value)) {
            return defaultValue;
        }
        
        // 处理数组格式的字符串，如 "8/7/6/5/4"
        if (value.contains("/")) {
            String[] parts = value.split("/");
            StringBuilder sb = new StringBuilder();
            boolean hasValidData = false;
            
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i].trim();
                if (!part.equals("0") && !part.isEmpty()) {
                    if (hasValidData) sb.append("/");
                    sb.append(part);
                    hasValidData = true;
                }
            }
            
            if (hasValidData) {
                String result = sb.toString();
                // 添加单位
                if (fieldName.equals("cooldownBurn")) {
                    return result + "秒";
                } else if (fieldName.equals("costBurn")) {
                    return result + "蓝";
                }
                return result;
            }
        }
        
        // 为单个值添加单位
        if (!value.equals("0")) {
            if (fieldName.equals("cooldownBurn")) {
                return value + "秒";
            } else if (fieldName.equals("costBurn")) {
                return value + "蓝";
            }
        }
        
        return value.equals("0") ? defaultValue : value;
    }
    
    private String extractDamageInfo(JsonNode skillNode) {
        try {
            // 首先尝试从 tooltip 提取实际数值
            String tooltip = getTextOrDefault(skillNode, "tooltip", "");
            if (!tooltip.isEmpty()) {
                String damage = extractDamageFromTooltip(tooltip);
                if (!damage.isEmpty() && !damage.contains("0000")) {
                    return damage;
                }
            }
            
            // 从 effectBurn 提取（这通常包含实际的数值）
            JsonNode effectBurn = skillNode.path("effectBurn");
            if (effectBurn.isArray() && effectBurn.size() > 1) {
                JsonNode damageValue = effectBurn.get(1);
                if (damageValue != null && !damageValue.asText().equals("0")) {
                    return damageValue.asText();
                }
            }
            
            // 最后尝试从 effect 数组提取
            JsonNode effectNode = skillNode.path("effect");
            if (effectNode.isArray() && effectNode.size() > 1) {
                JsonNode damageArray = effectNode.get(1);
                if (damageArray != null && damageArray.isArray()) {
                    StringBuilder damage = new StringBuilder();
                    boolean hasValidData = false;
                    for (int i = 0; i < damageArray.size(); i++) {
                        String value = damageArray.get(i).asText();
                        if (!value.equals("0") && !value.isEmpty()) {
                            if (hasValidData) damage.append("/");
                            damage.append(value);
                            hasValidData = true;
                        }
                    }
                    if (hasValidData) {
                        return damage.toString();
                    }
                }
            }
            
        } catch (Exception e) {
            logger.debug("Could not extract damage info", e);
        }
        
        return "";
    }
    
    private String extractDamageFromTooltip(String tooltip) {
        try {
            // 使用正则表达式提取数值模式，如 "60/90/120/150/180"
            java.util.regex.Pattern damagePattern = java.util.regex.Pattern.compile("(\\d+(?:/\\d+){4})");
            java.util.regex.Matcher matcher = damagePattern.matcher(tooltip);
            if (matcher.find()) {
                return matcher.group(1);
            }
            
            // 提取单个数值范围，如 "60-180"
            java.util.regex.Pattern rangePattern = java.util.regex.Pattern.compile("(\\d+)(?:-|至)(\\d+)");
            matcher = rangePattern.matcher(tooltip);
            if (matcher.find()) {
                return matcher.group(1) + "-" + matcher.group(2);
            }
            
            // 提取百分比数值，如 "8% - 16%"
            java.util.regex.Pattern percentPattern = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)%(?:\\s*-\\s*(\\d+(?:\\.\\d+)?)%)?");
            matcher = percentPattern.matcher(tooltip);
            if (matcher.find()) {
                if (matcher.group(2) != null) {
                    return matcher.group(1) + "%-" + matcher.group(2) + "%";
                } else {
                    return matcher.group(1) + "%";
                }
            }
            
            // 提取基础伤害值
            java.util.regex.Pattern basicPattern = java.util.regex.Pattern.compile("造成(\\d+)");
            matcher = basicPattern.matcher(tooltip);
            if (matcher.find()) {
                return matcher.group(1);
            }
            
        } catch (Exception e) {
            logger.debug("Error extracting damage from tooltip", e);
        }
        
        return "";
    }
    
    private String getCorrectedKey(String championKey) {
        return KEY_CORRECTIONS.getOrDefault(championKey, championKey);
    }
    
    private ChampionSkills createEmptySkills() {
        return ChampionSkills.createEmpty();
    }
    
    private String extractEffectInfo(JsonNode skillNode) {
        try {
            String tooltip = getTextOrDefault(skillNode, "tooltip", "");
            if (tooltip.contains("眩晕") || tooltip.contains("减速") || tooltip.contains("击飞")) {
                if (tooltip.contains("眩晕")) return "眩晕";
                if (tooltip.contains("减速")) return "减速";
                if (tooltip.contains("击飞")) return "击飞";
            }
            
            // 从效果数组提取
            JsonNode effectNode = skillNode.path("effect");
            if (effectNode.isArray() && effectNode.size() > 2) {
                JsonNode effectArray = effectNode.get(2);
                if (effectArray != null && effectArray.isArray() && effectArray.size() > 0) {
                    return effectArray.get(0).asText() + "效果";
                }
            }
            
        } catch (Exception e) {
            logger.debug("Could not extract effect info", e);
        }
        return "";
    }
    
    private String extractScalingInfo(JsonNode skillNode) {
        try {
            String tooltip = getTextOrDefault(skillNode, "tooltip", "");
            if (tooltip.contains("AP") && tooltip.contains("%")) {
                return "AP缩放";
            }
            if (tooltip.contains("AD") && tooltip.contains("%")) {
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
    
    private String extractPassiveEffect(JsonNode skillNode) {
        try {
            String desc = getTextOrDefault(skillNode, "description", "");
            if (desc.length() > 50) {
                return desc.substring(0, 47) + "...";
            }
            return desc;
        } catch (Exception e) {
            logger.debug("Could not extract passive effect", e);
        }
        return "";
    }
    
    public void clearCache() {
        skillsCache.invalidateAll();
        damageDataManager.clearCache();
        logger.info("Skills cache cleared");
    }
    
    /**
     * Load skills from local complete data files
     */
    private ChampionSkills loadFromLocalCompleteData(String championKey) {
        try {
            java.io.File skillFile = new java.io.File("src/main/resources/champion/data/full", championKey + "_complete.json");
            
            if (!skillFile.exists()) {
                logger.debug("Local complete data file not found for champion: {}", championKey);
                return null;
            }
            
            JsonNode rootNode = objectMapper.readTree(skillFile);
            JsonNode skillsNode = rootNode.path("skills");
            
            if (skillsNode.isMissingNode()) {
                logger.debug("Skills data missing in local file for champion: {}", championKey);
                return null;
            }
            
            ChampionSkills skills = parseLocalCompleteSkills(skillsNode, championKey);
            logger.debug("Successfully loaded skills from local complete data for: {}", championKey);
            return skills;
            
        } catch (Exception e) {
            logger.debug("Error loading skills from local complete data for {}: {}", championKey, e.getMessage());
            return null;
        }
    }
    
    /**
     * Parse skills from local complete data format
     */
    private ChampionSkills parseLocalCompleteSkills(JsonNode skillsNode, String championKey) {
        ChampionSkills skills = new ChampionSkills();
        
        // Parse passive skill
        JsonNode passiveNode = skillsNode.path("passive");
        if (!passiveNode.isMissingNode()) {
            Skill passive = parseLocalCompleteSkill(passiveNode);
            // Enhance passive with damage data
            damageDataManager.enhanceSkillWithDamageData(passive, championKey, "passive");
            skills.setPassive(passive);
        }
        
        // Parse active skills
        JsonNode spellsNode = skillsNode.path("spells");
        if (spellsNode.isArray()) {
            List<Skill> spells = new ArrayList<>();
            String[] spellKeys = {"q", "w", "e", "r"};
            int spellIndex = 0;
            
            for (JsonNode spellNode : spellsNode) {
                Skill spell = parseLocalCompleteSkill(spellNode);
                
                // Enhance spell with damage data
                if (spellIndex < spellKeys.length) {
                    damageDataManager.enhanceSkillWithDamageData(spell, championKey, spellKeys[spellIndex]);
                }
                
                spells.add(spell);
                spellIndex++;
            }
            skills.setSpells(spells);
        }
        
        return skills;
    }
    
    /**
     * Parse a single skill from local complete data format
     */
    private Skill parseLocalCompleteSkill(JsonNode skillNode) {
        Skill skill = new Skill();
        
        // Use Chinese names/descriptions by default
        skill.setName(getTextOrDefault(skillNode, "nameZh", skillNode.path("name").asText()));
        skill.setDescription(getTextOrDefault(skillNode, "descriptionZh", skillNode.path("description").asText()));
        skill.setTooltip(getTextOrDefault(skillNode, "tooltipZh", skillNode.path("tooltip").asText()));
        
        // Extract image URL
        JsonNode imageNode = skillNode.path("image");
        if (!imageNode.isMissingNode()) {
            String imageFileName = getTextOrDefault(imageNode, "full", "");
            skill.setImageUrl(imageFileName);
        }
        
        // Other properties
        skill.setCooldown(getTextOrDefault(skillNode, "cooldownBurn", ""));
        skill.setCost(getTextOrDefault(skillNode, "costBurn", ""));
        skill.setRange(getTextOrDefault(skillNode, "rangeBurn", ""));
        
        // Extract damage from tooltip
        String tooltip = skill.getTooltip();
        if (tooltip != null && !tooltip.isEmpty()) {
            skill.setDamage(extractDamageFromTooltip(tooltip));
        }
        
        return skill;
    }
    
    public void shutdown() {
        try {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
            logger.info("SkillsManager shut down successfully");
        } catch (Exception e) {
            logger.warn("Error during SkillsManager shutdown", e);
        }
    }
}