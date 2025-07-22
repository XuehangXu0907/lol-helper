package com.lol.championselector.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lol.championselector.model.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class SkillDamageDataManager {
    private static final Logger logger = LoggerFactory.getLogger(SkillDamageDataManager.class);
    
    private static final String SKILL_DAMAGE_DATA_PATH = "/champion/skill_damage_data/skills/";
    private static final String CHAMPIONS_METADATA_PATH = "/champion/skill_damage_data/champions.json";
    
    private final Cache<String, Map<String, SkillDamageData>> damageDataCache;
    private final ObjectMapper objectMapper;
    private final Map<String, String> championKeyMap; // alias -> heroKey mapping
    
    public SkillDamageDataManager() {
        this.damageDataCache = Caffeine.newBuilder()
            .maximumSize(200)
            .expireAfterWrite(Duration.ofHours(2))
            .build();
            
        this.objectMapper = new ObjectMapper();
        this.championKeyMap = new HashMap<>();
        
        loadChampionKeyMapping();
    }
    
    /**
     * Load champion alias to key mapping from champions.json
     */
    private void loadChampionKeyMapping() {
        try (InputStream inputStream = getClass().getResourceAsStream(CHAMPIONS_METADATA_PATH)) {
            if (inputStream == null) {
                logger.warn("Champions metadata file not found: {}", CHAMPIONS_METADATA_PATH);
                return;
            }
            
            JsonNode championsArray = objectMapper.readTree(inputStream);
            if (championsArray.isArray()) {
                for (JsonNode championNode : championsArray) {
                    String alias = championNode.path("alias").asText();
                    String heroKey = championNode.path("alias").asText(); // Using alias as key
                    if (!alias.isEmpty()) {
                        championKeyMap.put(alias, heroKey);
                    }
                }
                logger.info("Loaded {} champion key mappings", championKeyMap.size());
            }
        } catch (Exception e) {
            logger.warn("Failed to load champion key mapping: {}", e.getMessage());
        }
    }
    
    /**
     * Get damage data for a champion's skills
     */
    public Map<String, SkillDamageData> getChampionDamageData(String championKey) {
        Map<String, SkillDamageData> cached = damageDataCache.getIfPresent(championKey);
        if (cached != null) {
            return cached;
        }
        
        try {
            // Try to load from damage data files
            String damageDataPath = SKILL_DAMAGE_DATA_PATH + championKey + ".json";
            
            try (InputStream inputStream = getClass().getResourceAsStream(damageDataPath)) {
                if (inputStream == null) {
                    logger.debug("Damage data file not found for champion: {}", championKey);
                    return new HashMap<>();
                }
                
                JsonNode rootNode = objectMapper.readTree(inputStream);
                JsonNode skillsArray = rootNode.path("skills");
                
                Map<String, SkillDamageData> damageData = new HashMap<>();
                
                if (skillsArray.isArray()) {
                    for (JsonNode skillNode : skillsArray) {
                        SkillDamageData skillDamage = parseSkillDamageData(skillNode);
                        if (skillDamage != null) {
                            damageData.put(skillDamage.getSpellKey(), skillDamage);
                        }
                    }
                }
                
                damageDataCache.put(championKey, damageData);
                logger.debug("Loaded damage data for {} skills of champion: {}", damageData.size(), championKey);
                
                return damageData;
            }
        } catch (Exception e) {
            logger.warn("Failed to load damage data for champion {}: {}", championKey, e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Parse skill damage data from JSON
     */
    private SkillDamageData parseSkillDamageData(JsonNode skillNode) {
        try {
            String spellKey = skillNode.path("spellKey").asText();
            String name = skillNode.path("name").asText();
            String description = skillNode.path("description").asText();
            
            JsonNode damageDataNode = skillNode.path("damageData");
            JsonNode ratiosNode = skillNode.path("ratios");
            
            SkillDamageData skillDamage = new SkillDamageData();
            skillDamage.setSpellKey(spellKey);
            skillDamage.setName(name);
            skillDamage.setDescription(description);
            
            // Parse base damage array
            JsonNode baseDamageNode = damageDataNode.path("BaseDamage");
            if (baseDamageNode.isArray()) {
                double[] baseDamage = new double[baseDamageNode.size()];
                for (int i = 0; i < baseDamageNode.size(); i++) {
                    baseDamage[i] = baseDamageNode.get(i).asDouble();
                }
                skillDamage.setBaseDamage(baseDamage);
            }
            
            // Parse percentage damage array if exists
            JsonNode percentageDamageNode = damageDataNode.path("PercentageDamage");
            if (percentageDamageNode.isArray()) {
                double[] percentageDamage = new double[percentageDamageNode.size()];
                for (int i = 0; i < percentageDamageNode.size(); i++) {
                    percentageDamage[i] = percentageDamageNode.get(i).asDouble();
                }
                skillDamage.setPercentageDamage(percentageDamage);
            }
            
            // Parse ratios
            Map<String, Double> ratios = new HashMap<>();
            if (ratiosNode.isObject()) {
                ratiosNode.fields().forEachRemaining(entry -> {
                    ratios.put(entry.getKey(), entry.getValue().asDouble());
                });
            }
            skillDamage.setRatios(ratios);
            
            // Primary ratio and damage
            skillDamage.setPrimaryRatio(skillNode.path("primaryRatio").asDouble());
            JsonNode primaryDamageNode = skillNode.path("primaryDamage");
            if (primaryDamageNode.isArray()) {
                double[] primaryDamage = new double[primaryDamageNode.size()];
                for (int i = 0; i < primaryDamageNode.size(); i++) {
                    primaryDamage[i] = primaryDamageNode.get(i).asDouble();
                }
                skillDamage.setPrimaryDamage(primaryDamage);
            }
            
            return skillDamage;
            
        } catch (Exception e) {
            logger.warn("Failed to parse skill damage data: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Enhance a skill with damage data
     */
    public void enhanceSkillWithDamageData(Skill skill, String championKey, String spellKey) {
        try {
            Map<String, SkillDamageData> damageDataMap = getChampionDamageData(championKey);
            SkillDamageData damageData = damageDataMap.get(spellKey);
            
            if (damageData != null) {
                // Update damage information with data from skill_damage_data
                skill.setDamage(formatDamageString(damageData));
                
                // Keep existing description unless damage data has better Chinese description
                if (damageData.getDescription() != null && !damageData.getDescription().isEmpty()) {
                    // Only use damage data description if it's in Chinese and more detailed
                    if (isChineseText(damageData.getDescription()) && 
                        damageData.getDescription().length() > (skill.getDescription() != null ? skill.getDescription().length() : 0)) {
                        skill.setDescription(damageData.getDescription());
                    }
                }
                
                logger.debug("Enhanced skill {} with damage data for champion {}", spellKey, championKey);
            }
        } catch (Exception e) {
            logger.debug("Failed to enhance skill with damage data: {}", e.getMessage());
        }
    }
    
    /**
     * Format damage data into a readable string
     */
    private String formatDamageString(SkillDamageData damageData) {
        StringBuilder damage = new StringBuilder();
        
        // Base damage
        if (damageData.getBaseDamage() != null && damageData.getBaseDamage().length > 0) {
            damage.append(formatDamageArray(damageData.getBaseDamage()));
        }
        
        // Primary damage (if different from base damage)
        if (damageData.getPrimaryDamage() != null && damageData.getPrimaryDamage().length > 0 &&
            !java.util.Arrays.equals(damageData.getBaseDamage(), damageData.getPrimaryDamage())) {
            if (damage.length() > 0) damage.append(" / ");
            damage.append(formatDamageArray(damageData.getPrimaryDamage()));
        }
        
        // Percentage damage
        if (damageData.getPercentageDamage() != null && damageData.getPercentageDamage().length > 0) {
            if (damage.length() > 0) damage.append(" + ");
            damage.append(formatDamageArray(damageData.getPercentageDamage())).append("%");
        }
        
        // Add scaling ratios
        if (damageData.getRatios() != null && !damageData.getRatios().isEmpty()) {
            for (Map.Entry<String, Double> ratio : damageData.getRatios().entrySet()) {
                if (ratio.getValue() > 0) {
                    damage.append(" (").append(String.format("%.0f%%", ratio.getValue() * 100))
                           .append(ratio.getKey()).append(")");
                }
            }
        }
        
        return damage.toString();
    }
    
    /**
     * Format damage array into string like "80/120/160/200/240"
     */
    private String formatDamageArray(double[] damageArray) {
        if (damageArray == null || damageArray.length == 0) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < damageArray.length; i++) {
            if (i > 0) sb.append("/");
            // Format as integer if it's a whole number, otherwise as decimal
            if (damageArray[i] == (int) damageArray[i]) {
                sb.append((int) damageArray[i]);
            } else {
                sb.append(String.format("%.1f", damageArray[i]));
            }
        }
        return sb.toString();
    }
    
    /**
     * Check if text contains Chinese characters
     */
    private boolean isChineseText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }
    
    public void clearCache() {
        damageDataCache.invalidateAll();
        logger.info("Skill damage data cache cleared");
    }
    
    /**
     * Data class for skill damage information
     */
    public static class SkillDamageData {
        private String spellKey;
        private String name;
        private String description;
        private double[] baseDamage;
        private double[] percentageDamage;
        private double[] primaryDamage;
        private Map<String, Double> ratios;
        private double primaryRatio;
        
        // Getters and setters
        public String getSpellKey() { return spellKey; }
        public void setSpellKey(String spellKey) { this.spellKey = spellKey; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public double[] getBaseDamage() { return baseDamage; }
        public void setBaseDamage(double[] baseDamage) { this.baseDamage = baseDamage; }
        
        public double[] getPercentageDamage() { return percentageDamage; }
        public void setPercentageDamage(double[] percentageDamage) { this.percentageDamage = percentageDamage; }
        
        public double[] getPrimaryDamage() { return primaryDamage; }
        public void setPrimaryDamage(double[] primaryDamage) { this.primaryDamage = primaryDamage; }
        
        public Map<String, Double> getRatios() { return ratios; }
        public void setRatios(Map<String, Double> ratios) { this.ratios = ratios; }
        
        public double getPrimaryRatio() { return primaryRatio; }
        public void setPrimaryRatio(double primaryRatio) { this.primaryRatio = primaryRatio; }
    }
}