package com.lol.championselector.util;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Safe JavaFX Platform utility with error handling and initialization checks
 */
public class SafePlatformUtil {
    private static final Logger logger = LoggerFactory.getLogger(SafePlatformUtil.class);
    private static final AtomicBoolean javafxInitialized = new AtomicBoolean(false);
    private static final Object initLock = new Object();
    
    // Performance optimization: batch Platform.runLater calls
    private static final BlockingQueue<Runnable> pendingTasks = new LinkedBlockingQueue<>();
    private static final AtomicInteger pendingTaskCount = new AtomicInteger(0);
    private static final int BATCH_SIZE = 10;
    private static volatile boolean batchProcessingEnabled = true;
    
    static {
        // Start batch processor
        Thread batchProcessor = new Thread(SafePlatformUtil::processBatchedTasks, "JavaFX-BatchProcessor");
        batchProcessor.setDaemon(true);
        batchProcessor.start();
    }
    
    /**
     * Safely execute a runnable on the JavaFX Application Thread
     * @param runnable the task to execute
     * @return true if successfully executed, false otherwise
     */
    public static boolean runLater(Runnable runnable) {
        return runLater(runnable, false);
    }
    
    /**
     * Safely execute a runnable on the JavaFX Application Thread
     * @param runnable the task to execute
     * @param priority if true, execute immediately without batching
     * @return true if successfully executed, false otherwise
     */
    public static boolean runLater(Runnable runnable, boolean priority) {
        if (runnable == null) {
            logger.warn("Attempted to run null runnable on JavaFX thread");
            return false;
        }
        
        try {
            // Check if JavaFX is available and initialized
            if (!isJavaFXAvailable()) {
                logger.warn("JavaFX is not available, cannot execute runnable");
                return false;
            }
            
            // If we're already on the JavaFX thread, execute directly
            if (Platform.isFxApplicationThread()) {
                try {
                    runnable.run();
                    return true;
                } catch (Exception e) {
                    logger.error("Error executing runnable directly on JavaFX thread", e);
                    return false;
                }
            }
            
            // Use batching for better performance unless priority is requested
            if (!priority && batchProcessingEnabled && pendingTaskCount.get() < 100) {
                pendingTasks.offer(runnable);
                pendingTaskCount.incrementAndGet();
                return true;
            }
            
            // Execute immediately
            Platform.runLater(() -> {
                try {
                    runnable.run();
                } catch (Exception e) {
                    logger.error("Error executing runnable on JavaFX thread", e);
                }
            });
            return true;
            
        } catch (IllegalStateException e) {
            logger.error("JavaFX Platform not initialized properly", e);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error when running on JavaFX thread", e);
            return false;
        }
    }
    
