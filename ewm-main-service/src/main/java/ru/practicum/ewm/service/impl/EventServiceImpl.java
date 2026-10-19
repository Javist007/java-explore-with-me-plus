package ru.practicum.ewm.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.common.DateTimeConstants;
import ru.practicum.ewm.dto.event.request.*;
import ru.practicum.ewm.dto.event.response.EventFullDto;
import ru.practicum.ewm.dto.event.response.EventShortDto;
import ru.practicum.ewm.exception.model.BadRequestException;
import ru.practicum.ewm.exception.model.ConflictException;
import ru.practicum.ewm.exception.model.NotFoundException;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.repository.spec.AdminEventSpecs;
import ru.practicum.ewm.repository.spec.PublicEventSpecs;
import ru.practicum.ewm.service.EventService;
import ru.practicum.ewm.service.RequestService;
import ru.practicum.ewm.service.mapper.EventMapper;
import ru.practicum.stats.client.StatClient;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Реализация сервиса для работы с событиями.
 * Обеспечивает создание, поиск, обновление и получение информации о событиях
 * с учетом прав доступа пользователей и администраторов.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final StatClient statClient;
    private final RequestService requestService;

    private static final String EVENT_URI_PREFIX = "/events/";
    private static final LocalDateTime STATS_START = LocalDateTime.of(2020, 1, 1, 0, 0);

    /**
     * Создает новое событие от имени пользователя.
     * Проверяет существование пользователя и категории, валидирует дату события.
     *
     * @param request данные для создания события
     * @param userId  идентификатор пользователя-инициатора
     * @return полное DTO созданного события
     * @throws NotFoundException если пользователь или категория не найдены
     * @throws ConflictException если дата события раньше чем через 2 часа от текущего момента
     */
    @Override
    @Transactional
    public EventFullDto createEvent(NewEventRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User не найден"));

        Category category = categoryRepository.findById(request.getCategory())
                .orElseThrow(() -> new NotFoundException("Category не найдена"));

        LocalDateTime eventDate = parseDateOrNull(request.getEventDate());

        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }

        return EventMapper.toDto(eventRepository.save(EventMapper.toEntity(request, user, category)));
    }

    /**
     * Получает список событий пользователя с пагинацией.
     * Загружает количество подтвержденных заявок для каждого события.
     *
     * @param userId  идентификатор пользователя
     * @param request параметры поиска и пагинации
     * @return список кратких DTO событий пользователя
     * @throws NotFoundException   если пользователь не найден
     * @throws BadRequestException если параметр size некорректен
     */
    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getEvents(Long userId, EventSearchRequest request) {
        if (request.getSize() == null || request.getSize() <= 0) {
            throw new BadRequestException("Параметр size должен быть больше 0");
        }
        int from = request.getFrom() == null ? 0 : Math.max(0, request.getFrom());
        int size = request.getSize();

        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User не найден"));

        List<Event> events = eventRepository.findAllByInitiatorIdWithPagination(userId, from, size);
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmedCounts = requestService.getConfirmedRequestsCount(eventIds);

        return events.stream()
                .map(EventMapper::toShortDto)
                .peek(dto -> dto.setConfirmedRequests(confirmedCounts.getOrDefault(dto.getId(), 0L)))
                .toList();
    }

    /**
     * Получает полную информацию о событии пользователя.
     * Проверяет принадлежность события пользователю и загружает количество подтвержденных заявок.
     *
     * @param userId  идентификатор пользователя
     * @param eventId идентификатор события
     * @return полное DTO события
     * @throws NotFoundException если событие не найдено или не принадлежит пользователю
     */
    @Override
    @Transactional(readOnly = true)
    public EventFullDto getEventsUser(Long userId, Long eventId) {
        Event event = eventRepository.findByInitiatorIdAndId(userId, eventId)
                .orElseThrow(() -> new NotFoundException("Event не найден"));
        long confirmedRequests = requestService.getConfirmedRequestsCount(List.of(eventId)).getOrDefault(eventId, 0L);
        EventFullDto dto = EventMapper.toDto(event);
        dto.setConfirmedRequests(confirmedRequests);
        return dto;
    }

    /**
     * Обновляет информацию о событии пользователя.
     * Разрешает редактирование только для событий в статусе PENDING или CANCELED.
     * Обновляет только те поля, которые указаны в запросе.
     *
     * @param context контекст обновления с данными запроса, userId и eventId
     * @return полное DTO обновленного события
     * @throws NotFoundException если событие не найдено или категория не существует
     * @throws ConflictException если событие опубликовано или дата некорректна
     */
    @Override
    @Transactional
    public EventFullDto updateEvent(UpdateEventContext context) {
        Event event = eventRepository.findByInitiatorIdAndId(context.getUserId(), context.getEventId())
                .orElseThrow(() -> new NotFoundException("Event не найден"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Редактирование опубликованного или отменённого события невозможно");
        }

        if (context.getRequest().hasAnnotation()) {
            event.setAnnotation(context.getRequest().getAnnotation());
        }

        if (context.getRequest().hasCategory()) {
            Category category = categoryRepository.findById(context.getRequest().getCategory())
                    .orElseThrow(() -> new NotFoundException("Category не найдена"));
            event.setCategory(category);
        }

        if (context.getRequest().hasDescription()) {
            event.setDescription(context.getRequest().getDescription());
        }

        if (context.getRequest().hasEventDate()) {
            LocalDateTime eventDate = parseDateOrNull(context.getRequest().getEventDate());
            if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new BadRequestException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
            }
            event.setEventDate(eventDate);
        }

        if (context.getRequest().hasLocation()) {
            event.setLocation(context.getRequest().getLocation());
        }

        if (context.getRequest().hasPaid()) {
            event.setPaid(context.getRequest().getPaid());
        }

        if (context.getRequest().hasParticipantLimit()) {
            event.setParticipantLimit(context.getRequest().getParticipantLimit());
        }

        if (context.getRequest().hasRequestModeration()) {
            event.setRequestModeration(context.getRequest().getRequestModeration());
        }

        if (context.getRequest().hasTitle()) {
            event.setTitle(context.getRequest().getTitle());
        }

        if (context.getRequest().hasStateAction()) {
            switch (context.getRequest().getStateAction()) {
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
                case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
            }
        }

        eventRepository.save(event);
        return EventMapper.toDto(event);
    }


    /**
     * Получает список событий с фильтрацией по параметрам администратора.
     * Поддерживает фильтрацию по пользователям, состояниям, категориям и датам.
     * Загружает количество подтвержденных заявок для каждого события.
     *
     * @param request параметры поиска и фильтрации событий
     * @return список полных DTO событий
     * @throws BadRequestException если параметр size некорректен
     */
    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> adminGetEvents(AdminParamSearch request) {
        Specification<Event> spec = Specification.where(null);

        LocalDateTime rangeStart = parseDateOrNull(request.getRangeStart());
        LocalDateTime rangeEnd = parseDateOrNull(request.getRangeEnd());

        if (request.getUsers() != null) {
            spec = spec.and(AdminEventSpecs.hasUser(request));
        }

        if (request.getStates() != null) {
            spec = spec.and(AdminEventSpecs.hasStates(request));
        }

        if (request.getCategories() != null) {
            spec = spec.and(AdminEventSpecs.hasCategories(request));
        }

        if (rangeStart != null) {
            spec = spec.and(AdminEventSpecs.hasRangeStart(rangeStart));
        }

        if (rangeEnd != null) {
            spec = spec.and(AdminEventSpecs.hasRangeEnd(rangeEnd));
        }

        if (request.getSize() == null || request.getSize() <= 0) {
            throw new BadRequestException("Параметр size должен быть больше 0");
        }

        int page = Math.max(0, request.getFrom()) / request.getSize();
        PageRequest pageable = PageRequest.of(page, request.getSize());

        Page<Event> eventPage = eventRepository.findAll(spec, pageable);

        if (eventPage.isEmpty()) {
            return Collections.emptyList();
        }

        List<Event> events = eventPage.getContent();
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmedCounts = requestService.getConfirmedRequestsCount(eventIds);

        return events.stream()
                .map(EventMapper::toDto)
                .peek(dto -> dto.setConfirmedRequests(confirmedCounts.getOrDefault(dto.getId(), 0L)))
                .toList();
    }

    /**
     * Обновляет информацию о событии от имени администратора.
     * Позволяет публиковать и отклонять события с проверкой текущего статуса.
     * Обновляет только те поля, которые указаны в запросе.
     *
     * @param eventId идентификатор события
     * @param request данные для обновления события
     * @return полное DTO обновленного события
     * @throws NotFoundException если событие или категория не найдены
     * @throws ConflictException если статус события не позволяет выполнить действие
     */
    @Override
    @Transactional
    public EventFullDto adminUpdateEvent(Long eventId, UpdateEventAdminRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event не найден"));

        if (request.hasAnnotation()) {
            event.setAnnotation(request.getAnnotation());
        }

        if (request.hasCategory()) {
            Category category = categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category не найдена"));
            event.setCategory(category);
        }

        if (request.hasDescription()) {
            event.setDescription(request.getDescription());
        }

        if (request.hasEventDate()) {
            LocalDateTime eventDate = parseDateOrNull(request.getEventDate());
            if (eventDate.isBefore(LocalDateTime.now())) {
                throw new BadRequestException("Дата события не может быть в прошлом");
            }
            LocalDateTime publishedOn = event.getPublishedOn() != null
                    ? event.getPublishedOn()
                    : event.getCreatedOn();
            if (eventDate.isBefore(publishedOn.plusHours(1))) {
                throw new ConflictException("Дата события должна быть не ранее чем за час от даты публикации");
            }
            event.setEventDate(eventDate);
        }

        if (request.hasLocation()) {
            event.setLocation(request.getLocation());
        }

        if (request.hasPaid()) {
            event.setPaid(request.getPaid());
        }

        if (request.hasParticipantLimit()) {
            event.setParticipantLimit(request.getParticipantLimit());
        }

        if (request.hasRequestModeration()) {
            event.setRequestModeration(request.getRequestModeration());
        }

        if (request.hasStateAction()) {
            if (request.getStateAction() == UpdateEventAdminRequest.StateAction.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Событие можно публиковать, только если оно в состоянии ожидания публикации");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (request.getStateAction() == UpdateEventAdminRequest.StateAction.REJECT_EVENT) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Событие можно отклонить, только если оно еще не опубликовано");
                }
                event.setState(EventState.CANCELED);
            }
        }

        if (request.hasTitle()) {
            event.setTitle(request.getTitle());
        }

        eventRepository.save(event);
        return EventMapper.toDto(event);
    }

    /**
     * Получает список опубликованных событий с публичным доступом.
     * Поддерживает фильтрацию по тексту, категориям, платности, датам и доступности мест.
     * Загружает статистику просмотров и количество подтвержденных заявок.
     *
     * @param request параметры поиска и фильтрации событий
     * @return список кратких DTO событий
     * @throws BadRequestException если параметр size некорректен
     */
    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getPublicEvents(PublicParamSearch request, HttpServletRequest servletRequest) {
        Specification<Event> spec = Specification.where(PublicEventSpecs.isPublished());

        if (request.getText() != null && !request.getText().isBlank()) {
            spec = spec.and(PublicEventSpecs.textSearch(request.getText()));
        }
        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            spec = spec.and(PublicEventSpecs.hasCategories(request.getCategories()));
        }
        if (request.getPaid() != null) {
            spec = spec.and(PublicEventSpecs.isPaid(request.getPaid()));
        }

        LocalDateTime rangeStart = parseDateOrNull(request.getRangeStart());
        LocalDateTime rangeEnd = parseDateOrNull(request.getRangeEnd());

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("rangeStart не может быть позже rangeEnd");
        }
        spec = spec.and(PublicEventSpecs.eventDateRange(rangeStart, rangeEnd));

        if (rangeStart == null && rangeEnd == null) {
            spec = spec.and(PublicEventSpecs.isFuture());
        }

        if (Boolean.TRUE.equals(request.getOnlyAvailable())) {
            spec = spec.and(PublicEventSpecs.isAvailable());
        }

        if (request.getSize() == null || request.getSize() <= 0) {
            throw new BadRequestException("Параметр size должен быть больше 0");
        }

        // Запрос корректен: фиксируем его в статистике до выборки, чтобы учесть и пустой результат
        statClient.saveHit(EndpointHitDto.builder()
                .app("ewm-main-service").uri(servletRequest.getRequestURI())
                .ip(servletRequest.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build());

        int page = Math.max(0, request.getFrom()) / request.getSize();
        PageRequest pageable = PageRequest.of(page, request.getSize(),
                Sort.by(Sort.Direction.ASC, "eventDate"));

        Page<Event> eventPage = eventRepository.findAll(spec, pageable);
        List<Event> events = eventPage.getContent();

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, EventShortDto> statsMap = getEventsWithStats(events);
        List<EventShortDto> dtos = events.stream()
                .map(event -> statsMap.get(event.getId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (request.getSort() == PublicParamSearch.Sort.VIEWS) {
            dtos.sort(Comparator.comparingLong(EventShortDto::getViews).reversed());
        }
        return dtos;
    }

    /**
     * Получает полную информацию о конкретном опубликованном событии.
     * Фиксирует статистику просмотров события и загружает количество подтвержденных заявок.
     *
     * @param eventId        идентификатор события
     * @param servletRequest HTTP запрос для фиксации статистики
     * @return полное DTO события
     * @throws NotFoundException если событие не найдено или не опубликовано
     */
    @Override
    @Transactional(readOnly = true)
    public EventFullDto getPublicEventId(Long eventId, HttpServletRequest servletRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event не найден"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event не опубликован");
        }

        String uri = EVENT_URI_PREFIX + eventId;
        statClient.saveHit(EndpointHitDto.builder()
                .app("ewm-main-service").uri(uri)
                .ip(servletRequest.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build());

        Map<Long, EventShortDto> statsMap = getEventsWithStats(List.of(event));
        EventShortDto shortDto = statsMap.get(eventId);

        EventFullDto dto = EventMapper.toDto(event);
        dto.setViews(shortDto.getViews());
        dto.setConfirmedRequests(shortDto.getConfirmedRequests());
        return dto;
    }

    /**
     * Преобразует список событий в карту EventShortDto с актуальной статистикой.
     * Общий метод для всех мест, где нужно обогатить события views и confirmedRequests.
     *
     * @param events список событий
     * @return карта id события -> EventShortDto со статистикой
     */
    @Override
    public Map<Long, EventShortDto> getEventsWithStats(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> confirmedCounts = requestService.getConfirmedRequestsCount(eventIds);

        List<String> uris = eventIds.stream()
                .map(id -> EVENT_URI_PREFIX + id)
                .toList();

        LocalDateTime statsEnd = LocalDateTime.now().plusSeconds(1);
        List<ViewStatsDto> stats = statClient.getStats(STATS_START, statsEnd, uris, true);

        Map<Long, Long> viewsMap = stats.stream()
                .filter(v -> v.getUri() != null && v.getUri().matches("/events/\\d+"))
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

    private LocalDateTime parseDateOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DateTimeConstants.FORMATTER);
        } catch (java.time.format.DateTimeParseException e) {
            throw new BadRequestException(
                    "Неверный формат даты: \"" + value + "\", ожидается yyyy-MM-dd HH:mm:ss");
        }
    }

}