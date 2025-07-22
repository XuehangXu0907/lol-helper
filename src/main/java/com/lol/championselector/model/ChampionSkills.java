package com.lol.championselector.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChampionSkills {
    private String championKey;
    private Skill passive;
    private List<Skill> spells;
    private List<Skill> skills; // 兼容新的数据结构
    private String dataSource;
    private Long lastUpdated;

    public ChampionSkills() {
        this.spells = new ArrayList<>();
        this.skills = new ArrayList<>();
    }

    public ChampionSkills(Skill passive, List<Skill> spells) {
        this.passive = passive;
        this.spells = spells != null ? spells : new ArrayList<>();
        this.skills = new ArrayList<>();
    }

    public static ChampionSkills createEmpty() {
        return new ChampionSkills();
    }

    public Skill getPassive() {
        return passive;
    }

    public void setPassive(Skill passive) {
        this.passive = passive;
    }

    public List<Skill> getSpells() {
        return spells;
    }

    public void setSpells(List<Skill> spells) {
        this.spells = spells != null ? spells : new ArrayList<>();
    }

    public String getChampionKey() {
        return championKey;
    }

    public void setChampionKey(String championKey) {
        this.championKey = championKey;
    }

    public List<Skill> getSkills() {
        return skills;
    }

    public void setSkills(List<Skill> skills) {
        this.skills = skills != null ? skills : new ArrayList<>();
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public Long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public boolean isEmpty() {
        return passive == null && 
               (spells == null || spells.isEmpty()) && 
               (skills == null || skills.isEmpty());
    }

    /**
     * 获取所有技能（包括spells和skills）
     */
    public List<Skill> getAllSkills() {
        List<Skill> allSkills = new ArrayList<>();
        if (spells != null) {
            allSkills.addAll(spells);
        }
        if (skills != null) {
            allSkills.addAll(skills);
        }
        return allSkills;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChampionSkills that = (ChampionSkills) o;
        return Objects.equals(passive, that.passive) && Objects.equals(spells, that.spells);
    }

    @Override
    public int hashCode() {
        return Objects.hash(passive, spells);
    }

    @Override
    public String toString() {
        return "ChampionSkills{" +
                "passive=" + passive +
                ", spells=" + spells +
                '}';
    }
}