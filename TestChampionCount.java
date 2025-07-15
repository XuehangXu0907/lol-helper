import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;

public class TestChampionCount {
    public static void main(String[] args) {
        try {
            // 添加Maven依赖到classpath
            String userHome = System.getProperty("user.home");
            String[] jarPaths = {
                "target/classes",
                userHome + "/.m2/repository/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar",
                userHome + "/.m2/repository/ch/qos/logback/logback-classic/1.4.14/logback-classic-1.4.14.jar",
                userHome + "/.m2/repository/ch/qos/logback/logback-core/1.4.14/logback-core-1.4.14.jar"
            };
            
            URL[] urls = new URL[jarPaths.length];
            for (int i = 0; i < jarPaths.length; i++) {
                File file = new File(jarPaths[i]);
                urls[i] = file.toURI().toURL();
            }
            
            URLClassLoader loader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());
            
            // 测试原始ChampionDataManager
            Class<?> oldManagerClass = loader.loadClass("com.lol.championselector.manager.ChampionDataManager");
            Object oldManager = oldManagerClass.getDeclaredConstructor().newInstance();
            Method getCountMethod = oldManagerClass.getMethod("getChampionCount");
            int oldCount = (Integer) getCountMethod.invoke(oldManager);
            
            // 测试新的ChampionDataManagerNew
            Class<?> newManagerClass = loader.loadClass("com.lol.championselector.manager.ChampionDataManagerNew");
            Object newManager = newManagerClass.getDeclaredConstructor().newInstance();
            Method getCountMethodNew = newManagerClass.getMethod("getChampionCount");
            int newCount = (Integer) getCountMethodNew.invoke(newManager);
            
            System.out.println("Original ChampionDataManager: " + oldCount + " champions");
            System.out.println("New ChampionDataManagerNew: " + newCount + " champions");
            System.out.println("Difference: " + (newCount - oldCount) + " champions");
            
            if (oldCount == 171) {
                System.out.println("SUCCESS: Original manager now has 171 champions!");
            } else {
                System.out.println("ISSUE: Original manager has " + oldCount + " champions, expected 171");
            }
            
        } catch (Exception e) {
            System.out.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}