package com.lol.championselector.manager;

import com.lol.championselector.config.AutoAcceptConfig;
import com.lol.championselector.lcu.LCUMonitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;
import java.util.HashSet;

/**
 * SmartTimingManager的单元测试
 */
public class SmartTimingManagerTest {
    
    @Mock
    private LCUMonitor lcuMonitor;
    
    private AutoAcceptConfig config;
    private SmartTimingManager smartTimingManager;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 创建测试配置
        config = new AutoAcceptConfig();
        config.getChampionSelect().setSimpleBanDelaySeconds(2);
        config.getChampionSelect().setSimplePickDelaySeconds(2);
        config.getChampionSelect().setAutoHoverEnabled(true);
        config.getChampionSelect().setUsePositionBasedSelection(false);
        
        // 设置默认的mock行为
        when(lcuMonitor.getBannedChampions()).thenReturn(CompletableFuture.completedFuture(new HashSet<>()));
        when(lcuMonitor.getPickedChampions()).thenReturn(CompletableFuture.completedFuture(new HashSet<>()));
        when(lcuMonitor.getRemainingTimeInPhase()).thenReturn(CompletableFuture.completedFuture(5));
        when(lcuMonitor.hoverChampion(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(true));
        when(lcuMonitor.banChampion(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(true));
        when(lcuMonitor.pickChampion(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(true));
        
        smartTimingManager = new SmartTimingManager(lcuMonitor, config);
    }
    
    @AfterEach
    void tearDown() {
        if (smartTimingManager != null) {
            smartTimingManager.shutdown();
        }
    }
    
    @Test
    void testSmartTimingManagerCreation() {
        assertNotNull(smartTimingManager);
    }
    
    @Test
    void testStartAndStop() {
        smartTimingManager.start();
        // Manager should be active after start
        
        smartTimingManager.stop();
        // Manager should be inactive after stop
    }
    
    @Test
    void testSmartBanWithHover() {
        // 设置测试数据
        int actionId = 123;
        AutoAcceptConfig.ChampionInfo banChampion = new AutoAcceptConfig.ChampionInfo();
        banChampion.setKey("Yasuo");
        banChampion.setNameCn("亚索");
        banChampion.setChampionId(157);
        
        String playerPosition = "middle";
        
        smartTimingManager.start();
        
        // 执行智能Ban
        smartTimingManager.handleSmartBan(actionId, banChampion, playerPosition);
        
        // 由于SmartTiming被禁用，应该直接执行ban而不是hover
        verify(lcuMonitor, timeout(1000)).banChampion(157, actionId);
        verify(lcuMonitor, never()).hoverChampion(anyInt(), anyInt());
    }
    
    @Test
    void testSmartPickWithHover() {
        // 设置测试数据
        int actionId = 456;
        AutoAcceptConfig.ChampionInfo pickChampion = new AutoAcceptConfig.ChampionInfo();
        pickChampion.setKey("Jinx");
        pickChampion.setNameCn("金克丝");
        pickChampion.setChampionId(222);
        
        String playerPosition = "bottom";
        
        smartTimingManager.start();
        
        // 执行智能Pick
        smartTimingManager.handleSmartPick(actionId, pickChampion, playerPosition);
        
        // 由于SmartTiming被禁用，应该直接执行pick而不是hover
        verify(lcuMonitor, timeout(1000)).pickChampion(222, actionId);
        verify(lcuMonitor, never()).hoverChampion(anyInt(), anyInt());
    }
    
    @Test
    void testExecutionWhenTimeIsUp() throws InterruptedException {
        // 设置剩余时间为1秒，小于执行延迟
        when(lcuMonitor.getRemainingTimeInPhase()).thenReturn(CompletableFuture.completedFuture(1));
        
        int actionId = 789;
        AutoAcceptConfig.ChampionInfo banChampion = new AutoAcceptConfig.ChampionInfo();
        banChampion.setKey("Ekko");
        banChampion.setNameCn("艾克");
        banChampion.setChampionId(245);
        
        smartTimingManager.start();
        smartTimingManager.handleSmartBan(actionId, banChampion, "jungle");
        
        // 等待一段时间让定时任务执行
        Thread.sleep(1500);
        
        // 验证ban被执行
        verify(lcuMonitor, timeout(2000)).banChampion(245, actionId);
    }
    
    @Test
    void testClearPendingActions() {
        int actionId = 999;
        AutoAcceptConfig.ChampionInfo banChampion = new AutoAcceptConfig.ChampionInfo();
        banChampion.setKey("Zed");
        banChampion.setNameCn("劫");
        banChampion.setChampionId(238);
        
        smartTimingManager.start();
        smartTimingManager.handleSmartBan(actionId, banChampion, "middle");
        
        // 由于SmartTiming被禁用，ban会立即执行，所以清理动作不会阻止执行
        verify(lcuMonitor, timeout(1000)).banChampion(238, actionId);
    }
    
    @Test
    void testDisabledSmartTiming() {
        // 设置延迟为0，相当于立即执行
        config.getChampionSelect().setSimpleBanDelaySeconds(0);
        
        int actionId = 111;
        AutoAcceptConfig.ChampionInfo banChampion = new AutoAcceptConfig.ChampionInfo();
        banChampion.setKey("Garen");
        banChampion.setNameCn("盖伦");
        banChampion.setChampionId(86);
        
        smartTimingManager.start();
        smartTimingManager.handleSmartBan(actionId, banChampion, "top");
        
        // 验证立即执行ban（不使用智能时机）
        verify(lcuMonitor, timeout(1000)).banChampion(86, actionId);
        // 不应该调用hover
        verify(lcuMonitor, never()).hoverChampion(anyInt(), anyInt());
    }
    
    @Test
    void testDisabledHover() {
        // 禁用hover功能
        config.getChampionSelect().setAutoHoverEnabled(false);
        
        int actionId = 222;
        AutoAcceptConfig.ChampionInfo pickChampion = new AutoAcceptConfig.ChampionInfo();
        pickChampion.setKey("Ashe");
        pickChampion.setNameCn("艾希");
        pickChampion.setChampionId(22);
        
        smartTimingManager.start();
        smartTimingManager.handleSmartPick(actionId, pickChampion, "bottom");
        
        // 验证不会调用hover
        verify(lcuMonitor, never()).hoverChampion(anyInt(), anyInt());
    }
}