package com.lol.championselector.model;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;

public class AvatarCache {
    private String championKey;
    private String url;
    private Path localPath;
    private LocalDateTime downloadTime;
    private boolean isValid;

    public AvatarCache() {}

    public AvatarCache(String championKey, String url, Path localPath, 
                      LocalDateTime downloadTime, boolean isValid) {
        this.championKey = championKey;
        this.url = url;
        this.localPath = localPath;
        this.downloadTime = downloadTime;
        this.isValid = isValid;
    }

    public String getChampionKey() {
        return championKey;
    }

    public void setChampionKey(String championKey) {
        this.championKey = championKey;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Path getLocalPath() {
        return localPath;
    }

    public void setLocalPath(Path localPath) {
        this.localPath = localPath;
    }

    public LocalDateTime getDownloadTime() {
        return downloadTime;
    }

    public void setDownloadTime(LocalDateTime downloadTime) {
        this.downloadTime = downloadTime;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AvatarCache that = (AvatarCache) o;
        return Objects.equals(championKey, that.championKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(championKey);
    }

    @Override
    public String toString() {
        return "AvatarCache{" +
                "championKey='" + championKey + '\'' +
                ", url='" + url + '\'' +
                ", localPath=" + localPath +
                ", downloadTime=" + downloadTime +
                ", isValid=" + isValid +
                '}';
    }
}