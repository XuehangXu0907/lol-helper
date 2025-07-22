package com.lol.championselector.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PopupSuppressionManager的单元测试
 */
public class PopupSuppressionManagerTest {
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }
    
    @Test
    void testSessionIdGeneration() throws Exception {
        // 测试用例1：使用gameId作为session ID
        String sessionJson1 = """
            {
                "gameId": "12345678",
                "localPlayerCellId": 0,
                "myTeam": [],
                "actions": []
            }
            """;
        JsonNode session1 = objectMapper.readTree(sessionJson1);
        
        // 由于generateStableSessionId是private方法，我们通过反射来测试
        PopupSuppressionManager manager = new PopupSuppressionManager(null);
        java.lang.reflect.Method method = PopupSuppressionManager.class.getDeclaredMethod("generateStableSessionId", JsonNode.class);
        method.setAccessible(true);
        
        String sessionId1 = (String) method.invoke(manager, session1);
        assertEquals("champselect_game_12345678", sessionId1);
        
        // 测试用例2：使用相同的gameId应该生成相同的session ID
        String sessionId1Repeat = (String) method.invoke(manager, session1);
        assertEquals(sessionId1, sessionId1Repeat);
        
        // 测试用例3：没有gameId时使用聊天室名称
        String sessionJson2 = """
            {
                "gameId": "",
                "localPlayerCellId": 0,
                "chatDetails": {
                    "chatRoomName": "championselect-1234567890"
                },
                "myTeam": [],
                "actions": []
            }
            """;
        JsonNode session2 = objectMapper.readTree(sessionJson2);
        String sessionId2 = (String) method.invoke(manager, session2);
        assertEquals("champselect_chat_championselect-1234567890", sessionId2);
        
        // 测试用例4：没有gameId和聊天室名称时使用myTeam哈希
        String sessionJson3 = """
            {
                "gameId": "",
                "localPlayerCellId": 0,
                "chatDetails": {},
                "myTeam": [
                    {"cellId": 0, "championId": 157},
                    {"cellId": 1, "championId": 238}
                ],
                "actions": []
            }
            """;
        JsonNode session3 = objectMapper.readTree(sessionJson3);
        String sessionId3 = (String) method.invoke(manager, session3);
        assertTrue(sessionId3.startsWith("champselect_team_"));
        
        // 相同的myTeam应该生成相同的session ID
        String sessionId3Repeat = (String) method.invoke(manager, session3);
        assertEquals(sessionId3, sessionId3Repeat);
    }
    
    @Test
    void testSessionIdStability() throws Exception {
        // 测试session ID的稳定性
        String sessionJson = """
            {
                "gameId": "987654321",
                "localPlayerCellId": 0,
                "timer": {
                    "totalTimeInPhase": 30000,
                    "phase": "BAN_PICK"
                },
                "myTeam": [],
                "actions": []
            }
            """;
        
        JsonNode session = objectMapper.readTree(sessionJson);
        PopupSuppressionManager manager = new PopupSuppressionManager(null);
        java.lang.reflect.Method method = PopupSuppressionManager.class.getDeclaredMethod("generateStableSessionId", JsonNode.class);
        method.setAccessible(true);
        
        // 多次调用应该返回相同的session ID
        String sessionId1 = (String) method.invoke(manager, session);
        String sessionId2 = (String) method.invoke(manager, session);
        String sessionId3 = (String) method.invoke(manager, session);
        
        assertEquals(sessionId1, sessionId2);
        assertEquals(sessionId2, sessionId3);
        assertEquals("champselect_game_987654321", sessionId1);
    }
}