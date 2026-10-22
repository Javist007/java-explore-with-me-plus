package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.participation.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.dto.participation.request.RequestStatusUpdateParams;
import ru.practicum.ewm.dto.participation.response.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.participation.response.ParticipationRequestDto;
import ru.practicum.ewm.exception.model.BadRequestException;
import ru.practicum.ewm.exception.model.ConflictException;
import ru.practicum.ewm.exception.model.NotFoundException;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.model.ParticipationRequest;
import ru.practicum.ewm.model.RequestStatus;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.repository.projection.EventRequestCount;
import ru.practicum.ewm.service.RequestService;
import ru.practicum.ewm.service.mapper.RequestMapper;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Реализация сервиса заявок на участие в событиях.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        checkUserExists(userId);
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(RequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может подать заявку на участие в своём событии");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }
        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Заявка пользователя id=" + userId + " на событие id=" + eventId
                    + " уже существует");
        }
        if (hasParticipantLimit(event) && countConfirmed(eventId) >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит участников события id=" + eventId);
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .event(event)
                .requester(requester)
                .created(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .status(isModerationRequired(event) ? RequestStatus.PENDING : RequestStatus.CONFIRMED)
                .build();
        return RequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Заявка с id=" + requestId + " не найдена"));
        request.setStatus(RequestStatus.CANCELED);
        return RequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        if (!eventRepository.existsByIdAndInitiatorId(eventId, userId)) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }
        return requestRepository.findAllByEventId(eventId).stream()
                .map(RequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatuses(RequestStatusUpdateParams params) {
        Long eventId = params.getEventId();
        EventRequestStatusUpdateRequest update = params.getUpdateRequest();
        RequestStatus newStatus = update.getStatus();
        if (newStatus != RequestStatus.CONFIRMED && newStatus != RequestStatus.REJECTED) {
            throw new BadRequestException("Новый статус заявок может быть только CONFIRMED или REJECTED");
        }

        Event event = eventRepository.findByIdAndInitiatorId(eventId, params.getUserId())
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        List<ParticipationRequest> requests = requestRepository
                .findAllByIdInAndEventId(update.getRequestIds(), eventId).stream()
                .sorted(Comparator.comparing(ParticipationRequest::getId))
                .toList();
        if (requests.size() != new HashSet<>(update.getRequestIds()).size()) {
            throw new NotFoundException("Не все заявки из списка найдены у события id=" + eventId);
        }
        if (requests.stream().anyMatch(r -> r.getStatus() != RequestStatus.PENDING)) {
            throw new ConflictException("Статус можно изменить только у заявок, находящихся в состоянии ожидания");
        }

        return newStatus == RequestStatus.CONFIRMED
                ? confirm(event, requests)
                : reject(requests);
    }

    @Override
    public Map<Long, Long> getConfirmedRequestsCount(Collection<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }
        return requestRepository.countByEventIdsAndStatus(eventIds, RequestStatus.CONFIRMED).stream()
                .collect(Collectors.toMap(EventRequestCount::getEventId, EventRequestCount::getRequestCount));
    }

    /**
     * Подтверждает заявки по порядку, пока не исчерпан лимит участников; остальные отклоняет.
     * Если лимит исчерпан, все оставшиеся неподтверждённые заявки события тоже отклоняются.
     */
    private EventRequestStatusUpdateResult confirm(Event event, List<ParticipationRequest> requests) {
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        if (!hasParticipantLimit(event)) {
            requests.forEach(r -> confirmTo(result, r));
            requestRepository.saveAll(requests);
            return result;
        }

        long confirmed = countConfirmed(event.getId());
        if (confirmed >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит участников события id=" + event.getId());
        }
        for (ParticipationRequest request : requests) {
            if (confirmed < event.getParticipantLimit()) {
                confirmTo(result, request);
                confirmed++;
            } else {
                rejectTo(result, request);
            }
        }
        requestRepository.saveAll(requests);

        if (confirmed >= event.getParticipantLimit()) {
            requestRepository.updateStatusByEventId(event.getId(), RequestStatus.PENDING, RequestStatus.REJECTED);
        }
        return result;
    }

    private EventRequestStatusUpdateResult reject(List<ParticipationRequest> requests) {
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        requests.forEach(r -> rejectTo(result, r));
        requestRepository.saveAll(requests);
        return result;
    }

    private void confirmTo(EventRequestStatusUpdateResult result, ParticipationRequest request) {
        request.setStatus(RequestStatus.CONFIRMED);
        result.getConfirmedRequests().add(RequestMapper.toDto(request));
    }

    private void rejectTo(EventRequestStatusUpdateResult result, ParticipationRequest request) {
        request.setStatus(RequestStatus.REJECTED);
        result.getRejectedRequests().add(RequestMapper.toDto(request));
    }

    /**
     * Лимит 0 означает отсутствие ограничения на количество участников.
     */
    private boolean hasParticipantLimit(Event event) {
        return event.getParticipantLimit() > 0;
    }

    /**
     * Заявки нужно подтверждать вручную, только если включена пре-модерация и есть лимит участников.
     */
    private boolean isModerationRequired(Event event) {
        return event.getRequestModeration() && hasParticipantLimit(event);
    }

    private long countConfirmed(Long eventId) {
        return requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
    }

    private void checkUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }
}
