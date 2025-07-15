package com.lol.championselector;

import com.lol.championselector.manager.ChampionDataManager;
import com.lol.championselector.manager.ChampionDataManagerNew;

public class TestChampionCountApp {
    public static void main(String[] args) {
        try {
            // 测试原始ChampionDataManager
            ChampionDataManager oldManager = new ChampionDataManager();
            int oldCount = oldManager.getChampionCount();
            
            System.out.println("Original ChampionDataManager: " + oldCount + " champions");
            
            if (oldCount == 171) {
                System.out.println("✅ SUCCESS: Original manager now has 171 champions!");
            } else {
                System.out.println("❌ ISSUE: Original manager has " + oldCount + " champions, expected 171");
            }
            
            // 测试新的ChampionDataManagerNew（如果本地数据可用）
            try {
                ChampionDataManagerNew newManager = new ChampionDataManagerNew();
                int newCount = newManager.getChampionCount();
                System.out.println("New ChampionDataManagerNew: " + newCount + " champions");
                System.out.println("Difference: " + (newCount - oldCount) + " champions");
            } catch (Exception e) {
                System.out.println("ChampionDataManagerNew test skipped: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.out.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}