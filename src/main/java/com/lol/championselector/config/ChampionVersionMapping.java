package com.lol.championselector.config;

import java.util.Map;

public class ChampionVersionMapping {
    private static final Map<String, String> VERSION_MAP = Map.of(
        "Mel", "15.2.1",
        "Ambessa", "14.24.1", 
        "Smolder", "14.24.1",
        "Aurora", "14.24.1",
        "Hwei", "14.1.1"
    );
    
    private static final String DEFAULT_VERSION = "14.24.1";
    
    public static String getVersion(String championKey) {
        return VERSION_MAP.getOrDefault(championKey, DEFAULT_VERSION);
    }
    
    public static String getDefaultVersion() {
        return DEFAULT_VERSION;
    }
    
    public static boolean hasCustomVersion(String championKey) {
        return VERSION_MAP.containsKey(championKey);
    }
}