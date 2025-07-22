package com.lol.championselector.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WindowsAutoStartManager {
    private static final Logger logger = LoggerFactory.getLogger(WindowsAutoStartManager.class);
    
    private static final String REGISTRY_KEY = "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String APP_NAME = "LOLHelper";
    
    private final String applicationPath;
    private final String jarPath;
    
    public WindowsAutoStartManager() {
        this.applicationPath = getApplicationPath();
        this.jarPath = getJarPath();
        logger.debug("Application path: {}", applicationPath);
        logger.debug("JAR path: {}", jarPath);
    }
    
    private String getApplicationPath() {
        try {
            String classPath = System.getProperty("java.class.path");
            if (classPath.contains(".jar")) {
                return classPath.split(System.getProperty("path.separator"))[0];
            }
            
            String userDir = System.getProperty("user.dir");
            Path targetDir = Paths.get(userDir, "target");
            if (Files.exists(targetDir)) {
                try {
                    return Files.list(targetDir)
                            .filter(path -> path.getFileName().toString().endsWith(".jar"))
                            .filter(path -> !path.getFileName().toString().contains("original"))
                            .filter(path -> path.getFileName().toString().contains("lol-auto-ban-pick-tool"))
                            .findFirst()
                            .map(Path::toString)
                            .orElse(null);
                } catch (IOException e) {
                    logger.warn("Failed to list target directory", e);
                }
            }
            
            return Paths.get(userDir, "target", "lol-auto-ban-pick-tool-1.0.0-shaded.jar").toString();
        } catch (Exception e) {
            logger.error("Failed to determine application path", e);
            return null;
        }
    }
    
    private String getJarPath() {
        String userDir = System.getProperty("user.dir");
        
        // Try shaded JAR first (preferred for distribution)
        Path shadedJar = Paths.get(userDir, "target", "lol-auto-ban-pick-tool-1.0.0-shaded.jar");
        if (Files.exists(shadedJar)) {
            return shadedJar.toString();
        }
        
        // Try regular JAR
        Path regularJar = Paths.get(userDir, "target", "lol-auto-ban-pick-tool-1.0.0.jar");
        if (Files.exists(regularJar)) {
            return regularJar.toString();
        }
        
        // Try application path from class path
        if (applicationPath != null) {
            Path appPath = Paths.get(applicationPath);
            if (Files.exists(appPath)) {
                return appPath.toString();
            }
        }
        
        // Fallback to expected shaded JAR location
        return shadedJar.toString();
    }
    
    public boolean isAutoStartEnabled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("reg", "query", REGISTRY_KEY, "/v", APP_NAME);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(APP_NAME)) {
                        logger.debug("Auto-start is enabled");
                        return true;
                    }
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.debug("Registry query successful, but no entry found");
            } else {
                logger.debug("Registry query failed with exit code: {}", exitCode);
            }
            
        } catch (Exception e) {
            logger.warn("Failed to check auto-start status", e);
        }
        
        return false;
    }
    
    public boolean enableAutoStart() {
        if (jarPath == null || !Files.exists(Paths.get(jarPath))) {
            logger.error("Cannot enable auto-start: JAR file not found at {}", jarPath);
            return false;
        }
        
        try {
            String javaPath = getJavaExecutablePath();
            String command = String.format("\"%s\" -jar \"%s\" --minimized", javaPath, jarPath);
            
            ProcessBuilder pb = new ProcessBuilder("reg", "add", REGISTRY_KEY, "/v", APP_NAME, "/t", "REG_SZ", "/d", command, "/f");
            Process process = pb.start();
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.info("Auto-start enabled successfully");
                return true;
            } else {
                logger.error("Failed to enable auto-start, exit code: {}", exitCode);
                logProcessError(process);
            }
            
        } catch (Exception e) {
            logger.error("Failed to enable auto-start", e);
        }
        
        return false;
    }
    
    public boolean disableAutoStart() {
        try {
            ProcessBuilder pb = new ProcessBuilder("reg", "delete", REGISTRY_KEY, "/v", APP_NAME, "/f");
            Process process = pb.start();
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.info("Auto-start disabled successfully");
                return true;
            } else {
                logger.debug("Auto-start disable command exit code: {} (might not exist)", exitCode);
                return true;
            }
            
        } catch (Exception e) {
            logger.error("Failed to disable auto-start", e);
        }
        
        return false;
    }
    
    private String getJavaExecutablePath() {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            Path javaExe = Paths.get(javaHome, "bin", "java.exe");
            if (Files.exists(javaExe)) {
                return javaExe.toString();
            }
        }
        
        String javawExe = "javaw.exe";
        try {
            ProcessBuilder pb = new ProcessBuilder("where", javawExe);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    return line.trim();
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to find javaw.exe using 'where' command", e);
        }
        
        return "javaw";
    }
    
    private void logProcessError(Process process) {
        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = errorReader.readLine()) != null) {
                logger.error("Process error: {}", line);
            }
        } catch (IOException e) {
            logger.debug("Failed to read process error stream", e);
        }
    }
    
    public boolean isSupported() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("windows");
    }
    
    public String getRegistryCommand() {
        if (jarPath == null) {
            return "JAR file not found";
        }
        
        String javaPath = getJavaExecutablePath();
        return String.format("\"%s\" -jar \"%s\" --minimized", javaPath, jarPath);
    }
    
    public String getJarLocation() {
        return jarPath;
    }
}