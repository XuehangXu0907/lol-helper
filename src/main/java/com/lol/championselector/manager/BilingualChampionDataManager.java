package com.lol.championselector.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.championselector.model.Champion;
import com.lol.championselector.model.ChampionSkills;
import com.lol.championselector.model.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Manager for bilingual champion data
 * Handles champion data with both English and Chinese names/descriptions
 */
public class BilingualChampionDataManager {
    private static final Logger logger = LoggerFactory.getLogger(BilingualChampionDataManager.class);
    
    private static final String DATA_DIR = "src/main/resources/champion/data";
    private static final String CHAMPIONS_FILE = "champions_bilingual.json";
    private static final String SKILLS_DIR = "skills";
    
    private final ObjectMapper objectMapper;
    private List<Champion> champions;
    private Map<String, Champion> championByKey;
    private boolean useChineseNames = true;
    
    public BilingualChampionDataManager() {
        this.objectMapper = new ObjectMapper();
        this.champions = new ArrayList<>();
        this.championByKey = new HashMap<>();
    }
    
    /**
     * Sets whether to use Chinese names by default
     */
    public void setUseChineseNames(boolean useChineseNames) {
        this.useChineseNames = useChineseNames;
    }
    
    /**
     * Loads bilingual champion data
     */
    public boolean loadChampions() {
        try {
            File championsFile = new File(DATA_DIR, CHAMPIONS_FILE);
            if (!championsFile.exists()) {
                logger.warn("Bilingual champions data file not found: {}", championsFile.getAbsolutePath());
                return false;
            }
            
            JsonNode rootNode = objectMapper.readTree(championsFile);
            JsonNode dataNode = rootNode.path("data");
            
            if (!dataNode.isObject()) {
                logger.error("Invalid champions data structure");
                return false;
            }
            
            champions.clear();
            championByKey.clear();
            
            dataNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode championNode = entry.getValue();
                
                Champion champion = parseBilingualChampion(championNode);
                champions.add(champion);
                championByKey.put(key, champion);
            });
            
            // Sort by name
            champions.sort((a, b) -> {
                String nameA = useChineseNames ? a.getNameCn() : a.getNameEn();
                String nameB = useChineseNames ? b.getNameCn() : b.getNameEn();
                return nameA.compareTo(nameB);
            });
            
