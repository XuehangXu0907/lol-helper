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
    }
    
    public CompletableFuture<ChampionSkills> getSkillsAsync(String championKey) {
        ChampionSkills cached = skillsCache.getIfPresent(championKey);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        return CompletableFuture.supplyAsync(() -> {
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
                    
                    ChampionSkills skills = parseSkillsData(championData);
                    skillsCache.put(championKey, skills);
                    
                    return skills;
                }
            } catch (Exception e) {
                logger.warn("Failed to get skills for: {}", championKey, e);
                return createEmptySkills();
            }
        }, ForkJoinPool.commonPool());
    }
    
    private ChampionSkills parseSkillsData(JsonNode championData) {
        ChampionSkills skills = new ChampionSkills();
        
        try {
            JsonNode passiveNode = championData.path("passive");
            if (!passiveNode.isMissingNode()) {
                skills.setPassive(parseSkill(passiveNode, true));
            }
            
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
                skill.setCooldown(getArrayOrDefault(skillNode, "cooldownBurn", ""));
                skill.setCost(getArrayOrDefault(skillNode, "costBurn", ""));
                skill.setRange(getArrayOrDefault(skillNode, "rangeBurn", ""));
                skill.setDamage(extractDamageInfo(skillNode));
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
        
        if (field.isArray() && field.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < field.size(); i++) {
                if (i > 0) sb.append("/");
                sb.append(field.get(i).asText());
            }
            return sb.toString();
        }
        
        return field.asText(defaultValue);
    }
    
    private String extractDamageInfo(JsonNode skillNode) {
        try {
            JsonNode effectNode = skillNode.path("effect");
            if (effectNode.isArray() && effectNode.size() > 1) {
                JsonNode damageArray = effectNode.get(1);
                if (damageArray != null && damageArray.isArray()) {
                    StringBuilder damage = new StringBuilder();
                    for (int i = 0; i < damageArray.size(); i++) {
                        if (i > 0) damage.append("/");
                        damage.append(damageArray.get(i).asText());
                    }
                    return damage.toString();
                }
            }
            
            String tooltip = getTextOrDefault(skillNode, "tooltip", "");
            if (tooltip.contains("伤害")) {
                return extractDamageFromTooltip(tooltip);
            }
            
        } catch (Exception e) {
            logger.debug("Could not extract damage info", e);
        }
        
        return "";
    }
    
    private String extractDamageFromTooltip(String tooltip) {
        try {
            if (tooltip.contains("{{ e1 }}")) {
                return "基于技能等级";
            }
            
            if (tooltip.contains("法术强度")) {
                return "法术伤害";
            }
            
            if (tooltip.contains("攻击力")) {
                return "物理伤害";
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
    
    public void clearCache() {
        skillsCache.invalidateAll();
        logger.info("Skills cache cleared");
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