package ru.practicum.stats.server.projection;

public interface ViewStatsProjection {
    String getApp();
    String getUri();
    Long getHits();
}