package com.lol.championselector.model;

import java.util.List;
import java.util.Objects;

public class Champion {
    private String key;
    private String id;
    private String nameEn;
    private String nameCn;
    private List<String> keywords;
    private String title;
    private List<String> tags;

    public Champion() {}

    public Champion(String key, String id, String nameEn, String nameCn, 
                   List<String> keywords, String title, List<String> tags) {
        this.key = key;
        this.id = id;
        this.nameEn = nameEn;
        this.nameCn = nameCn;
        this.keywords = keywords;
        this.title = title;
        this.tags = tags;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public String getNameCn() {
        return nameCn;
    }

    public void setNameCn(String nameCn) {
        this.nameCn = nameCn;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Champion champion = (Champion) o;
        return Objects.equals(key, champion.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return "Champion{" +
                "key='" + key + '\'' +
                ", nameEn='" + nameEn + '\'' +
                ", nameCn='" + nameCn + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}