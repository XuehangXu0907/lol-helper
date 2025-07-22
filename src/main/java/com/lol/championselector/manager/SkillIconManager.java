package com.lol.championselector.manager;

import javafx.scene.image.Image;
import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SkillIconManager {
    private static final Logger logger = LoggerFactory.getLogger(SkillIconManager.class);
    
    private static final String SKILL_ICON_BASE_URL = "https://ddragon.leagueoflegends.com/cdn/15.14.1/img/spell/";
    private static final String PASSIVE_ICON_BASE_URL = "https://ddragon.leagueoflegends.com/cdn/15.14.1/img/passive/";
    private static final String CACHE_DIR = "skill_icons";
    private static final String LOCAL_SKILL_ICONS_DIR = "champion/skill_icons";
    private static final int MAX_CACHE_SIZE = 500;
    private static final int CACHE_EXPIRE_HOURS = 24;
    
    private final AsyncCache<String, Image> memoryCache;
    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    private final Path cacheDirectory;
    private final Image defaultSkillIcon;
    private final Image defaultPassiveIcon;
    
    public SkillIconManager() {
        this.memoryCache = Caffeine.newBuilder()
            .maximumSize(MAX_CACHE_SIZE)
            .expireAfterWrite(Duration.ofHours(CACHE_EXPIRE_HOURS))
            .buildAsync();
            
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(10))
            .build();
            
        this.executor = Executors.newFixedThreadPool(3);
        this.cacheDirectory = Paths.get(CACHE_DIR);
        
        // Create cache directory
        try {
            Files.createDirectories(cacheDirectory);
        } catch (IOException e) {
            logger.warn("Failed to create cache directory: {}", e.getMessage());
        }
        
        // Create default icons
        this.defaultSkillIcon = createDefaultIcon("⚡", 48, 48);
        this.defaultPassiveIcon = createDefaultIcon("🔮", 48, 48);
    }
    
    public CompletableFuture<Image> getSkillIconAsync(String iconFileName) {
        return getSkillIconAsync(iconFileName, null);
    }
    
    public CompletableFuture<Image> getSkillIconAsync(String iconFileName, String championKey) {
        if (iconFileName == null || iconFileName.isEmpty()) {
            return CompletableFuture.completedFuture(defaultSkillIcon);
        }
        
        String cacheKey = "skill_" + iconFileName;
        return getIconAsync(cacheKey, SKILL_ICON_BASE_URL + iconFileName, defaultSkillIcon, championKey);
    }
    
    public CompletableFuture<Image> getPassiveIconAsync(String iconFileName) {
        return getPassiveIconAsync(iconFileName, null);
    }
    
    public CompletableFuture<Image> getPassiveIconAsync(String iconFileName, String championKey) {
        if (iconFileName == null || iconFileName.isEmpty()) {
            return CompletableFuture.completedFuture(defaultPassiveIcon);
        }
        
        String cacheKey = "passive_" + iconFileName;
        return getIconAsync(cacheKey, PASSIVE_ICON_BASE_URL + iconFileName, defaultPassiveIcon, championKey);
    }
    
    private CompletableFuture<Image> getIconAsync(String cacheKey, String url, Image defaultIcon, String championKey) {
        return memoryCache.get(cacheKey, (key, executor) -> {
            return loadIconFromCacheOrNetwork(key, url, defaultIcon, championKey);
        });
    }
    
    private CompletableFuture<Image> loadIconFromCacheOrNetwork(String cacheKey, String url, Image defaultIcon, String championKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // First, try to load from local resources
                Image localImage = loadFromLocalResources(cacheKey, championKey);
                if (localImage != null) {
                    logger.debug("Loaded skill icon from local resources: {}", cacheKey);
                    return localImage;
                }
                
                // Try to load from file cache second
                Path cachedFile = cacheDirectory.resolve(cacheKey + ".png");
                if (Files.exists(cachedFile)) {
                    try {
                        byte[] imageData = Files.readAllBytes(cachedFile);
                        Image image = new Image(new ByteArrayInputStream(imageData));
                        if (!image.isError()) {
                            logger.debug("Loaded skill icon from cache: {}", cacheKey);
                            return image;
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to load cached skill icon: {}", e.getMessage());
                    }
                }
                
                // Download from network as last resort
                return downloadIcon(url, cachedFile, defaultIcon);
                
            } catch (Exception e) {
                logger.warn("Failed to load skill icon {}: {}", cacheKey, e.getMessage());
                return defaultIcon;
            }
        }, executor);
    }
    
    private Image downloadIcon(String url, Path cacheFile, Image defaultIcon) {
        try {
            Request request = new Request.Builder()
                .url(url)
                .build();
                
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    byte[] imageData = response.body().bytes();
                    
                    // Save to cache
                    try {
                        Files.createDirectories(cacheFile.getParent());
                        try (FileOutputStream fos = new FileOutputStream(cacheFile.toFile())) {
                            fos.write(imageData);
                        }
                        logger.debug("Downloaded and cached skill icon: {}", url);
                    } catch (IOException e) {
                        logger.warn("Failed to cache skill icon: {}", e.getMessage());
                    }
                    
                    // Create image
                    Image image = new Image(new ByteArrayInputStream(imageData));
                    if (!image.isError()) {
                        return image;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to download skill icon from {}: {}", url, e.getMessage());
        }
        
        return defaultIcon;
    }
    
    private Image loadFromLocalResources(String cacheKey, String championKey) {
        try {
            if (championKey == null || championKey.isEmpty()) {
                return null;
            }
            
            // Extract filename from cache key
            String fileName = cacheKey.replace("passive_", "").replace("skill_", "");
            
            // Try to load from champion folder in skill_icons directory
            String resourcePath = "/" + LOCAL_SKILL_ICONS_DIR + "/" + championKey + "/" + fileName;
            try (var inputStream = getClass().getResourceAsStream(resourcePath)) {
                if (inputStream != null) {
                    Image image = new Image(inputStream);
                    if (!image.isError()) {
                        return image;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to load skill icon from local resources: {}", cacheKey);
        }
        
        return null;
    }
    
    private Image createDefaultIcon(String emoji, int width, int height) {
        try {
            // Create a simple colored rectangle as default icon
            javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(width, height);
            javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
            
            // Set background color
            gc.setFill(javafx.scene.paint.Color.web("#2c2c2c"));
            gc.fillRect(0, 0, width, height);
            
            // Set border
            gc.setStroke(javafx.scene.paint.Color.web("#463714"));
            gc.setLineWidth(2);
            gc.strokeRect(1, 1, width - 2, height - 2);
            
            // Set text
            gc.setFill(javafx.scene.paint.Color.web("#c9aa71"));
            gc.setFont(javafx.scene.text.Font.font("Arial", 20));
            gc.fillText(emoji, width / 2 - 10, height / 2 + 5);
            
            javafx.scene.image.WritableImage image = new javafx.scene.image.WritableImage(width, height);
            canvas.snapshot(null, image);
            return image;
        } catch (Exception e) {
            logger.warn("Failed to create default skill icon: {}", e.getMessage());
            return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");
        }
    }
    
    public void shutdown() {
        try {
            executor.shutdown();
            if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            memoryCache.asMap().clear();
            logger.info("SkillIconManager shut down successfully");
        } catch (Exception e) {
            logger.warn("Error during SkillIconManager shutdown", e);
        }
    }
}