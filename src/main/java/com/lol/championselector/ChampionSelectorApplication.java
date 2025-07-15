package com.lol.championselector;

import com.lol.championselector.config.ChampionSelectorConfig;
import com.lol.championselector.controller.AutoAcceptController;
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

public class ChampionSelectorApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(ChampionSelectorApplication.class);
    
    private ChampionSelectorConfig config;
    private AutoAcceptController controller;
    
    public static void main(String[] args) {
        // 设置系统属性以优化JavaFX性能
        System.setProperty("javafx.animation.fullspeed", "true");
        System.setProperty("javafx.animation.pulse", "60");
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("prism.subpixeltext", "false");
        
        logger.info("Starting League of Legends Champion Selector Application");
        launch(args);
    }
    
    @Override
    public void init() throws Exception {
        super.init();
        
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
            // 加载FXML布局
            FXMLLoader fxmlLoader = new FXMLLoader();
            URL fxmlUrl = getClass().getResource("/fxml/AutoAcceptView.fxml");
            if (fxmlUrl == null) {
                throw new IOException("Cannot find AutoAcceptView.fxml");
            }
            fxmlLoader.setLocation(fxmlUrl);
            
            Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
            
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
            
            // 显示窗口
            primaryStage.show();
            
            logger.info("Application started successfully");
            
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            showErrorAndExit("应用程序启动失败", e.getMessage());
        }
    }
    
    private void setupPrimaryStage(Stage primaryStage, Scene scene) {
        primaryStage.setTitle(config.getApplicationTitle() + " v" + config.getVersion());
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
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        
        // 设置窗口关闭事件
        primaryStage.setOnCloseRequest(event -> {
            logger.info("Application close requested");
            shutdown();
        });
        
        // 居中显示
        primaryStage.centerOnScreen();
        
        logger.debug("Primary stage configured: {}x{}", 
                    primaryStage.getWidth(), primaryStage.getHeight());
    }
    
    @Override
    public void stop() throws Exception {
        shutdown();
        super.stop();
    }
    
    private void shutdown() {
        try {
            logger.info("Shutting down application...");
            
            if (controller != null) {
                controller.shutdown();
            }
            
            // 强制退出所有后台线程
            Platform.exit();
            System.exit(0);
            
        } catch (Exception e) {
            logger.error("Error during application shutdown", e);
            System.exit(1);
        }
    }
    
    private void showErrorAndExit(String title, String message) {
        Platform.runLater(() -> {
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
    }
    
    // 提供配置访问方法
    public ChampionSelectorConfig getConfig() {
        return config;
    }
    
    // 提供控制器访问方法
    public AutoAcceptController getController() {
        return controller;
    }
}