package com.lol.championselector.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;
import java.util.function.Consumer;

/**
 * Centralized exception handling utility to prevent resource leaks
 * and provide consistent error handling across the application
 */
public class ExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);
    
    /**
     * Execute a task with automatic exception handling and resource cleanup
     */
    public static <T> T executeWithFallback(Supplier<T> task, T fallbackValue, String operationName) {
        return executeWithFallback(task, fallbackValue, operationName, null);
    }
    
    /**
     * Execute a task with automatic exception handling, resource cleanup, and custom cleanup action
     */
    public static <T> T executeWithFallback(Supplier<T> task, T fallbackValue, String operationName, Runnable cleanup) {
        try {
            return task.get();
        } catch (OutOfMemoryError e) {
            logger.error("Out of memory during {}", operationName, e);
            performEmergencyCleanup();
            return fallbackValue;
        } catch (RuntimeException e) {
            if (e.getCause() instanceof InterruptedException) {
                logger.warn("Interrupted during {}", operationName);
                Thread.currentThread().interrupt();
            } else {
                logger.error("Runtime error during {}", operationName, e);
            }
            return fallbackValue;
        } catch (Exception e) {
            logger.error("Error during {}", operationName, e);
            return fallbackValue;
        } finally {
            if (cleanup != null) {
                try {
                    cleanup.run();
                } catch (Exception e) {
                    logger.warn("Error during cleanup for {}", operationName, e);
                }
            }
        }
    }
    
    /**
     * Execute a void task with automatic exception handling
     */
    public static void executeWithLogging(Runnable task, String operationName) {
        executeWithLogging(task, operationName, null);
    }
    
    /**
     * Execute a void task with automatic exception handling and cleanup
     */
    public static void executeWithLogging(Runnable task, String operationName, Runnable cleanup) {
        try {
            task.run();
        } catch (OutOfMemoryError e) {
            logger.error("Out of memory during {}", operationName, e);
            performEmergencyCleanup();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof InterruptedException) {
                logger.warn("Interrupted during {}", operationName);
                Thread.currentThread().interrupt();
            } else {
                logger.error("Runtime error during {}", operationName, e);
            }
        } catch (Exception e) {
            logger.error("Error during {}", operationName, e);
        } finally {
            if (cleanup != null) {
                try {
                    cleanup.run();
                } catch (Exception e) {
                    logger.warn("Error during cleanup for {}", operationName, e);
                }
            }
        }
    }
    
    
    /**
     * Safe resource closer - closes resources without throwing exceptions
     */
    public static void safeClose(AutoCloseable resource, String resourceName) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                logger.warn("Error closing {}: {}", resourceName, e.getMessage());
            }
        }
    }
    
    
    /**
     * Execute a callback with exception safety
     */
    public static <T> void safeCallback(Consumer<T> callback, T value, String callbackName) {
        if (callback != null) {
            try {
                callback.accept(value);
            } catch (Exception e) {
                logger.error("Error in callback {}", callbackName, e);
            }
        }
    }
    
    /**
     * Execute a runnable callback with exception safety
     */
    public static void safeCallback(Runnable callback, String callbackName) {
        if (callback != null) {
            try {
                callback.run();
            } catch (Exception e) {
                logger.error("Error in callback {}", callbackName, e);
            }
        }
    }
    
    /**
     * Emergency cleanup in case of severe memory issues
     */
    private static void performEmergencyCleanup() {
        try {
            // Force garbage collection
            System.gc();
            // Note: System.runFinalization() is deprecated and removed in newer Java versions
            // The JVM will handle finalization automatically when needed
            
            // Log memory status
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            logger.warn("Emergency cleanup triggered - Memory usage: {}MB / {}MB", 
                       usedMemory / (1024 * 1024), totalMemory / (1024 * 1024));
            
            // Trigger resource manager cleanup if available
            try {
                Class<?> resourceManagerClass = Class.forName("com.lol.championselector.manager.ResourceManager");
                Object instance = resourceManagerClass.getMethod("getInstance").invoke(null);
                resourceManagerClass.getMethod("shutdown").invoke(instance);
            } catch (Exception e) {
                logger.debug("Could not trigger ResourceManager cleanup", e);
            }
            
        } catch (Exception e) {
            logger.error("Error during emergency cleanup", e);
        }
    }
    
}