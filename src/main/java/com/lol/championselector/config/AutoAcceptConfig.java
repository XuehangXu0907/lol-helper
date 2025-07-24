package com.lol.championselector.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.championselector.model.Champion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AutoAcceptConfig {
    private static final Logger logger = LoggerFactory.getLogger(AutoAcceptConfig.class);
    private static final String CONFIG_FILE = "auto-accept-config.json";
    
    private boolean autoAcceptEnabled = true;
    private boolean autoDeclineEnabled = false;
    private int checkIntervalMs = 1000;
    
    private ChampionSelectConfig championSelect = new ChampionSelectConfig();
    
    // System Tray and Auto Start settings
    private boolean systemTrayEnabled = true;
    private boolean autoStartEnabled = false;
    
    // Auto connect settings
    private boolean autoConnectEnabled = true; // 启动时自动连接
    private boolean autoReconnectEnabled = true; // 断线自动重连
    private int reconnectIntervalSeconds = 10; // 重连检测间隔
    
    // Window close behavior settings
    private boolean minimizeOnClose = true; // 关闭窗口时最小化到托盘
    private boolean confirmOnExit = false; // 退出时显示确认对话框
    
    // Popup suppression settings
    private boolean suppressReadyCheckPopup = false; // 抑制准备检查弹窗
    private boolean suppressBanPhasePopup = false; // 抑制Ban阶段弹窗
    private boolean suppressPickPhasePopup = false; // 抑制Pick阶段弹窗
    
    // System tray menu settings
    private boolean forceEnglishTrayMenu = false; // 强制使用英文托盘菜单
    
    public static class ChampionSelectConfig {
        private boolean autoBanEnabled = false;
        private boolean autoPickEnabled = false;
        private ChampionInfo banChampion;
        private ChampionInfo pickChampion;
        
        // 新增：智能时机控制
        private boolean smartTimingEnabled = true; // 启用智能时机控制
        private int banExecutionDelaySeconds = 3; // Ban阶段剩余秒数时执行
        private int pickExecutionDelaySeconds = 2; // Pick阶段剩余秒数时执行
        private boolean enableHover = true; // 启用hover预选
        
        // 新增：自动预选功能
        private boolean autoHoverEnabled = false; // 启用自动预选（进入选人界面时立即亮英雄）
        
        // 新增：智能禁用功能
        private boolean smartBanEnabled = true; // 启用智能禁用（避免禁用队友预选的英雄）
        
        // 新增：简单延迟执行选项
        private boolean useSimpleDelayBan = true; // 使用简单延迟执行ban（默认启用）
        private int simpleBanDelaySeconds = 25; // 简单延迟执行时间（默认25秒）
        private boolean useSimpleDelayPick = false; // 使用简单延迟执行pick（默认禁用）
        private int simplePickDelaySeconds = 25; // 简单延迟执行时间（默认25秒）
        
        // 新增：分路配置
        private boolean usePositionBasedSelection = false; // 启用基于分路的选择
        private Map<String, PositionConfig> positionConfigs = new HashMap<>();
        
        public ChampionSelectConfig() {
            // 默认Ban英雄：艾克
            this.banChampion = new ChampionInfo("Ekko", "艾克", "时间刺客");
            // 默认Pick英雄：金克丝
            this.pickChampion = new ChampionInfo("Jinx", "金克丝", "暴走萝莉");
            
            // 初始化默认分路配置
            initializeDefaultPositionConfigs();
        }
        
        private void initializeDefaultPositionConfigs() {
            // Global configuration - applies to all positions
            PositionConfig globalConfig = new PositionConfig("global");
            globalConfig.addBanChampion(new ChampionInfo("Yasuo", "亚索", "疾风剑豪"));
            globalConfig.addBanChampion(new ChampionInfo("Zed", "劫", "影流之主"));
            globalConfig.addBanChampion(new ChampionInfo("Katarina", "卡特琳娜", "不祥之刃"));
            globalConfig.addBanChampion(new ChampionInfo("Akali", "阿卡丽", "离群之刺"));
            globalConfig.addBanChampion(new ChampionInfo("Darius", "德莱厄斯", "诺克萨斯之手"));
            globalConfig.addPickChampion(new ChampionInfo("Garen", "盖伦", "德玛西亚之力"));
            globalConfig.addPickChampion(new ChampionInfo("Annie", "安妮", "黑暗之女"));
            globalConfig.addPickChampion(new ChampionInfo("Ashe", "艾希", "寒冰射手"));
            globalConfig.addPickChampion(new ChampionInfo("Soraka", "索拉卡", "众星之子"));
            globalConfig.addPickChampion(new ChampionInfo("Warwick", "沃里克", "祖安怒兽"));
            positionConfigs.put("global", globalConfig);
            
            // Top lane
            PositionConfig topConfig = new PositionConfig("top");
            topConfig.addBanChampion(new ChampionInfo("Darius", "德莱厄斯", "诺克萨斯之手"));
            topConfig.addBanChampion(new ChampionInfo("Garen", "盖伦", "德玛西亚之力"));
            topConfig.addBanChampion(new ChampionInfo("Fiora", "菲奥娜", "无双剑姬"));
            topConfig.addBanChampion(new ChampionInfo("Camille", "卡密尔", "青钢影"));
            topConfig.addBanChampion(new ChampionInfo("Riven", "锐雯", "放逐之刃"));
            topConfig.addPickChampion(new ChampionInfo("Garen", "盖伦", "德玛西亚之力"));
            topConfig.addPickChampion(new ChampionInfo("Malphite", "墨菲特", "熔岩巨兽"));
            topConfig.addPickChampion(new ChampionInfo("Ornn", "奥恩", "山隐之焰"));
            topConfig.addPickChampion(new ChampionInfo("Shen", "慎", "暮光之眼"));
            topConfig.addPickChampion(new ChampionInfo("Maokai", "茂凯", "扭曲树精"));
            positionConfigs.put("top", topConfig);
            
            // Jungle
            PositionConfig jungleConfig = new PositionConfig("jungle");
            jungleConfig.addBanChampion(new ChampionInfo("Graves", "格雷福斯", "法外狂徒"));
            jungleConfig.addBanChampion(new ChampionInfo("Ekko", "艾克", "时间刺客"));
            jungleConfig.addBanChampion(new ChampionInfo("Kindred", "千珏", "永猎双子"));
            jungleConfig.addBanChampion(new ChampionInfo("Lillia", "莉莉娅", "含羞蓓蕾"));
            jungleConfig.addBanChampion(new ChampionInfo("Hecarim", "赫卡里姆", "战争之影"));
            jungleConfig.addPickChampion(new ChampionInfo("Graves", "格雷福斯", "法外狂徒"));
            jungleConfig.addPickChampion(new ChampionInfo("Warwick", "沃里克", "祖安怒兽"));
            jungleConfig.addPickChampion(new ChampionInfo("Sejuani", "瑟庄妮", "北地之怒"));
            jungleConfig.addPickChampion(new ChampionInfo("JarvanIV", "嘉文四世", "德玛西亚皇子"));
            jungleConfig.addPickChampion(new ChampionInfo("Ammu", "阿木木", "殇之木乃伊"));
            positionConfigs.put("jungle", jungleConfig);
            
            // Middle
            PositionConfig midConfig = new PositionConfig("middle");
            midConfig.addBanChampion(new ChampionInfo("Yasuo", "亚索", "疾风剑豪"));
            midConfig.addBanChampion(new ChampionInfo("Zed", "劫", "影流之主"));
            midConfig.addBanChampion(new ChampionInfo("Katarina", "卡特琳娜", "不祥之刃"));
            midConfig.addBanChampion(new ChampionInfo("Akali", "阿卡丽", "离群之刺"));
            midConfig.addBanChampion(new ChampionInfo("Yone", "永恩", "封尘绝念"));
            midConfig.addPickChampion(new ChampionInfo("Annie", "安妮", "黑暗之女"));
            midConfig.addPickChampion(new ChampionInfo("Malzahar", "玛尔扎哈", "虚空先知"));
            midConfig.addPickChampion(new ChampionInfo("Orianna", "奥莉安娜", "发条魔灵"));
            midConfig.addPickChampion(new ChampionInfo("Lux", "拉克丝", "光辉女郎"));
            midConfig.addPickChampion(new ChampionInfo("Viktor", "维克托", "机械先驱"));
            positionConfigs.put("middle", midConfig);
            
            // Bottom ADC
            PositionConfig botConfig = new PositionConfig("bottom");
            botConfig.addBanChampion(new ChampionInfo("Draven", "德莱文", "荣耀行刑官"));
            botConfig.addBanChampion(new ChampionInfo("Vayne", "薇恩", "暗夜猎手"));
            botConfig.addBanChampion(new ChampionInfo("Jinx", "金克丝", "暴走萝莉"));
            botConfig.addBanChampion(new ChampionInfo("Caitlyn", "凯特琳", "皮城女警"));
            botConfig.addBanChampion(new ChampionInfo("Kaisa", "卡莎", "虚空之女"));
            botConfig.addPickChampion(new ChampionInfo("Jinx", "金克丝", "暴走萝莉"));
            botConfig.addPickChampion(new ChampionInfo("Ashe", "艾希", "寒冰射手"));
            botConfig.addPickChampion(new ChampionInfo("Ezreal", "伊泽瑞尔", "探险家"));
            botConfig.addPickChampion(new ChampionInfo("MissFortune", "厄运小姐", "赏金猎人"));
            botConfig.addPickChampion(new ChampionInfo("Sivir", "希维尔", "战争女神"));
            positionConfigs.put("bottom", botConfig);
            
            // Support
            PositionConfig supportConfig = new PositionConfig("utility");
            supportConfig.addBanChampion(new ChampionInfo("Thresh", "锤石", "魂锁典狱长"));
            supportConfig.addBanChampion(new ChampionInfo("Blitzcrank", "布里茨", "蒸汽机器人"));
            supportConfig.addBanChampion(new ChampionInfo("Pyke", "派克", "血港鬼影"));
            supportConfig.addBanChampion(new ChampionInfo("Leona", "蕾欧娜", "曙光女神"));
            supportConfig.addBanChampion(new ChampionInfo("Nautilus", "诺提勒斯", "深海泰坦"));
            supportConfig.addPickChampion(new ChampionInfo("Soraka", "索拉卡", "众星之子"));
            supportConfig.addPickChampion(new ChampionInfo("Janna", "迦娜", "风暴之怒"));
            supportConfig.addPickChampion(new ChampionInfo("Lulu", "璐璐", "仙灵女巫"));
            supportConfig.addPickChampion(new ChampionInfo("Braum", "布隆", "弗雷尔卓德之心"));
            supportConfig.addPickChampion(new ChampionInfo("Alistar", "阿利斯塔", "牛头酋长"));
            positionConfigs.put("utility", supportConfig);
        }
        
        // Getters and Setters
        public boolean isAutoBanEnabled() { return autoBanEnabled; }
        public void setAutoBanEnabled(boolean autoBanEnabled) { this.autoBanEnabled = autoBanEnabled; }
        
        public boolean isAutoPickEnabled() { return autoPickEnabled; }
        public void setAutoPickEnabled(boolean autoPickEnabled) { this.autoPickEnabled = autoPickEnabled; }
        
        public ChampionInfo getBanChampion() { return banChampion; }
        public void setBanChampion(ChampionInfo banChampion) { this.banChampion = banChampion; }
        
        public ChampionInfo getPickChampion() { return pickChampion; }
        public void setPickChampion(ChampionInfo pickChampion) { this.pickChampion = pickChampion; }
        
        // 智能时机控制相关
        public boolean isSmartTimingEnabled() { return smartTimingEnabled; }
        public void setSmartTimingEnabled(boolean smartTimingEnabled) { this.smartTimingEnabled = smartTimingEnabled; }
        
        public int getBanExecutionDelaySeconds() { return banExecutionDelaySeconds; }
        public void setBanExecutionDelaySeconds(int banExecutionDelaySeconds) { 
            this.banExecutionDelaySeconds = Math.max(1, Math.min(10, banExecutionDelaySeconds)); 
        }
        
        public int getPickExecutionDelaySeconds() { return pickExecutionDelaySeconds; }
        public void setPickExecutionDelaySeconds(int pickExecutionDelaySeconds) { 
            this.pickExecutionDelaySeconds = Math.max(1, Math.min(10, pickExecutionDelaySeconds)); 
        }
        
        public boolean isEnableHover() { return enableHover; }
        public void setEnableHover(boolean enableHover) { this.enableHover = enableHover; }
        
        public boolean isUseSimpleDelayBan() { return useSimpleDelayBan; }
        public void setUseSimpleDelayBan(boolean useSimpleDelayBan) { this.useSimpleDelayBan = useSimpleDelayBan; }
        
        public int getSimpleBanDelaySeconds() { return simpleBanDelaySeconds; }
        public void setSimpleBanDelaySeconds(int simpleBanDelaySeconds) { 
            this.simpleBanDelaySeconds = Math.max(1, Math.min(60, simpleBanDelaySeconds)); 
        }
        
        public boolean isUseSimpleDelayPick() { return useSimpleDelayPick; }
        public void setUseSimpleDelayPick(boolean useSimpleDelayPick) { this.useSimpleDelayPick = useSimpleDelayPick; }
        
        public int getSimplePickDelaySeconds() { return simplePickDelaySeconds; }
        public void setSimplePickDelaySeconds(int simplePickDelaySeconds) { 
            this.simplePickDelaySeconds = Math.max(1, Math.min(60, simplePickDelaySeconds)); 
        }
        
        // 自动预选功能相关
        public boolean isAutoHoverEnabled() { return autoHoverEnabled; }
        public void setAutoHoverEnabled(boolean autoHoverEnabled) { this.autoHoverEnabled = autoHoverEnabled; }
        
        // 智能禁用功能相关
        public boolean isSmartBanEnabled() { return smartBanEnabled; }
        public void setSmartBanEnabled(boolean smartBanEnabled) { this.smartBanEnabled = smartBanEnabled; }
        
        // 分路配置相关
        public boolean isUsePositionBasedSelection() { return usePositionBasedSelection; }
        public void setUsePositionBasedSelection(boolean usePositionBasedSelection) { this.usePositionBasedSelection = usePositionBasedSelection; }
        
        public Map<String, PositionConfig> getPositionConfigs() { return positionConfigs; }
        public void setPositionConfigs(Map<String, PositionConfig> positionConfigs) { this.positionConfigs = positionConfigs; }
        
        public PositionConfig getPositionConfig(String position) { 
            return positionConfigs.getOrDefault(position, null); 
        }
    }
    
    /**
     * 分路配置类
     */
    public static class PositionConfig {
        private String position;
        private List<ChampionInfo> banChampions = new ArrayList<>();
        private List<ChampionInfo> pickChampions = new ArrayList<>();
        private ChampionInfo preferredBanChampion;
        private ChampionInfo preferredPickChampion;
        
        public PositionConfig() {}
        
        public PositionConfig(String position) {
            this.position = position;
        }
        
        public void addBanChampion(ChampionInfo champion) {
            banChampions.add(champion);
        }
        
        public void addPickChampion(ChampionInfo champion) {
            pickChampions.add(champion);
        }
        
        public ChampionInfo getPreferredBanChampion() {
            // 优先返回明确设置的首选英雄
            if (preferredBanChampion != null) {
                return preferredBanChampion;
            }
            // 否则返回列表中的第一个
            return banChampions.isEmpty() ? null : banChampions.get(0);
        }
        
        public ChampionInfo getPreferredPickChampion() {
            // 优先返回明确设置的首选英雄
            if (preferredPickChampion != null) {
                return preferredPickChampion;
            }
            // 否则返回列表中的第一个
            return pickChampions.isEmpty() ? null : pickChampions.get(0);
        }
        
        public ChampionInfo getAlternateBanChampion(Set<Integer> bannedChampionIds) {
            for (ChampionInfo champion : banChampions) {
                if (champion.getChampionId() != null && 
                    !bannedChampionIds.contains(champion.getChampionId())) {
                    return champion;
                }
            }
            return null;
        }
        
        public ChampionInfo getAlternatePickChampion(Set<Integer> bannedChampionIds, Set<Integer> pickedChampionIds) {
            for (ChampionInfo champion : pickChampions) {
                if (champion.getChampionId() != null && 
                    !bannedChampionIds.contains(champion.getChampionId()) &&
                    !pickedChampionIds.contains(champion.getChampionId())) {
                    return champion;
                }
            }
            return null;
        }
        
        // Getters and Setters
        public String getPosition() { return position; }
        public void setPosition(String position) { this.position = position; }
        
        public List<ChampionInfo> getBanChampions() { return banChampions; }
        public void setBanChampions(List<ChampionInfo> banChampions) { this.banChampions = banChampions; }
        
        public List<ChampionInfo> getPickChampions() { return pickChampions; }
        public void setPickChampions(List<ChampionInfo> pickChampions) { this.pickChampions = pickChampions; }
        
        public void setPreferredBanChampion(ChampionInfo preferredBanChampion) { this.preferredBanChampion = preferredBanChampion; }
        public void setPreferredPickChampion(ChampionInfo preferredPickChampion) { this.preferredPickChampion = preferredPickChampion; }
    }
    
    public static class ChampionInfo {
        private String key;
        private String nameCn;
        private String nameEn;
        private String title;
        private Integer championId;
        
        public ChampionInfo() {}
        
        public ChampionInfo(String key, String nameCn, String title) {
            this.key = key;
            this.nameCn = nameCn;
            this.nameEn = key; // 默认使用key作为英文名
            this.title = title;
            // 尝试从ChampionDataManager获取championId
            this.championId = getChampionIdByKey(key);
        }
        
        public ChampionInfo(Champion champion) {
            this.key = champion.getKey();
            this.nameCn = champion.getNameCn();
            this.nameEn = champion.getNameEn();
            this.title = champion.getTitle();
            
            // 安全地解析 championId
            try {
                if (champion.getId() != null && !champion.getId().trim().isEmpty()) {
                    this.championId = Integer.parseInt(champion.getId().trim());
                } else {
                    logger.warn("Champion {} has null or empty ID", champion.getKey());
                    this.championId = null;
                }
            } catch (NumberFormatException e) {
                logger.error("Failed to parse champion ID for {}: {}", champion.getKey(), champion.getId(), e);
                this.championId = null;
            }
        }
        
        // Getters and Setters
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        
        public String getNameCn() { return nameCn; }
        public void setNameCn(String nameCn) { this.nameCn = nameCn; }
        
        public String getNameEn() { return nameEn; }
        public void setNameEn(String nameEn) { this.nameEn = nameEn; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public Integer getChampionId() { return championId; }
        public void setChampionId(Integer championId) { this.championId = championId; }
        
        /**
         * 根据英雄key获取championId的工具方法
         */
        private static Integer getChampionIdByKey(String key) {
            if (key == null || key.trim().isEmpty()) {
                return null;
            }
            
            // 简化的ID映射表，基于常见英雄
            Map<String, Integer> keyToIdMap = new HashMap<>();
            keyToIdMap.put("Aatrox", 266);
            keyToIdMap.put("Ahri", 103);
            keyToIdMap.put("Akali", 84);
            keyToIdMap.put("Alistar", 12);
            keyToIdMap.put("Ammu", 32);
            keyToIdMap.put("Anivia", 34);
            keyToIdMap.put("Annie", 1);
            keyToIdMap.put("Aphelios", 523);
            keyToIdMap.put("Ashe", 22);
            keyToIdMap.put("AurelionSol", 136);
            keyToIdMap.put("Azir", 268);
            keyToIdMap.put("Bard", 432);
            keyToIdMap.put("Blitzcrank", 53);
            keyToIdMap.put("Brand", 63);
            keyToIdMap.put("Braum", 201);
            keyToIdMap.put("Caitlyn", 51);
            keyToIdMap.put("Camille", 164);
            keyToIdMap.put("Cassiopeia", 69);
            keyToIdMap.put("Chogath", 31);
            keyToIdMap.put("Corki", 42);
            keyToIdMap.put("Darius", 122);
            keyToIdMap.put("Diana", 131);
            keyToIdMap.put("DrMundo", 36);
            keyToIdMap.put("Draven", 119);
            keyToIdMap.put("Ekko", 245);
            keyToIdMap.put("Elise", 60);
            keyToIdMap.put("Evelynn", 28);
            keyToIdMap.put("Ezreal", 81);
            keyToIdMap.put("Fiddlesticks", 9);
            keyToIdMap.put("Fiora", 114);
            keyToIdMap.put("Fizz", 105);
            keyToIdMap.put("Galio", 3);
            keyToIdMap.put("Gangplank", 41);
            keyToIdMap.put("Garen", 86);
            keyToIdMap.put("Gnar", 150);
            keyToIdMap.put("Gragas", 79);
            keyToIdMap.put("Graves", 104);
            keyToIdMap.put("Gwen", 887);
            keyToIdMap.put("Hecarim", 120);
            keyToIdMap.put("Heimerdinger", 74);
            keyToIdMap.put("Illaoi", 420);
            keyToIdMap.put("Irelia", 39);
            keyToIdMap.put("Ivern", 427);
            keyToIdMap.put("Janna", 40);
            keyToIdMap.put("JarvanIV", 59);
            keyToIdMap.put("Jax", 24);
            keyToIdMap.put("Jayce", 126);
            keyToIdMap.put("Jhin", 202);
            keyToIdMap.put("Jinx", 222);
            keyToIdMap.put("Kaisa", 145);
            keyToIdMap.put("Kalista", 429);
            keyToIdMap.put("Karma", 43);
            keyToIdMap.put("Karthus", 30);
            keyToIdMap.put("Kassadin", 38);
            keyToIdMap.put("Katarina", 55);
            keyToIdMap.put("Kayle", 10);
            keyToIdMap.put("Kayn", 141);
            keyToIdMap.put("Kennen", 85);
            keyToIdMap.put("Khazix", 121);
            keyToIdMap.put("Kindred", 203);
            keyToIdMap.put("Kled", 240);
            keyToIdMap.put("KogMaw", 96);
            keyToIdMap.put("Leblanc", 7);
            keyToIdMap.put("LeeSin", 64);
            keyToIdMap.put("Leona", 89);
            keyToIdMap.put("Lillia", 876);
            keyToIdMap.put("Lissandra", 127);
            keyToIdMap.put("Lucian", 236);
            keyToIdMap.put("Lulu", 117);
            keyToIdMap.put("Lux", 99);
            keyToIdMap.put("Malphite", 54);
            keyToIdMap.put("Malzahar", 90);
            keyToIdMap.put("Maokai", 57);
            keyToIdMap.put("MasterYi", 11);
            keyToIdMap.put("MissFortune", 21);
            keyToIdMap.put("Mordekaiser", 82);
            keyToIdMap.put("Morgana", 25);
            keyToIdMap.put("Nami", 267);
            keyToIdMap.put("Nasus", 75);
            keyToIdMap.put("Nautilus", 111);
            keyToIdMap.put("Neeko", 518);
            keyToIdMap.put("Nidalee", 76);
            keyToIdMap.put("Nocturne", 56);
            keyToIdMap.put("Nunu", 20);
            keyToIdMap.put("Olaf", 2);
            keyToIdMap.put("Orianna", 61);
            keyToIdMap.put("Ornn", 516);
            keyToIdMap.put("Pantheon", 80);
            keyToIdMap.put("Poppy", 78);
            keyToIdMap.put("Pyke", 555);
            keyToIdMap.put("Qiyana", 246);
            keyToIdMap.put("Quinn", 133);
            keyToIdMap.put("Rakan", 497);
            keyToIdMap.put("Rammus", 33);
            keyToIdMap.put("RekSai", 421);
            keyToIdMap.put("Rell", 526);
            keyToIdMap.put("Renata", 888);
            keyToIdMap.put("Renekton", 58);
            keyToIdMap.put("Rengar", 107);
            keyToIdMap.put("Riven", 92);
            keyToIdMap.put("Rumble", 68);
            keyToIdMap.put("Ryze", 13);
            keyToIdMap.put("Samira", 360);
            keyToIdMap.put("Sejuani", 113);
            keyToIdMap.put("Senna", 235);
            keyToIdMap.put("Seraphine", 147);
            keyToIdMap.put("Sett", 875);
            keyToIdMap.put("Shaco", 35);
            keyToIdMap.put("Shen", 98);
            keyToIdMap.put("Shyvana", 102);
            keyToIdMap.put("Singed", 27);
            keyToIdMap.put("Sion", 14);
            keyToIdMap.put("Sivir", 15);
            keyToIdMap.put("Skarner", 72);
            keyToIdMap.put("Sona", 37);
            keyToIdMap.put("Soraka", 16);
            keyToIdMap.put("Swain", 50);
            keyToIdMap.put("Sylas", 517);
            keyToIdMap.put("Syndra", 134);
            keyToIdMap.put("TahmKench", 223);
            keyToIdMap.put("Taliyah", 163);
            keyToIdMap.put("Talon", 91);
            keyToIdMap.put("Taric", 44);
            keyToIdMap.put("Teemo", 17);
            keyToIdMap.put("Thresh", 412);
            keyToIdMap.put("Tristana", 18);
            keyToIdMap.put("Trundle", 48);
            keyToIdMap.put("Tryndamere", 23);
            keyToIdMap.put("TwistedFate", 4);
            keyToIdMap.put("Twitch", 29);
            keyToIdMap.put("Udyr", 77);
            keyToIdMap.put("Urgot", 6);
            keyToIdMap.put("Varus", 110);
            keyToIdMap.put("Vayne", 67);
            keyToIdMap.put("Veigar", 45);
            keyToIdMap.put("Velkoz", 161);
            keyToIdMap.put("Vex", 711);
            keyToIdMap.put("Vi", 254);
            keyToIdMap.put("Viego", 234);
            keyToIdMap.put("Viktor", 112);
            keyToIdMap.put("Vladimir", 8);
            keyToIdMap.put("Volibear", 106);
            keyToIdMap.put("Warwick", 19);
            keyToIdMap.put("Wukong", 62);
            keyToIdMap.put("Xayah", 498);
            keyToIdMap.put("Xerath", 101);
            keyToIdMap.put("XinZhao", 5);
            keyToIdMap.put("Yasuo", 157);
            keyToIdMap.put("Yone", 777);
            keyToIdMap.put("Yorick", 83);
            keyToIdMap.put("Yuumi", 350);
            keyToIdMap.put("Zac", 154);
            keyToIdMap.put("Zed", 238);
            keyToIdMap.put("Zeri", 221);
            keyToIdMap.put("Ziggs", 115);
            keyToIdMap.put("Zilean", 26);
            keyToIdMap.put("Zoe", 142);
            keyToIdMap.put("Zyra", 143);
            
            return keyToIdMap.get(key);
        }
        
        /**
         * 确保championId有效的方法
         */
        public void ensureChampionId() {
            if (this.championId == null && this.key != null && !this.key.trim().isEmpty()) {
                try {
                    this.championId = getChampionIdByKey(this.key.trim());
                    if (this.championId != null) {
                        logger.debug("Auto-resolved championId {} for key {}", this.championId, this.key);
                    } else {
                        logger.warn("Failed to resolve championId for key: {} - trying fallback methods", this.key);
                        // 尝试备用方法解析championId
                        this.championId = tryFallbackChampionIdResolution(this.key.trim());
                        if (this.championId != null) {
                            logger.info("Successfully resolved championId {} for key {} using fallback method", this.championId, this.key);
                        } else {
                            logger.error("All methods failed to resolve championId for key: {}", this.key);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error resolving championId for key: {}", this.key, e);
                    // 即使出现异常，也尝试备用方法
                    try {
                        this.championId = tryFallbackChampionIdResolution(this.key.trim());
                        if (this.championId != null) {
                            logger.info("Fallback method succeeded for key {} after exception", this.key);
                        }
                    } catch (Exception fallbackEx) {
                        logger.error("Fallback method also failed for key: {}", this.key, fallbackEx);
                    }
                }
            } else if (this.championId == null) {
                logger.warn("Cannot ensure championId - key is null or empty: '{}'", this.key);
            }
        }
        
        /**
         * 备用的championId解析方法
         */
        private static Integer tryFallbackChampionIdResolution(String key) {
            if (key == null || key.trim().isEmpty()) {
                return null;
            }
            
            // 尝试忽略大小写匹配
            Map<String, Integer> keyToIdMap = getFallbackChampionMap();
            
            // 第一次尝试：完全匹配
            Integer championId = keyToIdMap.get(key);
            if (championId != null) {
                return championId;
            }
            
            // 第二次尝试：忽略大小写匹配
            for (Map.Entry<String, Integer> entry : keyToIdMap.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    return entry.getValue();
                }
            }
            
            // 第三次尝试：移除特殊字符和空格后匹配
            String cleanKey = key.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            for (Map.Entry<String, Integer> entry : keyToIdMap.entrySet()) {
                String cleanEntryKey = entry.getKey().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                if (cleanEntryKey.equals(cleanKey)) {
                    return entry.getValue();
                }
            }
            
            return null;
        }
        
        /**
         * 获取完整的英雄ID映射表（包含更多英雄和别名）
         */
        private static Map<String, Integer> getFallbackChampionMap() {
            Map<String, Integer> keyToIdMap = new HashMap<>();
            
            // 添加所有英雄的标准映射
            keyToIdMap.put("Aatrox", 266);
            keyToIdMap.put("Ahri", 103);
            keyToIdMap.put("Akali", 84);
            keyToIdMap.put("Akshan", 166);
            keyToIdMap.put("Alistar", 12);
            keyToIdMap.put("Ammu", 32);
            keyToIdMap.put("Amumu", 32); // 阿木木的另一种拼写
            keyToIdMap.put("Anivia", 34);
            keyToIdMap.put("Annie", 1);
            keyToIdMap.put("Aphelios", 523);
            keyToIdMap.put("Ashe", 22);
            keyToIdMap.put("AurelionSol", 136);
            keyToIdMap.put("Azir", 268);
            keyToIdMap.put("Bard", 432);
            keyToIdMap.put("Belveth", 200);
            keyToIdMap.put("Blitzcrank", 53);
            keyToIdMap.put("Brand", 63);
            keyToIdMap.put("Braum", 201);
            keyToIdMap.put("Caitlyn", 51);
            keyToIdMap.put("Camille", 164);
            keyToIdMap.put("Cassiopeia", 69);
            keyToIdMap.put("Chogath", 31);
            keyToIdMap.put("Corki", 42);
            keyToIdMap.put("Darius", 122);
            keyToIdMap.put("Diana", 131);
            keyToIdMap.put("DrMundo", 36);
            keyToIdMap.put("Draven", 119);
            keyToIdMap.put("Ekko", 245);
            keyToIdMap.put("Elise", 60);
            keyToIdMap.put("Evelynn", 28);
            keyToIdMap.put("Ezreal", 81);
            keyToIdMap.put("Fiddlesticks", 9);
            keyToIdMap.put("Fiora", 114);
            keyToIdMap.put("Fizz", 105);
            keyToIdMap.put("Galio", 3);
            keyToIdMap.put("Gangplank", 41);
            keyToIdMap.put("Garen", 86);
            keyToIdMap.put("Gnar", 150);
            keyToIdMap.put("Gragas", 79);
            keyToIdMap.put("Graves", 104);
            keyToIdMap.put("Gwen", 887);
            keyToIdMap.put("Hecarim", 120);
            keyToIdMap.put("Heimerdinger", 74);
            keyToIdMap.put("Illaoi", 420);
            keyToIdMap.put("Irelia", 39);
            keyToIdMap.put("Ivern", 427);
            keyToIdMap.put("Janna", 40);
            keyToIdMap.put("JarvanIV", 59);
            keyToIdMap.put("Jax", 24);
            keyToIdMap.put("Jayce", 126);
            keyToIdMap.put("Jhin", 202);
            keyToIdMap.put("Jinx", 222);
            keyToIdMap.put("Kaisa", 145);
            keyToIdMap.put("Kalista", 429);
            keyToIdMap.put("Karma", 43);
            keyToIdMap.put("Karthus", 30);
            keyToIdMap.put("Kassadin", 38);
            keyToIdMap.put("Katarina", 55);
            keyToIdMap.put("Kayle", 10);
            keyToIdMap.put("Kayn", 141);
            keyToIdMap.put("Kennen", 85);
            keyToIdMap.put("Khazix", 121);
            keyToIdMap.put("Kindred", 203);
            keyToIdMap.put("Kled", 240);
            keyToIdMap.put("KogMaw", 96);
            keyToIdMap.put("Leblanc", 7);
            keyToIdMap.put("LeeSin", 64);
            keyToIdMap.put("Leona", 89);
            keyToIdMap.put("Lillia", 876);
            keyToIdMap.put("Lissandra", 127);
            keyToIdMap.put("Lucian", 236);
            keyToIdMap.put("Lulu", 117);
            keyToIdMap.put("Lux", 99);
            keyToIdMap.put("Malphite", 54);
            keyToIdMap.put("Malzahar", 90);
            keyToIdMap.put("Maokai", 57);
            keyToIdMap.put("MasterYi", 11);
            keyToIdMap.put("MissFortune", 21);
            keyToIdMap.put("Mordekaiser", 82);
            keyToIdMap.put("Morgana", 25);
            keyToIdMap.put("Nami", 267);
            keyToIdMap.put("Nasus", 75);
            keyToIdMap.put("Nautilus", 111);
            keyToIdMap.put("Neeko", 518);
            keyToIdMap.put("Nidalee", 76);
            keyToIdMap.put("Nilah", 895);
            keyToIdMap.put("Nocturne", 56);
            keyToIdMap.put("Nunu", 20);
            keyToIdMap.put("Olaf", 2);
            keyToIdMap.put("Orianna", 61);
            keyToIdMap.put("Ornn", 516);
            keyToIdMap.put("Pantheon", 80);
            keyToIdMap.put("Poppy", 78);
            keyToIdMap.put("Pyke", 555);
            keyToIdMap.put("Qiyana", 246);
            keyToIdMap.put("Quinn", 133);
            keyToIdMap.put("Rakan", 497);
            keyToIdMap.put("Rammus", 33);
            keyToIdMap.put("RekSai", 421);
            keyToIdMap.put("Rell", 526);
            keyToIdMap.put("Renata", 888);
            keyToIdMap.put("Renekton", 58);
            keyToIdMap.put("Rengar", 107);
            keyToIdMap.put("Riven", 92);
            keyToIdMap.put("Rumble", 68);
            keyToIdMap.put("Ryze", 13);
            keyToIdMap.put("Samira", 360);
            keyToIdMap.put("Sejuani", 113);
            keyToIdMap.put("Senna", 235);
            keyToIdMap.put("Seraphine", 147);
            keyToIdMap.put("Sett", 875);
            keyToIdMap.put("Shaco", 35);
            keyToIdMap.put("Shen", 98);
            keyToIdMap.put("Shyvana", 102);
            keyToIdMap.put("Singed", 27);
            keyToIdMap.put("Sion", 14);
            keyToIdMap.put("Sivir", 15);
            keyToIdMap.put("Skarner", 72);
            keyToIdMap.put("Sona", 37);
            keyToIdMap.put("Soraka", 16);
            keyToIdMap.put("Swain", 50);
            keyToIdMap.put("Sylas", 517);
            keyToIdMap.put("Syndra", 134);
            keyToIdMap.put("TahmKench", 223);
            keyToIdMap.put("Taliyah", 163);
            keyToIdMap.put("Talon", 91);
            keyToIdMap.put("Taric", 44);
            keyToIdMap.put("Teemo", 17);
            keyToIdMap.put("Thresh", 412);
            keyToIdMap.put("Tristana", 18);
            keyToIdMap.put("Trundle", 48);
            keyToIdMap.put("Tryndamere", 23);
            keyToIdMap.put("TwistedFate", 4);
            keyToIdMap.put("Twitch", 29);
            keyToIdMap.put("Udyr", 77);
            keyToIdMap.put("Urgot", 6);
            keyToIdMap.put("Varus", 110);
            keyToIdMap.put("Vayne", 67);
            keyToIdMap.put("Veigar", 45);
            keyToIdMap.put("Velkoz", 161);
            keyToIdMap.put("Vex", 711);
            keyToIdMap.put("Vi", 254);
            keyToIdMap.put("Viego", 234);
            keyToIdMap.put("Viktor", 112);
            keyToIdMap.put("Vladimir", 8);
            keyToIdMap.put("Volibear", 106);
            keyToIdMap.put("Warwick", 19);
            keyToIdMap.put("Wukong", 62);
            keyToIdMap.put("Xayah", 498);
            keyToIdMap.put("Xerath", 101);
            keyToIdMap.put("XinZhao", 5);
            keyToIdMap.put("Yasuo", 157);
            keyToIdMap.put("Yone", 777);
            keyToIdMap.put("Yorick", 83);
            keyToIdMap.put("Yuumi", 350);
            keyToIdMap.put("Zac", 154);
            keyToIdMap.put("Zed", 238);
            keyToIdMap.put("Zeri", 221);
            keyToIdMap.put("Ziggs", 115);
            keyToIdMap.put("Zilean", 26);
            keyToIdMap.put("Zoe", 142);
            keyToIdMap.put("Zyra", 143);
            
            return keyToIdMap;
        }
        
        @Override
        public String toString() {
            return nameCn + " (" + nameEn + ")";
        }
    }
    
    // 静态方法：加载配置
    public static AutoAcceptConfig load() {
        ObjectMapper mapper = new ObjectMapper();
        File configFile = new File(CONFIG_FILE);
        
        if (configFile.exists()) {
            try {
                AutoAcceptConfig config = mapper.readValue(configFile, AutoAcceptConfig.class);
                logger.info("Loaded configuration from {}", CONFIG_FILE);
                
                // 验证和修复配置
                config.validateAndFixConfiguration();
                
                return config;
            } catch (IOException e) {
                logger.error("Failed to load configuration from {}, using defaults", CONFIG_FILE, e);
            } catch (Exception e) {
                logger.error("Error validating configuration from {}, using defaults", CONFIG_FILE, e);
            }
        } else {
            logger.info("Configuration file {} not found, using defaults", CONFIG_FILE);
        }
        
        AutoAcceptConfig defaultConfig = new AutoAcceptConfig();
        defaultConfig.validateAndFixConfiguration();
        return defaultConfig;
    }
    
    // 保存配置
    public void save() {
        try {
            // 保存前验证配置
            validateAndFixConfiguration();
            
            ObjectMapper mapper = new ObjectMapper();
            File configFile = new File(CONFIG_FILE);
            
            mapper.writerWithDefaultPrettyPrinter().writeValue(configFile, this);
            logger.info("Saved configuration to {}", CONFIG_FILE);
        } catch (IOException e) {
            logger.error("Failed to save configuration to {}", CONFIG_FILE, e);
        } catch (Exception e) {
            logger.error("Error validating configuration before save", e);
        }
    }
    
    // Getters and Setters
    public boolean isAutoAcceptEnabled() { return autoAcceptEnabled; }
    public void setAutoAcceptEnabled(boolean autoAcceptEnabled) { this.autoAcceptEnabled = autoAcceptEnabled; }
    
    public boolean isAutoDeclineEnabled() { return autoDeclineEnabled; }
    public void setAutoDeclineEnabled(boolean autoDeclineEnabled) { this.autoDeclineEnabled = autoDeclineEnabled; }
    
    public int getCheckIntervalMs() { return checkIntervalMs; }
    public void setCheckIntervalMs(int checkIntervalMs) { this.checkIntervalMs = Math.max(100, Math.min(5000, checkIntervalMs)); }
    
    public ChampionSelectConfig getChampionSelect() { return championSelect; }
    public void setChampionSelect(ChampionSelectConfig championSelect) { this.championSelect = championSelect; }
    
    // System Tray and Auto Start getters and setters
    public boolean isSystemTrayEnabled() { return systemTrayEnabled; }
    public void setSystemTrayEnabled(boolean systemTrayEnabled) { this.systemTrayEnabled = systemTrayEnabled; }
    
    
    public boolean isAutoStartEnabled() { return autoStartEnabled; }
    public void setAutoStartEnabled(boolean autoStartEnabled) { this.autoStartEnabled = autoStartEnabled; }
    
    // Window close behavior getters and setters
    public boolean isMinimizeOnClose() { return minimizeOnClose; }
    public void setMinimizeOnClose(boolean minimizeOnClose) { this.minimizeOnClose = minimizeOnClose; }
    
    public boolean isConfirmOnExit() { return confirmOnExit; }
    public void setConfirmOnExit(boolean confirmOnExit) { this.confirmOnExit = confirmOnExit; }
    
    // Popup suppression getters and setters
    public boolean isSuppressReadyCheckPopup() { return suppressReadyCheckPopup; }
    public void setSuppressReadyCheckPopup(boolean suppressReadyCheckPopup) { this.suppressReadyCheckPopup = suppressReadyCheckPopup; }
    
    public boolean isSuppressBanPhasePopup() { return suppressBanPhasePopup; }
    public void setSuppressBanPhasePopup(boolean suppressBanPhasePopup) { this.suppressBanPhasePopup = suppressBanPhasePopup; }
    
    public boolean isSuppressPickPhasePopup() { return suppressPickPhasePopup; }
    public void setSuppressPickPhasePopup(boolean suppressPickPhasePopup) { this.suppressPickPhasePopup = suppressPickPhasePopup; }
    
    public boolean isForceEnglishTrayMenu() { return forceEnglishTrayMenu; }
    public void setForceEnglishTrayMenu(boolean forceEnglishTrayMenu) { this.forceEnglishTrayMenu = forceEnglishTrayMenu; }
    
    // Auto connect getters and setters
    public boolean isAutoConnectEnabled() { return autoConnectEnabled; }
    public void setAutoConnectEnabled(boolean autoConnectEnabled) { this.autoConnectEnabled = autoConnectEnabled; }
    
    public boolean isAutoReconnectEnabled() { return autoReconnectEnabled; }
    public void setAutoReconnectEnabled(boolean autoReconnectEnabled) { this.autoReconnectEnabled = autoReconnectEnabled; }
    
    public int getReconnectIntervalSeconds() { return reconnectIntervalSeconds; }
    public void setReconnectIntervalSeconds(int reconnectIntervalSeconds) { 
        this.reconnectIntervalSeconds = Math.max(5, Math.min(60, reconnectIntervalSeconds)); 
    }
    
    /**
     * 验证和修复配置的方法
     */
    public void validateAndFixConfiguration() {
        logger.debug("Validating and fixing configuration...");
        
        try {
            // 验证基本配置
            if (championSelect == null) {
                logger.warn("ChampionSelect config is null, creating default");
                championSelect = new ChampionSelectConfig();
            }
            
            // 验证英雄配置
            if (championSelect.getBanChampion() != null) {
                championSelect.getBanChampion().ensureChampionId();
            }
            
            if (championSelect.getPickChampion() != null) {
                championSelect.getPickChampion().ensureChampionId();
            }
            
            // 验证分路配置
            if (championSelect.getPositionConfigs() != null) {
                for (PositionConfig posConfig : championSelect.getPositionConfigs().values()) {
                    if (posConfig != null) {
                        // 验证ban英雄列表
                        if (posConfig.getBanChampions() != null) {
                            for (ChampionInfo champion : posConfig.getBanChampions()) {
                                if (champion != null) {
                                    champion.ensureChampionId();
                                }
                            }
                        }
                        
                        // 验证pick英雄列表
                        if (posConfig.getPickChampions() != null) {
                            for (ChampionInfo champion : posConfig.getPickChampions()) {
                                if (champion != null) {
                                    champion.ensureChampionId();
                                }
                            }
                        }
                        
                        // 验证首选英雄
                        if (posConfig.getPreferredBanChampion() != null) {
                            posConfig.getPreferredBanChampion().ensureChampionId();
                        }
                        
                        if (posConfig.getPreferredPickChampion() != null) {
                            posConfig.getPreferredPickChampion().ensureChampionId();
                        }
                    }
                }
            }
            
            logger.debug("Configuration validation completed successfully");
        } catch (Exception e) {
            logger.error("Error during configuration validation", e);
        }
    }
    
    @Override
    public String toString() {
        return "AutoAcceptConfig{" +
                "autoAcceptEnabled=" + autoAcceptEnabled +
                ", autoBanEnabled=" + championSelect.autoBanEnabled +
                ", autoPickEnabled=" + championSelect.autoPickEnabled +
                ", banChampion=" + championSelect.banChampion +
                ", pickChampion=" + championSelect.pickChampion +
                ", systemTrayEnabled=" + systemTrayEnabled +
                ", autoStartEnabled=" + autoStartEnabled +
                ", minimizeOnClose=" + minimizeOnClose +
                ", suppressReadyCheckPopup=" + suppressReadyCheckPopup +
                ", suppressBanPhasePopup=" + suppressBanPhasePopup +
                ", suppressPickPhasePopup=" + suppressPickPhasePopup +
                '}';
    }
}