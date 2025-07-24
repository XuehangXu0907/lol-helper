package com.lol.championselector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.InvocationTargetException;

/**
 * LOL Helper Launcher
 * Responsible for configuring system properties and starting the main application
 */
public class Launcher {
    private static Logger logger;
    
    // Static initializer with error handling
    static {
        try {
            logger = LoggerFactory.getLogger(Launcher.class);
        } catch (Exception e) {
            // Fallback to System.err if logger initialization fails
            System.err.println("Failed to initialize logger: " + e.getMessage());
            e.printStackTrace();
            logger = null;
        }
    }
    
    public static void main(String[] args) {
        try {
            // Early initialization check
            safeLog("Starting LOL Helper initialization...");
            
            // Configure application name and identification
            safeLog("Configuring system properties...");
            configureSystemProperties();
            
            // Configure JavaFX related properties
            safeLog("Configuring JavaFX properties...");
            configureJavaFXProperties();
            
            // Configure logging and encoding
            safeLog("Configuring logging and encoding...");
            configureLoggingAndEncoding();
            
            // Print startup information
            safeLog("Printing startup information...");
            printStartupInfo();
            
            // Start main application
            safeLog("Starting LOL Helper main application...");
            ChampionSelectorApplication.main(args);
            
        } catch (ExceptionInInitializerError e) {
            handleInitializerError(e);
        } catch (Exception e) {
            handleGeneralError(e);
            
            // Try to show error dialog if possible
            try {
                javax.swing.JOptionPane.showMessageDialog(
                    null,
                    "LOL Helper startup failed:\n" + e.getMessage() + 
                    "\n\nPlease check log files or contact technical support.",
                    "LOL Helper - Startup Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE
                );
            } catch (Exception ex) {
                // If even error dialog fails, output to console
                System.err.println("Startup failed: " + e.getMessage());
                e.printStackTrace();
            }
            
            System.exit(1);
        }
    }
    
    /**
     * Configure system properties
     */
    private static void configureSystemProperties() {
        // Application identification
        System.setProperty("java.awt.application.name", "LOL Helper");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "LOL Helper");
        
        // Network configuration
        System.setProperty("java.net.useSystemProxies", "true");
        
        // Security configuration
        System.setProperty("java.security.manager", "");
        