            logger.info("Loaded {} champions with bilingual data", champions.size());
            return true;
            
        } catch (Exception e) {
            logger.error("Error loading bilingual champion data", e);
            return false;
        }
    }
    
    private Champion parseBilingualChampion(JsonNode node) {
        Champion champion = new Champion();
        
        champion.setKey(node.path("key").asText());
        champion.setId(node.path("id").asText());
        
        // Set bilingual names
        champion.setNameEn(node.path("nameEn").asText());
        champion.setNameCn(node.path("nameZh").asText());
        
        // Name is already set based on language preference
        
        // Set bilingual titles
        String titleEn = node.path("titleEn").asText();
        String titleZh = node.path("titleZh").asText();
        champion.setTitle(useChineseNames ? titleZh : titleEn);
        
        // Tags
        List<String> tags = new ArrayList<>();
        node.path("tags").forEach(tag -> tags.add(tag.asText()));
        champion.setTags(tags);
        
        // Keywords for search - include both languages
        List<String> keywords = new ArrayList<>();
        keywords.add(champion.getNameEn().toLowerCase());
        keywords.add(champion.getNameCn());
        keywords.add(titleEn.toLowerCase());
        keywords.add(titleZh);
        // Add pinyin if needed (can be enhanced later)
        champion.setKeywords(keywords);
        
        return champion;
    }
    
    /**
     * Gets skills for a champion with bilingual data
     */
    public ChampionSkills getChampionSkills(String championKey) {
        try {
            File skillFile = new File(DATA_DIR, SKILLS_DIR + "/" + championKey + "_bilingual.json");
            if (!skillFile.exists()) {
                logger.warn("Bilingual skill file not found for champion: {}", championKey);
                return null;
            }
            
            JsonNode rootNode = objectMapper.readTree(skillFile);
            JsonNode dataNode = rootNode.path("data");
            
            if (!dataNode.isObject()) {
                return null;
            }
            
            // Get the first (and only) champion data
            JsonNode championData = dataNode.elements().next();
            
            return parseBilingualSkills(championData);
            
        } catch (Exception e) {
            logger.error("Error loading bilingual skills for champion: {}", championKey, e);
            return null;
        }
    }
    
    private ChampionSkills parseBilingualSkills(JsonNode championData) {
        ChampionSkills skills = new ChampionSkills();
        
        // Parse passive
        JsonNode passiveNode = championData.path("passive");
        skills.setPassive(parseBilingualPassive(passiveNode));
        
        // Parse spells
        List<Skill> spells = new ArrayList<>();
        JsonNode spellsNode = championData.path("spells");
        if (spellsNode.isArray()) {
            for (JsonNode spellNode : spellsNode) {
                spells.add(parseBilingualSpell(spellNode));
            }
        }
        skills.setSpells(spells);
        
        return skills;
    }
    
    private Skill parseBilingualPassive(JsonNode passiveNode) {
        Skill passive = new Skill();
        
        // Names
        String nameEn = passiveNode.path("nameEn").asText();
        String nameZh = passiveNode.path("nameZh").asText();
        passive.setName(useChineseNames ? nameZh : nameEn);
        
        // Descriptions
        String descEn = passiveNode.path("descriptionEn").asText();
        String descZh = passiveNode.path("descriptionZh").asText();
        passive.setDescription(useChineseNames ? descZh : descEn);
        
        return passive;
    }
    
    private Skill parseBilingualSpell(JsonNode spellNode) {
        Skill skill = new Skill();
        
        // Names
        String nameEn = spellNode.path("nameEn").asText();
        String nameZh = spellNode.path("nameZh").asText();
        skill.setName(useChineseNames ? nameZh : nameEn);
        
        // Descriptions
        String descEn = spellNode.path("descriptionEn").asText();
        String descZh = spellNode.path("descriptionZh").asText();
        skill.setDescription(useChineseNames ? descZh : descEn);
        
        // Tooltip
        String tooltipEn = spellNode.path("tooltipEn").asText();
        String tooltipZh = spellNode.path("tooltipZh").asText();
        skill.setTooltip(useChineseNames ? tooltipZh : tooltipEn);
        
        // Other properties
        skill.setCooldown(spellNode.path("cooldownBurn").asText());
        skill.setCost(spellNode.path("costBurn").asText());
        skill.setRange(spellNode.path("rangeBurn").asText());
        
        // Extract damage from tooltip
        String tooltip = skill.getTooltip();
        skill.setDamage(extractDamageFromTooltip(tooltip));
        
        return skill;
    }
    
    private String extractDamageFromTooltip(String tooltip) {
        // Simple extraction of damage values from tooltip
        // This can be enhanced with more sophisticated parsing
        if (tooltip == null || tooltip.isEmpty()) {
            return "";
        }
        
        // Look for patterns like "造成 X 伤害" or "deals X damage"
        // This is a simplified version - can be improved
        if (tooltip.contains("伤害") || tooltip.contains("damage")) {
            return "查看技能描述了解详细伤害";
        }
        
        return "";
    }
    
    /**
     * Search champions by keyword in both languages
     */
    public List<Champion> searchChampions(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>(champions);
        }
        
        String lowerKeyword = keyword.toLowerCase().trim();
        
        return champions.stream()
            .filter(champion -> matchesKeyword(champion, lowerKeyword))
            .toList();
    }
    
    private boolean matchesKeyword(Champion champion, String keyword) {
        // Check English name
        if (champion.getNameEn().toLowerCase().contains(keyword)) {
            return true;
        }
        
        // Check Chinese name
        if (champion.getNameCn().contains(keyword)) {
            return true;
        }
        
        // Check title
        if (champion.getTitle().toLowerCase().contains(keyword)) {
            return true;
        }
        
        // Check keywords
        return champion.getKeywords().stream()
            .anyMatch(k -> k.contains(keyword));
    }
    
    public List<Champion> getAllChampions() {
        return new ArrayList<>(champions);
    }
    
    public Champion getChampionByKey(String key) {
        return championByKey.get(key);
    }
    
    public int getChampionCount() {
        return champions.size();
    }
    
    public boolean isDataAvailable() {
        return !champions.isEmpty();
    }
}