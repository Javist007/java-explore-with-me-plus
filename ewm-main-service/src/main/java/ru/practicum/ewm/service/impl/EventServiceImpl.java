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
import ru.practicum.ewm.dto.event.response.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.event.response.EventShortDto;
import ru.practicum.ewm.dto.event.response.ParticipationRequestDto;
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
import ru.practicum.ewm.service.mapper.EventMapper;
import ru.practicum.stats.client.StatClient;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
//import ru.practicum.ewm.model.ParticipationRequest;
//import ru.practicum.ewm.model.RequestStatus;


import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository participationRepository;
    private final StatClient statClient;

    @Override
    @Transactional
    public EventFullDto createEvent(NewEventRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User не найден"));

        Category category = categoryRepository.findById(request.getCategory())
                .orElseThrow(() -> new NotFoundException("Category не найдена"));

        LocalDateTime eventDate = LocalDateTime.parse(request.getEventDate(),DateTimeConstants.FORMATTER);

        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }

        return EventMapper.toDto(eventRepository.save(EventMapper.toEntity(request, user, category)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getEvents(Long userId, EventSearchRequest request) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User не найден"));

        return eventRepository.findAllByInitiatorIdWithPagination(userId, request.getFrom(), request.getSize()).stream()
                .map(EventMapper::toShortDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getEventsUser(Long userId, Long eventId) {
        Event event = eventRepository.findByInitiatorIdAndId(userId, eventId)
                .orElseThrow(()-> new NotFoundException("Event не найден"));
        return EventMapper.toDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(UpdateEventContext context) {
        userRepository.findById(context.getUserId())
                .orElseThrow(() -> new NotFoundException("User не найден"));
        Event event = eventRepository.findById(context.getEventId())
                .orElseThrow(() -> new NotFoundException("Event не найден"));

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
            LocalDateTime eventDate = LocalDateTime.parse(context.getRequest().getEventDate(), DateTimeConstants.FORMATTER);
            if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new IllegalArgumentException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
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
                case CANCEL_REVIEW  -> event.setState(EventState.CANCELED);
            }
        }

        eventRepository.save(event);
        return EventMapper.toDto(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventRequest(Long userId, Long eventId) {
        Event event = eventRepository.findByInitiatorIdAndId(userId, eventId)
                .orElseThrow(()-> new NotFoundException("Event не найден"));
        if (event == null) {
            throw new NotFoundException("Event не найден");
        }
        return participationRepository.findAllByEventIdOrderByCreated(eventId).stream()
                .map(RequestMapper ::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateEventRequests(EventRequestStatusUpdateContext context) {
        User user = userRepository.findById(context.getUserId())
                .orElseThrow(() -> new NotFoundException("User не найден"));
        Event event = eventRepository.findById(context.getEventId())
                .orElseThrow(() -> new NotFoundException("Event не найден"));

        if (!Objects.equals(event.getInitiator().getId(), context.getUserId())) {
            throw new NotFoundException("Event не найден");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Event не опубликован");
        }

        List<ParticipationRequest> requests = participationRepository
                .findAllByIdInAndEventId(context.getRequest().getRequestIds(), context.getEventId());

        if (requests.size() != context.getRequest().getRequestIds().size()) {
            throw new NotFoundException("Некоторые заявки не найдены или не относятся к событию");
        }

        boolean confirming = "CONFIRMED".equals(context.getRequest().getStatus());

        if (confirming && event.getParticipantLimit() > 0) {   // <-- только если лимит задан
            long confirmedCount = participationRepository.countByEventIdAndStatus(
                    context.getEventId(), RequestStatus.CONFIRMED);
            if (confirmedCount + requests.size() > event.getParticipantLimit()) {
                throw new ConflictException("Превышен лимит участников события");   // 409
            }
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();
        List<ParticipationRequest> toSave = new ArrayList<>();

        RequestStatus newStatus;
        try {
            newStatus = RequestStatus.valueOf(context.getRequest().getStatus());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException("Недопустимый статус: \"" + context.getRequest().getStatus()
                    + "\". Допустимые значения: CONFIRMED, REJECTED");
        }


        for (ParticipationRequest request : requests) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Обновлять можно только заявки со статусом PENDING");
            }
            request.setStatus(newStatus);
            toSave.add(request);

            ParticipationRequestDto dto = ParticipationRequestMapper.toDto(request);
            if (newStatus == RequestStatus.CONFIRMED) {
                confirmed.add(dto);
            } else {
                rejected.add(dto);
            }
        }

        participationRepository.saveAll(toSave);

        long newConfirmed = participationRepository.countByEventIdAndStatus(
                context.getEventId(), RequestStatus.CONFIRMED);
        event.setConfirmedRequests((int) newConfirmed);
        if (event.getParticipantLimit() > 0 && newConfirmed >= event.getParticipantLimit()) {
            event.setState(EventState.CLOSED);   // места больше нет — закрываем событие
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed)
                .rejectedRequests(rejected)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> adminGetEvents(AdminParamSearch request) {
        Specification<Event> spec = Specification.where(null);

        if (request.getUsers() != null) {
            spec = spec.and(AdminEventSpecs.hasUser(request));
        }

        if (request.getStates() != null) {
            spec = spec.and(AdminEventSpecs.hasStates(request));
        }

        if (request.getCategories() != null) {
            spec = spec.and(AdminEventSpecs.hasCategories(request));
        }

        if (request.getRangeStart() != null) {
            spec = spec.and(AdminEventSpecs.hasRangeStart(request));
        }

        if (request.getRangeEnd() != null) {
            spec = spec.and(AdminEventSpecs.hasRangeEnd(request));
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

        return eventPage.getContent().stream()
                .map(EventMapper::toDto)
                .toList();
    }

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
            LocalDateTime eventDate = LocalDateTime.parse(request.getEventDate(), DateTimeConstants.FORMATTER);
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

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getPublicEvents(PublicParamSearch request) {

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

        spec = spec.and(PublicEventSpecs.eventDateRange(rangeStart, rangeEnd));
        if (request.getSort() != PublicParamSearch.Sort.VIEWS
                && rangeStart == null && rangeEnd == null) {
            spec = spec.and(PublicEventSpecs.isFuture());

            if (Boolean.TRUE.equals(request.getOnlyAvailable())) {
                spec = spec.and(PublicEventSpecs.isAvailable());
            }

            Sort sort = (request.getSort() == PublicParamSearch.Sort.VIEWS)
                    ? Sort.by(Sort.Direction.DESC, "views")
                    : Sort.by(Sort.Direction.ASC, "eventDate"); // EVENT_DATE — по возрастанию (ближайшие вперёд)      : Sort.by(Sort.Direction.DESC, "eventDate");

            if (request.getSize() == null || request.getSize() <= 0) {
                throw new BadRequestException("Параметр size должен быть больше 0");
            }
            int page = Math.max(0, request.getFrom()) / request.getSize();
            PageRequest pageable = PageRequest.of(page, request.getSize());

            Page<Event> eventPage = eventRepository.findAll(spec, pageable);
            List<Event> events = eventPage.getContent();

            if (events.isEmpty()) {
                return Collections.emptyList();
            }

            List<String> uris = events.stream()
                    .map(event -> "/events/" + event.getId())
                    .collect(Collectors.toList());

            LocalDateTime start = request.getRangeStart() != null
                    ? LocalDateTime.parse(request.getRangeStart(), DateTimeConstants.FORMATTER)
                    : LocalDateTime.of(2020,1,1,0,0);
            LocalDateTime end = request.getRangeEnd() != null
                    ? LocalDateTime.parse(request.getRangeEnd(), DateTimeConstants.FORMATTER)
                    : LocalDateTime.now();

            List<ViewStatsDto> stats = statClient.getStats(
                    start,
                    end,
                    uris,
                    false
            );

            Map<Long, Long> viewsMap = stats.stream()
                    .collect(Collectors.toMap(
                            viewStat -> Long.parseLong(viewStat.getUri().replace("/events/", "")),
                            ViewStatsDto::getHits,
                            Long::sum
                    ));

            return events.stream()
                    .map(EventMapper::toShortDto)
                    .map(dto -> {
                        dto.setViews(viewsMap.getOrDefault(dto.getId(), 0L));
                        return dto;
                    })
                    .toList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getPublicEventId(Long eventId, HttpServletRequest servletRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(()-> new NotFoundException("Event не найден"));

        if(event.getState() != EventState.PUBLISHED){
            throw new NotFoundException("Event не опубликован");
        }

        String ip = servletRequest.getRemoteAddr();

        String uri = "/events/" + eventId;
        statClient.saveHit(EndpointHitDto.builder()
                .app("ewm-main-service").uri(uri)
                .ip(servletRequest.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build());

        List<ViewStatsDto> stats = statClient.getStats(
                LocalDateTime.of(2020, 1, 1, 0, 0),
                LocalDateTime.now().plusSeconds(1),
                List.of(uri), false);

        long views = stats.isEmpty() ? 0L : stats.get(0).getHits();

        return EventMapper.toDto(event);
    }

    private LocalDateTime parseDateOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DateTimeConstants.FORMATTER);
        } catch (java.time.format.DateTimeParseException e) {
            throw new ru.practicum.ewm.exception.model.BadRequestException(
                    "Неверный формат даты: \"" + value + "\", ожидается yyyy-MM-dd HH:mm:ss");
        }
    }

}