        safeLog("System properties configured");
    }
    
    /**
     * Configure JavaFX related properties
     */
    private static void configureJavaFXProperties() {
        // JavaFX performance optimization
        System.setProperty("javafx.animation.fullspeed", "true");
        System.setProperty("javafx.animation.pulse", "60");
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("prism.subpixeltext", "false");
        
        // JavaFX DPI settings - Use compatible numeric values
        // Removed problematic "auto" values that cause NumberFormatException
        System.setProperty("glass.win.minHiDPI", "1");
        System.setProperty("prism.allowhidpi", "true");
        
        // Enhanced font rendering
        System.setProperty("prism.text", "t2k");
        System.setProperty("prism.fontSizeLimit", "150");
        
        // Prevent headless mode
        System.setProperty("java.awt.headless", "false");
        
        // AWT font rendering - Enhanced settings (removed problematic auto values)
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        // Removed sun.java2d.win.uiScaleX/Y="auto" - causes NumberFormatException
        
        safeLog("JavaFX properties configured with enhanced DPI support");
    }
    
    /**
     * Configure logging and encoding
     */
    private static void configureLoggingAndEncoding() {
        // Character encoding settings
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("user.language", "zh");
        System.setProperty("user.country", "CN");
        System.setProperty("user.timezone", "Asia/Shanghai");
        
        // Console encoding
        System.setProperty("console.encoding", "UTF-8");
        System.setProperty("sun.stdout.encoding", "UTF-8");
        System.setProperty("sun.stderr.encoding", "UTF-8");
        
        safeLog("Encoding and logging configured");
    }
    
    /**
     * Print startup information
     */
    private static void printStartupInfo() {
        safeLog("===========================================");
        safeLog("        LOL Helper v2.2.4 Starting");
        safeLog("===========================================");
        safeLog("Java version: " + System.getProperty("java.version"));
        safeLog("Java vendor: " + System.getProperty("java.vendor"));
        safeLog("Operating System: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        safeLog("System architecture: " + System.getProperty("os.arch"));
        safeLog("User directory: " + System.getProperty("user.home"));
        safeLog("Working directory: " + System.getProperty("user.dir"));
        safeLog("Character encoding: " + System.getProperty("file.encoding"));
        
        // Check memory configuration
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        safeLog("Maximum memory: " + maxMemory + " MB");
        
        safeLog("===========================================");
        
        // Check critical components availability
        checkCriticalComponents();
    }
    
    /**
     * Check critical components availability with enhanced error handling
     */
    private static void checkCriticalComponents() {
        safeLog("Checking critical components availability...");
        
        // Track missing components for better error reporting
        java.util.List<String> missingComponents = new java.util.ArrayList<>();
        java.util.List<String> availableComponents = new java.util.ArrayList<>();
        
        // Check JavaFX with enhanced error handling
        try {
            Class.forName("javafx.application.Application");
            Class.forName("javafx.scene.Scene");
            Class.forName("javafx.stage.Stage");
            safeLog("✓ JavaFX runtime available");
            availableComponents.add("JavaFX");
        } catch (ClassNotFoundException e) {
            String error = "JavaFX runtime unavailable: " + e.getMessage();
            System.err.println("✗ " + error);
            missingComponents.add("JavaFX Runtime");
            safeLog("JavaFX missing - some features will be disabled");
        } catch (Exception e) {
            String error = "JavaFX runtime check failed: " + e.getMessage();
            System.err.println("✗ " + error);
            missingComponents.add("JavaFX Runtime (check failed)");
        }
        
        // Check network components with enhanced error handling
        try {
            Class.forName("okhttp3.OkHttpClient");
            Class.forName("okhttp3.Request");
            Class.forName("okhttp3.Response");
            // Test instantiation
            Class.forName("okhttp3.OkHttpClient").getDeclaredConstructor().newInstance();
            safeLog("✓ HTTP client component available");
            availableComponents.add("OkHttp");
        } catch (ClassNotFoundException e) {
            String error = "HTTP client component unavailable: " + e.getMessage();
            System.err.println("✗ " + error);
            missingComponents.add("OkHttp Library");
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            String error = "HTTP client component instantiation failed: " + e.getMessage();
            System.err.println("✗ " + error);
            missingComponents.add("OkHttp Library (instantiation failed)");
        } catch (Exception e) {
            String error = "HTTP client component check failed: " + e.getMessage();
            System.err.println("✗ " + error);
            missingComponents.add("OkHttp Library (check failed)");
        }
        
        // Check JSON processing components with enhanced error handling
        try {
            Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            Class.forName("com.fasterxml.jackson.databind.JsonNode");
            // Test instantiation
            Class.forName("com.fasterxml.jackson.databind.ObjectMapper").getDeclaredConstructor().newInstance();
            safeLog("✓ JSON processing component available");
            availableComponents.add("Jackson");
        } catch (ClassNotFoundException e) {
            String error = "JSON processing component unavailable: " + e.getMessage();
            System.err.println("✗ " + error);
            missingComponents.add("Jackson JSON Library");
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            String error = "JSON processing component instantiation failed: " + e.getMessage();
            System.err.println("✗ " + error);
            missingComponents.add("Jackson JSON Library (instantiation failed)");
        } catch (Exception e) {
            String error = "JSON processing component check failed: " + e.getMessage();
            System.err.println("✗ " + error);
            missingComponents.add("Jackson JSON Library (check failed)");
        }
        
        // Check caching components
        try {
            Class.forName("com.github.benmanes.caffeine.cache.Cache");
            safeLog("✓ Caffeine cache component available");
            availableComponents.add("Caffeine");
        } catch (ClassNotFoundException e) {
            System.err.println("⚠ Caffeine cache component unavailable: " + e.getMessage());
            safeLog("Caffeine cache missing - caching features will be limited");
        } catch (Exception e) {
            System.err.println("⚠ Caffeine cache component check failed: " + e.getMessage());
        }
        
        // Check logging components
        try {
            Class.forName("ch.qos.logback.classic.Logger");
            safeLog("✓ Logback logging component available");
            availableComponents.add("Logback");
        } catch (ClassNotFoundException e) {
            System.err.println("⚠ Logback logging component unavailable: " + e.getMessage());
            safeLog("Logback missing - using fallback logging");
        } catch (Exception e) {
            System.err.println("⚠ Logback logging component check failed: " + e.getMessage());
        }
        
        // Check system tray support
        try {
            if (java.awt.SystemTray.isSupported()) {
                safeLog("✓ System tray functionality available");
                availableComponents.add("System Tray");
            } else {
                System.out.println("⚠ System tray functionality unavailable, some features may be limited");
            }
        } catch (Exception e) {
            System.err.println("⚠ System tray check failed: " + e.getMessage());
        }
        
        // Summary report
        safeLog("Component availability summary:");
        safeLog("Available components (" + availableComponents.size() + "): " + String.join(", ", availableComponents));
        
        if (!missingComponents.isEmpty()) {
            System.err.println("Missing components (" + missingComponents.size() + "): " + String.join(", ", missingComponents));
            
            // Determine if we can continue
            boolean canContinue = !missingComponents.contains("JavaFX Runtime") && 
                                 !missingComponents.contains("OkHttp Library") && 
                                 !missingComponents.contains("Jackson JSON Library");
            
            if (!canContinue) {
                System.err.println("");
                System.err.println("Critical components are missing. Application cannot start.");
                System.err.println("Please ensure all required dependencies are included in the JAR file.");
                System.err.println("");
                System.err.println("Required components:");
                System.err.println("- JavaFX Runtime (for UI)");
                System.err.println("- OkHttp Library (for network communication)");
                System.err.println("- Jackson JSON Library (for data processing)");
                
                throw new RuntimeException("Critical dependencies missing: " + String.join(", ", missingComponents));
            } else {
                System.err.println("Some optional components are missing, but application can continue with limited functionality.");
            }
        }
        
        safeLog("Critical components check completed - Application can start");
    }
    
    /**
     * Safe logging method that handles logger initialization failures
     */
    private static void safeLog(String message) {
        if (logger != null) {
            logger.info(message);
        } else {
            System.out.println("[LAUNCHER] " + message);
        }
    }
    
    /**
     * Handle ExceptionInInitializerError with detailed diagnosis
     */
    private static void handleInitializerError(ExceptionInInitializerError e) {
        System.err.println("=== ExceptionInInitializerError Diagnosis ===");
        System.err.println("This error occurs during static initialization of a class.");
        System.err.println();
        
        Throwable cause = e.getCause();
        if (cause != null) {
            System.err.println("Root cause: " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
            System.err.println();
            System.err.println("Possible solutions:");
            
            if (cause instanceof ClassNotFoundException) {
                System.err.println("1. Missing dependency - check if all required JAR files are included");
                System.err.println("2. Classpath issue - verify the application classpath");
            } else if (cause.getMessage() != null && cause.getMessage().contains("logback")) {
                System.err.println("1. Logback configuration issue - check logback.xml file");
                System.err.println("2. SLF4J binding problem - verify SLF4J dependencies");
            } else if (cause.getMessage() != null && cause.getMessage().contains("JavaFX")) {
                System.err.println("1. JavaFX runtime missing - ensure JavaFX is available");
                System.err.println("2. JavaFX module path issue - check --module-path configuration");
            }
            
            System.err.println();
            System.err.println("Stack trace:");
            cause.printStackTrace();
        } else {
            System.err.println("No root cause available");
            e.printStackTrace();
        }
        
        showErrorDialog("Initialization Error", 
            "LOL Helper failed to initialize.\n\n" +
            "Error: " + (cause != null ? cause.getMessage() : e.getMessage()) + "\n\n" +
            "This is usually caused by:\n" +
            "• Missing Java dependencies\n" +
            "• JavaFX runtime issues\n" +
            "• Configuration file problems\n\n" +
            "Please check the console output for detailed information.");
        
        System.exit(1);
    }
    
    /**
     * Handle general errors during startup
     */
    private static void handleGeneralError(Exception e) {
        if (logger != null) {
            logger.error("Error occurred while starting application", e);
        } else {
            System.err.println("Error occurred while starting application: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Show error dialog with fallback to console output
     */
    private static void showErrorDialog(String title, String message) {
        try {
            javax.swing.JOptionPane.showMessageDialog(null, message, title, javax.swing.JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            System.err.println("Failed to show error dialog: " + ex.getMessage());
            System.err.println("Original error message: " + message);
        }
    }
}