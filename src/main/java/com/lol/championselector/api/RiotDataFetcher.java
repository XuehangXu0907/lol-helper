package com.lol.championselector.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Fetches champion data from Riot Data Dragon API in both English and Chinese
 */
public class RiotDataFetcher {
    private static final Logger logger = LoggerFactory.getLogger(RiotDataFetcher.class);
    
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
    
    public RiotDataFetcher() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        this.objectMapper = new ObjectMapper();
        
        // Ensure data directory exists
        createDataDirectories();
    }
    
    private void createDataDirectories() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            Files.createDirectories(Paths.get(DATA_DIR, "bilingual"));
            Files.createDirectories(Paths.get(DATA_DIR, "skills"));
            logger.info("Data directories created/verified");
        } catch (IOException e) {
            logger.error("Failed to create data directories", e);
        }
    }
    
    /**
     * Fetches the latest champion data in both English and Chinese
     */
    public boolean fetchLatestChampionData() {
        try {
            logger.info("Starting to fetch latest champion data from Riot API...");
            
            // 1. Get latest version
            String latestVersion = getLatestVersion();
            if (latestVersion == null) {
                logger.error("Failed to get latest version");
                return false;
            }
            logger.info("Latest version: {}", latestVersion);
            
            // 2. Fetch champion list in both languages
            JsonNode championsEn = fetchChampionList(latestVersion, LANG_EN);
            JsonNode championsZh = fetchChampionList(latestVersion, LANG_ZH);
            
            if (championsEn == null || championsZh == null) {
                logger.error("Failed to fetch champion lists");
                return false;
            }
            
            // 3. Merge bilingual data
            JsonNode mergedChampions = mergeBilingualChampionData(championsEn, championsZh);
            
            // 4. Save merged champion list
            saveMergedChampionList(mergedChampions, latestVersion);
            
            // 5. Fetch detailed data for each champion
            boolean detailsSuccess = fetchAllChampionDetails(latestVersion, championsEn);
            
            // 6. Save version info
            saveVersionInfo(latestVersion);
            
            logger.info("Champion data fetch completed successfully");
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
    
    private JsonNode mergeBilingualChampionData(JsonNode enData, JsonNode zhData) {
        ObjectNode merged = objectMapper.createObjectNode();
        merged.put("type", enData.path("type").asText());
        merged.put("format", enData.path("format").asText());
        merged.put("version", enData.path("version").asText());
        
        ObjectNode mergedData = objectMapper.createObjectNode();
        JsonNode enChampions = enData.path("data");
        JsonNode zhChampions = zhData.path("data");
        
        // Iterate through all champions
        enChampions.fieldNames().forEachRemaining(championKey -> {
            JsonNode enChamp = enChampions.get(championKey);
            JsonNode zhChamp = zhChampions.get(championKey);
            
            ObjectNode championNode = objectMapper.createObjectNode();
            
            // Basic info from English
            championNode.put("version", enChamp.path("version").asText());
            championNode.put("id", enChamp.path("id").asText());
            championNode.put("key", enChamp.path("key").asText());
            
            // Names in both languages
            championNode.put("name", enChamp.path("name").asText());
            championNode.put("nameEn", enChamp.path("name").asText());
            championNode.put("nameZh", zhChamp.path("name").asText());
            
            // Titles in both languages
            championNode.put("title", zhChamp.path("title").asText()); // Default to Chinese
            championNode.put("titleEn", enChamp.path("title").asText());
            championNode.put("titleZh", zhChamp.path("title").asText());
            
            // Other info
            championNode.put("blurb", zhChamp.path("blurb").asText());
            championNode.set("info", enChamp.path("info"));
            championNode.set("image", enChamp.path("image"));
            championNode.set("tags", enChamp.path("tags"));
            championNode.put("partype", enChamp.path("partype").asText());
            championNode.set("stats", enChamp.path("stats"));
            
            mergedData.set(championKey, championNode);
        });
        
        merged.set("data", mergedData);
        return merged;
    }
    
    private boolean fetchAllChampionDetails(String version, JsonNode championList) {
        JsonNode champions = championList.path("data");
        int totalChampions = champions.size();
        int successCount = 0;
        int errorCount = 0;
        
        logger.info("Fetching detailed data for {} champions", totalChampions);
        
        for (JsonNode champion : champions) {
            String championKey = champion.path("id").asText();
            
            try {
                // Fetch English version
                JsonNode detailEn = fetchChampionDetail(version, LANG_EN, championKey);
                // Fetch Chinese version
                JsonNode detailZh = fetchChampionDetail(version, LANG_ZH, championKey);
                
                if (detailEn != null && detailZh != null) {
                    // Merge and save detailed data
                    JsonNode mergedDetail = mergeBilingualDetailData(detailEn, detailZh);
                    saveChampionDetail(championKey, mergedDetail);
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
    
    private JsonNode mergeBilingualDetailData(JsonNode enData, JsonNode zhData) {
        ObjectNode merged = objectMapper.createObjectNode();
        merged.put("type", enData.path("type").asText());
        merged.put("format", enData.path("format").asText());
        merged.put("version", enData.path("version").asText());
        
        ObjectNode mergedData = objectMapper.createObjectNode();
        
        // Get the champion data (there should be only one)
        String championKey = enData.path("data").fieldNames().next();
        JsonNode enChamp = enData.path("data").get(championKey);
        JsonNode zhChamp = zhData.path("data").get(championKey);
        
        ObjectNode championNode = objectMapper.createObjectNode();
        
        // Basic info
        championNode.put("id", enChamp.path("id").asText());
        championNode.put("key", enChamp.path("key").asText());
        championNode.put("name", enChamp.path("name").asText());
        championNode.put("nameEn", enChamp.path("name").asText());
        championNode.put("nameZh", zhChamp.path("name").asText());
        championNode.put("title", zhChamp.path("title").asText());
        championNode.put("titleEn", enChamp.path("title").asText());
        championNode.put("titleZh", zhChamp.path("title").asText());
        
        // Lore in both languages
        championNode.put("lore", zhChamp.path("lore").asText());
        championNode.put("loreEn", enChamp.path("lore").asText());
        championNode.put("loreZh", zhChamp.path("lore").asText());
        
        // Other data
        championNode.set("image", enChamp.path("image"));
        championNode.set("tags", enChamp.path("tags"));
        championNode.put("partype", enChamp.path("partype").asText());
        championNode.set("info", enChamp.path("info"));
        championNode.set("stats", enChamp.path("stats"));
        
        // Spells - merge bilingual
        championNode.set("spells", mergeBilingualSpells(enChamp.path("spells"), zhChamp.path("spells")));
        
        // Passive - merge bilingual
        championNode.set("passive", mergeBilingualPassive(enChamp.path("passive"), zhChamp.path("passive")));
        
        // Allytips and enemytips in Chinese
        championNode.set("allytips", zhChamp.path("allytips"));
        championNode.set("enemytips", zhChamp.path("enemytips"));
        
        mergedData.set(championKey, championNode);
        merged.set("data", mergedData);
        
        return merged;
    }
    
    private JsonNode mergeBilingualSpells(JsonNode enSpells, JsonNode zhSpells) {
        if (!enSpells.isArray() || !zhSpells.isArray()) {
            return enSpells;
        }
        
        ObjectNode[] mergedSpells = new ObjectNode[enSpells.size()];
        
        for (int i = 0; i < enSpells.size(); i++) {
            JsonNode enSpell = enSpells.get(i);
            JsonNode zhSpell = zhSpells.get(i);
            
            ObjectNode spell = objectMapper.createObjectNode();
            
            // IDs and keys
            spell.put("id", enSpell.path("id").asText());
            spell.put("name", zhSpell.path("name").asText());
            spell.put("nameEn", enSpell.path("name").asText());
            spell.put("nameZh", zhSpell.path("name").asText());
            
            // Descriptions
            spell.put("description", zhSpell.path("description").asText());
            spell.put("descriptionEn", enSpell.path("description").asText());
            spell.put("descriptionZh", zhSpell.path("description").asText());
            
            // Tooltip
            spell.put("tooltip", zhSpell.path("tooltip").asText());
            spell.put("tooltipEn", enSpell.path("tooltip").asText());
            spell.put("tooltipZh", zhSpell.path("tooltip").asText());
            
            // Other data
            spell.set("leveltip", zhSpell.path("leveltip"));
            spell.put("maxrank", enSpell.path("maxrank").asInt());
            spell.set("cooldown", enSpell.path("cooldown"));
            spell.put("cooldownBurn", enSpell.path("cooldownBurn").asText());
            spell.set("cost", enSpell.path("cost"));
            spell.put("costBurn", enSpell.path("costBurn").asText());
            spell.set("datavalues", enSpell.path("datavalues"));
            spell.set("effect", enSpell.path("effect"));
            spell.set("effectBurn", enSpell.path("effectBurn"));
            spell.set("vars", enSpell.path("vars"));
            spell.put("costType", zhSpell.path("costType").asText());
            spell.put("maxammo", enSpell.path("maxammo").asText());
            spell.set("range", enSpell.path("range"));
            spell.put("rangeBurn", enSpell.path("rangeBurn").asText());
            spell.set("image", enSpell.path("image"));
            spell.put("resource", zhSpell.path("resource").asText());
            
            mergedSpells[i] = spell;
        }
        
        return objectMapper.valueToTree(mergedSpells);
    }
    
    private JsonNode mergeBilingualPassive(JsonNode enPassive, JsonNode zhPassive) {
        ObjectNode passive = objectMapper.createObjectNode();
        
        passive.put("name", zhPassive.path("name").asText());
        passive.put("nameEn", enPassive.path("name").asText());
        passive.put("nameZh", zhPassive.path("name").asText());
        
        passive.put("description", zhPassive.path("description").asText());
        passive.put("descriptionEn", enPassive.path("description").asText());
        passive.put("descriptionZh", zhPassive.path("description").asText());
        
        passive.set("image", enPassive.path("image"));
        
        return passive;
    }
    
    private void saveMergedChampionList(JsonNode champions, String version) throws IOException {
        Path filePath = Paths.get(DATA_DIR, "champions_bilingual.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), champions);
        logger.info("Saved bilingual champion list to: {}", filePath);
    }
    
    private void saveChampionDetail(String championKey, JsonNode detail) throws IOException {
        Path filePath = Paths.get(DATA_DIR, "skills", championKey + "_bilingual.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), detail);
        logger.debug("Saved bilingual details for champion: {}", championKey);
    }
    
    private void saveVersionInfo(String version) throws IOException {
        Path versionFile = Paths.get(DATA_DIR, "version.txt");
        Files.write(versionFile, version.getBytes());
        
        Path updateTimeFile = Paths.get(DATA_DIR, "last_update.txt");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        Files.write(updateTimeFile, timestamp.getBytes());
        
        logger.info("Saved version info: {}", version);
    }
    
    public void shutdown() {
        try {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
            logger.info("RiotDataFetcher shut down successfully");
        } catch (Exception e) {
            logger.warn("Error during shutdown", e);
        }
    }
}