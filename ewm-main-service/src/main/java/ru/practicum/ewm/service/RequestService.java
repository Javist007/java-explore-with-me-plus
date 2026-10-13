package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.participation.request.RequestStatusUpdateParams;
import ru.practicum.ewm.dto.participation.response.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.participation.response.ParticipationRequestDto;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Интерфейс сервиса для работы с заявками на участие в событиях.
 */
public interface RequestService {

    /**
     * Заявки текущего пользователя на участие в чужих событиях.
     */
    List<ParticipationRequestDto> getUserRequests(Long userId);

    /**
     * Добавление заявки текущего пользователя на участие в событии.
     */
    ParticipationRequestDto addRequest(Long userId, Long eventId);

    /**
     * Отмена своей заявки на участие в событии.
     */
    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    /**
     * Заявки на участие в событии текущего пользователя.
     */
    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

    /**
     * Подтверждение или отклонение заявок на участие в событии текущего пользователя.
     */
    EventRequestStatusUpdateResult updateRequestStatuses(RequestStatusUpdateParams params);

    /**
     * Количество подтверждённых заявок для каждого события одним запросом.
     * Для событий без подтверждённых заявок ключа в результате нет: используйте getOrDefault(id, 0L).
     */
    Map<Long, Long> getConfirmedRequestsCount(Collection<Long> eventIds);
}
