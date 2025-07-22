package com.lol.championselector.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lol.championselector.config.ChampionVersionMapping;
import javafx.scene.image.Image;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class AvatarManager {
    private static final Logger logger = LoggerFactory.getLogger(AvatarManager.class);
    
    private static final int MEMORY_CACHE_SIZE = 200;
    private static final Duration CACHE_EXPIRE_TIME = Duration.ofHours(24);
    private static final String DDRAGON_URL_TEMPLATE = 
        "https://ddragon.leagueoflegends.com/cdn/%s/img/champion/%s.png";
    
    private final Cache<String, Image> memoryCache;
    private final OkHttpClient httpClient;
    private final Path cacheDirectory;
    private final ExecutorService downloadExecutor;
    private Image defaultImage;
    
    public AvatarManager() {
        this.memoryCache = Caffeine.newBuilder()
            .maximumSize(MEMORY_CACHE_SIZE)
            .expireAfterWrite(CACHE_EXPIRE_TIME)
            .build();
        
        this.httpClient = new OkHttpClient.Builder()
            .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
            
        this.cacheDirectory = Paths.get("src/main/resources/champion/avatars");
        this.downloadExecutor = ForkJoinPool.commonPool();
        
        initializeCacheDirectory();
        loadDefaultImage();
    }
    
    private void initializeCacheDirectory() {
        try {
            Files.createDirectories(cacheDirectory);
        } catch (IOException e) {
            logger.error("Failed to create cache directory: {}", cacheDirectory, e);
            throw new RuntimeException("Failed to create cache directory", e);
        }
    }
    
    private void loadDefaultImage() {
        try {
            defaultImage = new Image(getClass().getResourceAsStream("/default_champion.png"));
            if (defaultImage.isError()) {
                logger.warn("Default image failed to load, creating empty image");
                defaultImage = new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");
            }
        } catch (Exception e) {
            logger.warn("Failed to load default image, creating empty image", e);
            defaultImage = new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");
        }
    }
    
    public CompletableFuture<Image> getAvatarAsync(String championKey) {
        Image cached = memoryCache.getIfPresent(championKey);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        Path localFile = getCachePath(championKey);
        if (Files.exists(localFile)) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Image image = new Image(localFile.toUri().toString());
                    if (!image.isError()) {
                        memoryCache.put(championKey, image);
                        return image;
                    }
                } catch (Exception e) {
                    logger.warn("Failed to load cached image for {}: {}", championKey, e.getMessage());
                }
                return getDefaultImage();
            }, downloadExecutor);
        }
        
        return downloadAvatarAsync(championKey);
    }
    
    private CompletableFuture<Image> downloadAvatarAsync(String championKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = buildAvatarUrl(championKey);
                Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "LoL Champion Selector")
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        logger.warn("Failed to download avatar for {}: HTTP {}", championKey, response.code());
                        return getDefaultImage();
                    }
                    
                    ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        logger.warn("Empty response body for avatar: {}", championKey);
                        return getDefaultImage();
                    }
                    
                    byte[] imageData = responseBody.bytes();
                    
                    saveToLocalCache(championKey, imageData);
                    
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
                    Image image = new Image(inputStream);
                    
                    if (!image.isError()) {
                        memoryCache.put(championKey, image);
                        return image;
                    } else {
                        logger.warn("Image error for {}: {}", championKey, image.getException());
                        return getDefaultImage();
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to download avatar for: {}", championKey, e);
                return getDefaultImage();
            }
        }, downloadExecutor);
    }
    
    private String buildAvatarUrl(String championKey) {
        String version = ChampionVersionMapping.getVersion(championKey);
        return String.format(DDRAGON_URL_TEMPLATE, version, championKey);
    }
    
    private Path getCachePath(String championKey) {
        return cacheDirectory.resolve(championKey + ".png");
    }
    
    private void saveToLocalCache(String championKey, byte[] imageData) {
        try {
            Path cachePath = getCachePath(championKey);
            Files.write(cachePath, imageData);
            logger.debug("Saved avatar to cache: {}", cachePath);
        } catch (IOException e) {
            logger.warn("Failed to save avatar to cache for {}: {}", championKey, e.getMessage());
        }
    }
    
    public Image getDefaultImage() {
        return defaultImage;
    }
    
    public void clearCache() {
        memoryCache.invalidateAll();
        logger.info("Avatar cache cleared");
    }
    
    public void shutdown() {
        try {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
            logger.info("AvatarManager shut down successfully");
        } catch (Exception e) {
            logger.warn("Error during AvatarManager shutdown", e);
        }
    }
}