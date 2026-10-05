package ru.practicum.stats.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.StatsDateTimeFormat;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.service.StatsService;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED) // Возвращаем 201 Created
    public void saveHit(@RequestBody @Valid EndpointHitDto dto) {
        statsService.saveHit(dto);
    }

    @GetMapping("/stats")
    public ResponseEntity<List<ViewStatsDto>> getStats(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") Boolean unique) {

        // Парсим строки в LocalDateTime
        LocalDateTime startTime = LocalDateTime.parse(start, StatsDateTimeFormat.FORMATTER);
        LocalDateTime endTime = LocalDateTime.parse(end, StatsDateTimeFormat.FORMATTER);

        // Валидация: start не может быть позже end
        if (startTime.isAfter(endTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Дата начала не может быть позже даты конца");
        }

        List<ViewStatsDto> stats = statsService.getStats(startTime, endTime, uris, unique);
        return ResponseEntity.ok(stats);
    }
}