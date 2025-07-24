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
    private Language currentLanguageMode = Language.CHINESE; // 跟踪当前语言模式
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
        Language savedLanguageMode = loadLanguagePreference();
        if (savedLanguageMode != null) {
            setLanguageMode(savedLanguageMode);
        } else {
            // Default to Chinese
            setLanguageMode(Language.CHINESE);
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
    
    /**
     * 设置语言模式（包括双语模式）
     */
    public void setLanguageMode(Language languageMode) {
        this.currentLanguageMode = languageMode;
        setLanguage(languageMode.getLocale());
        saveLanguagePreference(languageMode);
    }
    
    public String getString(String key) {
        try {
            return currentBundle.getString(key);
        } catch (MissingResourceException e) {
            logger.warn("Missing translation for key: {}", key);
            return key; // Return the key itself as fallback
        }
    }
    
    /**
     * 获取当前ResourceBundle供FXML使用
     */
    public ResourceBundle getResourceBundle() {
        return currentBundle;
    }
    
    
    public Locale getCurrentLocale() {
        return currentLocale.get();
    }
    
    public ObjectProperty<Locale> currentLocaleProperty() {
        return currentLocale;
    }
    
    public Language getCurrentLanguage() {
        return currentLanguageMode;
    }
    
    /**
     * 切换到下一个语言模式（在中文和英文之间切换）
     */
    public Language switchToNextLanguage() {
        Language nextLanguage = (currentLanguageMode == Language.CHINESE) ? 
            Language.ENGLISH : Language.CHINESE;
        
        setLanguageMode(nextLanguage);
        return nextLanguage;
    }
    
    public Map<String, Locale> getSupportedLocales() {
        return new HashMap<>(supportedLocales);
    }
    
    private void saveLanguagePreference(Locale locale) {
        // 保持现有方法以向后兼容
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
    
    /**
     * 保存语言模式偏好设置
     */
    private void saveLanguagePreference(Language languageMode) {
        Properties props = new Properties();
        props.setProperty("languageMode", languageMode.name());
        
        // 保存locale信息
        Locale locale = languageMode.getLocale();
        props.setProperty("language", locale.getLanguage());
        props.setProperty("country", locale.getCountry());
        
        try (FileOutputStream out = new FileOutputStream(DEFAULT_LANGUAGE_FILE)) {
            props.store(out, "Language Preference");
            logger.info("Language mode preference saved: {}", languageMode);
        } catch (IOException e) {
            logger.error("Failed to save language mode preference", e);
        }
    }
    
    /**
     * 加载语言模式偏好设置（新版本）
     */
    private Language loadLanguagePreference() {
        File file = new File(DEFAULT_LANGUAGE_FILE);
        if (!file.exists()) {
            return null;
        }
        
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
            
            // 优先尝试加载语言模式设置
            String languageMode = props.getProperty("languageMode");
            if (languageMode != null) {
                try {
                    Language lang = Language.valueOf(languageMode);
                    logger.info("Loaded language mode preference: {}", lang);
                    return lang;
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid language mode: {}, falling back to locale-based loading", languageMode);
                }
            }
            
            // 向后兼容：如果没有语言模式设置，尝试从locale加载
            String language = props.getProperty("language");
            String country = props.getProperty("country");
            if (language != null && country != null) {
                Locale locale = Locale.forLanguageTag(language + "-" + country);
                // 将locale转换为Language枚举
                for (Language lang : Language.values()) {
                    if (lang.getLocale() != null && lang.getLocale().equals(locale)) {
                        logger.info("Loaded language preference from locale: {}", lang);
                        return lang;
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to load language preference", e);
        }
        return null;
    }
    
    /**
     * 加载Locale偏好设置（保留用于向后兼容）
     */
    private Locale loadLocalePreference() {
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
                logger.info("Loaded locale preference: {}", locale);
                return locale;
            }
        } catch (IOException e) {
            logger.error("Failed to load locale preference", e);
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