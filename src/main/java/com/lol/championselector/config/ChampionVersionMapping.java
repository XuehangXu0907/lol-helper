package com.lol.championselector.config;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public class ChampionVersionMapping {
    private static final Map<String, String> VERSION_MAP;
    
    static {
        Map<String, String> map = new HashMap<>();
        map.put("Mel", "15.2.1");
        map.put("Yunara", "25.14.1");  // Added Yunara with correct version
        map.put("Ambessa", "14.24.1");
        map.put("Smolder", "14.24.1");
        map.put("Aurora", "14.24.1");
        map.put("Hwei", "14.1.1");
        VERSION_MAP = Collections.unmodifiableMap(map);
    }
    
    private static final String DEFAULT_VERSION = "25.1.1";  // Updated default version for newer champions
    
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