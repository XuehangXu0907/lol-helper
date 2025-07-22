package com.lol.championselector.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Enhanced Riot Data Fetcher that saves complete champion data including stats and skills
 */
public class EnhancedRiotDataFetcher {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedRiotDataFetcher.class);
    
    // API URLs
    private static final String VERSION_API = "https://ddragon.leagueoflegends.com/api/versions.json";
    private static final String CHAMPION_LIST_API = "https://ddragon.leagueoflegends.com/cdn/%s/data/%s/champion.json";
    private static final String CHAMPION_DETAIL_API = "https://ddragon.leagueoflegends.com/cdn/%s/data/%s/champion/%s.json";
    
    // Languages
    private static final String LANG_EN = "en_US";
    private static final String LANG_ZH = "zh_CN";
    
    // Data directory
    private static final String DATA_DIR = "src/main/resources/champion/data";
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String currentVersion;
    
    public EnhancedRiotDataFetcher() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        this.objectMapper = new ObjectMapper();
        
        createDataDirectories();
    }
    
    private void createDataDirectories() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            Files.createDirectories(Paths.get(DATA_DIR, "champions"));
            Files.createDirectories(Paths.get(DATA_DIR, "full"));
            logger.info("Data directories created/verified");
        } catch (IOException e) {
            logger.error("Failed to create data directories", e);
        }
    }
    
    /**
     * Fetches all champion data with complete stats and skills
     */
    public boolean fetchCompleteChampionData() {
        try {
            logger.info("Starting complete champion data fetch from Riot API...");
            
            // 1. Get latest version
            currentVersion = getLatestVersion();
            if (currentVersion == null) {
                logger.error("Failed to get latest version");
                return false;
            }
            logger.info("Latest version: {}", currentVersion);
            
            // 2. Fetch champion list in both languages
            JsonNode championsEn = fetchChampionList(currentVersion, LANG_EN);
            JsonNode championsZh = fetchChampionList(currentVersion, LANG_ZH);
            
            if (championsEn == null || championsZh == null) {
                logger.error("Failed to fetch champion lists");
                return false;
            }
            
            // 3. Create summary with basic info and stats
            JsonNode championSummary = createChampionSummary(championsEn, championsZh);
            saveChampionSummary(championSummary);
            
            // 4. Fetch and save complete data for each champion
            boolean detailsSuccess = fetchAndSaveAllChampionDetails(championsEn);
            
            // 5. Create consolidated data file with all champions
            createConsolidatedDataFile(championsEn);
            
            // 6. Save version and metadata
            saveMetadata();
            
            logger.info("Complete champion data fetch finished successfully");
            return detailsSuccess;
            
        } catch (Exception e) {
            logger.error("Error fetching champion data", e);
            return false;
        }
    }
    
    private String getLatestVersion() throws IOException {
        Request request = new Request.Builder()
            .url(VERSION_API)
            .build();
            
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Failed to get version info: HTTP {}", response.code());
                return null;
            }
            
            JsonNode versions = objectMapper.readTree(response.body().string());
            if (versions.isArray() && versions.size() > 0) {
                return versions.get(0).asText();
            }
        }
        
        return null;
    }
    
    private JsonNode fetchChampionList(String version, String language) throws IOException {
        String url = String.format(CHAMPION_LIST_API, version, language);
        Request request = new Request.Builder()
            .url(url)
            .build();
            
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Failed to fetch {} champion list: HTTP {}", language, response.code());
                return null;
            }
            
            return objectMapper.readTree(response.body().string());
        }
    }
    
    /**
     * Creates a summary file with all champions' basic info and stats
     */
    private JsonNode createChampionSummary(JsonNode enData, JsonNode zhData) {
        ObjectNode summary = objectMapper.createObjectNode();
        summary.put("version", currentVersion);
        summary.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        ArrayNode championArray = objectMapper.createArrayNode();
        JsonNode enChampions = enData.path("data");
        JsonNode zhChampions = zhData.path("data");
        
        enChampions.fieldNames().forEachRemaining(championKey -> {
            JsonNode enChamp = enChampions.get(championKey);
            JsonNode zhChamp = zhChampions.get(championKey);
            
            ObjectNode champion = objectMapper.createObjectNode();
            
            // Basic info
            champion.put("id", enChamp.path("id").asText());
            champion.put("key", enChamp.path("key").asText());
            champion.put("name", enChamp.path("name").asText());
            champion.put("nameEn", enChamp.path("name").asText());
            champion.put("nameZh", zhChamp.path("name").asText());
            champion.put("title", zhChamp.path("title").asText());
            champion.put("titleEn", enChamp.path("title").asText());
            champion.put("titleZh", zhChamp.path("title").asText());
            
            // Tags and info
            champion.set("tags", enChamp.path("tags"));
            champion.set("info", enChamp.path("info"));
            champion.put("partype", enChamp.path("partype").asText());
            
            // Complete stats (三围属性)
            JsonNode stats = enChamp.path("stats");
            ObjectNode statsNode = objectMapper.createObjectNode();
            
            // 基础属性
            statsNode.put("hp", stats.path("hp").asDouble());
            statsNode.put("hpperlevel", stats.path("hpperlevel").asDouble());
            statsNode.put("mp", stats.path("mp").asDouble());
            statsNode.put("mpperlevel", stats.path("mpperlevel").asDouble());
            statsNode.put("movespeed", stats.path("movespeed").asDouble());
            statsNode.put("armor", stats.path("armor").asDouble());
            statsNode.put("armorperlevel", stats.path("armorperlevel").asDouble());
            statsNode.put("spellblock", stats.path("spellblock").asDouble());
            statsNode.put("spellblockperlevel", stats.path("spellblockperlevel").asDouble());
            statsNode.put("attackrange", stats.path("attackrange").asDouble());
            statsNode.put("hpregen", stats.path("hpregen").asDouble());
            statsNode.put("hpregenperlevel", stats.path("hpregenperlevel").asDouble());
            statsNode.put("mpregen", stats.path("mpregen").asDouble());
            statsNode.put("mpregenperlevel", stats.path("mpregenperlevel").asDouble());
            statsNode.put("crit", stats.path("crit").asDouble());
            statsNode.put("critperlevel", stats.path("critperlevel").asDouble());
            statsNode.put("attackdamage", stats.path("attackdamage").asDouble());
            statsNode.put("attackdamageperlevel", stats.path("attackdamageperlevel").asDouble());
            statsNode.put("attackspeedperlevel", stats.path("attackspeedperlevel").asDouble());
            statsNode.put("attackspeed", stats.path("attackspeed").asDouble());
            
            champion.set("stats", statsNode);
            
            championArray.add(champion);
        });
        
        summary.set("champions", championArray);
        return summary;
    }
    
    /**
     * Fetches and saves detailed data for all champions
     */
    private boolean fetchAndSaveAllChampionDetails(JsonNode championList) {
        JsonNode champions = championList.path("data");
        int totalChampions = champions.size();
        int successCount = 0;
        int errorCount = 0;
        
        logger.info("Fetching complete data for {} champions", totalChampions);
        
        for (JsonNode champion : champions) {
            String championKey = champion.path("id").asText();
            
            try {
                // Fetch both language versions
                JsonNode detailEn = fetchChampionDetail(currentVersion, LANG_EN, championKey);
                JsonNode detailZh = fetchChampionDetail(currentVersion, LANG_ZH, championKey);
                
                if (detailEn != null && detailZh != null) {
                    // Create complete champion data file
                    JsonNode completeData = createCompleteChampionData(detailEn, detailZh);
                    saveChampionCompleteData(championKey, completeData);
                    successCount++;
                    
                    if (successCount % 10 == 0) {
                        logger.info("Progress: {}/{} champions", successCount, totalChampions);
                    }
                } else {
                    errorCount++;
                    logger.warn("Failed to fetch details for champion: {}", championKey);
                }
                
                // Rate limiting
                Thread.sleep(100);
                
            } catch (Exception e) {
                errorCount++;
                logger.error("Error processing champion: {}", championKey, e);
            }
        }
        
        logger.info("Champion details fetch completed: {} success, {} errors", successCount, errorCount);
        return errorCount == 0 || (double) successCount / totalChampions > 0.8;
    }
    
    private JsonNode fetchChampionDetail(String version, String language, String championKey) throws IOException {
        String url = String.format(CHAMPION_DETAIL_API, version, language, championKey);
        Request request = new Request.Builder()
            .url(url)
            .build();
            
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }
            
            return objectMapper.readTree(response.body().string());
        }
    }
    
    /**
     * Creates complete champion data with all stats, skills, and bilingual info
     */
    private JsonNode createCompleteChampionData(JsonNode enData, JsonNode zhData) {
        ObjectNode complete = objectMapper.createObjectNode();
        
        // Get champion data
        String championKey = enData.path("data").fieldNames().next();
        JsonNode enChamp = enData.path("data").get(championKey);
        JsonNode zhChamp = zhData.path("data").get(championKey);
        
        // Metadata
        complete.put("version", currentVersion);
        complete.put("lastUpdate", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // Basic info
        ObjectNode info = objectMapper.createObjectNode();
        info.put("id", enChamp.path("id").asText());
        info.put("key", enChamp.path("key").asText());
        info.put("name", enChamp.path("name").asText());
        info.put("nameEn", enChamp.path("name").asText());
        info.put("nameZh", zhChamp.path("name").asText());
        info.put("title", zhChamp.path("title").asText());
        info.put("titleEn", enChamp.path("title").asText());
        info.put("titleZh", zhChamp.path("title").asText());
        info.put("lore", zhChamp.path("lore").asText());
        info.put("loreEn", enChamp.path("lore").asText());
        info.put("blurb", zhChamp.path("blurb").asText());
        info.put("blurbEn", enChamp.path("blurb").asText());
        
        complete.set("info", info);
        
        // Tags and classifications
        complete.set("tags", enChamp.path("tags"));
        complete.set("info_numeric", enChamp.path("info")); // attack, defense, magic, difficulty
        complete.put("partype", enChamp.path("partype").asText());
        
        // Complete stats with descriptions
        complete.set("stats", enChamp.path("stats"));
        
        // Image info
        complete.set("image", enChamp.path("image"));
        
        // Skills - complete data
        ObjectNode skills = objectMapper.createObjectNode();
        
        // Passive
        ObjectNode passive = createCompleteSkillData(
            enChamp.path("passive"), 
            zhChamp.path("passive"),
            "passive"
        );
        skills.set("passive", passive);
        
        // Active skills (Q, W, E, R)
        ArrayNode spells = objectMapper.createArrayNode();
        JsonNode enSpells = enChamp.path("spells");
        JsonNode zhSpells = zhChamp.path("spells");
        
        String[] skillKeys = {"Q", "W", "E", "R"};
        for (int i = 0; i < enSpells.size() && i < skillKeys.length; i++) {
            ObjectNode spell = createCompleteSkillData(
                enSpells.get(i),
                zhSpells.get(i),
                skillKeys[i]
            );
            spells.add(spell);
        }
        
        skills.set("spells", spells);
        complete.set("skills", skills);
        
        // Tips
        complete.set("allytips", zhChamp.path("allytips"));
        complete.set("enemytips", zhChamp.path("enemytips"));
        
        // Recommended items (if available)
        complete.set("recommended", enChamp.path("recommended"));
        
        return complete;
    }
    
    /**
     * Creates complete skill data with all details
     */
    private ObjectNode createCompleteSkillData(JsonNode enSkill, JsonNode zhSkill, String skillKey) {
        ObjectNode skill = objectMapper.createObjectNode();
        
        skill.put("key", skillKey);
        skill.put("id", enSkill.path("id").asText());
        skill.put("name", zhSkill.path("name").asText());
        skill.put("nameEn", enSkill.path("name").asText());
        skill.put("nameZh", zhSkill.path("name").asText());
        
        // Descriptions
        skill.put("description", zhSkill.path("description").asText());
        skill.put("descriptionEn", enSkill.path("description").asText());
        
        // For active skills
        if (!skillKey.equals("passive")) {
            // Tooltip with damage formulas
            skill.put("tooltip", zhSkill.path("tooltip").asText());
            skill.put("tooltipEn", enSkill.path("tooltip").asText());
            
            // Level progression
            skill.set("leveltip", zhSkill.path("leveltip"));
            skill.put("maxrank", enSkill.path("maxrank").asInt());
            
            // Cooldowns
            skill.set("cooldown", enSkill.path("cooldown"));
            skill.put("cooldownBurn", enSkill.path("cooldownBurn").asText());
            
            // Costs
            skill.set("cost", enSkill.path("cost"));
            skill.put("costBurn", enSkill.path("costBurn").asText());
            skill.put("costType", zhSkill.path("costType").asText());
            
            // Range
            skill.set("range", enSkill.path("range"));
            skill.put("rangeBurn", enSkill.path("rangeBurn").asText());
            
            // Effects and scaling
            skill.set("effect", enSkill.path("effect"));
            skill.set("effectBurn", enSkill.path("effectBurn"));
            skill.set("vars", enSkill.path("vars"));
            
            // Additional data
            skill.set("datavalues", enSkill.path("datavalues"));
            skill.put("resource", zhSkill.path("resource").asText());
            skill.put("maxammo", enSkill.path("maxammo").asText());
        }
        
        // Image
        skill.set("image", enSkill.path("image"));
        
        return skill;
    }
    
    /**
     * Creates a consolidated file with all champions' data
     */
    private void createConsolidatedDataFile(JsonNode championList) {
        try {
            ObjectNode consolidated = objectMapper.createObjectNode();
            consolidated.put("version", currentVersion);
            consolidated.put("dataType", "consolidated_champion_data");
            consolidated.put("lastUpdate", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // Load all individual champion files and consolidate
            ObjectNode allChampions = objectMapper.createObjectNode();
            File fullDir = new File(DATA_DIR, "full");
            File[] championFiles = fullDir.listFiles((dir, name) -> name.endsWith(".json"));
            
            if (championFiles != null) {
                for (File file : championFiles) {
                    try {
                        JsonNode championData = objectMapper.readTree(file);
                        String championId = championData.path("info").path("id").asText();
                        allChampions.set(championId, championData);
                    } catch (Exception e) {
                        logger.warn("Failed to read champion file: {}", file.getName());
                    }
                }
            }
            
            consolidated.set("champions", allChampions);
            consolidated.put("totalChampions", allChampions.size());
            
            // Save consolidated file
            Path consolidatedPath = Paths.get(DATA_DIR, "all_champions_data.json");
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(consolidatedPath.toFile(), consolidated);
                
            logger.info("Created consolidated data file with {} champions", allChampions.size());
            
        } catch (Exception e) {
            logger.error("Failed to create consolidated data file", e);
        }
    }
    
    private void saveChampionSummary(JsonNode summary) throws IOException {
        Path filePath = Paths.get(DATA_DIR, "champions_summary.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), summary);
        logger.info("Saved champion summary to: {}", filePath);
    }
    
    private void saveChampionCompleteData(String championKey, JsonNode data) throws IOException {
        Path filePath = Paths.get(DATA_DIR, "full", championKey + "_complete.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), data);
        logger.debug("Saved complete data for champion: {}", championKey);
    }
    
    private void saveMetadata() throws IOException {
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("version", currentVersion);
        metadata.put("lastUpdate", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        metadata.put("dataSource", "Riot Data Dragon API");
        metadata.put("languages", "en_US, zh_CN");
        
        // Save metadata
        Path metadataPath = Paths.get(DATA_DIR, "metadata.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataPath.toFile(), metadata);
        
        // Save version file
        Path versionFile = Paths.get(DATA_DIR, "version.txt");
        Files.write(versionFile, currentVersion.getBytes());
        
        logger.info("Saved metadata and version info");
    }
    
    public void shutdown() {
        try {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
            logger.info("EnhancedRiotDataFetcher shut down successfully");
        } catch (Exception e) {
            logger.warn("Error during shutdown", e);
        }
    }
}