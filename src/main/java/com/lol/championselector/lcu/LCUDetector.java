package com.lol.championselector.lcu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LCUDetector {
    private static final Logger logger = LoggerFactory.getLogger(LCUDetector.class);
    
    private static final String LEAGUE_PROCESS = "LeagueClientUx.exe";
    private static final Pattern PORT_PATTERN = Pattern.compile("--app-port=(\\d+)");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("--remoting-auth-token=([\\w-]+)");
    
    public static class LCUInfo {
        private final int port;
        private final String password;
        
        public LCUInfo(int port, String password) {
            this.port = port;
            this.password = password;
        }
        
        public int getPort() {
            return port;
        }
        
        public String getPassword() {
            return password;
        }
        
        @Override
        public String toString() {
            return "LCUInfo{port=" + port + ", password=" + password.substring(0, 8) + "...}";
        }
    }
    
    public static CompletableFuture<Optional<LCUInfo>> detectLCU() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Detecting League Client process...");
                
                // 使用wmic命令获取进程信息
                Process process = new ProcessBuilder(
                    "wmic", "process", "where", 
                    "name='" + LEAGUE_PROCESS + "'", 
                    "get", "commandline", "/format:value"
                ).start();
                
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), "GBK"))) {
                    
                    StringBuilder output = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                    
                    process.waitFor();
                    String commandLine = output.toString();
                    
                    if (commandLine.contains(LEAGUE_PROCESS)) {
                        return parseLCUInfo(commandLine);
                    } else {
                        logger.warn("League Client process not found");
                        return Optional.empty();
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to detect League Client", e);
                return Optional.empty();
            }
        });
    }
    
    private static Optional<LCUInfo> parseLCUInfo(String commandLine) {
        try {
            Matcher portMatcher = PORT_PATTERN.matcher(commandLine);
            Matcher passwordMatcher = PASSWORD_PATTERN.matcher(commandLine);
            
            if (portMatcher.find() && passwordMatcher.find()) {
                int port = Integer.parseInt(portMatcher.group(1));
                String password = passwordMatcher.group(1);
                
                LCUInfo info = new LCUInfo(port, password);
                logger.info("Detected LCU: {}", info);
                return Optional.of(info);
            } else {
                logger.warn("Could not parse LCU connection info from command line");
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("Failed to parse LCU info", e);
            return Optional.empty();
        }
    }
    
    public static CompletableFuture<Boolean> isLeagueClientRunning() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Process process = new ProcessBuilder(
                    "tasklist", "/FI", "IMAGENAME eq " + LEAGUE_PROCESS
                ).start();
                
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), "GBK"))) {
                    
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains(LEAGUE_PROCESS)) {
                            return true;
                        }
                    }
                    
                    process.waitFor();
                    return false;
                }
            } catch (Exception e) {
                logger.error("Failed to check if League Client is running", e);
                return false;
            }
        });
    }
}