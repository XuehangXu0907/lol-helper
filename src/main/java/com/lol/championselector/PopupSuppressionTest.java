package com.lol.championselector;

import com.fasterxml.jackson.databind.JsonNode;
import com.lol.championselector.lcu.LCUConnection;
import com.lol.championselector.lcu.LCUDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * 弹窗抑制功能测试
 * 测试不同的LCU API端点以找到合适的弹窗抑制方法
 */
public class PopupSuppressionTest {
    private static final Logger logger = LoggerFactory.getLogger(PopupSuppressionTest.class);
    
    private LCUConnection connection;
    
    public static void main(String[] args) {
        PopupSuppressionTest test = new PopupSuppressionTest();
        test.runTests();
    }
    
    public void runTests() {
        logger.info("开始弹窗抑制功能测试...");
        
        // 连接到LCU
        LCUDetector.detectLCU()
            .thenCompose(lcuInfoOpt -> {
                if (lcuInfoOpt.isPresent()) {
                    LCUDetector.LCUInfo info = lcuInfoOpt.get();
                    this.connection = new LCUConnection(info.getPort(), info.getPassword());
                    return this.connection.testConnection();
                } else {
                    logger.error("未找到League of Legends客户端，请确保客户端正在运行");
                    return CompletableFuture.completedFuture(false);
                }
            })
            .thenCompose(connected -> {
                if (connected) {
                    logger.info("✓ 成功连接到LCU");
                    return testPopupSuppressionApis();
                } else {
                    logger.error("✗ 连接LCU失败");
                    return CompletableFuture.completedFuture(null);
                }
            })
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.error("测试过程中发生错误", throwable);
                }
                
                if (connection != null) {
                    connection.shutdown();
                }
                
                logger.info("测试完成");
                
                // 延迟后退出，确保日志输出完成
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.exit(0);
            });
        
        // 等待异步操作完成
        try {
            Thread.sleep(30000); // 等待30秒
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private CompletableFuture<Void> testPopupSuppressionApis() {
        logger.info("\n=== 测试弹窗抑制相关API ===");
        
        return CompletableFuture.allOf(
            // 1. 测试客户端UX状态控制
            testUxStateApis(),
            
            // 2. 测试窗口控制API
            testWindowControlApis(),
            
            // 3. 测试通知系统API
            testNotificationApis(),
            
            // 4. 测试游戏状态API
            testGameStateApis(),
            
            // 5. 测试设置相关API
            testSettingsApis()
        );
    }
    
    private CompletableFuture<Void> testUxStateApis() {
        logger.info("\n--- 测试UX状态控制API ---");
        
        return connection.get("/riotclient/ux-state")
            .thenCompose(response -> {
                logger.info("当前UX状态: {}", formatResponse(response));
                
                // 测试不同的UX状态请求
                return CompletableFuture.allOf(
                    testEndpoint("GET", "/riotclient/ux-state/request", null),
                    testEndpoint("GET", "/riotclient/region-locale", null),
                    testEndpoint("GET", "/riotclient/zoom-scale", null)
                );
            });
    }
    
    private CompletableFuture<Void> testWindowControlApis() {
        logger.info("\n--- 测试窗口控制API ---");
        
        return CompletableFuture.allOf(
            // 注意：这些API可能会影响客户端窗口，谨慎使用
            testEndpoint("GET", "/riotclient/ux-minimize", null),
            testEndpoint("GET", "/riotclient/ux-show", null),
            testEndpoint("GET", "/riotclient/system-info/v1/basic-info", null)
        );
    }
    
    private CompletableFuture<Void> testNotificationApis() {
        logger.info("\n--- 测试通知系统API ---");
        
        return CompletableFuture.allOf(
            testEndpoint("GET", "/lol-notifications/v1/notifications", null),
            testEndpoint("GET", "/lol-platform-config/v1/namespaces/Notifications", null),
            testEndpoint("GET", "/lol-client-config/v3/client-config", null)
        );
    }
    
    private CompletableFuture<Void> testGameStateApis() {
        logger.info("\n--- 测试游戏状态API ---");
        
        return CompletableFuture.allOf(
            testEndpoint("GET", "/lol-gameflow/v1/gameflow-phase", null),
            testEndpoint("GET", "/lol-gameflow/v1/availability", null),
            testEndpoint("GET", "/lol-gameflow/v1/session", null),
            testEndpoint("GET", "/lol-matchmaking/v1/ready-check", null),
            testEndpoint("GET", "/lol-champ-select/v1/session", null)
        );
    }
    
    private CompletableFuture<Void> testSettingsApis() {
        logger.info("\n--- 测试设置相关API ---");
        
        return CompletableFuture.allOf(
            testEndpoint("GET", "/lol-settings/v2/local", null),
            testEndpoint("GET", "/lol-settings/v1/local", null),
            testEndpoint("GET", "/lol-platform-config/v1/namespaces", null),
            testEndpoint("GET", "/lol-platform-config/v1/namespaces/LolClientConfig", null)
        );
    }
    
    private CompletableFuture<Void> testEndpoint(String method, String endpoint, Object body) {
        CompletableFuture<JsonNode> request;
        
        switch (method.toUpperCase()) {
            case "GET":
                request = connection.get(endpoint);
                break;
            case "POST":
                request = connection.post(endpoint, body);
                break;
            case "PUT":
                request = connection.put(endpoint, body);
                break;
            case "PATCH":
                request = connection.patch(endpoint, body);
                break;
            default:
                logger.warn("不支持的HTTP方法: {}", method);
                return CompletableFuture.completedFuture(null);
        }
        
        return request
            .handle((response, throwable) -> {
                if (throwable != null) {
                    logger.debug("  ✗ {} {} - 错误: {}", method, endpoint, throwable.getMessage());
                } else if (response != null && !response.isMissingNode()) {
                    logger.info("  ✓ {} {} - 数据: {}", method, endpoint, formatResponse(response));
                } else {
                    logger.debug("  - {} {} - 无数据", method, endpoint);
                }
                return null;
            });
    }
    
    private String formatResponse(JsonNode response) {
        if (response == null || response.isMissingNode()) {
            return "无数据";
        }
        
        String jsonStr = response.toString();
        // 限制输出长度
        if (jsonStr.length() > 150) {
            return jsonStr.substring(0, 150) + "... (省略)";
        }
        return jsonStr;
    }
}