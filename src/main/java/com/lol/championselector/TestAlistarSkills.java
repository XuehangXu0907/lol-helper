package com.lol.championselector;

import com.lol.championselector.manager.LocalSkillDataManager;
import com.lol.championselector.model.Champion;
import com.lol.championselector.model.ChampionSkills;
import com.lol.championselector.model.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 测试Alistar技能数据加载
 */
public class TestAlistarSkills {
    private static final Logger logger = LoggerFactory.getLogger(TestAlistarSkills.class);
    
    public static void main(String[] args) {
        logger.info("Testing Alistar skill data loading...");
        
        LocalSkillDataManager manager = new LocalSkillDataManager();
        
        try {
            // 创建Alistar英雄
            Champion alistar = new Champion();
            alistar.setKey("Alistar");
            alistar.setNameEn("Alistar");
            alistar.setNameCn("牛头酋长");
            alistar.setTitle("阿利斯塔");
            
            // 加载技能数据
            ChampionSkills skills = manager.getChampionSkills(alistar).get();
            
            if (skills != null && !skills.isEmpty()) {
                logger.info("成功加载 {} 的技能数据:", alistar.getNameCn());
                logger.info("数据来源: {}", skills.getDataSource());
                logger.info("技能数量: {}", skills.getAllSkills().size());
                
                List<Skill> allSkills = skills.getAllSkills();
                for (int i = 0; i < allSkills.size(); i++) {
                    Skill skill = allSkills.get(i);
                    logger.info("\n技能 {}: {}", i + 1, skill.getName());
                    logger.info("  伤害: {}", skill.getDamage());
                    logger.info("  系数: {}", skill.getRatio());
                    logger.info("  等级: {}", skill.getMaxRank());
                    logger.info("  描述: {}", skill.getDescription().substring(0, Math.min(100, skill.getDescription().length())) + "...");
                }
            } else {
                logger.error("未能加载 {} 的技能数据", alistar.getNameCn());
            }
            
        } catch (Exception e) {
            logger.error("测试失败", e);
        } finally {
            manager.shutdown();
        }
    }
}