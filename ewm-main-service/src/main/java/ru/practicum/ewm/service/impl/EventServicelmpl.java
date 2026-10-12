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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EventServicelmpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    //private final ParticipationRequestRepository participationRepository;
    private final StatClient statClient;

    @Override
    @Transactional
    public void createEvent(NewEventRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User не найден"));

        Category category = categoryRepository.findById(request.getCategory())
                .orElseThrow(() -> new NotFoundException("Category не найдена"));

        LocalDateTime eventDate = LocalDateTime.parse(request.getEventDate(),
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new IllegalArgumentException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }

        eventRepository.save(EventMapper.toEntity(request, user, category));
    }

    @Override
    public List<EventShortDto> getEvents(Long userId, EventSearchRequest request) {
        int page = request.getFrom() / request.getSize();
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User не найден"));

        return eventRepository.findAllByInitiatorIdWithPagination(userId, request.getFrom(), request.getSize()).stream()
                .map(EventMapper::toShortDto)
                .toList();
    }

    @Override
    public EventFullDto getEventsUser(Long userId, Long eventId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User не найден"));

        eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event не найден"));

        Event event = eventRepository.findByInitiatorIdAndId(userId, eventId);
        return EventMapper.toDto(event);
    }

    @Override
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
            LocalDateTime eventDate = LocalDateTime.parse(context.getRequest().getEventDate(),
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
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

        eventRepository.save(event);
        return EventMapper.toDto(event);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequest(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User не найден"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event не найден"));
        //return participationRepository.findAllByEventId(eventId);
        return null;
    }

    @Override
    public EventRequestStatusUpdateResult updateEventRequests(EventRequestStatusUpdateContext context) {
        User user = userRepository.findById(context.getUserId())
                .orElseThrow(() -> new NotFoundException("User не найден"));
        Event event = eventRepository.findById(context.getEventId())
                .orElseThrow(() -> new NotFoundException("Event не найден"));

        if (event.getInitiator() != user) {
            throw new IllegalArgumentException("User не является инициатором события");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new IllegalArgumentException("Event не опубликован");
        }

        List<ParticipationRequest> requests = participationRepository
                .findAllByIdInAndEventId(context.getRequest().getRequestIds(), context.getEventId());

        if (context.getRequest().getStatus().equals("CONFIRMED")) {
            long confirmedCount = participationRepository.countByEventIdAndStatus(
                    context.getEventId(), RequestStatus.CONFIRMED);
            if (confirmedCount + requests.size() > event.getParticipantLimit()) {
                throw new IllegalArgumentException("Превышен лимит участников");
            }
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();
        List<ParticipationRequest> toSave = new ArrayList<>();

        RequestStatus newStatus = RequestStatus.valueOf(context.getRequest().getStatus());


        for (ParticipationRequest request : requests) {
            request.setStatus(newStatus);
            toSave.add(request);  // Собираем в список

            ParticipationRequestDto dto = ParticipationRequestMapper.toDto(request);
            if (newStatus == RequestStatus.CONFIRMED) {
                confirmed.add(dto);
            } else {
                rejected.add(dto);
            }
        }

        participationRepository.saveAll(toSave);

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed)
                .rejectedRequests(rejected)
                .build();
    }

    @Override
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

        int page = request.getFrom() / request.getSize();
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
            if (eventDate.isBefore(event.getPublishedOn().plusHours(1))) {
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
    public List<EventShortDto> getPublicEvents(PublicParamSearch request) {

        // 1. Собираем условия фильтрации (WHERE)
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

        spec = spec.and(PublicEventSpecs.eventDateRange(
                request.getRangeStart(),
                request.getRangeEnd()
        ));

        if (Boolean.TRUE.equals(request.getOnlyAvailable())) {
            spec = spec.and(PublicEventSpecs.isAvailable());
        }

        Sort sort = "VIEWS".equals(request.getSort())
                ? Sort.by(Sort.Direction.DESC, "views")
                : Sort.by(Sort.Direction.DESC, "eventDate");

        PageRequest pageable = PageRequest.of(
                request.getFrom() / request.getSize(),
                request.getSize(),
                sort
        );

        Page<Event> eventPage = eventRepository.findAll(spec, pageable);
        List<Event> events = eventPage.getContent();

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        LocalDateTime start = request.getRangeStart() != null
                ? LocalDateTime.parse(request.getRangeStart(),DateTimeConstants.FORMATTER)
                : LocalDateTime.now();
        LocalDateTime end = request.getRangeEnd() != null
                ? LocalDateTime.parse(request.getRangeEnd(),DateTimeConstants.FORMATTER)
                : LocalDateTime.now().plusYears(10);

        List<ViewStatsDto> stats = statClient.getStats(
                start,
                end,
                uris,
                false
        );

        Map<Long, Long> viewsMap = stats.stream()
                .collect(Collectors.toMap(
                        viewStat -> Long.parseLong(viewStat.getUri().replace("/events/", "")),
                        ViewStatsDto::getHits
                ));

        return events.stream()
                .map(EventMapper::toShortDto)
                .map(dto -> {
                    dto.setViews(viewsMap.getOrDefault(dto.getId(), 0L));
                    return dto;
                })
                .toList();
    }

    @Override
    public EventFullDto getPublicEventId(Long eventId, HttpServletRequest servletRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(()-> new NotFoundException("Event не найден"));

        if(event.getState() != EventState.PUBLISHED){
            throw new NotFoundException("Event не опубликован");
        }

        String ip = servletRequest.getRemoteAddr();

        EndpointHitDto hit = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events/" + eventId)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();

        statClient.saveHit(hit);

        return EventMapper.toDto(event);
    }

}
