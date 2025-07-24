package com.lol.championselector;

import com.lol.championselector.config.ChampionSelectorConfig;
import com.lol.championselector.controller.AutoAcceptController;
import com.lol.championselector.manager.SystemTrayManager;
import com.lol.championselector.manager.ResourceManager;
import com.lol.championselector.util.SafePlatformUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class ChampionSelectorApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(ChampionSelectorApplication.class);
    
    private ChampionSelectorConfig config;
    private AutoAcceptController controller;
    private SystemTrayManager systemTrayManager;
    private boolean startMinimized = false;
    private boolean isExiting = false;
    private boolean minimizeOnClose = true; // 默认关闭窗口时最小化到托盘
    
    public static void main(String[] args) {
        // 设置系统属性以优化JavaFX性能
        System.setProperty("javafx.animation.fullspeed", "true");
        System.setProperty("javafx.animation.pulse", "60");
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("prism.subpixeltext", "false");
        
        // 设置应用程序名称相关属性
        System.setProperty("java.awt.headless", "false");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("file.encoding", "UTF-8");
        
        // 设置应用程序名称和标识
        System.setProperty("java.awt.application.name", "LOL Helper");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "LOL Helper");
        
        logger.info("Starting League of Legends Champion Selector Application");
        launch(args);
    }
    
    @Override
    public void init() throws Exception {
        super.init();
        
        // 解析命令行参数
        List<String> parameters = getParameters().getRaw();
        startMinimized = parameters.contains("--minimized");
        logger.info("Start minimized: {}", startMinimized);
        
        // 初始化配置
        config = new ChampionSelectorConfig();
        if (!config.isValid()) {
            throw new IllegalStateException("Invalid configuration");
        }
        
        logger.info("Application initialized with config: {}", config);
    }
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // 禁用JavaFX的隐式退出，让我们自己控制生命周期
            Platform.setImplicitExit(false);
            
            // 加载FXML布局
            FXMLLoader fxmlLoader = new FXMLLoader();
            URL fxmlUrl = getClass().getResource("/fxml/AutoAcceptView.fxml");
            if (fxmlUrl == null) {
                throw new IOException("Cannot find AutoAcceptView.fxml");
            }
            fxmlLoader.setLocation(fxmlUrl);
            
            Scene scene = new Scene(fxmlLoader.load(), 500, 650);
            
            // 获取控制器引用
            controller = fxmlLoader.getController();
            
            // 加载CSS样式
            URL cssUrl = getClass().getResource("/css/auto-accept.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                logger.warn("Cannot find auto-accept.css");
            }
            
            // 配置主窗口
            setupPrimaryStage(primaryStage, scene);
            
            // 初始化系统托盘
            initializeSystemTray(primaryStage);
            
            // 连接SystemTrayManager到AutoAcceptController
            if (controller != null && systemTrayManager != null) {
                controller.setSystemTrayManager(systemTrayManager);
            }
            
            // 连接Application到AutoAcceptController
            if (controller != null) {
                controller.setApplication(this);
            }
            
            // 根据启动参数决定是否显示窗口
            if (startMinimized && systemTrayManager != null && systemTrayManager.isSupported()) {
                logger.info("Starting minimized to system tray");
                systemTrayManager.hideWindow();
                systemTrayManager.showInfo("LOL Helper", "程序已启动并最小化到系统托盘");
            } else {
                primaryStage.show();
            }
            
            logger.info("Application started successfully");
            
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            showErrorAndExit("应用程序启动失败", e.getMessage());
        }
    }
    
    private void setupPrimaryStage(Stage primaryStage, Scene scene) {
        // 设置窗口标题为LOL Helper而不是配置文件中的名称
        primaryStage.setTitle("LOL Helper v" + config.getVersion());
        primaryStage.setScene(scene);
        
        // 设置窗口图标
        try {
            URL iconUrl = getClass().getResource("/icon.png");
            if (iconUrl != null) {
                primaryStage.getIcons().add(new Image(iconUrl.toExternalForm()));
            }
        } catch (Exception e) {
            logger.debug("Could not load application icon", e);
        }
        
        // 设置最小窗口大小
        primaryStage.setMinWidth(450);
        primaryStage.setMinHeight(600);
        
        // 设置窗口关闭事件
        primaryStage.setOnCloseRequest(event -> {
            logger.info("Application close requested, isExiting: {}, minimizeOnClose: {}", isExiting, minimizeOnClose);
            
            if (isExiting) {
                // 已经在退出过程中，允许窗口关闭
                logger.info("Application is exiting, allowing window to close");
                return;
            }
            
            if (minimizeOnClose && systemTrayManager != null && systemTrayManager.isSupported()) {
                // 阻止窗口关闭，最小化到托盘
                event.consume();
                logger.info("Minimizing to system tray instead of closing");
                
                // 防止JavaFX Platform自动退出
                Platform.setImplicitExit(false);
                
                systemTrayManager.hideWindow();
                systemTrayManager.showInfo("LOL Helper", "程序已最小化到系统托盘，双击托盘图标可重新显示");
            } else {
                // 用户选择直接退出或托盘不可用
                logger.info("Closing application directly");
                requestExit();
            }
        });
        
        // 居中显示
        primaryStage.centerOnScreen();
        
        logger.debug("Primary stage configured: {}x{}", 
                    primaryStage.getWidth(), primaryStage.getHeight());
    }
    
    @Override
    public void stop() throws Exception {
        logger.info("JavaFX Application.stop() called, isExiting: {}", isExiting);
        
        // 只有在明确请求退出时才真正关闭应用程序
        if (isExiting) {
            logger.info("Application is explicitly exiting, proceeding with shutdown");
            shutdown();
            super.stop();
        } else {
            logger.info("Application.stop() called but not exiting - likely due to window close to tray");
            // 不调用shutdown()，让程序继续在后台运行
            // 但是要调用super.stop()来正常关闭 JavaFX 窗口
            super.stop();
        }
    }
    
    private void shutdown() {
        try {
            logger.info("Shutting down application...");
            
            if (controller != null) {
                controller.shutdown();
            }
            
            if (systemTrayManager != null) {
                systemTrayManager.destroy();
            }
            
            // Shutdown resource manager (this will clean up all ExecutorServices and Timelines)
            ResourceManager.getInstance().shutdown();
            
            // Give a moment for cleanup
            Thread.sleep(500);
            
            // 强制退出所有后台线程
            Platform.exit();
            System.exit(0);
            
        } catch (Exception e) {
            logger.error("Error during application shutdown", e);
            System.exit(1);
        }
    }
    
    // 请求退出应用程序
    public void requestExit() {
        logger.info("Exit requested");
        isExiting = true;
        
        // 恢复JavaFX的隐式退出行为
        Platform.setImplicitExit(true);
        
        shutdown();
    }
    
    // 设置关闭窗口时的行为
    public void setMinimizeOnClose(boolean minimize) {
        this.minimizeOnClose = minimize;
        logger.info("Minimize on close set to: {}", minimize);
    }
    
    // 获取当前关闭窗口的行为
    public boolean isMinimizeOnClose() {
        return minimizeOnClose;
    }
    
    private void showErrorAndExit(String title, String message) {
        boolean success = SafePlatformUtil.runLater(() -> {
            try {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle(title);
                alert.setHeaderText("应用程序遇到错误");
                alert.setContentText(message);
                alert.showAndWait();
            } catch (Exception e) {
                logger.error("Failed to show error dialog", e);
            } finally {
                Platform.exit();
                System.exit(1);
            }
        });
        
        if (!success) {
            // Fallback to console output if JavaFX is not available
            System.err.println("Application Error: " + title);
            System.err.println(message);
            System.exit(1);
        }
    }
    
    // 提供配置访问方法
    public ChampionSelectorConfig getConfig() {
        return config;
    }
    
    // 提供控制器访问方法
    public AutoAcceptController getController() {
        return controller;
    }
    
    // 提供系统托盘管理器访问方法
    public SystemTrayManager getSystemTrayManager() {
        return systemTrayManager;
    }
    
    private void initializeSystemTray(Stage primaryStage) {
        try {
            systemTrayManager = new SystemTrayManager(primaryStage);
            
            if (systemTrayManager.isSupported()) {
                systemTrayManager.setOnExitCallback(this::requestExit);
                systemTrayManager.initialize();
                logger.info("System tray initialized successfully");
            } else {
                logger.warn("System tray is not supported on this platform");
                systemTrayManager = null;
            }
            
        } catch (Exception e) {
            logger.error("Failed to initialize system tray", e);
            systemTrayManager = null;
        }
    }
}