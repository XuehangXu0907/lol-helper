package com.lol.championselector.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChampionSkills {
    private Skill passive;
    private List<Skill> spells;

    public ChampionSkills() {
        this.spells = new ArrayList<>();
    }

    public ChampionSkills(Skill passive, List<Skill> spells) {
        this.passive = passive;
        this.spells = spells != null ? spells : new ArrayList<>();
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

    public boolean isEmpty() {
        return passive == null && (spells == null || spells.isEmpty());
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