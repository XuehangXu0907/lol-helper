import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;

public class TestImprovedSkills {
    public static void main(String[] args) {
        try {
            String userHome = System.getProperty("user.home");
            String[] jarPaths = {
                "target/classes",
                userHome + "/.m2/repository/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar",
                userHome + "/.m2/repository/ch/qos/logback/logback-classic/1.4.14/logback-classic-1.4.14.jar",
                userHome + "/.m2/repository/ch/qos/logback/logback-core/1.4.14/logback-core-1.4.14.jar",
                userHome + "/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.16.1/jackson-core-2.16.1.jar",
                userHome + "/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.16.1/jackson-databind-2.16.1.jar",
                userHome + "/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.16.1/jackson-annotations-2.16.1.jar"
            };
            
            URL[] urls = new URL[jarPaths.length];
            for (int i = 0; i < jarPaths.length; i++) {
                File file = new File(jarPaths[i]);
                urls[i] = file.toURI().toURL();
            }
            
            URLClassLoader loader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());
            
            // 测试LocalDataManager的技能加载
            Class<?> managerClass = loader.loadClass("com.lol.championselector.manager.LocalDataManager");
            Object manager = managerClass.getDeclaredConstructor().newInstance();
            
            // 加载英雄数据
            Method loadChampionsMethod = managerClass.getMethod("loadChampions");
            boolean loadResult = (Boolean) loadChampionsMethod.invoke(manager);
            
            if (loadResult) {
                System.out.println("✅ 英雄数据加载成功！");
                
                // 测试技能加载
                Method loadSkillsMethod = managerClass.getMethod("loadChampionSkills", String.class);
                
                String[] testChampions = {"Brand", "Lux", "Aatrox"};
                
                for (String champion : testChampions) {
                    System.out.println("\n=== 测试 " + champion + " 技能数据 ===");
                    Object skills = loadSkillsMethod.invoke(manager, champion);
                    
                    if (skills != null) {
                        Class<?> skillsClass = skills.getClass();
                        
                        // 获取技能列表
                        Method getSpellsMethod = skillsClass.getMethod("getSpells");
                        Object spells = getSpellsMethod.invoke(skills);
                        
                        if (spells != null) {
                            System.out.println("技能数据已加载，类型: " + spells.getClass().getSimpleName());
                            
                            // 如果是List，显示技能数量
                            if (spells instanceof java.util.List) {
                                java.util.List<?> spellList = (java.util.List<?>) spells;
                                System.out.println("技能数量: " + spellList.size());
                                
                                // 显示第一个技能的详细信息
                                if (!spellList.isEmpty()) {
                                    Object firstSkill = spellList.get(0);
                                    Class<?> skillClass = firstSkill.getClass();
                                    
                                    try {
                                        Method getNameMethod = skillClass.getMethod("getName");
                                        Method getDamageMethod = skillClass.getMethod("getDamage");
                                        Method getDescriptionMethod = skillClass.getMethod("getDescription");
                                        Method getCooldownMethod = skillClass.getMethod("getCooldown");
                                        
                                        String name = (String) getNameMethod.invoke(firstSkill);
                                        String damage = (String) getDamageMethod.invoke(firstSkill);
                                        String description = (String) getDescriptionMethod.invoke(firstSkill);
                                        String cooldown = (String) getCooldownMethod.invoke(firstSkill);
                                        
                                        System.out.println("  技能名: " + name);
                                        System.out.println("  描述: " + (description.length() > 50 ? description.substring(0, 50) + "..." : description));
                                        System.out.println("  伤害: " + damage);
                                        System.out.println("  冷却: " + cooldown);
                                        
                                    } catch (Exception e) {
                                        System.out.println("  获取技能详情失败: " + e.getMessage());
                                    }
                                }
                            }
                        } else {
                            System.out.println("❌ 技能数据为空");
                        }
                    } else {
                        System.out.println("❌ 无法加载 " + champion + " 的技能数据");
                    }
                }
            } else {
                System.out.println("❌ 英雄数据加载失败");
            }
            
        } catch (Exception e) {
            System.out.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}