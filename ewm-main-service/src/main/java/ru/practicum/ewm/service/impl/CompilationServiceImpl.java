package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.compilation.request.NewCompilationDto;
import ru.practicum.ewm.dto.compilation.request.UpdateCompilationRequest;
import ru.practicum.ewm.dto.compilation.response.CompilationDto;
import ru.practicum.ewm.dto.event.response.EventShortDto;
import ru.practicum.ewm.exception.model.NotFoundException;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.repository.CompilationRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.service.CompilationService;
import ru.practicum.ewm.service.RequestService;
import ru.practicum.ewm.service.mapper.CompilationMapper;
import ru.practicum.ewm.service.mapper.EventMapper;
import ru.practicum.stats.client.StatClient;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
    private final RequestService requestService;
    private final StatClient statClient;

    private static final String EVENT_URI_PREFIX = "/events/";
    private static final LocalDateTime STATS_START = LocalDateTime.of(2020, 1, 1, 0, 0);

    @Override
    @Transactional
    public CompilationDto save(NewCompilationDto dto) {
        List<Event> events = findEventsOrThrow(dto.getEvents());
        Compilation compilation = CompilationMapper.toEntity(dto, events);
        Compilation saved = compilationRepository.save(compilation);

        Compilation savedWithEvents = compilationRepository.findByIdWithEvents(saved.getId())
                .orElseThrow(() -> new NotFoundException(
                        String.format("Compilation with id=%d was not found", saved.getId())));

        Map<Long, EventShortDto> stats = buildEventsWithStats(savedWithEvents.getEvents().stream().toList());
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

        Map<Long, EventShortDto> stats = buildEventsWithStats(updated.getEvents().stream().toList());
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
    public List<CompilationDto> getAll(Boolean pinned, Pageable pageable) {
        Page<Compilation> page;
        if (pinned != null) {
            page = compilationRepository.findAllByPinnedWithEvents(pinned, pageable);
        } else {
            page = compilationRepository.findAllWithEvents(pageable);
        }

        List<Compilation> compilations = page.getContent();

        List<Event> allEvents = compilations.stream()
                .flatMap(c -> c.getEvents().stream())
                .distinct()
                .toList();

        Map<Long, EventShortDto> eventsWithStats = buildEventsWithStats(allEvents);

        return CompilationMapper.toDtoList(compilations, eventsWithStats);
    }

    @Override
    public CompilationDto getById(Long compId) {
        Compilation compilation = compilationRepository.findByIdWithEvents(compId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Compilation with id=%d was not found", compId)));
        Map<Long, EventShortDto> stats = buildEventsWithStats(compilation.getEvents().stream().toList());
        return CompilationMapper.toDtoWithStats(compilation, stats);
    }

    /**
     * Строит карту EventShortDto с актуальными views и confirmedRequests
     * для списка событий. Логика аналогична EventServiceImpl.getPublicEvents.
     */
    private Map<Long, EventShortDto> buildEventsWithStats(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> confirmedCounts = requestService.getConfirmedRequestsCount(eventIds);

        List<String> uris = eventIds.stream()
                .map(id -> EVENT_URI_PREFIX + id)
                .toList();

        List<ViewStatsDto> stats = statClient.getStats(
                STATS_START,
                LocalDateTime.now().plusSeconds(1),
                uris,
                false
        );

        Map<Long, Long> viewsMap = stats.stream()
                .filter(v -> v.getUri() != null && v.getUri().startsWith(EVENT_URI_PREFIX))
                .collect(Collectors.toMap(
                        v -> Long.parseLong(v.getUri().substring(EVENT_URI_PREFIX.length())),
                        ViewStatsDto::getHits,
                        Long::sum
                ));

        return events.stream()
                .map(EventMapper::toShortDto)
                .peek(dto -> {
                    dto.setConfirmedRequests(confirmedCounts.getOrDefault(dto.getId(), 0L));
                    dto.setViews(viewsMap.getOrDefault(dto.getId(), 0L));
                })
                .collect(Collectors.toMap(EventShortDto::getId, Function.identity()));
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