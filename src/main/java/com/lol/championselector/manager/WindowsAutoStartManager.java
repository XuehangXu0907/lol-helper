package com.lol.championselector.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

public class WindowsAutoStartManager {
    private static final Logger logger = LoggerFactory.getLogger(WindowsAutoStartManager.class);
    
    private static final String REGISTRY_KEY = "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String APP_NAME = "LOLHelper";
    
    public static class PermissionDiagnostic {
        private final boolean hasPermission;
        private final String issue;
        private final String solution;
        private final PermissionType type;
        
        public enum PermissionType {
            REGISTRY_READ, REGISTRY_WRITE, FILE_ACCESS, JAVA_EXECUTABLE, SYSTEM_COMMAND
        }
        
        public PermissionDiagnostic(boolean hasPermission, String issue, String solution, PermissionType type) {
            this.hasPermission = hasPermission;
            this.issue = issue;
            this.solution = solution;
            this.type = type;
        }
        
        public boolean hasPermission() { return hasPermission; }
        public String getIssue() { return issue; }
        public String getSolution() { return solution; }
        public PermissionType getType() { return type; }
    }
    
    private final String applicationPath;
    private final String jarPath;
    private final String currentVersion;
    
    public WindowsAutoStartManager() {
        this.currentVersion = getCurrentVersion();
        this.applicationPath = getApplicationPath();
        this.jarPath = getJarPath();
        logger.debug("Current version: {}", currentVersion);
        logger.debug("Application path: {}", applicationPath);
        logger.debug("JAR path: {}", jarPath);
    }
    
