package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.compilation.request.NewCompilationDto;
import ru.practicum.ewm.dto.compilation.request.UpdateCompilationRequest;
import ru.practicum.ewm.dto.compilation.response.CompilationDto;

import java.util.List;

/**
 * Сервис для работы с подборками событий.
 */
public interface CompilationService {

    /**
     * Создание новой подборки.
     */
    CompilationDto save(NewCompilationDto dto);

    /**
     * Обновление подборки по ID.
     */
    CompilationDto update(Long compId, UpdateCompilationRequest request);

    /**
     * Удаление подборки по ID.
     */
    void delete(Long compId);

    /**
     * Получение списка подборок с фильтрацией по pinned и пагинацией.
     */
    List<CompilationDto> getAll(Boolean pinned, Integer from, Integer size);

    /**
     * Получение подборки по ID.
     */
    CompilationDto getById(Long compId);
}