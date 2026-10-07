package ru.practicum.stats.server.projection;

public interface ViewStatsProjection {
    /**
     * Возвращает название приложения.
     * @return название приложения
     */
    String getApp();

    /**
     * Возвращает URI.
     * @return URI
     */
    String getUri();

    /**
     * Возвращает количество посещений.
     * @return количество посещений
     */
    Long getHits();
}