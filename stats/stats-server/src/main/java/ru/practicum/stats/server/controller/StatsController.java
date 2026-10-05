package ru.practicum.stats.server.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.dto.StatsRequestDto;
import ru.practicum.stats.server.service.StatsService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @PostMapping("/hit")
    public ResponseEntity<Void> saveHit(@RequestBody @Valid EndpointHitDto dto) {
        statsService.saveHit(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/stats")
    public ResponseEntity<List<ViewStatsDto>> getStats(@Valid @ModelAttribute StatsRequestDto request) {
        // Простая и понятная проверка без кастомных аннотаций
        if (request.getStart().isAfter(request.getEnd())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Дата начала не может быть позже даты конца");
        }

        List<ViewStatsDto> stats = statsService.getStats(request);
        return ResponseEntity.ok(stats);
    }
}