package com.lol.championselector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SkillIconDownloadTool {
    
    private static final String SKILL_ICON_BASE_URL = "https://ddragon.leagueoflegends.com/cdn/15.14.1/img/spell/";
    private static final String PASSIVE_ICON_BASE_URL = "https://ddragon.leagueoflegends.com/cdn/15.14.1/img/passive/";
    private static final String CHAMPION_DATA_DIR = "src/main/resources/champion/data/full";
    private static final String SKILL_ICONS_DIR = "src/main/resources/champion";
    private static final int MAX_CONCURRENT_DOWNLOADS = 10;
    
    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    private final ObjectMapper objectMapper;
    private final Path skillIconsDir;
    
    public SkillIconDownloadTool() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(15))
            .readTimeout(Duration.ofSeconds(30))
            .build();
        
        this.executor = Executors.newFixedThreadPool(MAX_CONCURRENT_DOWNLOADS);
        this.objectMapper = new ObjectMapper();
        this.skillIconsDir = Paths.get(SKILL_ICONS_DIR);
        
        // Create base directory
        try {
            Files.createDirectories(skillIconsDir);
        } catch (IOException e) {
            System.err.println("Failed to create skill icons directories: " + e.getMessage());
        }
    }
    
    public void downloadAllSkillIcons() {
        System.out.println("Starting skill icon download process...");
        
        File dataDir = new File(CHAMPION_DATA_DIR);
        if (!dataDir.exists()) {
            System.err.println("Champion data directory not found: " + CHAMPION_DATA_DIR);
            return;
        }
        
        File[] championFiles = dataDir.listFiles((dir, name) -> name.endsWith("_complete.json"));
        if (championFiles == null) {
            System.err.println("No champion files found in: " + CHAMPION_DATA_DIR);
            return;
        }
        
        AtomicInteger totalIcons = new AtomicInteger(0);
        AtomicInteger downloadedIcons = new AtomicInteger(0);
        AtomicInteger failedIcons = new AtomicInteger(0);
        
        System.out.println("Found " + championFiles.length + " champions to process");
        
        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] futures = new CompletableFuture[championFiles.length];
        
        for (int i = 0; i < championFiles.length; i++) {
            File championFile = championFiles[i];
            futures[i] = CompletableFuture.runAsync(() -> 
                processChampionFile(championFile, totalIcons, downloadedIcons, failedIcons), 
                executor);
        }
        
        // Wait for all downloads to complete
        CompletableFuture.allOf(futures).join();
        
        System.out.println("Skill icon download completed!");
        System.out.println("Total icons: " + totalIcons.get() + 
                          ", Downloaded: " + downloadedIcons.get() + 
                          ", Failed: " + failedIcons.get() + 
                          ", Skipped: " + (totalIcons.get() - downloadedIcons.get() - failedIcons.get()));
    }
    
    private void processChampionFile(File championFile, AtomicInteger totalIcons, 
                                   AtomicInteger downloadedIcons, AtomicInteger failedIcons) {
        try {
            String championName = championFile.getName().replace("_complete.json", "");
            JsonNode rootNode = objectMapper.readTree(championFile);
            JsonNode skillsNode = rootNode.path("skills");
            
            if (skillsNode.isMissingNode()) {
                System.out.println("No skills data found for: " + championName);
                return;
            }
            
            // Process passive skill
            JsonNode passiveNode = skillsNode.path("passive");
            if (!passiveNode.isMissingNode()) {
                processSkillIcon(passiveNode, championName, true, totalIcons, downloadedIcons, failedIcons);
            }
            
            // Process active skills
            JsonNode spellsNode = skillsNode.path("spells");
            if (spellsNode.isArray()) {
                for (JsonNode spellNode : spellsNode) {
                    processSkillIcon(spellNode, championName, false, totalIcons, downloadedIcons, failedIcons);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error processing champion file: " + championFile.getName() + " - " + e.getMessage());
        }
    }
    
    private void processSkillIcon(JsonNode skillNode, String championName, boolean isPassive,
                                AtomicInteger totalIcons, AtomicInteger downloadedIcons, AtomicInteger failedIcons) {
        try {
            JsonNode imageNode = skillNode.path("image");
            if (imageNode.isMissingNode()) {
                return;
            }
            
            String iconFileName = imageNode.path("full").asText();
            if (iconFileName.isEmpty()) {
                return;
            }
            
            totalIcons.incrementAndGet();
            
            // Create champion-specific directory structure
            Path championDir = skillIconsDir.resolve(championName);
            Path localFile = championDir.resolve(iconFileName);
            
            // Skip if already exists
            if (Files.exists(localFile)) {
                System.out.println("Skill icon already exists: " + iconFileName);
                return;
            }
            
            // Download icon
            String baseUrl = isPassive ? PASSIVE_ICON_BASE_URL : SKILL_ICON_BASE_URL;
            String downloadUrl = baseUrl + iconFileName;
            
            if (downloadIcon(downloadUrl, localFile)) {
                downloadedIcons.incrementAndGet();
                System.out.println("Downloaded: " + iconFileName + " -> " + championName + "/" + iconFileName);
            } else {
                failedIcons.incrementAndGet();
                System.err.println("Failed to download: " + iconFileName);
            }
            
        } catch (Exception e) {
            System.err.println("Error processing skill icon for: " + championName + " - " + e.getMessage());
            failedIcons.incrementAndGet();
        }
    }
    
    private boolean downloadIcon(String url, Path localFile) {
        try {
            Request request = new Request.Builder()
                .url(url)
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    byte[] iconData = response.body().bytes();
                    
                    // Save to local file
                    Files.createDirectories(localFile.getParent());
                    try (FileOutputStream fos = new FileOutputStream(localFile.toFile())) {
                        fos.write(iconData);
                    }
                    
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to download icon from: " + url + " - " + e.getMessage());
        }
        
        return false;
    }
    
    public void shutdown() {
        try {
            executor.shutdown();
            if (!executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        SkillIconDownloadTool downloader = new SkillIconDownloadTool();
        try {
            downloader.downloadAllSkillIcons();
        } finally {
            downloader.shutdown();
        }
    }
}