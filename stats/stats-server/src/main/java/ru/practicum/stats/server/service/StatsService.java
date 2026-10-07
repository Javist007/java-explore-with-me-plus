package ru.practicum.stats.server.service;

import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.dto.StatsRequestDto;

import java.util.List;

public interface StatsService {
    /**
     * Сохраняет информацию о посещении endpoint.
     * @param dto DTO с данными о посещении
     */
    void saveHit(EndpointHitDto dto);

    /**
     * Возвращает статистику посещений.
     * @param request параметры запроса статистики
     * @return список статистики
     */
    List<ViewStatsDto> getStats(StatsRequestDto request);
}