    /**
     * Safely execute a runnable and wait for completion
     * @param runnable the task to execute
     * @param timeoutSeconds maximum time to wait
     * @return true if successfully executed within timeout, false otherwise
     */
    public static boolean runAndWait(Runnable runnable, int timeoutSeconds) {
        if (runnable == null) {
            logger.warn("Attempted to run null runnable on JavaFX thread");
            return false;
        }
        
        if (Platform.isFxApplicationThread()) {
            // Already on JavaFX thread, execute directly
            try {
                runnable.run();
                return true;
            } catch (Exception e) {
                logger.error("Error executing runnable directly on JavaFX thread", e);
                return false;
            }
        }
        
        try {
            if (!isJavaFXAvailable()) {
                logger.warn("JavaFX is not available, cannot execute runnable");
                return false;
            }
            
            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean success = new AtomicBoolean(false);
            
            Platform.runLater(() -> {
                try {
                    runnable.run();
                    success.set(true);
                } catch (Exception e) {
                    logger.error("Error executing runnable on JavaFX thread", e);
                } finally {
                    latch.countDown();
                }
            });
            
            boolean completed = latch.await(timeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                logger.warn("JavaFX runnable timed out after {} seconds", timeoutSeconds);
                return false;
            }
            
            return success.get();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while waiting for JavaFX runnable", e);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error when running and waiting on JavaFX thread", e);
            return false;
        }
    }
    
    /**
     * Check if JavaFX is available and properly initialized
     * @return true if JavaFX is available, false otherwise
     */
    public static boolean isJavaFXAvailable() {
        if (javafxInitialized.get()) {
            return true;
        }
        
        synchronized (initLock) {
            if (javafxInitialized.get()) {
                return true;
            }
            
            try {
                // Try to access JavaFX classes
                Class.forName("javafx.application.Platform");
                
                // Check if we can create JavaFX objects (this will fail if not initialized)
                Platform.runLater(() -> {
                    // Simple test to ensure Platform is working
                });
                
                javafxInitialized.set(true);
                logger.debug("JavaFX is available and initialized");
                return true;
                
            } catch (ClassNotFoundException e) {
                logger.error("JavaFX classes not found in classpath", e);
                return false;
            } catch (IllegalStateException e) {
                logger.warn("JavaFX toolkit not initialized: {}", e.getMessage());
                return false;
            } catch (Exception e) {
                logger.error("Error checking JavaFX availability", e);
                return false;
            }
        }
    }
    
    /**
     * Get the current thread information for debugging
     * @return thread information string
     */
    public static String getThreadInfo() {
        Thread currentThread = Thread.currentThread();
        boolean isFxThread = Platform.isFxApplicationThread();
        return String.format("Thread: %s, JavaFX Thread: %s", 
                            currentThread.getName(), isFxThread);
    }
    
    /**
     * Process batched JavaFX tasks for better performance
     */
    private static void processBatchedTasks() {
        while (true) {
            try {
                // Wait for tasks or timeout after 50ms
                Runnable task = pendingTasks.poll(50, TimeUnit.MILLISECONDS);
                if (task == null) {
                    continue;
                }
                
                // Collect batch of tasks
                java.util.List<Runnable> batch = new java.util.ArrayList<>();
                batch.add(task);
                pendingTaskCount.decrementAndGet();
                
                // Try to get more tasks up to batch size
                for (int i = 1; i < BATCH_SIZE; i++) {
                    Runnable nextTask = pendingTasks.poll();
                    if (nextTask == null) {
                        break;
                    }
                    batch.add(nextTask);
                    pendingTaskCount.decrementAndGet();
                }
                
                // Execute batch on JavaFX thread
                if (!batch.isEmpty()) {
                    Platform.runLater(() -> {
                        for (Runnable batchedTask : batch) {
                            try {
                                batchedTask.run();
                            } catch (Exception e) {
                                logger.error("Error executing batched runnable on JavaFX thread", e);
                            }
                        }
                    });
                }
                
            } catch (InterruptedException e) {
                logger.debug("Batch processor interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error in batch processor", e);
            }
        }
    }
    
    /**
     * Enable or disable batch processing
     */
    public static void setBatchProcessingEnabled(boolean enabled) {
        batchProcessingEnabled = enabled;
        logger.debug("Batch processing {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Get current batch processing stats
     */
    public static String getBatchStats() {
        return String.format("Pending tasks: %d, Batch processing: %s", 
                           pendingTaskCount.get(), batchProcessingEnabled ? "enabled" : "disabled");
    }
    
    /**
     * Force reset the initialization flag (for testing purposes)
     */
    public static void resetInitializationFlag() {
        synchronized (initLock) {
            javafxInitialized.set(false);
            logger.debug("JavaFX initialization flag reset");
        }
    }
    
    /**
     * Flush all pending batched tasks immediately
     */
    public static void flushPendingTasks() {
        int taskCount = pendingTaskCount.get();
        if (taskCount > 0) {
            logger.debug("Flushing {} pending JavaFX tasks", taskCount);
            
            java.util.List<Runnable> allTasks = new java.util.ArrayList<>();
            Runnable task;
            while ((task = pendingTasks.poll()) != null) {
                allTasks.add(task);
                pendingTaskCount.decrementAndGet();
            }
            
            if (!allTasks.isEmpty()) {
                Platform.runLater(() -> {
                    for (Runnable flushTask : allTasks) {
                        try {
                            flushTask.run();
                        } catch (Exception e) {
                            logger.error("Error executing flushed runnable on JavaFX thread", e);
                        }
                    }
                });
            }
        }
    }
}