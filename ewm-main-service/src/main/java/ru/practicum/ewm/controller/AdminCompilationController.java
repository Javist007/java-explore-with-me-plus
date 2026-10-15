package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.compilation.request.NewCompilationDto;
import ru.practicum.ewm.dto.compilation.request.UpdateCompilationRequest;
import ru.practicum.ewm.dto.compilation.response.CompilationDto;
import ru.practicum.ewm.service.CompilationService;

/**
 * Административный контроллер для работы с подборками событий.
 */
@RestController
@RequestMapping("/admin/compilations")
@RequiredArgsConstructor
public class AdminCompilationController {

    private final CompilationService compilationService;

    /**
     * Создание новой подборки.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto create(@Valid @RequestBody NewCompilationDto dto) {
        return compilationService.save(dto);
    }

    /**
     * Обновление подборки по ID.
     */
    @PatchMapping("/{compId}")
    public CompilationDto update(
            @PathVariable Long compId,
            @Valid @RequestBody UpdateCompilationRequest request) {
        return compilationService.update(compId, request);
    }

    /**
     * Удаление подборки по ID.
     */
    @DeleteMapping("/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long compId) {
        compilationService.delete(compId);
    }
}
