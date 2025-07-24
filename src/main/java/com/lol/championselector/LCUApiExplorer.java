package com.lol.championselector;

import com.fasterxml.jackson.databind.JsonNode;
import com.lol.championselector.lcu.LCUConnection;
import com.lol.championselector.lcu.LCUDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * LCU API Explorer - 探索可用的LCU API端点以实现弹窗抑制功能
 * 重点关注窗口控制、UI状态和客户端行为相关的API
 */
public class LCUApiExplorer {
    private static final Logger logger = LoggerFactory.getLogger(LCUApiExplorer.class);
    
    private LCUConnection connection;
    
    public static void main(String[] args) {
        // 设置系统属性，避免JavaFX冲突
        System.setProperty("javafx.animation.fullspeed", "false");
        System.setProperty("java.awt.headless", "true");
        
        LCUApiExplorer explorer = new LCUApiExplorer();
        try {
            explorer.exploreApis().get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("API exploration failed", e);
        }
        
        // 确保程序能正常退出
        System.exit(0);
    }
    
    public CompletableFuture<Void> exploreApis() {
        return connectToLCU()
            .thenCompose(connected -> {
                if (connected) {
                    logger.info("✓ Connected to LCU successfully!");
                    return exploreWindowAndUiApis();
                } else {
                    logger.error("✗ Failed to connect to LCU. Please ensure League of Legends client is running.");
                    return CompletableFuture.completedFuture(null);
                }
            });
    }
    
    private CompletableFuture<Boolean> connectToLCU() {
        return LCUDetector.detectLCU()
            .thenCompose(lcuInfoOpt -> {
                if (lcuInfoOpt.isPresent()) {
                    LCUDetector.LCUInfo info = lcuInfoOpt.get();
                    this.connection = new LCUConnection(info.getPort(), info.getPassword());
                    return this.connection.testConnection();
                } else {
                    return CompletableFuture.completedFuture(false);
                }
            });
    }
    
    private CompletableFuture<Void> exploreWindowAndUiApis() {
        logger.info("==================== LCU API 探索开始 ====================");
        
        return CompletableFuture.allOf(
            // 1. 客户端窗口和UI状态相关API
            exploreClientUxApis(),
            
            // 2. Riot客户端控制API
            exploreRiotClientApis(),
            
            // 3. 游戏流程和状态API
            exploreGameflowApis(),
            
            // 4. 设置和配置API
            exploreSettingsApis(),
            
            // 5. 窗口和显示相关API
            exploreDisplayApis()
        ).thenRun(() -> {
            logger.info("==================== LCU API 探索结束 ====================");
            if (connection != null) {
                connection.shutdown();
            }
        });
    }
    
    private CompletableFuture<Void> exploreClientUxApis() {
        logger.info("\n--- 探索客户端UX相关API ---");
        
        String[] uxEndpoints = {
            "/riotclient/ux-state",           // 客户端UX状态
            "/riotclient/ux-state/request",   // UX状态请求
            "/riotclient/show-swagger",       // 显示API文档
            "/riotclient/region-locale",      // 区域和语言设置
            "/riotclient/app-name",          // 应用名称
            "/riotclient/app-port",          // 应用端口
            "/riotclient/machine-id",        // 机器ID
            "/riotclient/system-info/v1/basic-info"  // 系统基本信息
        };
        
        return exploreEndpoints("UX相关", uxEndpoints);
    }
    
    private CompletableFuture<Void> exploreRiotClientApis() {
        logger.info("\n--- 探索Riot客户端控制API ---");
        
        String[] clientEndpoints = {
            "/riotclient/zoom-scale",         // 缩放比例
            "/riotclient/ux-minimize",        // 最小化窗口
            "/riotclient/ux-show",           // 显示窗口
            "/riotclient/ux-state",          // UX状态
            "/riotclient/get_region_locale", // 获取区域语言
            "/riotclient/unload",            // 卸载
            "/riotclient/kill-and-restart-ux", // 重启UX
            "/riotclient/launch-ux"          // 启动UX
        };
        
        return exploreEndpoints("Riot客户端控制", clientEndpoints);
    }
    
    private CompletableFuture<Void> exploreGameflowApis() {
        logger.info("\n--- 探索游戏流程API ---");
        
        String[] gameflowEndpoints = {
            "/lol-gameflow/v1/gameflow-phase",           // 游戏阶段
            "/lol-gameflow/v1/availability",             // 可用性状态
            "/lol-gameflow/v1/session",                  // 游戏会话
            "/lol-gameflow/v1/watch",                    // 观战
            "/lol-gameflow/v1/spectate",                 // 观战
            "/lol-gameflow/v1/reconnect",                // 重连
            "/lol-gameflow/v1/early-exit",               // 早期退出
            "/lol-gameflow/v1/extra-game-client-args",   // 游戏客户端参数
            "/lol-gameflow/v1/client-received-message"   // 客户端消息
        };
        
        return exploreEndpoints("游戏流程", gameflowEndpoints);
    }
    
