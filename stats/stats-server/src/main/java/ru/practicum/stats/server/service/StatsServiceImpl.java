package ru.practicum.stats.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.mapper.StatsMapper;
import ru.practicum.stats.server.model.Hit;
import ru.practicum.stats.server.projection.ViewStatsProjection;
import ru.practicum.stats.server.repository.HitRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final HitRepository repository;

    @Override
    @Transactional
    public void saveHit(EndpointHitDto dto) {
        Hit hit = Hit.builder()
                .app(dto.getApp())
                .uri(dto.getUri())
                .ip(dto.getIp())
                .timestamp(dto.getTimestamp())
                .build();
        repository.save(hit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViewStatsDto> getStats(LocalDateTime start,
                                       LocalDateTime end,
                                       List<String> uris,
                                       Boolean unique) {
        boolean isUnique = unique != null && unique;
        boolean hasUris = uris != null && !uris.isEmpty();

        List<ViewStatsProjection> projections;

        if (isUnique) {
            projections = hasUris
                    ? repository.getUniqueStatsWithUris(start, end, uris)
                    : repository.getUniqueStatsWithoutUris(start, end);
        } else {
            projections = hasUris
                    ? repository.getStatsWithUris(start, end, uris)
                    : repository.getStatsWithoutUris(start, end);
        }

        return StatsMapper.toViewStatsDtoList(projections);
    }
}