    private String getCurrentVersion() {
        // Try to get version from package
        String version = getClass().getPackage().getImplementationVersion();
        if (version != null && !version.isEmpty()) {
            return version;
        }
        
        // Fallback to current version from pom.xml
        return "2.2.4";
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
                            .filter(path -> path.getFileName().toString().startsWith("lol-auto-ban-pick-tool"))
                            .findFirst()
                            .map(Path::toString)
                            .orElse(null);
                } catch (IOException e) {
                    logger.warn("Failed to list target directory", e);
                }
            }
            
            return Paths.get(userDir, "target", "lol-auto-ban-pick-tool-" + currentVersion + "-shaded.jar").toString();
        } catch (Exception e) {
            logger.error("Failed to determine application path", e);
            return null;
        }
    }
    
    private String getJarPath() {
        String userDir = System.getProperty("user.dir");
        
        // For installed applications, check if we're running from a JAR
        String classPath = System.getProperty("java.class.path");
        if (classPath.contains(".jar")) {
            // Split classpath and find first JAR file
            String[] paths = classPath.split(System.getProperty("path.separator"));
            for (String path : paths) {
                if (path.endsWith(".jar") && path.contains("lol-auto-ban-pick-tool")) {
                    Path jarPath = Paths.get(path);
                    if (Files.exists(jarPath)) {
                        return jarPath.toString();
                    }
                }
            }
        }
        
        // Try shaded JAR first (preferred for distribution)
        Path shadedJar = Paths.get(userDir, "target", "lol-auto-ban-pick-tool-" + currentVersion + "-shaded.jar");
        if (Files.exists(shadedJar)) {
            return shadedJar.toString();
        }
        
        // Try regular JAR
        Path regularJar = Paths.get(userDir, "target", "lol-auto-ban-pick-tool-" + currentVersion + ".jar");
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
        
        // Try to find any matching JAR in target directory
        try {
            Path targetDir = Paths.get(userDir, "target");
            if (Files.exists(targetDir)) {
                return Files.list(targetDir)
                        .filter(path -> path.getFileName().toString().endsWith(".jar"))
                        .filter(path -> !path.getFileName().toString().contains("original"))
                        .filter(path -> path.getFileName().toString().startsWith("lol-auto-ban-pick-tool"))
                        .findFirst()
                        .map(Path::toString)
                        .orElse(shadedJar.toString());
            }
        } catch (IOException e) {
            logger.warn("Failed to scan target directory for JAR files", e);
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
        // Pre-check permissions before attempting to enable
        List<PermissionDiagnostic> diagnostics = diagnosePermissions();
        for (PermissionDiagnostic diagnostic : diagnostics) {
            if (!diagnostic.hasPermission()) {
                logger.error("Cannot enable auto-start due to permission issue: {}", diagnostic.getIssue());
                logger.info("Suggested solution: {}", diagnostic.getSolution());
                return false;
            }
        }
        
        if (jarPath == null || !Files.exists(Paths.get(jarPath))) {
            logger.error("Cannot enable auto-start: JAR file not found at {}", jarPath);
            logger.info("To fix this issue: Build the application using 'mvn clean package'");
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
                logger.info("Try running the application as Administrator or check the permission diagnostic report");
            }
            
        } catch (Exception e) {
            logger.error("Failed to enable auto-start", e);
            logger.info("Use getPermissionReport() method to diagnose permission issues");
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
    
    /**
     * Performs comprehensive permission diagnostics for auto-start functionality
     * @return List of diagnostic results for different permission aspects
     */
    public List<PermissionDiagnostic> diagnosePermissions() {
        List<PermissionDiagnostic> diagnostics = new ArrayList<>();
        
        // Check registry read permissions
        diagnostics.add(checkRegistryReadPermission());
        
        // Check registry write permissions
        diagnostics.add(checkRegistryWritePermission());
        
        // Check JAR file access
        diagnostics.add(checkJarFileAccess());
        
        // Check Java executable access
        diagnostics.add(checkJavaExecutableAccess());
        
        // Check system command execution
        diagnostics.add(checkSystemCommandAccess());
        
        return diagnostics;
    }
    
    private PermissionDiagnostic checkRegistryReadPermission() {
        try {
            ProcessBuilder pb = new ProcessBuilder("reg", "query", REGISTRY_KEY);
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                return new PermissionDiagnostic(true, null, null, PermissionDiagnostic.PermissionType.REGISTRY_READ);
            } else {
                return new PermissionDiagnostic(false,
                    "Cannot read Windows registry key for auto-start entries",
                    "Run application as administrator or check if antivirus is blocking registry access",
                    PermissionDiagnostic.PermissionType.REGISTRY_READ);
            }
        } catch (Exception e) {
            return new PermissionDiagnostic(false,
                "Failed to execute registry read command: " + e.getMessage(),
                "Ensure Windows Command Prompt is available and check system PATH",
                PermissionDiagnostic.PermissionType.REGISTRY_READ);
        }
    }
    
    private PermissionDiagnostic checkRegistryWritePermission() {
        String testKey = REGISTRY_KEY;
        String testValue = "LOLHelper_PermissionTest";
        
        try {
            // Try to add a test entry
            ProcessBuilder addPb = new ProcessBuilder("reg", "add", testKey, "/v", testValue, "/t", "REG_SZ", "/d", "test", "/f");
            Process addProcess = addPb.start();
            int addExitCode = addProcess.waitFor();
            
            if (addExitCode == 0) {
                // Successfully added, now try to delete the test entry
                try {
                    ProcessBuilder delPb = new ProcessBuilder("reg", "delete", testKey, "/v", testValue, "/f");
                    Process delProcess = delPb.start();
                    delProcess.waitFor(); // Clean up, don't care about result
                } catch (Exception cleanupE) {
                    logger.debug("Failed to clean up test registry entry", cleanupE);
                }
                
                return new PermissionDiagnostic(true, null, null, PermissionDiagnostic.PermissionType.REGISTRY_WRITE);
            } else {
                return new PermissionDiagnostic(false,
                    "Cannot write to Windows registry for auto-start configuration",
                    "Run application as administrator or check User Account Control (UAC) settings",
                    PermissionDiagnostic.PermissionType.REGISTRY_WRITE);
            }
        } catch (Exception e) {
            return new PermissionDiagnostic(false,
                "Failed to test registry write permission: " + e.getMessage(),
                "Check if Windows Command Prompt has required permissions",
                PermissionDiagnostic.PermissionType.REGISTRY_WRITE);
        }
    }
    
    private PermissionDiagnostic checkJarFileAccess() {
        if (jarPath == null) {
            return new PermissionDiagnostic(false,
                "JAR file path is not determined",
                "Ensure application is properly built and JAR file exists in target directory",
                PermissionDiagnostic.PermissionType.FILE_ACCESS);
        }
        
        Path jarFile = Paths.get(jarPath);
        
        try {
            if (!Files.exists(jarFile)) {
                return new PermissionDiagnostic(false,
                    "JAR file does not exist at: " + jarPath,
                    "Build the application using 'mvn clean package' or check file location",
                    PermissionDiagnostic.PermissionType.FILE_ACCESS);
            }
            
            if (!Files.isReadable(jarFile)) {
                return new PermissionDiagnostic(false,
                    "JAR file is not readable: " + jarPath,
                    "Check file permissions and ensure no other process is locking the file",
                    PermissionDiagnostic.PermissionType.FILE_ACCESS);
            }
            
            // Try to read basic file attributes
            BasicFileAttributes attrs = Files.readAttributes(jarFile, BasicFileAttributes.class);
            if (attrs.size() == 0) {
                return new PermissionDiagnostic(false,
                    "JAR file appears to be empty: " + jarPath,
                    "Rebuild the application using 'mvn clean package'",
                    PermissionDiagnostic.PermissionType.FILE_ACCESS);
            }
            
            return new PermissionDiagnostic(true, null, null, PermissionDiagnostic.PermissionType.FILE_ACCESS);
            
        } catch (Exception e) {
            return new PermissionDiagnostic(false,
                "Cannot access JAR file: " + e.getMessage(),
                "Check file permissions and antivirus software settings",
                PermissionDiagnostic.PermissionType.FILE_ACCESS);
        }
    }
    
    private PermissionDiagnostic checkJavaExecutableAccess() {
        String javaPath = getJavaExecutablePath();
        
        try {
            Path javaExe = Paths.get(javaPath);
            
            if (javaPath.equals("javaw") || javaPath.equals("java")) {
                // Check if it's in PATH
                ProcessBuilder pb = new ProcessBuilder("where", javaPath);
                Process process = pb.start();
                int exitCode = process.waitFor();
                
                if (exitCode != 0) {
                    return new PermissionDiagnostic(false,
                        "Java executable not found in system PATH: " + javaPath,
                        "Add Java to system PATH or install Java runtime",
                        PermissionDiagnostic.PermissionType.JAVA_EXECUTABLE);
                }
            } else {
                // Check specific path
                if (!Files.exists(javaExe)) {
                    return new PermissionDiagnostic(false,
                        "Java executable not found at: " + javaPath,
                        "Reinstall Java or check JAVA_HOME environment variable",
                        PermissionDiagnostic.PermissionType.JAVA_EXECUTABLE);
                }
                
                if (!Files.isExecutable(javaExe)) {
                    return new PermissionDiagnostic(false,
                        "Java executable is not executable: " + javaPath,
                        "Check file permissions for Java installation directory",
                        PermissionDiagnostic.PermissionType.JAVA_EXECUTABLE);
                }
            }
            
            return new PermissionDiagnostic(true, null, null, PermissionDiagnostic.PermissionType.JAVA_EXECUTABLE);
            
        } catch (Exception e) {
            return new PermissionDiagnostic(false,
                "Failed to verify Java executable: " + e.getMessage(),
                "Check Java installation and system PATH configuration",
                PermissionDiagnostic.PermissionType.JAVA_EXECUTABLE);
        }
    }
    
    private PermissionDiagnostic checkSystemCommandAccess() {
        try {
            // Test basic command execution
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "echo", "test");
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                return new PermissionDiagnostic(true, null, null, PermissionDiagnostic.PermissionType.SYSTEM_COMMAND);
            } else {
                return new PermissionDiagnostic(false,
                    "Cannot execute system commands",
                    "Check if Command Prompt access is restricted by security policies",
                    PermissionDiagnostic.PermissionType.SYSTEM_COMMAND);
            }
        } catch (Exception e) {
            return new PermissionDiagnostic(false,
                "Failed to test system command execution: " + e.getMessage(),
                "Check system security settings and antivirus restrictions",
                PermissionDiagnostic.PermissionType.SYSTEM_COMMAND);
        }
    }
    
    /**
     * Gets a human-readable summary of permission issues and solutions
     * @return Formatted diagnostic report
     */
    public String getPermissionReport() {
        List<PermissionDiagnostic> diagnostics = diagnosePermissions();
        StringBuilder report = new StringBuilder();
        
        report.append("Auto-Start Permission Diagnostic Report\n");
        report.append("=====================================\n\n");
        
        boolean hasIssues = false;
        for (PermissionDiagnostic diagnostic : diagnostics) {
            if (!diagnostic.hasPermission()) {
                hasIssues = true;
                report.append("❌ ").append(diagnostic.getType()).append("\n");
                report.append("   Issue: ").append(diagnostic.getIssue()).append("\n");
                report.append("   Solution: ").append(diagnostic.getSolution()).append("\n\n");
            }
        }
        
        if (!hasIssues) {
            report.append("✅ All permissions are available for auto-start functionality.\n");
        } else {
            report.append("Common Solutions:\n");
            report.append("- Run the application as Administrator\n");
            report.append("- Check antivirus software settings\n");
            report.append("- Verify Java installation is complete\n");
            report.append("- Ensure the application JAR file was built successfully\n");
        }
        
        return report.toString();
    }
    
    /**
     * Attempts to fix common permission issues automatically
     * @return List of attempted fixes and their results
     */
    public List<String> attemptPermissionFixes() {
        List<String> fixResults = new ArrayList<>();
        List<PermissionDiagnostic> diagnostics = diagnosePermissions();
        
        for (PermissionDiagnostic diagnostic : diagnostics) {
            if (!diagnostic.hasPermission()) {
                switch (diagnostic.getType()) {
                    case FILE_ACCESS:
                        String fileFixResult = attemptFileAccessFix();
                        fixResults.add("File Access Fix: " + fileFixResult);
                        break;
                    case JAVA_EXECUTABLE:
                        String javaFixResult = attemptJavaExecutableFix();
                        fixResults.add("Java Executable Fix: " + javaFixResult);
                        break;
                    case REGISTRY_READ:
                    case REGISTRY_WRITE:
                        fixResults.add("Registry Permission Fix: Please run application as Administrator");
                        break;
                    case SYSTEM_COMMAND:
                        fixResults.add("System Command Fix: Check antivirus settings and system policies");
                        break;
                }
            }
        }
        
        if (fixResults.isEmpty()) {
            fixResults.add("No permission issues detected - auto-start should work properly");
        }
        
        return fixResults;
    }
    
    private String attemptFileAccessFix() {
        if (jarPath == null) {
            return "Cannot fix - JAR path not determined. Try rebuilding: mvn clean package";
        }
        
        Path jarFile = Paths.get(jarPath);
        
        try {
            if (!Files.exists(jarFile)) {
                // Check if we can find the JAR in target directory
                Path userDir = Paths.get(System.getProperty("user.dir"));
                Path targetDir = userDir.resolve("target");
                
                if (Files.exists(targetDir)) {
                    return "JAR file not found. Please run: mvn clean package";
                } else {
                    return "Project not built. Please run: mvn clean package";
                }
            }
            
            if (!Files.isReadable(jarFile)) {
                return "JAR file exists but is not readable. Check file permissions or restart the application.";
            }
            
            return "File access appears to be working now";
            
        } catch (Exception e) {
            return "Failed to check file access: " + e.getMessage();
        }
    }
    
    private String attemptJavaExecutableFix() {
        String javaPath = getJavaExecutablePath();
        
        try {
            // Try to find Java in common locations
            String[] commonJavaPaths = {
                System.getProperty("java.home") + "\\bin\\java.exe",
                System.getProperty("java.home") + "\\bin\\javaw.exe",
                "C:\\Program Files\\Java\\*\\bin\\java.exe",
                "C:\\Program Files (x86)\\Java\\*\\bin\\java.exe"
            };
            
            for (String path : commonJavaPaths) {
                if (path.contains("*")) {
                    // Skip wildcard paths for now
                    continue;
                }
                
                Path javePath = Paths.get(path);
                if (Files.exists(javePath) && Files.isExecutable(javePath)) {
                    return "Java executable found at: " + path;
                }
            }
            
            return "Java executable not found. Please ensure Java is installed and JAVA_HOME is set correctly.";
            
        } catch (Exception e) {
            return "Failed to locate Java executable: " + e.getMessage();
        }
    }
    
    /**
     * Provides step-by-step manual fix instructions
     * @return Detailed instructions for manually fixing permission issues
     */
    public String getManualFixInstructions() {
        StringBuilder instructions = new StringBuilder();
        List<PermissionDiagnostic> diagnostics = diagnosePermissions();
        
        instructions.append("Manual Fix Instructions for Auto-Start Issues\n");
        instructions.append("==============================================\n\n");
        
        boolean hasIssues = false;
        for (PermissionDiagnostic diagnostic : diagnostics) {
            if (!diagnostic.hasPermission()) {
                hasIssues = true;
                instructions.append(getDetailedFixInstructions(diagnostic));
                instructions.append("\n");
            }
        }
        
        if (!hasIssues) {
            instructions.append("✅ No issues detected. Auto-start functionality should work properly.\n");
        } else {
            instructions.append("General Tips:\n");
            instructions.append("• Right-click on the application and select 'Run as administrator'\n");
            instructions.append("• Temporarily disable antivirus software while setting up auto-start\n");
            instructions.append("• Make sure Windows Defender isn't blocking the application\n");
            instructions.append("• Check Windows User Account Control (UAC) settings\n");
        }
        
        return instructions.toString();
    }
    
    private String getDetailedFixInstructions(PermissionDiagnostic diagnostic) {
        StringBuilder fix = new StringBuilder();
        
        switch (diagnostic.getType()) {
            case REGISTRY_READ:
                fix.append("🔑 Registry Read Permission Issue:\n");
                fix.append("   1. Right-click on the application and select 'Run as administrator'\n");
                fix.append("   2. Check if antivirus software is blocking registry access\n");
                fix.append("   3. Verify Windows User Account Control (UAC) settings\n");
                break;
                
            case REGISTRY_WRITE:
                fix.append("🔑 Registry Write Permission Issue:\n");
                fix.append("   1. Run the application as Administrator:\n");
                fix.append("      • Right-click on the application executable\n");
                fix.append("      • Select 'Run as administrator'\n");
                fix.append("   2. If using a shortcut, modify it to always run as admin:\n");
                fix.append("      • Right-click shortcut → Properties → Advanced\n");
                fix.append("      • Check 'Run as administrator'\n");
                fix.append("   3. Check Windows UAC settings in Control Panel\n");
                break;
                
            case FILE_ACCESS:
                fix.append("📁 File Access Issue:\n");
                fix.append("   1. Rebuild the application: mvn clean package\n");
                fix.append("   2. Check if antivirus is quarantining the JAR file\n");
                fix.append("   3. Verify file permissions on the target directory\n");
                fix.append("   4. Make sure no other process is using the JAR file\n");
                break;
                
            case JAVA_EXECUTABLE:
                fix.append("☕ Java Executable Issue:\n");
                fix.append("   1. Verify Java installation:\n");
                fix.append("      • Open Command Prompt and type: java -version\n");
                fix.append("   2. Check JAVA_HOME environment variable:\n");
                fix.append("      • Control Panel → System → Advanced → Environment Variables\n");
                fix.append("   3. Add Java to system PATH if needed\n");
                fix.append("   4. Consider reinstalling Java if issues persist\n");
                break;
                
            case SYSTEM_COMMAND:
                fix.append("💻 System Command Issue:\n");
                fix.append("   1. Check if Command Prompt access is restricted\n");
                fix.append("   2. Verify antivirus software isn't blocking system commands\n");
                fix.append("   3. Check Windows security policies\n");
                fix.append("   4. Try running from a Command Prompt opened as Administrator\n");
                break;
        }
        
        return fix.toString();
    }
}