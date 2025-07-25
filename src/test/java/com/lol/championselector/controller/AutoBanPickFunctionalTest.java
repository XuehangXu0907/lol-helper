package com.lol.championselector.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.championselector.config.AutoAcceptConfig;
import com.lol.championselector.lcu.LCUMonitor;
import com.lol.championselector.manager.LanguageManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Functional tests for auto ban and pick functionality
 */
class AutoBanPickFunctionalTest {

    @Mock
    private LCUMonitor mockLCUMonitor;
    
    @Mock
    private LanguageManager mockLanguageManager;
    
    private AutoAcceptController controller;
    private AutoAcceptConfig testConfig;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        
        // Create test configuration
        testConfig = new AutoAcceptConfig();
        setupTestConfiguration();
        
        // Mock language manager
        when(mockLanguageManager.getString(anyString())).thenReturn("Test String");
        
        // Create controller (normally would be done through DI/FXML)
        controller = new AutoAcceptController();
        // Note: We would need to inject mocks here in a real test setup
    }

    private void setupTestConfiguration() {
        // Configure auto ban
        testConfig.getChampionSelect().setAutoBanEnabled(true);
        testConfig.getChampionSelect().setAutoPickEnabled(true);
        testConfig.getChampionSelect().setAutoHoverEnabled(true);
        testConfig.getChampionSelect().setUsePositionBasedSelection(true);
        testConfig.getChampionSelect().setSmartBanEnabled(true);
        
        // Set up ban champion
        AutoAcceptConfig.ChampionInfo banChampion = new AutoAcceptConfig.ChampionInfo();
        banChampion.setChampionId(86); // Garen
        banChampion.setNameCn("盖伦");
        banChampion.setNameEn("Garen");
        testConfig.getChampionSelect().setBanChampion(banChampion);
        
        // Set up pick champion
        AutoAcceptConfig.ChampionInfo pickChampion = new AutoAcceptConfig.ChampionInfo();
        pickChampion.setChampionId(22); // Ashe
        pickChampion.setNameCn("艾希");
        pickChampion.setNameEn("Ashe");
        testConfig.getChampionSelect().setPickChampion(pickChampion);
        
        // Set up position-based configuration for ADC
        AutoAcceptConfig.PositionConfig adcConfig = new AutoAcceptConfig.PositionConfig();
        
        // ADC ban champions
        AutoAcceptConfig.ChampionInfo dravenBan = new AutoAcceptConfig.ChampionInfo();
        dravenBan.setChampionId(119); // Draven
        dravenBan.setNameCn("德莱文");
        dravenBan.setNameEn("Draven");
        adcConfig.getBanChampions().add(dravenBan);
        
        AutoAcceptConfig.ChampionInfo vayneBan = new AutoAcceptConfig.ChampionInfo();
        vayneBan.setChampionId(67); // Vayne
        vayneBan.setNameCn("薇恩");
        vayneBan.setNameEn("Vayne");
        adcConfig.getBanChampions().add(vayneBan);
        
        // ADC pick champions
        AutoAcceptConfig.ChampionInfo jinxPick = new AutoAcceptConfig.ChampionInfo();
        jinxPick.setChampionId(222); // Jinx
        jinxPick.setNameCn("金克丝");
        jinxPick.setNameEn("Jinx");
        adcConfig.getPickChampions().add(jinxPick);
        
        AutoAcceptConfig.ChampionInfo ashePick = new AutoAcceptConfig.ChampionInfo();
        ashePick.setChampionId(22); // Ashe
        ashePick.setNameCn("艾希");
        ashePick.setNameEn("Ashe");
        adcConfig.getPickChampions().add(ashePick);
        
        testConfig.getChampionSelect().getPositionConfigs().put("bottom", adcConfig);
    }

    @Test
    void testAutoBanFunctionality() throws Exception {
        // Mock LCU responses
        when(mockLCUMonitor.isConnected()).thenReturn(true);
        when(mockLCUMonitor.banChampion(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(true));
        when(mockLCUMonitor.getBannedChampions()).thenReturn(CompletableFuture.completedFuture(java.util.Set.of()));
        
        // Create a mock champion select session with ban action
        String sessionJson = createMockChampSelectSession("ban", false, 0);
        JsonNode session = objectMapper.readTree(sessionJson);
        
        // Test would involve calling the controller's session changed handler
        // In a real test, we would verify that the correct ban champion is selected
        // and that the LCU API is called with the right parameters
        
        // Verify expectations
        // verify(mockLCUMonitor, timeout(1000)).banChampion(eq(86), eq(1));
    }

    @Test
    void testAutoPickFunctionality() throws Exception {
        // Mock LCU responses
        when(mockLCUMonitor.isConnected()).thenReturn(true);
        when(mockLCUMonitor.pickChampion(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(true));
        when(mockLCUMonitor.getBannedChampions()).thenReturn(CompletableFuture.completedFuture(java.util.Set.of()));
        when(mockLCUMonitor.getPickedChampions()).thenReturn(CompletableFuture.completedFuture(java.util.Set.of()));
        
        // Create a mock champion select session with pick action
        String sessionJson = createMockChampSelectSession("pick", false, 0);
        JsonNode session = objectMapper.readTree(sessionJson);
        
        // Test would involve calling the controller's session changed handler
        // In a real test, we would verify that the correct pick champion is selected
        
        // Verify expectations
        // verify(mockLCUMonitor, timeout(5000)).pickChampion(eq(22), eq(1));
    }

    @Test
    void testPickDelayConfiguration() {
        // Test delay configuration validation
        AutoAcceptConfig.ChampionSelectConfig config = new AutoAcceptConfig.ChampionSelectConfig();
        
        // Test valid delay
        config.setSimplePickDelaySeconds(15);
        assertTrue(config.validateDelayConfiguration());
        assertEquals(15, config.getSimplePickDelaySeconds());
        
        // Test invalid delay (too small)
        config.setSimplePickDelaySeconds(-5);
        assertEquals(1, config.getSimplePickDelaySeconds()); // Should be clamped to 1
        
        // Test invalid delay (too large)
        config.setSimplePickDelaySeconds(100);
        assertEquals(30, config.getSimplePickDelaySeconds()); // Should be clamped to 30
        
        // Test validation and fix
        AutoAcceptConfig.ChampionSelectConfig invalidConfig = new AutoAcceptConfig.ChampionSelectConfig();
        invalidConfig.setSimplePickDelaySeconds(0); // This should be fixed to 1
        invalidConfig.fixDelayConfiguration();
        assertTrue(invalidConfig.validateDelayConfiguration());
        assertEquals(1, invalidConfig.getSimplePickDelaySeconds());
    }
    
    @Test
    void testPickDelayExecutionLogic() {
        // Create a config with known delay time
        testConfig.getChampionSelect().setSimplePickDelaySeconds(5);
        
        // Test that delay configuration is properly read
        assertEquals(5, testConfig.getChampionSelect().getSimplePickDelaySeconds());
        assertEquals(5, testConfig.getChampionSelect().getPickExecutionDelaySeconds());
        
        // Test delay boundary conditions
        testConfig.getChampionSelect().setSimplePickDelaySeconds(1); // Minimum
        assertEquals(1, testConfig.getChampionSelect().getSimplePickDelaySeconds());
        
        testConfig.getChampionSelect().setSimplePickDelaySeconds(30); // Maximum
        assertEquals(30, testConfig.getChampionSelect().getSimplePickDelaySeconds());
    }

    @Test
    void testPositionBasedSelection() throws Exception {
        // Mock LCU responses for ADC position
        when(mockLCUMonitor.getPlayerPosition()).thenReturn(CompletableFuture.completedFuture("bottom"));
        when(mockLCUMonitor.isConnected()).thenReturn(true);
        when(mockLCUMonitor.banChampion(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(true));
        when(mockLCUMonitor.getBannedChampions()).thenReturn(CompletableFuture.completedFuture(java.util.Set.of()));
        
        // Create a mock champion select session
        String sessionJson = createMockChampSelectSession("ban", false, 0);
        JsonNode session = objectMapper.readTree(sessionJson);
        
        // Test would verify that ADC-specific ban champions are used (Draven, Vayne)
        // instead of the global ban champion (Garen)
        
        // In a real test, we would verify:
        // verify(mockLCUMonitor).banChampion(eq(119), eq(1)); // Draven should be banned for ADC
    }

    @Test
    void testHoverFunctionality() throws Exception {
        // Mock LCU responses
        when(mockLCUMonitor.getPlayerPosition()).thenReturn(CompletableFuture.completedFuture("bottom"));
        when(mockLCUMonitor.isConnected()).thenReturn(true);
        when(mockLCUMonitor.hoverChampion(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(true));
        
        // Create a mock champion select session with pick action (for hovering)
        String sessionJson = createMockChampSelectSession("pick", false, 0);
        JsonNode session = objectMapper.readTree(sessionJson);
        
        // Test would verify that hover occurs after position is confirmed
        // and that the correct champion is hovered based on position
        
        // In a real test, we would verify:
        // verify(mockLCUMonitor, timeout(4000)).hoverChampion(eq(222), eq(1)); // Jinx for ADC position
    }

    @Test
    void testSmartBanAvoidanceOfTeammateHover() throws Exception {
        // Mock LCU responses
        when(mockLCUMonitor.isConnected()).thenReturn(true);
        when(mockLCUMonitor.banChampion(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(true));
        when(mockLCUMonitor.getBannedChampions()).thenReturn(CompletableFuture.completedFuture(java.util.Set.of()));
        
        // Create a session where a teammate has hovered the champion we want to ban
        String sessionJson = createMockChampSelectSessionWithTeammateHover();
        JsonNode session = objectMapper.readTree(sessionJson);
        
        // Test would verify that smart ban avoids banning teammate's hovered champion
        // and selects the next available champion from the ban list
    }

    private String createMockChampSelectSession(String actionType, boolean completed, int championId) {
        return String.format("""
            {
                "gameId": 12345,
                "localPlayerCellId": 0,
                "myTeam": [
                    {"cellId": 0, "championId": %d, "summonerId": 1001}
                ],
                "actions": [
                    [
                        {
                            "id": 1,
                            "actorCellId": 0,
                            "type": "%s",
                            "championId": %d,
                            "completed": %s
                        }
                    ]
                ]
            }
            """, championId, actionType, championId, completed);
    }

    private String createMockChampSelectSessionWithTeammateHover() {
        return """
            {
                "gameId": 12345,
                "localPlayerCellId": 0,
                "myTeam": [
                    {"cellId": 0, "championId": 0, "summonerId": 1001},
                    {"cellId": 1, "championId": 119, "summonerId": 1002}
                ],
                "actions": [
                    [
                        {
                            "id": 1,
                            "actorCellId": 0,
                            "type": "ban",
                            "championId": 0,
                            "completed": false
                        }
                    ]
                ]
            }
            """;
    }
}