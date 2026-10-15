package ru.practicum.ewm.controller;

import lombok.RequiredArgsConstructor;
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
public class PublicCompilationController {

    private final CompilationService compilationService;

    /**
     * Получение списка подборок с фильтрацией по pinned и пагинацией.
     */
    @GetMapping
    public List<CompilationDto> getAll(
            @RequestParam(required = false) Boolean pinned,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {
        return compilationService.getAll(pinned, from, size);
    }

    /**
     * Получение подборки по ID.
     */
    @GetMapping("/{compId}")
    public CompilationDto getById(@PathVariable Long compId) {
        return compilationService.getById(compId);
    }
}