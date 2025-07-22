package com.lol.championselector.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommunityDragonSyncManager {
    private static final Logger logger = LoggerFactory.getLogger(CommunityDragonSyncManager.class);
    
    private static final String BASE_URL = "https://raw.communitydragon.org/latest/game/data/characters/";
    private static final String DATA_DIR = "src/main/resources/champion/data/community_dragon/";
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public CommunityDragonSyncManager() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        this.objectMapper = new ObjectMapper();
        
        // 创建数据目录
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }
    
    public boolean syncChampionSkillData(String championKey) {
        try {
            logger.debug("Syncing Community Dragon data for champion: {}", championKey);
            
            // Community Dragon 使用小写的英雄名
            String lowerChampionKey = championKey.toLowerCase();
            String url = BASE_URL + lowerChampionKey + "/" + lowerChampionKey + ".bin.json";
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "LOL-Helper/1.0.0")
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    
                    // 解析并提取技能数据
                    Map<String, Object> skillData = extractSkillData(responseBody, championKey);
                    
                    if (!skillData.isEmpty()) {
                        // 保存到本地文件
                        saveSkillData(championKey, skillData);
                        logger.debug("Successfully synced Community Dragon data for: {}", championKey);
                        return true;
                    } else {
                        logger.warn("No useful skill data found for: {}", championKey);
                        return false;
                    }
                } else {
                    logger.warn("Failed to fetch Community Dragon data for {}: HTTP {}", championKey, response.code());
                    return false;
                }
            }
            
        } catch (Exception e) {
            logger.error("Error syncing Community Dragon data for champion: {}", championKey, e);
            return false;
        }
    }
    
    private Map<String, Object> extractSkillData(String jsonData, String championKey) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            JsonNode rootNode = objectMapper.readTree(jsonData);
            
            // 在Community Dragon数据中查找技能相关信息
            result.put("damageData", extractDamageValues(rootNode));
            result.put("cooldownData", extractCooldownValues(rootNode));
            result.put("costData", extractCostValues(rootNode));
            result.put("rangeData", extractRangeValues(rootNode));
            
            logger.debug("Extracted {} damage entries, {} cooldown entries for {}", 
                        ((Map<?,?>)result.get("damageData")).size(),
                        ((Map<?,?>)result.get("cooldownData")).size(),
                        championKey);
            
        } catch (Exception e) {
            logger.error("Error parsing Community Dragon data for: {}", championKey, e);
        }
        
        return result;
    }
    
    private Map<String, List<Double>> extractDamageValues(JsonNode rootNode) {
        Map<String, List<Double>> damageData = new HashMap<>();
        
        // 遍历所有节点寻找伤害相关数据
        rootNode.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            
            if (key.toLowerCase().contains("spell") || key.toLowerCase().contains("damage")) {
                extractDamageFromNode(value, key, damageData);
            }
        });
        
        return damageData;
    }
    
    private void extractDamageFromNode(JsonNode node, String skillKey, Map<String, List<Double>> damageData) {
        if (node.isObject()) {
            node.fields().forEachRemaining(field -> {
                String fieldName = field.getKey();
                JsonNode fieldValue = field.getValue();
                
                // 查找包含伤害数值的字段
                if (fieldName.toLowerCase().contains("damage") || 
                    fieldName.toLowerCase().contains("ratio") ||
                    fieldName.toLowerCase().matches(".*[mM]\\d+.*")) {
                    
                    List<Double> values = extractNumberArray(fieldValue);
                    if (!values.isEmpty()) {
                        String damageKey = skillKey + "_" + fieldName;
                        damageData.put(damageKey, values);
                    }
                }
                
                // 递归搜索子节点
                if (fieldValue.isObject() || fieldValue.isArray()) {
                    extractDamageFromNode(fieldValue, skillKey, damageData);
                }
            });
        } else if (node.isArray()) {
            for (JsonNode arrayElement : node) {
                extractDamageFromNode(arrayElement, skillKey, damageData);
            }
        }
    }
    
    private Map<String, List<Double>> extractCooldownValues(JsonNode rootNode) {
        Map<String, List<Double>> cooldownData = new HashMap<>();
        
        rootNode.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            
            if (key.toLowerCase().contains("cooldown") || key.toLowerCase().contains("cd")) {
                List<Double> values = extractNumberArray(value);
                if (!values.isEmpty()) {
                    cooldownData.put(key, values);
                }
            }
        });
        
        return cooldownData;
    }
    
    private Map<String, List<Double>> extractCostValues(JsonNode rootNode) {
        Map<String, List<Double>> costData = new HashMap<>();
        
        rootNode.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            
            if (key.toLowerCase().contains("cost") || key.toLowerCase().contains("mana")) {
                List<Double> values = extractNumberArray(value);
                if (!values.isEmpty()) {
                    costData.put(key, values);
                }
            }
        });
        
        return costData;
    }
    
    private Map<String, List<Double>> extractRangeValues(JsonNode rootNode) {
        Map<String, List<Double>> rangeData = new HashMap<>();
        
        rootNode.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            
            if (key.toLowerCase().contains("range") || key.toLowerCase().contains("radius")) {
                List<Double> values = extractNumberArray(value);
                if (!values.isEmpty()) {
                    rangeData.put(key, values);
                }
            }
        });
        
        return rangeData;
    }
    
    private List<Double> extractNumberArray(JsonNode node) {
        List<Double> numbers = new ArrayList<>();
        
        if (node.isArray()) {
            for (JsonNode element : node) {
                if (element.isNumber()) {
                    numbers.add(element.asDouble());
                }
            }
        } else if (node.isNumber()) {
            numbers.add(node.asDouble());
        } else if (node.isTextual()) {
            // 尝试从文本中提取数字
            String text = node.asText();
            Pattern numberPattern = Pattern.compile("\\d+(?:\\.\\d+)?");
            Matcher matcher = numberPattern.matcher(text);
            while (matcher.find()) {
                try {
                    numbers.add(Double.parseDouble(matcher.group()));
                } catch (NumberFormatException e) {
                    // 忽略无效数字
                }
            }
        }
        
        return numbers;
    }
    
    private void saveSkillData(String championKey, Map<String, Object> skillData) throws IOException {
        File file = new File(DATA_DIR + championKey + "_community.json");
        
        // 添加元数据
        Map<String, Object> dataWithMeta = new HashMap<>();
        dataWithMeta.put("championKey", championKey);
        dataWithMeta.put("source", "Community Dragon");
        dataWithMeta.put("lastUpdate", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        dataWithMeta.put("skillData", skillData);
        
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, dataWithMeta);
    }
    
    public boolean syncAllChampions() {
        logger.info("Starting Community Dragon sync for all champions...");
        
        // 获取英雄列表
        LocalDataManager localDataManager = new LocalDataManager();
        localDataManager.loadChampions();
        List<String> championKeys = new ArrayList<>();
        
        localDataManager.getAllChampions().forEach(champion -> {
            championKeys.add(champion.getKey());
        });
        
        int successCount = 0;
        int failCount = 0;
        
        for (String championKey : championKeys) {
            try {
                if (syncChampionSkillData(championKey)) {
                    successCount++;
                } else {
                    failCount++;
                }
                
                // 进度报告
                if ((successCount + failCount) % 10 == 0) {
                    logger.info("Community Dragon sync progress: {}/{} champions", 
                               successCount + failCount, championKeys.size());
                }
                
                // 避免请求过于频繁
                Thread.sleep(100);
                
            } catch (Exception e) {
                logger.warn("Error syncing {}: {}", championKey, e.getMessage());
                failCount++;
            }
        }
        
        logger.info("Community Dragon sync completed: {} success, {} failed", successCount, failCount);
        return failCount == 0;
    }
    
    public void shutdown() {
        try {
            httpClient.dispatcher().executorService().shutdown();
            if (!httpClient.dispatcher().executorService().awaitTermination(5, TimeUnit.SECONDS)) {
                httpClient.dispatcher().executorService().shutdownNow();
            }
        } catch (InterruptedException e) {
            httpClient.dispatcher().executorService().shutdownNow();
        }
    }
}