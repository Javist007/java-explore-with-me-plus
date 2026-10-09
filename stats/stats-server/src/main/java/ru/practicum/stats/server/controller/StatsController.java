package ru.practicum.stats.server.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.dto.StatsRequestDto;
import ru.practicum.stats.server.service.StatsService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /**
     * Сохраняет информацию о посещении endpoint.
     *
     * @param dto DTO с данными о посещении
     */
    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveHit(@RequestBody @Valid EndpointHitDto dto) {
        statsService.saveHit(dto);
    }

    /**
     * Возвращает статистику посещений.
     *
     * @param request параметры запроса статистики
     * @return список статистики
     */
    @GetMapping("/stats")
    public List<ViewStatsDto> getStats(@Valid @ModelAttribute StatsRequestDto request) {
        return statsService.getStats(request);
    }
}