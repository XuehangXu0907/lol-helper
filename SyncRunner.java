import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;

public class SyncRunner {
    public static void main(String[] args) {
        try {
            // 添加Maven依赖到classpath
            String userHome = System.getProperty("user.home");
            String[] jarPaths = {
                "target/classes",
                userHome + "/.m2/repository/ch/qos/logback/logback-classic/1.4.14/logback-classic-1.4.14.jar",
                userHome + "/.m2/repository/ch/qos/logback/logback-core/1.4.14/logback-core-1.4.14.jar",
                userHome + "/.m2/repository/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar",
                userHome + "/.m2/repository/com/squareup/okhttp3/okhttp/4.12.0/okhttp-4.12.0.jar",
                userHome + "/.m2/repository/com/squareup/okio/okio/3.4.0/okio-3.4.0.jar",
                userHome + "/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.16.1/jackson-core-2.16.1.jar",
                userHome + "/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.16.1/jackson-databind-2.16.1.jar",
                userHome + "/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.16.1/jackson-annotations-2.16.1.jar",
                userHome + "/.m2/repository/org/jetbrains/kotlin/kotlin-stdlib/1.8.20/kotlin-stdlib-1.8.20.jar",
                userHome + "/.m2/repository/org/jetbrains/kotlin/kotlin-stdlib-common/1.8.20/kotlin-stdlib-common-1.8.20.jar",
                userHome + "/.m2/repository/org/jetbrains/annotations/13.0/annotations-13.0.jar"
            };
            
            URL[] urls = new URL[jarPaths.length];
            for (int i = 0; i < jarPaths.length; i++) {
                File file = new File(jarPaths[i]);
                urls[i] = file.toURI().toURL();
            }
            
            URLClassLoader loader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());
            Class<?> appClass = loader.loadClass("com.lol.championselector.DataSyncApp");
            Method mainMethod = appClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) new String[0]);
            
        } catch (Exception e) {
            System.out.println("Failed to run sync: " + e.getMessage());
            e.printStackTrace();
        }
    }
}