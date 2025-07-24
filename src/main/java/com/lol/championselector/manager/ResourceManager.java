package com.lol.championselector.manager;

import javafx.animation.Timeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified resource manager for ExecutorService and Timeline instances
 * Prevents resource leaks and provides centralized shutdown management
 */
public class ResourceManager {
    private static final Logger logger = LoggerFactory.getLogger(ResourceManager.class);
    
    private static final ResourceManager INSTANCE = new ResourceManager();
    
    // Thread pool management
    private final Set<ExecutorService> executorServices = ConcurrentHashMap.newKeySet();
    private final Set<ScheduledExecutorService> scheduledExecutorServices = ConcurrentHashMap.newKeySet();
    
    // Timeline management
    private final Set<Timeline> timelines = ConcurrentHashMap.newKeySet();
    
    // Shared thread pools
    private final ScheduledExecutorService sharedScheduler;
    private final ExecutorService sharedExecutor;
    
    // State management
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    private final AtomicInteger threadCounter = new AtomicInteger(0);
    
    // Resource monitoring
    private volatile long lastCleanupTime = System.currentTimeMillis();
    private static final long CLEANUP_INTERVAL_MS = 60000; // 1 minute
    
    private ResourceManager() {
        // Create shared thread pools with proper naming and daemon threads
        this.sharedScheduler = Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "SharedScheduler-" + threadCounter.incrementAndGet());
            t.setDaemon(true);
            t.setUncaughtExceptionHandler((thread, ex) -> 
                logger.error("Uncaught exception in shared scheduler thread: {}", thread.getName(), ex));
            return t;
        });
        
        this.sharedExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "SharedExecutor-" + threadCounter.incrementAndGet());
            t.setDaemon(true);
            t.setUncaughtExceptionHandler((thread, ex) -> 
                logger.error("Uncaught exception in shared executor thread: {}", thread.getName(), ex));
            return t;
        });
        
        // Register shared pools for shutdown
        scheduledExecutorServices.add(sharedScheduler);
        executorServices.add(sharedExecutor);
        
        // Schedule periodic cleanup
        schedulePeriodicCleanup();
        
        logger.info("ResourceManager initialized with shared thread pools");
    }
    
    public static ResourceManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Get the shared ScheduledExecutorService for light-weight tasks
     */
    public ScheduledExecutorService getSharedScheduler() {
        if (isShuttingDown.get()) {
            throw new IllegalStateException("ResourceManager is shutting down");
        }
        return sharedScheduler;
    }
    
    /**
     * Get the shared ExecutorService for general async tasks
     */
    public ExecutorService getSharedExecutor() {
        if (isShuttingDown.get()) {
            throw new IllegalStateException("ResourceManager is shutting down");
        }
        return sharedExecutor;
    }
    
    /**
     * Create and register a new ScheduledExecutorService
     */
    public ScheduledExecutorService createScheduledExecutor(String namePrefix, int corePoolSize) {
        if (isShuttingDown.get()) {
            throw new IllegalStateException("ResourceManager is shutting down");
        }
        
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(corePoolSize, r -> {
            Thread t = new Thread(r, namePrefix + "-" + threadCounter.incrementAndGet());
            t.setDaemon(true);
            t.setUncaughtExceptionHandler((thread, ex) -> 
                logger.error("Uncaught exception in thread: {}", thread.getName(), ex));
            return t;
        });
        
        scheduledExecutorServices.add(executor);
        logger.debug("Created ScheduledExecutorService: {} (total: {})", namePrefix, scheduledExecutorServices.size());
        return executor;
    }
    
    /**
     * Create and register a new ExecutorService
     */
    public ExecutorService createExecutor(String namePrefix, int corePoolSize, int maxPoolSize) {
        if (isShuttingDown.get()) {
            throw new IllegalStateException("ResourceManager is shutting down");
        }
        
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            corePoolSize, maxPoolSize,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            r -> {
                Thread t = new Thread(r, namePrefix + "-" + threadCounter.incrementAndGet());
                t.setDaemon(true);
                t.setUncaughtExceptionHandler((thread, ex) -> 
                    logger.error("Uncaught exception in thread: {}", thread.getName(), ex));
                return t;
            }
        );
        
        executor.allowCoreThreadTimeOut(true);
        executorServices.add(executor);
        logger.debug("Created ExecutorService: {} (total: {})", namePrefix, executorServices.size());
        return executor;
    }
    
    /**
     * Register a Timeline for automatic cleanup
     */
    public void registerTimeline(Timeline timeline) {
        if (isShuttingDown.get()) {
            logger.warn("Attempted to register Timeline while shutting down");
            return;
        }
        
        timelines.add(timeline);
        logger.debug("Registered Timeline (total: {})", timelines.size());
    }
    
    /**
     * Unregister a Timeline (should be called when Timeline is manually stopped)
     */
    public void unregisterTimeline(Timeline timeline) {
        if (timelines.remove(timeline)) {
            logger.debug("Unregistered Timeline (remaining: {})", timelines.size());
        }
    }
    
    /**
     * Unregister an ExecutorService (should be called when manually shutting down)
     */
    public void unregisterExecutor(ExecutorService executor) {
        if (executorServices.remove(executor) || scheduledExecutorServices.remove(executor)) {
            logger.debug("Unregistered ExecutorService (remaining: {} + {})", 
                       executorServices.size(), scheduledExecutorServices.size());
        }
    }
    
    private void schedulePeriodicCleanup() {
        sharedScheduler.scheduleWithFixedDelay(() -> {
            try {
                performPeriodicCleanup();
            } catch (Exception e) {
                logger.error("Error during periodic cleanup", e);
            }
        }, CLEANUP_INTERVAL_MS, CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }
    
    private void performPeriodicCleanup() {
        if (isShuttingDown.get()) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime < CLEANUP_INTERVAL_MS) {
            return;
        }
        
        lastCleanupTime = currentTime;
        
        // Clean up terminated executors
        executorServices.removeIf(ExecutorService::isTerminated);
        scheduledExecutorServices.removeIf(ExecutorService::isTerminated);
        
        // Clean up stopped timelines
        timelines.removeIf(timeline -> timeline.getStatus() == Timeline.Status.STOPPED);
        
        // Log resource status
        logger.debug("Resource cleanup - Executors: {} + {}, Timelines: {}, Active threads: {}", 
                   executorServices.size(), scheduledExecutorServices.size(), 
                   timelines.size(), Thread.activeCount());
    }
    
    /**
     * Get current resource statistics
     */
    public ResourceStats getResourceStats() {
        return new ResourceStats(
            executorServices.size(),
            scheduledExecutorServices.size(),
            timelines.size(),
            Thread.activeCount(),
            threadCounter.get()
        );
    }
    
    public static class ResourceStats {
        public final int executorServices;
        public final int scheduledExecutorServices;
        public final int timelines;
        public final int activeThreads;
        public final int totalThreadsCreated;
        
        public ResourceStats(int executorServices, int scheduledExecutorServices, 
                           int timelines, int activeThreads, int totalThreadsCreated) {
            this.executorServices = executorServices;
            this.scheduledExecutorServices = scheduledExecutorServices;
            this.timelines = timelines;
            this.activeThreads = activeThreads;
            this.totalThreadsCreated = totalThreadsCreated;
        }
        
        @Override
        public String toString() {
            return String.format("ResourceStats{executors=%d, schedulers=%d, timelines=%d, activeThreads=%d, totalCreated=%d}",
                               executorServices, scheduledExecutorServices, timelines, activeThreads, totalThreadsCreated);
        }
    }
    
    /**
     * Shutdown all managed resources
     */
    public void shutdown() {
        if (!isShuttingDown.compareAndSet(false, true)) {
            logger.warn("ResourceManager shutdown already in progress");
            return;
        }
        
        logger.info("Shutting down ResourceManager - Current stats: {}", getResourceStats());
        
        // Stop all timelines first
        for (Timeline timeline : timelines) {
            try {
                if (timeline.getStatus() == Timeline.Status.RUNNING) {
                    timeline.stop();
                }
            } catch (Exception e) {
                logger.warn("Error stopping Timeline", e);
            }
        }
        timelines.clear();
        
        // Shutdown all ExecutorServices
        shutdownExecutorServices(executorServices, "ExecutorService");
        shutdownExecutorServices(scheduledExecutorServices, "ScheduledExecutorService");
        
        logger.info("ResourceManager shutdown completed - Final stats: {}", getResourceStats());
    }
    
    private void shutdownExecutorServices(Set<? extends ExecutorService> services, String type) {
        for (ExecutorService service : services) {
            try {
                service.shutdown();
                if (!service.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warn("{} did not terminate gracefully, forcing shutdown", type);
                    service.shutdownNow();
                    
                    if (!service.awaitTermination(3, TimeUnit.SECONDS)) {
                        logger.error("{} could not be terminated", type);
                    }
                }
            } catch (InterruptedException e) {
                logger.warn("Interrupted while shutting down {}", type);
                service.shutdownNow();
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("Error shutting down {}", type, e);
            }
        }
        services.clear();
    }
    
    /**
     * Check if ResourceManager is in shutdown state
     */
    public boolean isShuttingDown() {
        return isShuttingDown.get();
    }
}