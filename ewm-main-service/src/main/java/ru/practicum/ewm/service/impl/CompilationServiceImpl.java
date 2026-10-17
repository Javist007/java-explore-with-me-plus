package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.compilation.request.NewCompilationDto;
import ru.practicum.ewm.dto.compilation.request.UpdateCompilationRequest;
import ru.practicum.ewm.dto.compilation.response.CompilationDto;
import ru.practicum.ewm.dto.event.response.EventShortDto;
import ru.practicum.ewm.exception.model.BadRequestException;
import ru.practicum.ewm.exception.model.NotFoundException;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.repository.CompilationRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.service.CompilationService;
import ru.practicum.ewm.service.EventService;
import ru.practicum.ewm.service.mapper.CompilationMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Реализация сервиса для работы с подборками событий.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final EventService eventService;

    @Override
    @Transactional
    public CompilationDto save(NewCompilationDto dto) {
        List<Event> events = findEventsOrThrow(dto.getEvents());
        Compilation compilation = CompilationMapper.toEntity(dto, events);
        Compilation saved = compilationRepository.save(compilation);

        Compilation savedWithEvents = compilationRepository.findByIdWithEvents(saved.getId())
                .orElseThrow(() -> new NotFoundException(
                        String.format("Compilation with id=%d was not found", saved.getId())));

        Map<Long, EventShortDto> stats = eventService.getEventsWithStats(
                savedWithEvents.getEvents().stream().toList());
        return CompilationMapper.toDtoWithStats(savedWithEvents, stats);
    }

    @Override
    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationRequest request) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Compilation with id=%d was not found", compId)));

        List<Event> newEvents = null;
        if (request.getEvents() != null) {
            newEvents = findEventsOrThrow(request.getEvents());
        }

        CompilationMapper.updateCompilation(request, compilation, newEvents);
        compilationRepository.save(compilation);

        Compilation updated = compilationRepository.findByIdWithEvents(compId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Compilation with id=%d was not found", compId)));

        Map<Long, EventShortDto> stats = eventService.getEventsWithStats(
                updated.getEvents().stream().toList());
        return CompilationMapper.toDtoWithStats(updated, stats);
    }

    @Override
    @Transactional
    public void delete(Long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException(
                    String.format("Compilation with id=%d was not found", compId));
        }
        compilationRepository.deleteById(compId);
    }

    @Override
    public List<CompilationDto> getAll(Boolean pinned, Integer from, Integer size) {
        if (size == null || size <= 0) {
            throw new BadRequestException("Parameter 'size' must be greater than 0");
        }
        if (from != null && from % size != 0) {
            throw new BadRequestException("Parameter 'from' must be a multiple of 'size'");
        }

        int fromValue = from != null ? from : 0;
        int sizeValue = size != null ? size : 10;

        Pageable pageable = PageRequest.of(
                fromValue / sizeValue,
                sizeValue,
                Sort.by(Sort.Direction.ASC, "id")
        );

        List<Long> ids;
        if (pinned != null) {
            ids = compilationRepository.findIdsByPinned(pinned, pageable);
        } else {
            ids = compilationRepository.findAllIds(pageable);
        }

        if (ids.isEmpty()) {
            return List.of();
        }

        List<Compilation> compilations = compilationRepository.findByIdsWithEvents(ids);

        List<Event> allEvents = compilations.stream()
                .flatMap(c -> c.getEvents().stream())
                .distinct()
                .toList();

        Map<Long, EventShortDto> eventsWithStats = eventService.getEventsWithStats(allEvents);

        return CompilationMapper.toDtoList(compilations, eventsWithStats);
    }

    @Override
    public CompilationDto getById(Long compId) {
        Compilation compilation = compilationRepository.findByIdWithEvents(compId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Compilation with id=%d was not found", compId)));
        Map<Long, EventShortDto> stats = eventService.getEventsWithStats(
                compilation.getEvents().stream().toList());
        return CompilationMapper.toDtoWithStats(compilation, stats);
    }

    /**
     * Находит события по списку ID. Если хотя бы одно не найдено — бросает NotFoundException.
     * Корректно обрабатывает дубликаты ID (например, [2, 2]).
     */
    private List<Event> findEventsOrThrow(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }

        List<Event> events = eventRepository.findAllById(eventIds);

        long uniqueIdsCount = eventIds.stream().distinct().count();
        if (events.size() != uniqueIdsCount) {
            Set<Long> foundIds = events.stream()
                    .map(Event::getId)
                    .collect(Collectors.toSet());
            List<Long> missingIds = eventIds.stream()
                    .distinct()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            throw new NotFoundException(
                    String.format("Events with ids %s were not found", missingIds));
        }

        return events;
    }
}