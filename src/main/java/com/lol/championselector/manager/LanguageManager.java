package com.lol.championselector.manager;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LanguageManager {
    private static final Logger logger = LoggerFactory.getLogger(LanguageManager.class);
    private static final String DEFAULT_LANGUAGE_FILE = "language_preference.properties";
    
    private static LanguageManager instance;
    private ResourceBundle currentBundle;
    private final ObjectProperty<Locale> currentLocale = new SimpleObjectProperty<>();
    private final Map<String, Locale> supportedLocales;
    
    public enum Language {
        CHINESE("中文", Locale.CHINA),
        ENGLISH("English", Locale.US);
        
        private final String displayName;
        private final Locale locale;
        
        Language(String displayName, Locale locale) {
            this.displayName = displayName;
            this.locale = locale;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public Locale getLocale() {
            return locale;
        }
    }
    
    private LanguageManager() {
        supportedLocales = new HashMap<>();
        supportedLocales.put("中文", Locale.CHINA);
        supportedLocales.put("English", Locale.US);
        
        // Load saved language preference
        Locale savedLocale = loadLanguagePreference();
        if (savedLocale != null) {
            setLanguage(savedLocale);
        } else {
            // Default to Chinese
            setLanguage(Locale.CHINA);
        }
    }
    
    public static LanguageManager getInstance() {
        if (instance == null) {
            instance = new LanguageManager();
        }
        return instance;
    }
    
    public void setLanguage(Locale locale) {
        try {
            currentBundle = ResourceBundle.getBundle("messages", locale, new UTF8Control());
            currentLocale.set(locale);
            saveLanguagePreference(locale);
            logger.info("Language changed to: {}", locale);
        } catch (Exception e) {
            logger.error("Failed to load language bundle for locale: {}", locale, e);
            // Fallback to Chinese
            try {
                currentBundle = ResourceBundle.getBundle("messages", Locale.CHINA, new UTF8Control());
                currentLocale.set(Locale.CHINA);
            } catch (Exception ex) {
                logger.error("Failed to load fallback language bundle", ex);
            }
        }
    }
    
    public void setLanguage(Language language) {
        setLanguage(language.getLocale());
    }
    
    public String getString(String key) {
        try {
            return currentBundle.getString(key);
        } catch (MissingResourceException e) {
            logger.warn("Missing translation for key: {}", key);
            return key; // Return the key itself as fallback
        }
    }
    
    public Locale getCurrentLocale() {
        return currentLocale.get();
    }
    
    public ObjectProperty<Locale> currentLocaleProperty() {
        return currentLocale;
    }
    
    public Language getCurrentLanguage() {
        Locale locale = getCurrentLocale();
        for (Language lang : Language.values()) {
            if (lang.getLocale().equals(locale)) {
                return lang;
            }
        }
        return Language.CHINESE; // Default
    }
    
    public Map<String, Locale> getSupportedLocales() {
        return new HashMap<>(supportedLocales);
    }
    
    private void saveLanguagePreference(Locale locale) {
        Properties props = new Properties();
        props.setProperty("language", locale.getLanguage());
        props.setProperty("country", locale.getCountry());
        
        try (FileOutputStream out = new FileOutputStream(DEFAULT_LANGUAGE_FILE)) {
            props.store(out, "Language Preference");
            logger.info("Language preference saved: {}", locale);
        } catch (IOException e) {
            logger.error("Failed to save language preference", e);
        }
    }
    
    private Locale loadLanguagePreference() {
        File file = new File(DEFAULT_LANGUAGE_FILE);
        if (!file.exists()) {
            return null;
        }
        
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
            String language = props.getProperty("language");
            String country = props.getProperty("country");
            if (language != null && country != null) {
                Locale locale = Locale.forLanguageTag(language + "-" + country);
                logger.info("Loaded language preference: {}", locale);
                return locale;
            }
        } catch (IOException e) {
            logger.error("Failed to load language preference", e);
        }
        return null;
    }
    
    // Custom control to handle UTF-8 encoding for properties files
    private static class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                      ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            
            ResourceBundle bundle = null;
            InputStream stream = null;
            
            if (reload) {
                URL url = loader.getResource(resourceName);
                if (url != null) {
                    URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }
            
            if (stream != null) {
                try {
                    bundle = new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
                } finally {
                    stream.close();
                }
            }
            
            return bundle;
        }
    }
}