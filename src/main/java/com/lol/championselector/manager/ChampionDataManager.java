package com.lol.championselector.manager;

import com.lol.championselector.model.Champion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class ChampionDataManager {
    private static final Logger logger = LoggerFactory.getLogger(ChampionDataManager.class);
    
    private final List<Champion> allChampions;
    private final Map<String, Champion> championMap;
    
    public ChampionDataManager() {
        this.allChampions = new ArrayList<>();
        this.championMap = new HashMap<>();
        initializeChampionData();
    }
    
    private void initializeChampionData() {
        // A系列英雄
        addChampion("Aatrox", "266", "Aatrox", "亚托克斯", 
                   Arrays.asList("上单", "战士", "暗裔剑魔", "亚托克斯"), "暗裔剑魔", 
                   Arrays.asList("Fighter", "Tank"));
        
        addChampion("Ahri", "103", "Ahri", "阿狸", 
                   Arrays.asList("中单", "法师", "九尾妖狐", "阿狸"), "九尾妖狐", 
                   Arrays.asList("Mage", "Assassin"));
        
        addChampion("Akali", "84", "Akali", "阿卡丽", 
                   Arrays.asList("中单", "上单", "刺客", "离群之刺", "阿卡丽"), "离群之刺", 
                   Arrays.asList("Assassin"));
        
        addChampion("Akshan", "166", "Akshan", "阿克尚", 
                   Arrays.asList("中单", "adc", "射手", "复仇哨兵", "阿克尚"), "复仇哨兵", 
                   Arrays.asList("Marksman", "Assassin"));
        
        addChampion("Alistar", "12", "Alistar", "阿利斯塔", 
                   Arrays.asList("辅助", "坦克", "牛头酋长", "阿利斯塔"), "牛头酋长", 
                   Arrays.asList("Tank", "Support"));
        
        addChampion("Amumu", "32", "Amumu", "阿木木", 
                   Arrays.asList("打野", "辅助", "坦克", "殇之木乃伊", "阿木木"), "殇之木乃伊", 
                   Arrays.asList("Tank", "Mage"));
        
        addChampion("Anivia", "34", "Anivia", "艾尼维亚", 
                   Arrays.asList("中单", "法师", "冰晶凤凰", "艾尼维亚"), "冰晶凤凰", 
                   Arrays.asList("Mage", "Support"));
        
        addChampion("Annie", "1", "Annie", "安妮", 
                   Arrays.asList("中单", "法师", "黑暗之女", "安妮"), "黑暗之女", 
                   Arrays.asList("Mage"));
        
        addChampion("Aphelios", "523", "Aphelios", "厄斐琉斯", 
                   Arrays.asList("adc", "射手", "残月之肃", "厄斐琉斯"), "残月之肃", 
                   Arrays.asList("Marksman"));
        
        addChampion("Ashe", "22", "Ashe", "艾希", 
                   Arrays.asList("adc", "射手", "寒冰射手", "艾希"), "寒冰射手", 
                   Arrays.asList("Marksman", "Support"));
        
        addChampion("AurelionSol", "136", "Aurelion Sol", "奥瑞利安·索尔", 
                   Arrays.asList("中单", "法师", "铸星龙王", "奥瑞利安·索尔", "龙王"), "铸星龙王", 
                   Arrays.asList("Mage"));
        
        addChampion("Aurora", "893", "Aurora", "极光", 
                   Arrays.asList("中单", "法师", "女巫", "极光"), "女巫", 
                   Arrays.asList("Mage", "Assassin"));
        
        addChampion("Azir", "268", "Azir", "阿兹尔", 
                   Arrays.asList("中单", "法师", "沙漠皇帝", "阿兹尔"), "沙漠皇帝", 
                   Arrays.asList("Mage", "Marksman"));
        
        // B系列英雄
        addChampion("Bard", "432", "Bard", "巴德", 
                   Arrays.asList("辅助", "星界游神", "巴德"), "星界游神", 
                   Arrays.asList("Support", "Mage"));
        
        addChampion("Belveth", "200", "Bel'Veth", "卑尔维斯", 
                   Arrays.asList("打野", "虚空女皇", "卑尔维斯"), "虚空女皇", 
                   Arrays.asList("Fighter"));
        
        addChampion("Blitzcrank", "53", "Blitzcrank", "布里茨", 
                   Arrays.asList("辅助", "机器人", "大发明家", "布里茨", "机器人"), "大发明家", 
                   Arrays.asList("Tank", "Fighter"));
        
        addChampion("Brand", "63", "Brand", "布兰德", 
                   Arrays.asList("中单", "辅助", "法师", "复仇焰魂", "布兰德"), "复仇焰魂", 
                   Arrays.asList("Mage"));
        
        addChampion("Braum", "201", "Braum", "布隆", 
                   Arrays.asList("辅助", "坦克", "弗雷尔卓德之心", "布隆"), "弗雷尔卓德之心", 
                   Arrays.asList("Support", "Tank"));
        
        addChampion("Briar", "233", "Briar", "贝蕾亚", 
                   Arrays.asList("打野", "刺客", "未竟之物", "贝蕾亚"), "未竟之物", 
                   Arrays.asList("Assassin", "Fighter"));
        
        // C系列英雄
        addChampion("Caitlyn", "51", "Caitlyn", "凯特琳", 
                   Arrays.asList("adc", "射手", "皮城女警", "凯特琳"), "皮城女警", 
                   Arrays.asList("Marksman"));
        
        addChampion("Camille", "164", "Camille", "卡蜜尔", 
                   Arrays.asList("上单", "刺客", "青钢影", "卡蜜尔"), "青钢影", 
                   Arrays.asList("Fighter", "Tank"));
        
        addChampion("Cassiopeia", "69", "Cassiopeia", "卡西奥佩娅", 
                   Arrays.asList("中单", "法师", "魔蛇之拥", "卡西奥佩娅"), "魔蛇之拥", 
                   Arrays.asList("Mage"));
        
        addChampion("Chogath", "31", "Cho'Gath", "科加斯", 
                   Arrays.asList("上单", "中单", "坦克", "虚空恐惧", "科加斯"), "虚空恐惧", 
                   Arrays.asList("Tank", "Mage"));
        
        addChampion("Corki", "42", "Corki", "库奇", 
                   Arrays.asList("中单", "adc", "英勇投弹手", "库奇"), "英勇投弹手", 
                   Arrays.asList("Marksman"));
        
        // D系列英雄
        addChampion("Darius", "122", "Darius", "德莱厄斯", 
                   Arrays.asList("上单", "战士", "诺克萨斯之手", "德莱厄斯"), "诺克萨斯之手", 
                   Arrays.asList("Fighter", "Tank"));
        
        addChampion("Diana", "131", "Diana", "黛安娜", 
                   Arrays.asList("中单", "打野", "法师", "皎月女神", "黛安娜"), "皎月女神", 
                   Arrays.asList("Fighter", "Mage"));
        
        addChampion("DrMundo", "36", "Dr. Mundo", "蒙多医生", 
                   Arrays.asList("上单", "打野", "坦克", "祖安狂人", "蒙多医生"), "祖安狂人", 
                   Arrays.asList("Fighter", "Tank"));
        
        addChampion("Draven", "119", "Draven", "德莱文", 
                   Arrays.asList("adc", "射手", "荣耀行刑官", "德莱文"), "荣耀行刑官", 
                   Arrays.asList("Marksman"));
        
        // E系列英雄
        addChampion("Ekko", "245", "Ekko", "艾克", 
                   Arrays.asList("中单", "打野", "刺客", "时间刺客", "艾克"), "时间刺客", 
                   Arrays.asList("Assassin", "Fighter"));
        
        addChampion("Elise", "60", "Elise", "伊莉丝", 
                   Arrays.asList("打野", "法师", "蜘蛛女皇", "伊莉丝"), "蜘蛛女皇", 
                   Arrays.asList("Mage", "Fighter"));
        
        addChampion("Evelynn", "28", "Evelynn", "伊芙琳", 
                   Arrays.asList("打野", "刺客", "痛苦之拥", "伊芙琳"), "痛苦之拥", 
                   Arrays.asList("Assassin", "Mage"));
        
        addChampion("Ezreal", "81", "Ezreal", "伊泽瑞尔", 
                   Arrays.asList("adc", "射手", "探险家", "伊泽瑞尔", "ez"), "探险家", 
                   Arrays.asList("Marksman", "Mage"));
        
        // F系列英雄
        addChampion("Fiddlesticks", "9", "Fiddlesticks", "费德提克", 
                   Arrays.asList("打野", "辅助", "法师", "末日使者", "费德提克"), "末日使者", 
                   Arrays.asList("Mage", "Support"));
        
        addChampion("Fiora", "114", "Fiora", "菲奥娜", 
                   Arrays.asList("上单", "战士", "无双剑姬", "菲奥娜"), "无双剑姬", 
                   Arrays.asList("Fighter", "Assassin"));
        
        addChampion("Fizz", "105", "Fizz", "菲兹", 
                   Arrays.asList("中单", "刺客", "潮汐海灵", "菲兹"), "潮汐海灵", 
                   Arrays.asList("Assassin", "Fighter"));
        
        // G系列英雄
        addChampion("Galio", "3", "Galio", "加里奥", 
                   Arrays.asList("中单", "辅助", "坦克", "正义巨像", "加里奥"), "正义巨像", 
                   Arrays.asList("Tank", "Mage"));
        
        addChampion("Gangplank", "41", "Gangplank", "普朗克", 
                   Arrays.asList("上单", "战士", "海洋之灾", "普朗克"), "海洋之灾", 
                   Arrays.asList("Fighter"));
        
        addChampion("Garen", "86", "Garen", "盖伦", 
                   Arrays.asList("上单", "坦克", "德玛西亚之力", "盖伦"), "德玛西亚之力", 
                   Arrays.asList("Fighter", "Tank"));
        
        addChampion("Gnar", "150", "Gnar", "纳尔", 
                   Arrays.asList("上单", "战士", "迷失之牙", "纳尔"), "迷失之牙", 
                   Arrays.asList("Fighter", "Tank"));
        
        addChampion("Gragas", "79", "Gragas", "古拉加斯", 
                   Arrays.asList("打野", "中单", "酒桶之王", "古拉加斯", "酒桶"), "酒桶之王", 
                   Arrays.asList("Fighter", "Mage"));
        
        addChampion("Graves", "104", "Graves", "格雷夫斯", 
                   Arrays.asList("打野", "adc", "法外狂徒", "格雷夫斯", "男枪"), "法外狂徒", 
                   Arrays.asList("Marksman"));
        
        addChampion("Gwen", "887", "Gwen", "格温", 
                   Arrays.asList("上单", "打野", "战士", "灵罗娃娃", "格温"), "灵罗娃娃", 
                   Arrays.asList("Fighter", "Assassin"));
        
        // H系列英雄
        addChampion("Hecarim", "120", "Hecarim", "赫卡里姆", 
                   Arrays.asList("打野", "上单", "战争之影", "赫卡里姆", "人马"), "战争之影", 
                   Arrays.asList("Fighter", "Tank"));
        
        addChampion("Heimerdinger", "74", "Heimerdinger", "黑默丁格", 
                   Arrays.asList("中单", "辅助", "法师", "大发明家", "黑默丁格"), "大发明家", 
                   Arrays.asList("Mage", "Support"));
        
        addChampion("Hwei", "910", "Hwei", "彗", 
                   Arrays.asList("中单", "辅助", "法师", "万华通灵", "彗"), "万华通灵", 
                   Arrays.asList("Mage"));
        
        // I系列英雄
        addChampion("Illaoi", "420", "Illaoi", "俄洛伊", 
                   Arrays.asList("上单", "战士", "海兽祭司", "俄洛伊"), "海兽祭司", 
                   Arrays.asList("Fighter", "Tank"));
        
        addChampion("Irelia", "39", "Irelia", "艾瑞莉娅", 
                   Arrays.asList("上单", "中单", "战士", "刀锋舞者", "艾瑞莉娅"), "刀锋舞者", 
                   Arrays.asList("Fighter", "Assassin"));
        
        addChampion("Ivern", "427", "Ivern", "艾翁", 
                   Arrays.asList("打野", "辅助", "翠神", "艾翁"), "翠神", 
                   Arrays.asList("Support", "Mage"));
        
        // J系列英雄
        addChampion("Janna", "40", "Janna", "迦娜", 
                   Arrays.asList("辅助", "风暴之怒", "迦娜"), "风暴之怒", 
                   Arrays.asList("Support", "Mage"));
        
        addChampion("JarvanIV", "59", "Jarvan IV", "嘉文四世", 
                   Arrays.asList("打野", "上单", "德玛西亚皇子", "嘉文四世", "皇子"), "德玛西亚皇子", 
                   Arrays.asList("Tank", "Fighter"));
        
        addChampion("Jax", "24", "Jax", "贾克斯", 
                   Arrays.asList("上单", "打野", "武器大师", "贾克斯"), "武器大师", 
                   Arrays.asList("Fighter", "Assassin"));
        
        addChampion("Jayce", "126", "Jayce", "杰斯", 
                   Arrays.asList("上单", "中单", "未来守护者", "杰斯"), "未来守护者", 
                   Arrays.asList("Fighter", "Marksman"));
        
        addChampion("Jhin", "202", "Jhin", "烬", 
                   Arrays.asList("adc", "射手", "戏命师", "烬"), "戏命师", 
                   Arrays.asList("Marksman"));
        
        addChampion("Jinx", "222", "Jinx", "金克丝", 
                   Arrays.asList("adc", "射手", "暴走萝莉", "金克丝"), "暴走萝莉", 
                   Arrays.asList("Marksman"));
        
        // K系列英雄
        addChampion("Kaisa", "145", "Kai'Sa", "卡莎", 
                   Arrays.asList("adc", "射手", "虚空之女", "卡莎"), "虚空之女", 
                   Arrays.asList("Marksman"));
        
        addChampion("Kalista", "429", "Kalista", "卡莉丝塔", 
                   Arrays.asList("adc", "射手", "复仇之矛", "卡莉丝塔"), "复仇之矛", 
                   Arrays.asList("Marksman"));
        
        addChampion("Karma", "43", "Karma", "卡尔玛", 
                   Arrays.asList("辅助", "中单", "法师", "天启者", "卡尔玛"), "天启者", 
                   Arrays.asList("Mage", "Support"));
        
        addChampion("Karthus", "30", "Karthus", "卡尔萨斯", 
                   Arrays.asList("打野", "中单", "法师", "死亡颂唱者", "卡尔萨斯"), "死亡颂唱者", 
                   Arrays.asList("Mage"));
        
        addChampion("Kassadin", "38", "Kassadin", "卡萨丁", 
                   Arrays.asList("中单", "刺客", "虚空行者", "卡萨丁"), "虚空行者", 
                   Arrays.asList("Assassin", "Mage"));
        
        addChampion("Katarina", "55", "Katarina", "卡特琳娜", 
                   Arrays.asList("中单", "刺客", "不祥之刃", "卡特琳娜"), "不祥之刃", 
                   Arrays.asList("Assassin", "Mage"));
        
        addChampion("Kayle", "10", "Kayle", "凯尔", 
                   Arrays.asList("上单", "中单", "正义天使", "凯尔"), "正义天使", 
                   Arrays.asList("Fighter", "Support"));
        
        addChampion("Kayn", "141", "Kayn", "凯隐", 
                   Arrays.asList("打野", "刺客", "影流之镰", "凯隐"), "影流之镰", 
                   Arrays.asList("Assassin", "Fighter"));
        
        addChampion("Kennen", "85", "Kennen", "凯南", 
                   Arrays.asList("上单", "中单", "狂暴之心", "凯南"), "狂暴之心", 
                   Arrays.asList("Mage", "Marksman"));
        
        addChampion("Khazix", "121", "Kha'Zix", "卡兹克", 
                   Arrays.asList("打野", "刺客", "虚空掠夺者", "卡兹克", "螳螂"), "虚空掠夺者", 
                   Arrays.asList("Assassin"));
        
        addChampion("Kindred", "203", "Kindred", "千珏", 
                   Arrays.asList("打野", "射手", "永猎双子", "千珏"), "永猎双子", 
                   Arrays.asList("Marksman"));
        
        addChampion("Kled", "240", "Kled", "克烈", 
                   Arrays.asList("上单", "战士", "暴怒骑士", "克烈"), "暴怒骑士", 
                   Arrays.asList("Fighter", "Tank"));
        
        addChampion("KogMaw", "96", "Kog'Maw", "克格莫", 
                   Arrays.asList("adc", "中单", "射手", "深渊巨口", "克格莫", "大嘴"), "深渊巨口", 
                   Arrays.asList("Marksman", "Mage"));
        
        // L系列英雄
        addChampion("Leblanc", "7", "LeBlanc", "乐芙兰", 
                   Arrays.asList("中单", "刺客", "诡术妖姬", "乐芙兰"), "诡术妖姬", 
                   Arrays.asList("Assassin", "Mage"));
        
        addChampion("LeeSin", "64", "Lee Sin", "李青", 
                   Arrays.asList("打野", "瞎子", "盲僧", "李青"), "盲僧", 
                   Arrays.asList("Fighter", "Assassin"));
        
        addChampion("Leona", "89", "Leona", "蕾欧娜", 
                   Arrays.asList("辅助", "坦克", "曙光女神", "蕾欧娜"), "曙光女神", 
                   Arrays.asList("Tank", "Support"));
        
        addChampion("Lillia", "876", "Lillia", "莉莉娅", 
                   Arrays.asList("打野", "上单", "法师", "含羞蓓蕾", "莉莉娅"), "含羞蓓蕾", 
                   Arrays.asList("Fighter", "Mage"));
        
        addChampion("Lissandra", "127", "Lissandra", "丽桑卓", 
                   Arrays.asList("中单", "法师", "冰霜女巫", "丽桑卓"), "冰霜女巫", 
                   Arrays.asList("Mage"));
        
        addChampion("Lucian", "236", "Lucian", "卢锡安", 
                   Arrays.asList("adc", "中单", "射手", "圣枪游侠", "卢锡安"), "圣枪游侠", 
                   Arrays.asList("Marksman"));
        
        addChampion("Lulu", "117", "Lulu", "璐璐", 
                   Arrays.asList("辅助", "中单", "仙灵女巫", "璐璐"), "仙灵女巫", 
                   Arrays.asList("Support", "Mage"));
        
        addChampion("Lux", "99", "Lux", "拉克丝", 
                   Arrays.asList("中单", "辅助", "光辉女郎", "拉克丝"), "光辉女郎", 
                   Arrays.asList("Mage", "Support"));
        
        // M系列英雄
        addChampion("Malphite", "54", "Malphite", "墨菲特", 
                   Arrays.asList("上单", "中单", "坦克", "熔岩巨兽", "墨菲特", "石头人"), "熔岩巨兽", 
                   Arrays.asList("Tank", "Fighter"));
        
        addChampion("Malzahar", "90", "Malzahar", "玛尔扎哈", 
                   Arrays.asList("中单", "法师", "虚空先知", "玛尔扎哈"), "虚空先知", 
                   Arrays.asList("Mage", "Assassin"));
        
        addChampion("Maokai", "57", "Maokai", "茂凯", 
                   Arrays.asList("上单", "辅助", "坦克", "扭曲树精", "茂凯", "大树"), "扭曲树精", 
                   Arrays.asList("Tank", "Mage"));
        
        addChampion("MasterYi", "11", "Master Yi", "易", 
                   Arrays.asList("打野", "剑圣", "无极剑圣", "易"), "无极剑圣", 
                   Arrays.asList("Assassin", "Fighter"));
        
        addChampion("Milio", "902", "Milio", "米利欧", 
                   Arrays.asList("辅助", "温柔之火", "米利欧"), "温柔之火", 
                   Arrays.asList("Support", "Mage"));
        
        addChampion("MissFortune", "21", "Miss Fortune", "厄运小姐", 
                   Arrays.asList("adc", "射手", "赏金猎人", "厄运小姐", "女枪"), "赏金猎人", 
                   Arrays.asList("Marksman"));
        
        addChampion("Mordekaiser", "82", "Mordekaiser", "莫德凯撒", 
                   Arrays.asList("上单", "战士", "铁铠冥魂", "莫德凯撒"), "铁铠冥魂", 
                   Arrays.asList("Fighter"));
        
        addChampion("Morgana", "25", "Morgana", "莫甘娜", 
                   Arrays.asList("辅助", "中单", "法师", "堕落天使", "莫甘娜"), "堕落天使", 
                   Arrays.asList("Mage", "Support"));
        
        // N系列英雄
        addChampion("Nami", "267", "Nami", "娜美", 
                   Arrays.asList("辅助", "唤潮鲛姬", "娜美"), "唤潮鲛姬", 
                   Arrays.asList("Support", "Mage"));
        
        addChampion("Nasus", "75", "Nasus", "内瑟斯", 
                   Arrays.asList("上单", "战士", "沙漠死神", "内瑟斯", "狗头"), "沙漠死神", 
                   Arrays.asList("Fighter", "Tank"));
        
        addChampion("Nautilus", "111", "Nautilus", "诺提勒斯", 
                   Arrays.asList("辅助", "打野", "坦克", "深海泰坦", "诺提勒斯"), "深海泰坦", 
                   Arrays.asList("Tank", "Fighter"));
        
        addChampion("Neeko", "518", "Neeko", "妮蔻", 
                   Arrays.asList("中单", "辅助", "法师", "万花通灵", "妮蔻"), "万花通灵", 
                   Arrays.asList("Mage", "Support"));
        
        addChampion("Nidalee", "76", "Nidalee", "奈德丽", 
                   Arrays.asList("打野", "中单", "法师", "狂野女猎手", "奈德丽", "豹女"), "狂野女猎手", 
                   Arrays.asList("Assassin", "Mage"));
        
        addChampion("Nilah", "895", "Nilah", "尼菈", 
                   Arrays.asList("adc", "射手", "欢愉之水", "尼菈"), "欢愉之水", 
                   Arrays.asList("Marksman", "Assassin"));
        
        addChampion("Nocturne", "56", "Nocturne", "诺克萨斯", 
                   Arrays.asList("打野", "刺客", "永恒梦魇", "诺克萨斯"), "永恒梦魇", 
                   Arrays.asList("Assassin", "Fighter"));
        
        addChampion("Nunu", "20", "Nunu & Willump", "努努和威朗普", 
                   Arrays.asList("打野", "坦克", "雪原双子", "努努和威朗普", "努努"), "雪原双子", 
                   Arrays.asList("Tank", "Fighter"));
        
        // O系列英雄
        addChampion("Olaf", "2", "Olaf", "奥拉夫", 
                   Arrays.asList("打野", "上单", "战士", "狂战士", "奥拉夫"), "狂战士", 
                   Arrays.asList("Fighter", "Tank"));
        
        addChampion("Orianna", "61", "Orianna", "奥莉安娜", 
                   Arrays.asList("中单", "法师", "发条魔灵", "奥莉安娜", "发条"), "发条魔灵", 
                   Arrays.asList("Mage", "Support"));
        
        addChampion("Ornn", "516", "Ornn", "奥恩", 
                   Arrays.asList("上单", "坦克", "山隐之焰", "奥恩"), "山隐之焰", 
                   Arrays.asList("Tank", "Fighter"));
        
        // P系列英雄
        addChampion("Pantheon", "80", "Pantheon", "潘森", 
                   Arrays.asList("上单", "中单", "辅助", "不屈之枪", "潘森"), "不屈之枪", 
                   Arrays.asList("Fighter", "Assassin"));
        
        addChampion("Poppy", "78", "Poppy", "波比", 
                   Arrays.asList("上单", "打野", "坦克", "圣锤之毅", "波比"), "圣锤之毅", 
                   Arrays.asList("Tank", "Fighter"));
        
        addChampion("Pyke", "555", "Pyke", "派克", 
                   Arrays.asList("辅助", "刺客", "血港鬼影", "派克"), "血港鬼影", 
                   Arrays.asList("Support", "Assassin"));
        
        // Q系列英雄
        addChampion("Qiyana", "246", "Qiyana", "奇亚娜", 
                   Arrays.asList("中单", "打野", "刺客", "元素女皇", "奇亚娜"), "元素女皇", 
                   Arrays.asList("Assassin", "Fighter"));
        
        addChampion("Quinn", "133", "Quinn", "奎因", 
                   Arrays.asList("上单", "adc", "射手", "德玛西亚雄鹰", "奎因"), "德玛西亚雄鹰", 
                   Arrays.asList("Marksman", "Assassin"));
        
        // R系列英雄
        addChampion("Rakan", "497", "Rakan", "洛", 
                   Arrays.asList("辅助", "幻翎", "洛"), "幻翎", 
                   Arrays.asList("Support"));
        
        addChampion("Rammus", "33", "Rammus", "拉莫斯", 
                   Arrays.asList("打野", "坦克", "披甲龙龟", "拉莫斯", "龙龟"), "披甲龙龟", 
                   Arrays.asList("Tank", "Fighter"));
        
        addChampion("RekSai", "421", "Rek'Sai", "雷克塞", 
                   Arrays.asList("打野", "战士", "虚空遁地兽", "雷克塞", "挖掘机"), "虚空遁地兽", 
                   Arrays.asList("Fighter"));
        
        addChampion("Rell", "526", "Rell", "芮尔", 
                   Arrays.asList("辅助", "坦克", "铁铠少女", "芮尔"), "铁铠少女", 
                   Arrays.asList("Tank", "Fighter"));
        
        addChampion("Renata", "888", "Renata Glasc", "烬娜塔·戈拉斯克", 
                   Arrays.asList("辅助", "炼金术士", "烬娜塔·戈拉斯克", "烬娜塔"), "炼金术士", 
                   Arrays.asList("Support", "Mage"));
        
        addChampion("Renekton", "58", "Renekton", "雷克顿", 
                   Arrays.asList("上单", "战士", "荒漠屠夫", "雷克顿", "鳄鱼"), "荒漠屠夫", 
                   Arrays.asList("Fighter", "Tank"));
        
        addChampion("Rengar", "107", "Rengar", "雷恩加尔", 
                   Arrays.asList("打野", "上单", "刺客", "傲之追猎者", "雷恩加尔", "狮子狗"), "傲之追猎者", 
                   Arrays.asList("Assassin", "Fighter"));
        
        addChampion("Riven", "92", "Riven", "锐雯", 
                   Arrays.asList("上单", "战士", "放逐之刃", "锐雯"), "放逐之刃", 
                   Arrays.asList("Fighter", "Assassin"));
        
        addChampion("Rumble", "68", "Rumble", "兰博", 
                   Arrays.asList("上单", "中单", "法师", "机械公敌", "兰博"), "机械公敌", 
                   Arrays.asList("Fighter", "Mage"));
        
        addChampion("Ryze", "13", "Ryze", "瑞兹", 
                   Arrays.asList("中单", "法师", "符文法师", "瑞兹"), "符文法师", 
                   Arrays.asList("Mage", "Fighter"));
        
        // S系列英雄
        addChampion("Samira", "360", "Samira", "莎弥拉", 
                   Arrays.asList("adc", "射手", "沙漠玫瑰", "莎弥拉"), "沙漠玫瑰", 
                   Arrays.asList("Marksman"));
        
        addChampion("Sejuani", "113", "Sejuani", "瑟庄妮", 
                   Arrays.asList("打野", "坦克", "北地之怒", "瑟庄妮", "猪妹"), "北地之怒", 
                   Arrays.asList("Tank", "Fighter"));
        
        addChampion("Senna", "235", "Senna", "赛娜", 
                   Arrays.asList("辅助", "adc", "射手", "涤魂圣枪", "赛娜"), "涤魂圣枪", 
                   Arrays.asList("Marksman", "Support"));
        
        addChampion("Seraphine", "147", "Seraphine", "萨勒芬妮", 
                   Arrays.asList("中单", "辅助", "法师", "星籁歌姬", "萨勒芬妮"), "星籁歌姬", 
                   Arrays.asList("Mage", "Support"));
        
        addChampion("Sett", "875", "Sett", "瑟提", 
                   Arrays.asList("上单", "辅助", "战士", "腕豪", "瑟提"), "腕豪", 
                   Arrays.asList("Fighter", "Tank"));
        
        addChampion("Shaco", "35", "Shaco", "萨科", 
                   Arrays.asList("打野", "辅助", "刺客", "恶魔小丑", "萨科", "小丑"), "恶魔小丑", 
                   Arrays.asList("Assassin"));
        
        addChampion("Shen", "98", "Shen", "慎", 
                   Arrays.asList("上单", "辅助", "坦克", "暮光之眼", "慎"), "暮光之眼", 
                   Arrays.asList("Tank"));
        
        addChampion("Shyvana", "102", "Shyvana", "希瓦娜", 
                   Arrays.asList("打野", "战士", "龙血武姬", "希瓦娜", "龙女"), "龙血武姬", 
                   Arrays.asList("Fighter", "Mage"));
        
        addChampion("Singed", "27", "Singed", "辛吉德", 
                   Arrays.asList("上单", "坦克", "炼金术士", "辛吉德"), "炼金术士", 
                   Arrays.asList("Tank", "Fighter"));
        
        addChampion("Sion", "14", "Sion", "赛恩", 
                   Arrays.asList("上单", "辅助", "坦克", "亡灵战神", "赛恩"), "亡灵战神", 
                   Arrays.asList("Tank", "Fighter"));
        
        addChampion("Sivir", "15", "Sivir", "希维尔", 
                   Arrays.asList("adc", "射手", "战争女神", "希维尔"), "战争女神", 
                   Arrays.asList("Marksman"));
        
        addChampion("Skarner", "72", "Skarner", "斯卡纳", 
                   Arrays.asList("打野", "战士", "水晶先锋", "斯卡纳", "蝎子"), "水晶先锋", 
                   Arrays.asList("Fighter", "Tank"));
        
        addChampion("Smolder", "901", "Smolder", "斯莫德", 
                   Arrays.asList("adc", "射手", "火龙宝宝", "斯莫德"), "火龙宝宝", 
                   Arrays.asList("Marksman", "Mage"));
        
        addChampion("Sona", "37", "Sona", "娑娜", 
                   Arrays.asList("辅助", "琴瑟仙女", "娑娜"), "琴瑟仙女", 
                   Arrays.asList("Support", "Mage"));
        
        addChampion("Soraka", "16", "Soraka", "索拉卡", 
                   Arrays.asList("辅助", "众星之子", "索拉卡"), "众星之子", 
                   Arrays.asList("Support", "Mage"));
        
        addChampion("Swain", "50", "Swain", "斯维因", 
                   Arrays.asList("中单", "辅助", "法师", "诺克萨斯统领", "斯维因"), "诺克萨斯统领", 
                   Arrays.asList("Mage", "Fighter"));
        
        addChampion("Sylas", "517", "Sylas", "塞拉斯", 
                   Arrays.asList("中单", "打野", "法师", "解脱者", "塞拉斯"), "解脱者", 
                   Arrays.asList("Mage", "Assassin"));
        
        addChampion("Syndra", "134", "Syndra", "辛德拉", 
                   Arrays.asList("中单", "法师", "暗黑元首", "辛德拉"), "暗黑元首", 
                   Arrays.asList("Mage"));
        
        // T系列英雄
        addChampion("TahmKench", "223", "Tahm Kench", "塔姆·肯奇", 
                   Arrays.asList("辅助", "上单", "坦克", "河流之王", "塔姆·肯奇", "蛤蟆"), "河流之王", 
                   Arrays.asList("Support", "Tank"));
        
        addChampion("Taliyah", "163", "Taliyah", "塔莉垭", 
                   Arrays.asList("中单", "打野", "法师", "岩雀", "塔莉垭"), "岩雀", 
                   Arrays.asList("Mage", "Support"));
        
        addChampion("Talon", "91", "Talon", "泰隆", 
                   Arrays.asList("中单", "打野", "刺客", "刀锋之影", "泰隆"), "刀锋之影", 
                   Arrays.asList("Assassin"));
        
        addChampion("Taric", "44", "Taric", "塔里克", 
                   Arrays.asList("辅助", "瓦洛兰之盾", "塔里克"), "瓦洛兰之盾", 
                   Arrays.asList("Support", "Fighter"));
        
        addChampion("Teemo", "17", "Teemo", "提莫", 
                   Arrays.asList("上单", "射手", "迅捷斥候", "提莫"), "迅捷斥候", 
                   Arrays.asList("Marksman", "Assassin"));
        
        addChampion("Thresh", "412", "Thresh", "锤石", 
                   Arrays.asList("辅助", "钩子", "魂锁典狱长", "锤石"), "魂锁典狱长", 
                   Arrays.asList("Support", "Tank"));
        
        addChampion("Tristana", "18", "Tristana", "崔丝塔娜", 
                   Arrays.asList("adc", "中单", "射手", "麦林炮手", "崔丝塔娜", "小炮"), "麦林炮手", 
                   Arrays.asList("Marksman", "Assassin"));
        
        addChampion("Trundle", "48", "Trundle", "特朗德尔", 
                   Arrays.asList("打野", "上单", "战士", "巨魔之王", "特朗德尔", "巨魔"), "巨魔之王", 
                   Arrays.asList("Fighter", "Tank"));
        
        addChampion("Tryndamere", "23", "Tryndamere", "泰达米尔", 
                   Arrays.asList("上单", "打野", "战士", "蛮族之王", "泰达米尔", "蛮王"), "蛮族之王", 
                   Arrays.asList("Fighter", "Assassin"));
        
        addChampion("TwistedFate", "4", "Twisted Fate", "崔斯特", 
                   Arrays.asList("中单", "法师", "卡牌大师", "崔斯特", "卡牌"), "卡牌大师", 
                   Arrays.asList("Mage"));
        
        addChampion("Twitch", "29", "Twitch", "图奇", 
                   Arrays.asList("adc", "打野", "射手", "瘟疫之源", "图奇", "老鼠"), "瘟疫之源", 
                   Arrays.asList("Marksman", "Assassin"));
        
        // U系列英雄
        addChampion("Udyr", "77", "Udyr", "乌迪尔", 
                   Arrays.asList("打野", "上单", "战士", "兽灵行者", "乌迪尔"), "兽灵行者", 
                   Arrays.asList("Fighter", "Tank"));
        
        addChampion("Urgot", "6", "Urgot", "厄加特", 
                   Arrays.asList("上单", "战士", "无畏战车", "厄加特"), "无畏战车", 
                   Arrays.asList("Fighter", "Tank"));
        
        // V系列英雄
        addChampion("Varus", "110", "Varus", "韦鲁斯", 
                   Arrays.asList("adc", "中单", "射手", "惩戒之箭", "韦鲁斯"), "惩戒之箭", 
                   Arrays.asList("Marksman"));
        
        addChampion("Vayne", "67", "Vayne", "薇恩", 
                   Arrays.asList("adc", "射手", "暗夜猎手", "薇恩"), "暗夜猎手", 
                   Arrays.asList("Marksman", "Assassin"));
        
        addChampion("Veigar", "45", "Veigar", "维迦", 
                   Arrays.asList("中单", "法师", "邪恶小法师", "维迦"), "邪恶小法师", 
                   Arrays.asList("Mage"));
        
        addChampion("Velkoz", "161", "Vel'Koz", "维克兹", 
                   Arrays.asList("中单", "辅助", "法师", "虚空之眼", "维克兹"), "虚空之眼", 
                   Arrays.asList("Mage"));
        
        addChampion("Vex", "711", "Vex", "薇古丝", 
                   Arrays.asList("中单", "法师", "愁云使者", "薇古丝"), "愁云使者", 
                   Arrays.asList("Mage"));
        
        addChampion("Vi", "254", "Vi", "蔚", 
                   Arrays.asList("打野", "战士", "皮城执法官", "蔚"), "皮城执法官", 
                   Arrays.asList("Fighter", "Assassin"));
        
        addChampion("Viego", "234", "Viego", "佛耶戈", 
                   Arrays.asList("打野", "刺客", "破败之王", "佛耶戈"), "破败之王", 
                   Arrays.asList("Assassin", "Fighter"));
        
        addChampion("Viktor", "112", "Viktor", "维克托", 
                   Arrays.asList("中单", "法师", "机械先驱", "维克托"), "机械先驱", 
                   Arrays.asList("Mage"));
        
        addChampion("Vladimir", "8", "Vladimir", "弗拉基米尔", 
                   Arrays.asList("中单", "上单", "法师", "猩红收割者", "弗拉基米尔", "吸血鬼"), "猩红收割者", 
                   Arrays.asList("Mage"));
        
        addChampion("Volibear", "106", "Volibear", "沃利贝尔", 
                   Arrays.asList("打野", "上单", "战士", "雷霆咆哮", "沃利贝尔", "狗熊"), "雷霆咆哮", 
                   Arrays.asList("Fighter", "Tank"));
        
        // W系列英雄
        addChampion("Warwick", "19", "Warwick", "沃里克", 
                   Arrays.asList("打野", "上单", "战士", "祖安怒兽", "沃里克", "狼人"), "祖安怒兽", 
                   Arrays.asList("Fighter", "Tank"));
        
        addChampion("MonkeyKing", "62", "Wukong", "孙悟空", 
                   Arrays.asList("上单", "打野", "战士", "齐天大圣", "孙悟空", "猴子"), "齐天大圣", 
                   Arrays.asList("Fighter", "Tank"));
        
        // X系列英雄
        addChampion("Xayah", "498", "Xayah", "霞", 
                   Arrays.asList("adc", "射手", "逆羽", "霞"), "逆羽", 
                   Arrays.asList("Marksman"));
        
        addChampion("Xerath", "101", "Xerath", "泽拉斯", 
                   Arrays.asList("中单", "辅助", "法师", "远古巫灵", "泽拉斯"), "远古巫灵", 
                   Arrays.asList("Mage"));
        
        addChampion("XinZhao", "5", "Xin Zhao", "赵信", 
                   Arrays.asList("打野", "战士", "德玛西亚总管", "赵信"), "德玛西亚总管", 
                   Arrays.asList("Fighter", "Assassin"));
        
        // Y系列英雄
        addChampion("Yasuo", "157", "Yasuo", "亚索", 
                   Arrays.asList("中单", "剑客", "快乐风男", "亚索"), "疾风剑豪", 
                   Arrays.asList("Fighter", "Assassin"));
        
        addChampion("Yone", "777", "Yone", "永恩", 
                   Arrays.asList("中单", "上单", "刺客", "封魔剑魂", "永恩"), "封魔剑魂", 
                   Arrays.asList("Assassin", "Fighter"));
        
        addChampion("Yorick", "83", "Yorick", "约里克", 
                   Arrays.asList("上单", "战士", "牧魂人", "约里克"), "牧魂人", 
                   Arrays.asList("Fighter", "Tank"));
        
        addChampion("Yuumi", "350", "Yuumi", "悠米", 
                   Arrays.asList("辅助", "魔法猫咪", "悠米"), "魔法猫咪", 
                   Arrays.asList("Support", "Mage"));
        
        // Z系列英雄
        addChampion("Zac", "154", "Zac", "扎克", 
                   Arrays.asList("打野", "上单", "坦克", "生化魔人", "扎克"), "生化魔人", 
                   Arrays.asList("Tank", "Fighter"));
        
        addChampion("Zed", "238", "Zed", "劫", 
                   Arrays.asList("中单", "刺客", "影流之主", "劫"), "影流之主", 
                   Arrays.asList("Assassin"));
        
        addChampion("Zeri", "221", "Zeri", "泽丽", 
                   Arrays.asList("adc", "射手", "祖安花火", "泽丽"), "祖安花火", 
                   Arrays.asList("Marksman"));
        
        addChampion("Ziggs", "115", "Ziggs", "吉格斯", 
                   Arrays.asList("中单", "adc", "法师", "爆破鬼才", "吉格斯"), "爆破鬼才", 
                   Arrays.asList("Mage"));
        
        addChampion("Zilean", "26", "Zilean", "基兰", 
                   Arrays.asList("中单", "辅助", "法师", "时光守护者", "基兰"), "时光守护者", 
                   Arrays.asList("Support", "Mage"));
        
        addChampion("Zoe", "142", "Zoe", "佐伊", 
                   Arrays.asList("中单", "法师", "暮光星灵", "佐伊"), "暮光星灵", 
                   Arrays.asList("Mage", "Support"));
        
        addChampion("Zyra", "143", "Zyra", "婕拉", 
                   Arrays.asList("辅助", "中单", "法师", "荆棘之兴", "婕拉"), "荆棘之兴", 
                   Arrays.asList("Mage", "Support"));
        
        // 新增英雄
        addChampion("Ambessa", "799", "Ambessa", "安蓓莎", 
                   Arrays.asList("上单", "战士", "刺客", "战争主母", "安蓓莎"), "战争主母", 
                   Arrays.asList("Fighter", "Assassin"));
        
        addChampion("KSante", "897", "K'Sante", "奎桑提", 
                   Arrays.asList("上单", "坦克", "战士", "纳祖马的傲慢", "奎桑提"), "纳祖马的傲慢", 
                   Arrays.asList("Tank", "Fighter"));
        
        addChampion("Mel", "800", "Mel", "梅尔", 
                   Arrays.asList("中单", "辅助", "法师", "灵魂映像", "梅尔"), "灵魂映像", 
                   Arrays.asList("Mage", "Support"));
        
        addChampion("Naafiri", "950", "Naafiri", "纳菲利", 
                   Arrays.asList("中单", "刺客", "战士", "百咬之犬", "纳菲利"), "百咬之犬", 
                   Arrays.asList("Assassin", "Fighter"));
        
        addChampion("Yunara", "804", "Yunara", "芸阿娜", 
                   Arrays.asList("adc", "射手", "不破之誓", "芸阿娜"), "不破之誓", 
                   Arrays.asList("Marksman"));
        
        logger.info("Initialized {} champions", allChampions.size());
    }
    
    private void addChampion(String key, String id, String nameEn, String nameCn, 
                           List<String> keywords, String title, List<String> tags) {
        Champion champion = new Champion(key, id, nameEn, nameCn, keywords, title, tags);
        allChampions.add(champion);
        championMap.put(key.toLowerCase(), champion);
        championMap.put(nameCn.toLowerCase(), champion);
        championMap.put(nameEn.toLowerCase(), champion);
    }
    
    public List<Champion> getAllChampions() {
        return new ArrayList<>(allChampions);
    }
    
    public List<Champion> searchChampions(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllChampions();
        }
        
        String lowercaseQuery = query.toLowerCase().trim();
        
        return allChampions.stream()
                .filter(champion -> matchesQuery(champion, lowercaseQuery))
                .collect(Collectors.toList());
    }
    
    private boolean matchesQuery(Champion champion, String query) {
        // 精确匹配英雄名称
        if (champion.getNameCn().toLowerCase().contains(query) ||
            champion.getNameEn().toLowerCase().contains(query) ||
            champion.getKey().toLowerCase().contains(query)) {
            return true;
        }
        
        // 匹配称号
        if (champion.getTitle() != null && champion.getTitle().toLowerCase().contains(query)) {
            return true;
        }
        
        // 匹配关键词
        if (champion.getKeywords() != null) {
            for (String keyword : champion.getKeywords()) {
                if (keyword.toLowerCase().contains(query)) {
                    return true;
                }
            }
        }
        
        // 匹配标签
        if (champion.getTags() != null) {
            for (String tag : champion.getTags()) {
                if (tag.toLowerCase().contains(query)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public Champion getChampionByKey(String key) {
        if (key == null) {
            return null;
        }
        return championMap.get(key.toLowerCase());
    }
    
    public List<Champion> getChampionsByTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String lowercaseTag = tag.toLowerCase().trim();
        return allChampions.stream()
                .filter(champion -> champion.getTags() != null && 
                        champion.getTags().stream()
                                .anyMatch(t -> t.toLowerCase().equals(lowercaseTag)))
                .collect(Collectors.toList());
    }
    
    public List<String> getAllTags() {
        return allChampions.stream()
                .filter(champion -> champion.getTags() != null)
                .flatMap(champion -> champion.getTags().stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
    
    public List<String> getSearchSuggestions(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String lowercaseQuery = query.toLowerCase().trim();
        Set<String> suggestions = new HashSet<>();
        
        for (Champion champion : allChampions) {
            // 添加英雄名称建议
            if (champion.getNameCn().toLowerCase().startsWith(lowercaseQuery)) {
                suggestions.add(champion.getNameCn());
            }
            if (champion.getNameEn().toLowerCase().startsWith(lowercaseQuery)) {
                suggestions.add(champion.getNameEn());
            }
            
            // 添加关键词建议
            if (champion.getKeywords() != null) {
                for (String keyword : champion.getKeywords()) {
                    if (keyword.toLowerCase().startsWith(lowercaseQuery)) {
                        suggestions.add(keyword);
                    }
                }
            }
        }
        
        return suggestions.stream()
                .sorted()
                .limit(10)
                .collect(Collectors.toList());
    }
    
    public int getChampionCount() {
        return allChampions.size();
    }
}