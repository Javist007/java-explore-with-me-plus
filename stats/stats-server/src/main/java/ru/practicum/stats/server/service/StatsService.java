package ru.practicum.stats.server.service;

import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.dto.StatsRequestDto;

import java.util.List;

public interface StatsService {
    void saveHit(EndpointHitDto dto);

    List<ViewStatsDto> getStats(StatsRequestDto request);
}