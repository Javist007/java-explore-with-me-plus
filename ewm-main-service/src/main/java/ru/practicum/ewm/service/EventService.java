package ru.practicum.ewm.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.ewm.dto.event.request.*;
import ru.practicum.ewm.dto.event.response.EventFullDto;
import ru.practicum.ewm.dto.event.response.EventShortDto;
import ru.practicum.ewm.model.Event;

import java.util.List;
import java.util.Map;

/**
 * Сервис для работы с событиями.
 * Предоставляет методы для создания, поиска, обновления и получения информации о событиях.
 */
public interface EventService {
    /**
     * Создает новое событие от имени пользователя.
     *
     * @param request данные для создания события
     * @param userId  идентификатор пользователя-инициатора
     * @return полное DTO созданного события
     */
    EventFullDto createEvent(NewEventRequest request, Long userId);

    /**
     * Получает список событий пользователя с пагинацией.
     *
     * @param userId  идентификатор пользователя
     * @param request параметры поиска и пагинации
     * @return список кратких DTO событий пользователя
     */
    List<EventShortDto> getEvents(Long userId, EventSearchRequest request);

    /**
     * Получает полную информацию о событии пользователя.
     *
     * @param userId  идентификатор пользователя
     * @param eventId идентификатор события
     * @return полное DTO события
     */
    EventFullDto getEventsUser(Long userId, Long eventId);

    /**
     * Обновляет информацию о событии пользователя.
     *
     * @param context контекст обновления с данными запроса, userId и eventId
     * @return полное DTO обновленного события
     */
    EventFullDto updateEvent(UpdateEventContext context);


    /**
     * Получает список событий с фильтрацией по параметрам администратора.
     *
     * @param request параметры поиска и фильтрации событий
     * @return список полных DTO событий
     */
    List<EventFullDto> adminGetEvents(AdminParamSearch request);

    /**
     * Обновляет информацию о событии от имени администратора.
     *
     * @param eventId идентификатор события
     * @param request данные для обновления события
     * @return полное DTO обновленного события
     */
    EventFullDto adminUpdateEvent(Long eventId, UpdateEventAdminRequest request);

    /**
     * Получает список опубликованных событий с публичным доступом.
     *
     * @param request        параметры поиска и фильтрации событий
     * @param servletRequest HTTP-запрос, из которого берутся uri и ip для сервиса статистики
     * @return список кратких DTO событий
     */
    List<EventShortDto> getPublicEvents(PublicParamSearch request, HttpServletRequest servletRequest);

    /**
     * Получает полную информацию о конкретном опубликованном событии.
     * Фиксирует статистику просмотров события.
     *
     * @param eventId        идентификатор события
     * @param servletRequest HTTP запрос для фиксации статистики
     * @return полное DTO события
     */
    EventFullDto getPublicEventId(Long eventId, HttpServletRequest servletRequest);

    /**
     * Преобразует список событий в список EventShortDto с актуальной статистикой
     * (views из StatClient и confirmedRequests из RequestService).
     * Используется для обогащения событий статистикой при маппинге.
     *
     * @param events список событий
     * @return карта id события -> EventShortDto со статистикой
     */
    Map<Long, EventShortDto> getEventsWithStats(List<Event> events);

}
