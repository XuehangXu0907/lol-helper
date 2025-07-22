package com.lol.championselector.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class TencentChampionApi {
    private static final Logger logger = LoggerFactory.getLogger(TencentChampionApi.class);
    private static final String BASE_URL = "https://game.gtimg.cn/images/lol/act/img/js";
    private static final String HERO_LIST_URL = BASE_URL + "/heroList/hero_list.js";
    private static final String HERO_DETAIL_URL = BASE_URL + "/hero/%d.js";
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public TencentChampionApi() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 获取英雄列表
     */
    public CompletableFuture<HeroListResponse> getHeroList() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url(HERO_LIST_URL)
                        .get()
                        .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("HTTP " + response.code() + ": " + response.message());
                    }
                    
                    String responseBody = response.body().string();
                    logger.debug("Hero list response: {}", responseBody.substring(0, Math.min(200, responseBody.length())));
                    
                    HeroListResponse heroList = objectMapper.readValue(responseBody, HeroListResponse.class);
                    logger.info("Successfully fetched {} heroes from Tencent API", heroList.getHero().size());
                    return heroList;
                }
            } catch (Exception e) {
                logger.error("Failed to fetch hero list from Tencent API", e);
                throw new RuntimeException("Failed to fetch hero list", e);
            }
        });
    }
    
    /**
     * 获取指定英雄的详细数据
     */
    public CompletableFuture<HeroDetailResponse> getHeroDetail(int heroId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = String.format(HERO_DETAIL_URL, heroId);
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("HTTP " + response.code() + ": " + response.message());
                    }
                    
                    String responseBody = response.body().string();
                    logger.debug("Hero detail response for {}: {}", heroId, responseBody.substring(0, Math.min(200, responseBody.length())));
                    
                    HeroDetailResponse heroDetail = objectMapper.readValue(responseBody, HeroDetailResponse.class);
                    logger.info("Successfully fetched details for hero ID: {}", heroId);
                    return heroDetail;
                }
            } catch (Exception e) {
                logger.error("Failed to fetch hero detail for ID: {}", heroId, e);
                throw new RuntimeException("Failed to fetch hero detail for ID: " + heroId, e);
            }
        });
    }
    
    /**
     * 英雄列表响应
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HeroListResponse {
        @JsonProperty("hero")
        private List<HeroInfo> hero;
        
        public List<HeroInfo> getHero() {
            return hero;
        }
        
        public void setHero(List<HeroInfo> hero) {
            this.hero = hero;
        }
    }
    
    /**
     * 英雄基本信息
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HeroInfo {
        @JsonProperty("heroId")
        private String heroId;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("alias")
        private String alias;
        
        @JsonProperty("title")
        private String title;
        
        @JsonProperty("roles")
        private List<String> roles;
        
        // Getters and setters
        public String getHeroId() {
            return heroId;
        }
        
        public void setHeroId(String heroId) {
            this.heroId = heroId;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getAlias() {
            return alias;
        }
        
        public void setAlias(String alias) {
            this.alias = alias;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public List<String> getRoles() {
            return roles;
        }
        
        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }
    
    /**
     * 英雄详细数据响应
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HeroDetailResponse {
        @JsonProperty("hero")
        private DetailedHeroInfo hero; // 单个对象，不是列表
        
        @JsonProperty("spells")
        private List<SpellInfo> spells;
        
        public DetailedHeroInfo getHero() {
            return hero;
        }
        
        public void setHero(DetailedHeroInfo hero) {
            this.hero = hero;
        }
        
        public List<SpellInfo> getSpells() {
            return spells;
        }
        
        public void setSpells(List<SpellInfo> spells) {
            this.spells = spells;
        }
    }
    
    /**
     * 详细英雄信息
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DetailedHeroInfo {
        @JsonProperty("heroId")
        private String heroId;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("alias")
        private String alias;
        
        @JsonProperty("title")
        private String title;
        
        @JsonProperty("shortBio")
        private String shortBio;
        
        // Getters and setters
        public String getHeroId() {
            return heroId;
        }
        
        public void setHeroId(String heroId) {
            this.heroId = heroId;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getAlias() {
            return alias;
        }
        
        public void setAlias(String alias) {
            this.alias = alias;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getShortBio() {
            return shortBio;
        }
        
        public void setShortBio(String shortBio) {
            this.shortBio = shortBio;
        }
    }
    
    /**
     * 技能信息
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SpellInfo {
        @JsonProperty("spellKey")
        private String spellKey;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("description")
        private String description;
        
        @JsonProperty("abilityVideoPath")
        private String abilityVideoPath;
        
        @JsonProperty("abilityIconPath")
        private String abilityIconPath;
        
        @JsonProperty("effectAmounts")
        private EffectAmounts effectAmounts;
        
        @JsonProperty("coefficients")
        private Coefficients coefficients;
        
        // Getters and setters
        public String getSpellKey() {
            return spellKey;
        }
        
        public void setSpellKey(String spellKey) {
            this.spellKey = spellKey;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getAbilityVideoPath() {
            return abilityVideoPath;
        }
        
        public void setAbilityVideoPath(String abilityVideoPath) {
            this.abilityVideoPath = abilityVideoPath;
        }
        
        public String getAbilityIconPath() {
            return abilityIconPath;
        }
        
        public void setAbilityIconPath(String abilityIconPath) {
            this.abilityIconPath = abilityIconPath;
        }
        
        public EffectAmounts getEffectAmounts() {
            return effectAmounts;
        }
        
        public void setEffectAmounts(EffectAmounts effectAmounts) {
            this.effectAmounts = effectAmounts;
        }
        
        public Coefficients getCoefficients() {
            return coefficients;
        }
        
        public void setCoefficients(Coefficients coefficients) {
            this.coefficients = coefficients;
        }
    }
    
    /**
     * 技能效果数值
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EffectAmounts {
        @JsonProperty("Effect1Amount")
        private List<Double> effect1Amount;
        
        @JsonProperty("Effect2Amount")
        private List<Double> effect2Amount;
        
        @JsonProperty("Effect3Amount")
        private List<Double> effect3Amount;
        
        @JsonProperty("Effect4Amount")
        private List<Double> effect4Amount;
        
        @JsonProperty("Effect5Amount")
        private List<Double> effect5Amount;
        
        public List<Double> getEffect1Amount() {
            return effect1Amount;
        }
        
        public void setEffect1Amount(List<Double> effect1Amount) {
            this.effect1Amount = effect1Amount;
        }
        
        public List<Double> getEffect2Amount() {
            return effect2Amount;
        }
        
        public void setEffect2Amount(List<Double> effect2Amount) {
            this.effect2Amount = effect2Amount;
        }
        
        public List<Double> getEffect3Amount() {
            return effect3Amount;
        }
        
        public void setEffect3Amount(List<Double> effect3Amount) {
            this.effect3Amount = effect3Amount;
        }
        
        public List<Double> getEffect4Amount() {
            return effect4Amount;
        }
        
        public void setEffect4Amount(List<Double> effect4Amount) {
            this.effect4Amount = effect4Amount;
        }
        
        public List<Double> getEffect5Amount() {
            return effect5Amount;
        }
        
        public void setEffect5Amount(List<Double> effect5Amount) {
            this.effect5Amount = effect5Amount;
        }
    }
    
    /**
     * 技能系数
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Coefficients {
        @JsonProperty("coefficient1")
        private Double coefficient1;
        
        @JsonProperty("coefficient2")
        private Double coefficient2;
        
        @JsonProperty("coefficient3")
        private Double coefficient3;
        
        @JsonProperty("coefficient4")
        private Double coefficient4;
        
        @JsonProperty("coefficient5")
        private Double coefficient5;
        
        public Double getCoefficient1() {
            return coefficient1;
        }
        
        public void setCoefficient1(Double coefficient1) {
            this.coefficient1 = coefficient1;
        }
        
        public Double getCoefficient2() {
            return coefficient2;
        }
        
        public void setCoefficient2(Double coefficient2) {
            this.coefficient2 = coefficient2;
        }
        
        public Double getCoefficient3() {
            return coefficient3;
        }
        
        public void setCoefficient3(Double coefficient3) {
            this.coefficient3 = coefficient3;
        }
        
        public Double getCoefficient4() {
            return coefficient4;
        }
        
        public void setCoefficient4(Double coefficient4) {
            this.coefficient4 = coefficient4;
        }
        
        public Double getCoefficient5() {
            return coefficient5;
        }
        
        public void setCoefficient5(Double coefficient5) {
            this.coefficient5 = coefficient5;
        }
    }
    
    /**
     * 关闭HTTP客户端
     */
    public void shutdown() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }
}