package ru.practicum.ewm.service.mapper;

import ru.practicum.ewm.dto.compilation.request.NewCompilationDto;
import ru.practicum.ewm.dto.compilation.request.UpdateCompilationRequest;
import ru.practicum.ewm.dto.compilation.response.CompilationDto;
import ru.practicum.ewm.dto.event.response.EventShortDto;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.model.Event;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Класс для преобразования между Compilation Entity и DTO.
 */
public final class CompilationMapper {

    private CompilationMapper() {
    }

    /**
     * Преобразует Compilation Entity в CompilationDto без статистики.
     * Используется только если статистика уже не нужна (например, в тестах).
     */
    public static CompilationDto toDto(Compilation compilation) {
        if (compilation == null) {
            return null;
        }

        List<EventShortDto> eventDtos = compilation.getEvents().stream()
                .map(EventMapper::toShortDto)
                .collect(Collectors.toList());

        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(eventDtos)
                .build();
    }

    /**
     * Преобразует Compilation Entity в CompilationDto, используя готовую карту
     * событий со статистикой (views и confirmedRequests).
     */
    public static CompilationDto toDtoWithStats(Compilation compilation, Map<Long, EventShortDto> eventsWithStats) {
        if (compilation == null) {
            return null;
        }

        List<EventShortDto> eventDtos = compilation.getEvents().stream()
                .map(event -> eventsWithStats.get(event.getId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(eventDtos)
                .build();
    }

    /**
     * Преобразует список Compilation в список CompilationDto,
     * используя готовую карту событий со статистикой.
     */
    public static List<CompilationDto> toDtoList(List<Compilation> compilations, Map<Long, EventShortDto> eventsWithStats) {
        return compilations.stream()
                .map(c -> toDtoWithStats(c, eventsWithStats))
                .collect(Collectors.toList());
    }

    /**
     * Преобразует NewCompilationDto в Compilation Entity (для создания).
     */
    public static Compilation toEntity(NewCompilationDto dto, List<Event> events) {
        return Compilation.builder()
                .title(dto.getTitle())
                .pinned(dto.getPinned() != null ? dto.getPinned() : false)
                .events(events != null ? new HashSet<>(events) : new HashSet<>())
                .build();
    }

    /**
     * Обновляет Compilation Entity данными из UpdateCompilationRequest.
     * Обновляет только те поля, которые не null в запросе.
     */
    public static void updateCompilation(UpdateCompilationRequest request, Compilation compilation, List<Event> newEvents) {
        if (request.getTitle() != null) {
            compilation.setTitle(request.getTitle());
        }
        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }
        if (request.getEvents() != null) {
            compilation.setEvents(newEvents != null ? new HashSet<>(newEvents) : new HashSet<>());
        }
    }
}