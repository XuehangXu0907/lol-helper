package com.lol.championselector.lcu;

public enum GamePhase {
    NONE("None", "未在游戏中"),
    LOBBY("Lobby", "大厅"),
    MATCHMAKING("Matchmaking", "匹配中"),
    READY_CHECK("ReadyCheck", "准备检查"),
    CHAMP_SELECT("ChampSelect", "英雄选择"),
    GAME_START("GameStart", "游戏开始"),
    IN_PROGRESS("InProgress", "游戏中"),
    RECONNECT("Reconnect", "重连"),
    WAITING_FOR_STATS("WaitingForStats", "等待结算"),
    PRE_END_OF_GAME("PreEndOfGame", "游戏结束前"),
    END_OF_GAME("EndOfGame", "游戏结束"),
    TERMINATED_IN_ERROR("TerminatedInError", "错误终止");
    
    private final String lcuName;
    private final String displayName;
    
    GamePhase(String lcuName, String displayName) {
        this.lcuName = lcuName;
        this.displayName = displayName;
    }
    
    public String getLcuName() {
        return lcuName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static GamePhase fromLcuName(String lcuName) {
        if (lcuName == null) {
            return NONE;
        }
        
        for (GamePhase phase : values()) {
            if (phase.lcuName.equalsIgnoreCase(lcuName)) {
                return phase;
            }
        }
        
        return NONE;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}