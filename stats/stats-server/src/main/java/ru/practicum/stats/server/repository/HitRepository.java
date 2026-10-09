package ru.practicum.stats.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.stats.server.model.Hit;
import ru.practicum.stats.server.projection.ViewStatsProjection;

import java.time.LocalDateTime;
import java.util.List;

public interface HitRepository extends JpaRepository<Hit, Long> {

    /**
     * Возвращает статистику всех посещений с фильтром по URI.
     * @param start начало периода
     * @param end конец периода
     * @param uris список URI для фильтрации
     * @return список проекций статистики
     */
    // 1. Все посещения + фильтр по URI
    @Query("SELECT h.app AS app, h.uri AS uri, COUNT(h.ip) AS hits " +
            "FROM Hit h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "AND h.uri IN :uris " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(h.ip) DESC")
    List<ViewStatsProjection> getStatsWithUris(@Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end,
                                               @Param("uris") List<String> uris);

    /**
     * Возвращает статистику всех посещений без фильтра по URI.
     * @param start начало периода
     * @param end конец периода
     * @return список проекций статистики
     */
    // 2. Все посещения без фильтра по URI
    @Query("SELECT h.app AS app, h.uri AS uri, COUNT(h.ip) AS hits " +
            "FROM Hit h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(h.ip) DESC")
    List<ViewStatsProjection> getStatsWithoutUris(@Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    /**
     * Возвращает статистику уникальных посещений с фильтром по URI.
     * @param start начало периода
     * @param end конец периода
     * @param uris список URI для фильтрации
     * @return список проекций статистики
     */
    // 3. Уникальные IP + фильтр по URI
    @Query("SELECT h.app AS app, h.uri AS uri, COUNT(DISTINCT h.ip) AS hits " +
            "FROM Hit h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "AND h.uri IN :uris " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(DISTINCT h.ip) DESC")
    List<ViewStatsProjection> getUniqueStatsWithUris(@Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end,
                                                     @Param("uris") List<String> uris);

    /**
     * Возвращает статистику уникальных посещений без фильтра по URI.
     * @param start начало периода
     * @param end конец периода
     * @return список проекций статистики
     */
    // 4. Уникальные IP без фильтра по URI
    @Query("SELECT h.app AS app, h.uri AS uri, COUNT(DISTINCT h.ip) AS hits " +
            "FROM Hit h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(DISTINCT h.ip) DESC")
    List<ViewStatsProjection> getUniqueStatsWithoutUris(@Param("start") LocalDateTime start,
                                                        @Param("end") LocalDateTime end);
}