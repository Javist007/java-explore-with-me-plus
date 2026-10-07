package ru.practicum.stats.client;

import jakarta.validation.Valid;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatClient {
    void saveHit(@Valid EndpointHitDto hit);

    List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique);
}
