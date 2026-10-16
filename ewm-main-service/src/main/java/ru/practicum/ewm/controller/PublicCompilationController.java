package ru.practicum.ewm.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.compilation.response.CompilationDto;
import ru.practicum.ewm.service.CompilationService;

import java.util.List;

/**
 * Публичный контроллер для работы с подборками событий.
 */
@RestController
@RequestMapping("/compilations")
@RequiredArgsConstructor
@Validated
public class PublicCompilationController {

    private final CompilationService compilationService;

    /**
     * Получение списка подборок с фильтрацией по pinned и пагинацией.
     */
    @GetMapping
    public List<CompilationDto> getAll(
            @RequestParam(required = false) Boolean pinned,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {

        Pageable pageable = PageRequest.of(
                from / size,
                size,
                Sort.by(Sort.Direction.ASC, "id")
        );

        return compilationService.getAll(pinned, pageable);
    }

    /**
     * Получение подборки по ID.
     */
    @GetMapping("/{compId}")
    public CompilationDto getById(@PathVariable @Positive Long compId) {
        return compilationService.getById(compId);
    }
}