    private CompletableFuture<Void> exploreSettingsApis() {
        logger.info("\n--- 探索设置和配置API ---");
        
        String[] settingsEndpoints = {
            "/lol-settings/v1/account",                    // 账户设置
            "/lol-settings/v2/local",                      // 本地设置
            "/lol-settings/v1/local",                      // 本地设置v1
            "/lol-settings/v2/config",                     // 配置设置
            "/lol-platform-config/v1/namespaces",         // 平台配置命名空间
            "/lol-platform-config/v1/namespaces/LolClientConfig", // 客户端配置
            "/lol-platform-config/v1/namespaces/Notifications"   // 通知配置
        };
        
        return exploreEndpoints("设置配置", settingsEndpoints);
    }
    
    private CompletableFuture<Void> exploreDisplayApis() {
        logger.info("\n--- 探索显示和窗口API ---");
        
        String[] displayEndpoints = {
            "/lol-client-config/v3/client-config",       // 客户端配置
            "/lol-notifications/v1/notifications",       // 通知
            "/lol-platform-config/v1/initial-configuration-complete", // 初始配置完成
            "/lol-login/v1/session",                     // 登录会话
            "/lol-rso-auth/v1/authorization",            // RSO授权
            "/lol-rso-auth/configuration/v3/ready-state" // RSO配置就绪状态
        };
        
        return exploreEndpoints("显示窗口", displayEndpoints);
    }
    
    private CompletableFuture<Void> exploreEndpoints(String category, String[] endpoints) {
        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] futures = new CompletableFuture[endpoints.length];
        
        for (int i = 0; i < endpoints.length; i++) {
            final String endpoint = endpoints[i];
            futures[i] = connection.get(endpoint)
                .orTimeout(3, TimeUnit.SECONDS)
                .handle((response, throwable) -> {
                    if (throwable != null) {
                        logger.debug("  ✗ {} - Error: {}", endpoint, throwable.getMessage());
                    } else if (response != null && !response.isMissingNode()) {
                        logger.info("  ✓ {} - 返回数据: {}", endpoint, 
                            formatJsonForLog(response));
                    } else {
                        logger.debug("  - {} - 无数据", endpoint);
                    }
                    return null;
                });
        }
        
        return CompletableFuture.allOf(futures);
    }
    
    private String formatJsonForLog(JsonNode json) {
        if (json == null) return "null";
        
        String jsonStr = json.toString();
        // 限制日志长度，避免过长的输出
        if (jsonStr.length() > 200) {
            return jsonStr.substring(0, 200) + "... (truncated)";
        }
        return jsonStr;
    }
    
    /**
     * 测试特定的弹窗抑制方法
     */
    public CompletableFuture<Void> testPopupSuppressionMethods() {
        logger.info("\n--- 测试弹窗抑制方法 ---");
        
        return CompletableFuture.allOf(
            // 测试最小化窗口
            testMinimizeWindow(),
            
            // 测试UX状态控制
            testUxStateControl(),
            
            // 测试通知控制
            testNotificationControl()
        );
    }
    
    private CompletableFuture<Void> testMinimizeWindow() {
        logger.info("测试窗口最小化功能...");
        
        return connection.post("/riotclient/ux-minimize", null)
            .handle((response, throwable) -> {
                if (throwable != null) {
                    logger.info("  ✗ 窗口最小化失败: {}", throwable.getMessage());
                } else {
                    logger.info("  ✓ 窗口最小化成功");
                }
                return null;
            });
    }
    
    private CompletableFuture<Void> testUxStateControl() {
        logger.info("测试UX状态控制...");
        
        return connection.get("/riotclient/ux-state")
            .handle((response, throwable) -> {
                if (throwable != null) {
                    logger.info("  ✗ 获取UX状态失败: {}", throwable.getMessage());
                } else {
                    logger.info("  ✓ 当前UX状态: {}", formatJsonForLog(response));
                }
                return null;
            });
    }
    
    private CompletableFuture<Void> testNotificationControl() {
        logger.info("测试通知控制...");
        
        return connection.get("/lol-notifications/v1/notifications")
            .handle((response, throwable) -> {
                if (throwable != null) {
                    logger.info("  ✗ 获取通知状态失败: {}", throwable.getMessage());
                } else {
                    logger.info("  ✓ 当前通知状态: {}", formatJsonForLog(response));
                }
                return null;
            });
    }
}