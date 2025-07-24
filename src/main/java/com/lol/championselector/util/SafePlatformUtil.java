package com.lol.championselector.util;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    /**
     * Safely execute a runnable on the JavaFX Application Thread
     * @param runnable the task to execute
     * @return true if successfully executed, false otherwise
     */
    public static boolean runLater(Runnable runnable) {
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
     * Force reset the initialization flag (for testing purposes)
     */
    public static void resetInitializationFlag() {
        synchronized (initLock) {
            javafxInitialized.set(false);
            logger.debug("JavaFX initialization flag reset");
        }
    }
}