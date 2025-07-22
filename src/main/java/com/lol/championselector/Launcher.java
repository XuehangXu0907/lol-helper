package com.lol.championselector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LOL Helper Launcher
 * Responsible for configuring system properties and starting the main application
 */
public class Launcher {
    private static final Logger logger = LoggerFactory.getLogger(Launcher.class);
    
    public static void main(String[] args) {
        try {
            // Configure application name and identification
            configureSystemProperties();
            
            // Configure JavaFX related properties
            configureJavaFXProperties();
            
            // Configure logging and encoding
            configureLoggingAndEncoding();
            
            // Print startup information
            printStartupInfo();
            
            // Start main application
            logger.info("Starting LOL Helper main application...");
            ChampionSelectorApplication.main(args);
            
        } catch (Exception e) {
            logger.error("Error occurred while starting application", e);
            
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
        
        logger.debug("System properties configured");
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
        
        // JavaFX DPI settings
        System.setProperty("glass.win.uiScale", "100%");
        System.setProperty("glass.win.renderScale", "100%");
        
        // Prevent headless mode
        System.setProperty("java.awt.headless", "false");
        
        // AWT font rendering
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        
        logger.debug("JavaFX properties configured");
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
        
        logger.debug("Encoding and logging configured");
    }
    
    /**
     * Print startup information
     */
    private static void printStartupInfo() {
        logger.info("===========================================");
        logger.info("        LOL Helper v2.1.0 Starting");
        logger.info("===========================================");
        logger.info("Java version: {}", System.getProperty("java.version"));
        logger.info("Java vendor: {}", System.getProperty("java.vendor"));
        logger.info("Operating System: {} {}", 
                   System.getProperty("os.name"), 
                   System.getProperty("os.version"));
        logger.info("System architecture: {}", System.getProperty("os.arch"));
        logger.info("User directory: {}", System.getProperty("user.home"));
        logger.info("Working directory: {}", System.getProperty("user.dir"));
        logger.info("Character encoding: {}", System.getProperty("file.encoding"));
        
        // Check memory configuration
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        logger.info("Maximum memory: {} MB", maxMemory);
        
        logger.info("===========================================");
        
        // Check critical components availability
        checkCriticalComponents();
    }
    
    /**
     * Check critical components availability
     */
    private static void checkCriticalComponents() {
        logger.info("Checking critical components availability...");
        
        // Check JavaFX
        try {
            Class.forName("javafx.application.Application");
            logger.info("✓ JavaFX runtime available");
        } catch (ClassNotFoundException e) {
            logger.error("✗ JavaFX runtime unavailable", e);
            throw new RuntimeException("JavaFX runtime unavailable, please check Java environment configuration");
        }
        
        // Check network components
        try {
            Class.forName("okhttp3.OkHttpClient");
            logger.info("✓ HTTP client component available");
        } catch (ClassNotFoundException e) {
            logger.error("✗ HTTP client component unavailable", e);
            throw new RuntimeException("HTTP client component unavailable, please check dependency libraries");
        }
        
        // Check JSON processing components
        try {
            Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            logger.info("✓ JSON processing component available");
        } catch (ClassNotFoundException e) {
            logger.error("✗ JSON processing component unavailable", e);
            throw new RuntimeException("JSON processing component unavailable, please check dependency libraries");
        }
        
        // Check system tray support
        if (java.awt.SystemTray.isSupported()) {
            logger.info("✓ System tray functionality available");
        } else {
            logger.warn("⚠ System tray functionality unavailable, some features may be limited");
        }
        
        logger.info("Critical components check completed");
    }
}