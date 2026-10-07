package ru.practicum.stats.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.dto.StatsRequestDto;
import ru.practicum.stats.server.mapper.StatsMapper;
import ru.practicum.stats.server.model.Hit;
import ru.practicum.stats.server.projection.ViewStatsProjection;
import ru.practicum.stats.server.repository.HitRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final HitRepository repository;

    /**
     * Сохраняет информацию о посещении endpoint.
     *
     * @param dto DTO с данными о посещении
     */
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

    /**
     * Возвращает статистику посещений.
     *
     * @param request параметры запроса статистики
     * @return список статистики
     */
    @Override
    @Transactional(readOnly = true)
    public List<ViewStatsDto> getStats(StatsRequestDto request) {
        if (request.getStart().isAfter(request.getEnd())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Дата начала не может быть позже даты конца");
        }

        boolean isUnique = Boolean.TRUE.equals(request.getUnique());
        boolean hasUris = request.getUris() != null && !request.getUris().isEmpty();

        List<ViewStatsProjection> projections;

        if (isUnique) {
            projections = hasUris
                    ? repository.getUniqueStatsWithUris(request.getStart(), request.getEnd(), request.getUris())
                    : repository.getUniqueStatsWithoutUris(request.getStart(), request.getEnd());
        } else {
            projections = hasUris
                    ? repository.getStatsWithUris(request.getStart(), request.getEnd(), request.getUris())
                    : repository.getStatsWithoutUris(request.getStart(), request.getEnd());
        }

        return StatsMapper.toViewStatsDtoList(projections);
    }
}