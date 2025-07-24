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
    
    // Enhanced cache configuration with memory limits
    private static final int MEMORY_CACHE_SIZE = 150; // Reduced from 200
    private static final long MAX_MEMORY_USAGE_MB = 64; // 64MB memory limit
    private static final Duration CACHE_EXPIRE_TIME = Duration.ofHours(12); // Reduced from 24h
    private static final Duration CACHE_REFRESH_TIME = Duration.ofMinutes(30); // Auto-refresh interval
    private static final String DDRAGON_URL_TEMPLATE = 
        "https://ddragon.leagueoflegends.com/cdn/%s/img/champion/%s.png";
    
    private final Cache<String, Image> memoryCache;
    private final OkHttpClient httpClient;
    private final Path cacheDirectory;
    private final ExecutorService downloadExecutor;
    private Image defaultImage;
    private volatile boolean isShuttingDown = false;
    
    // Cache statistics
    private long totalCacheHits = 0;
    private long totalCacheMisses = 0;
    private long lastCacheCleanupTime = System.currentTimeMillis();
    
    public AvatarManager() {
        this.memoryCache = Caffeine.newBuilder()
            .maximumWeight(MAX_MEMORY_USAGE_MB * 1024 * 1024) // Convert MB to bytes, use weight-based eviction
            .weigher((String key, Image image) -> {
                // Estimate image memory usage
                int width = (int) image.getWidth();
                int height = (int) image.getHeight();
                return width * height * 4; // Assume 4 bytes per pixel (RGBA)
            })
            .expireAfterWrite(CACHE_EXPIRE_TIME)
            .recordStats() // Enable statistics recording
            .removalListener((key, value, cause) -> {
                logger.debug("Cache entry removed: {} (cause: {})", key, cause);
            })
            .build();
        
        this.httpClient = new OkHttpClient.Builder()
            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES)) // Reduced pool size
            .connectTimeout(8, TimeUnit.SECONDS) // Reduced timeout
            .readTimeout(12, TimeUnit.SECONDS)   // Reduced timeout
            .writeTimeout(8, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false) // Disable automatic retry to prevent hanging
            .build();
            
        this.cacheDirectory = Paths.get("src/main/resources/champion/avatars");
        this.downloadExecutor = ForkJoinPool.commonPool();
        
        initializeCacheDirectory();
        loadDefaultImage();
        
        // Schedule periodic cache cleanup
        schedulePeriodicCleanup();
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
        if (isShuttingDown) {
            return CompletableFuture.completedFuture(getDefaultImage());
        }
        
        // Check memory cache first
        Image cached = memoryCache.getIfPresent(championKey);
        if (cached != null) {
            totalCacheHits++;
            return CompletableFuture.completedFuture(cached);
        }
        
        totalCacheMisses++;
        
        // Perform periodic cache maintenance
        performPeriodicMaintenance();
        
        Path localFile = getCachePath(championKey);
        if (Files.exists(localFile)) {
            return CompletableFuture.supplyAsync(() -> {
                if (isShuttingDown) {
                    return getDefaultImage();
                }
                
                try {
                    Image image = new Image(localFile.toUri().toString());
                    if (!image.isError()) {
                        // Only cache if not shutting down and image is valid
                        if (!isShuttingDown) {
                            memoryCache.put(championKey, image);
                        }
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
        if (isShuttingDown) {
            return CompletableFuture.completedFuture(getDefaultImage());
        }
        
        return CompletableFuture.supplyAsync(() -> {
            if (isShuttingDown) {
                return getDefaultImage();
            }
            
            try {
                String url = buildAvatarUrl(championKey);
                Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "LoL Champion Selector")
                    .addHeader("Cache-Control", "max-age=3600") // Cache for 1 hour
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (isShuttingDown) {
                        return getDefaultImage();
                    }
                    
                    if (!response.isSuccessful()) {
                        logger.warn("Failed to download avatar for {}: HTTP {}", championKey, response.code());
                        return getDefaultImage();
                    }
                    
                    ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        logger.warn("Empty response body for avatar: {}", championKey);
                        return getDefaultImage();
                    }
                    
                    // Check content length to avoid downloading huge files
                    long contentLength = responseBody.contentLength();
                    if (contentLength > 5 * 1024 * 1024) { // 5MB limit
                        logger.warn("Avatar file too large for {}: {} bytes", championKey, contentLength);
                        return getDefaultImage();
                    }
                    
                    byte[] imageData = responseBody.bytes();
                    
                    if (!isShuttingDown) {
                        saveToLocalCache(championKey, imageData);
                    }
                    
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
                    Image image = new Image(inputStream);
                    
                    if (!image.isError() && !isShuttingDown) {
                        memoryCache.put(championKey, image);
                        return image;
                    } else if (image.isError()) {
                        logger.warn("Image error for {}: {}", championKey, image.getException());
                        return getDefaultImage();
                    }
                    
                    return getDefaultImage();
                }
            } catch (java.net.SocketTimeoutException e) {
                logger.debug("Timeout downloading avatar for: {} - {}", championKey, e.getMessage());
                return getDefaultImage();
            } catch (java.io.IOException e) {
                if (!isShuttingDown) {
                    logger.debug("IO error downloading avatar for: {} - {}", championKey, e.getMessage());
                }
                return getDefaultImage();
            } catch (Exception e) {
                if (!isShuttingDown) {
                    logger.warn("Failed to download avatar for: {}", championKey, e);
                }
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
        totalCacheHits = 0;
        totalCacheMisses = 0;
        logger.info("Avatar cache cleared");
    }
    
    private void performPeriodicMaintenance() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheCleanupTime > 300000) { // 5 minutes
            lastCacheCleanupTime = currentTime;
            
            // Clean up cache statistics and log status
            memoryCache.cleanUp();
            
            long totalRequests = totalCacheHits + totalCacheMisses;
            if (totalRequests > 0) {
                double hitRate = (double) totalCacheHits / totalRequests * 100;
                logger.debug("Avatar cache stats - Size: {}, Hit rate: {:.1f}%, Memory usage: ~{}MB", 
                           memoryCache.estimatedSize(), hitRate, estimateMemoryUsage() / (1024 * 1024));
            }
        }
    }
    
    private void schedulePeriodicCleanup() {
        // This would ideally use a ScheduledExecutorService, but for simplicity
        // we'll rely on the periodic maintenance during normal operations
    }
    
    private long estimateMemoryUsage() {
        // Rough estimation of memory usage
        return memoryCache.estimatedSize() * 50 * 1024; // Assume ~50KB per image
    }
    
    public CacheStats getCacheStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats stats = memoryCache.stats();
        return new CacheStats(
            memoryCache.estimatedSize(),
            stats.hitCount(),
            stats.missCount(),
            stats.hitRate(),
            estimateMemoryUsage()
        );
    }
    
    public static class CacheStats {
        public final long size;
        public final long hitCount;
        public final long missCount;
        public final double hitRate;
        public final long estimatedMemoryUsage;
        
        public CacheStats(long size, long hitCount, long missCount, double hitRate, long estimatedMemoryUsage) {
            this.size = size;
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.hitRate = hitRate;
            this.estimatedMemoryUsage = estimatedMemoryUsage;
        }
        
        @Override
        public String toString() {
            return String.format("CacheStats{size=%d, hitRate=%.2f%%, memoryUsage=%dKB}", 
                               size, hitRate * 100, estimatedMemoryUsage / 1024);
        }
    }
    
    public void shutdown() {
        logger.info("Shutting down AvatarManager...");
        isShuttingDown = true;
        
        try {
            // Clear memory cache to free up memory immediately
            memoryCache.invalidateAll();
            
            // Cancel all pending HTTP calls
            httpClient.dispatcher().cancelAll();
            
            // Shutdown HTTP client resources
            httpClient.dispatcher().executorService().shutdown();
            try {
                if (!httpClient.dispatcher().executorService().awaitTermination(5, TimeUnit.SECONDS)) {
                    httpClient.dispatcher().executorService().shutdownNow();
                }
            } catch (InterruptedException e) {
                httpClient.dispatcher().executorService().shutdownNow();
                Thread.currentThread().interrupt();
            }
            
            // Close all connections
            httpClient.connectionPool().evictAll();
            
            logger.info("AvatarManager shut down successfully - Final cache stats: {}", getCacheStats());
        } catch (Exception e) {
            logger.warn("Error during AvatarManager shutdown", e);
        }
    }
}