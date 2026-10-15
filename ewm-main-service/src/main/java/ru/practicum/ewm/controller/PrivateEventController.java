package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.event.request.EventSearchRequest;
import ru.practicum.ewm.dto.event.request.NewEventRequest;
import ru.practicum.ewm.dto.event.request.UpdateEventContext;
import ru.practicum.ewm.dto.event.request.UpdateEventRequest;
import ru.practicum.ewm.dto.event.response.EventFullDto;
import ru.practicum.ewm.dto.event.response.EventShortDto;
import ru.practicum.ewm.service.EventService;

import java.util.List;

/**
 * Контроллер для частных пользователей по управлению своими событиями.
 * Предоставляет эндпоинты для создания, просмотра и обновления событий пользователя.
 */
@RestController
@Validated
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class PrivateEventController {

    private final EventService eventService;

    /**
     * Создает новое событие от имени пользователя.
     *
     * @param userId  идентификатор пользователя-инициатора
     * @param request данные для создания события
     * @return полное DTO созданного события
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto createEvent(@PathVariable Long userId, @RequestBody @Valid NewEventRequest request) {
        return eventService.createEvent(request, userId);
    }

    /**
     * Получает список событий пользователя с возможностью фильтрации.
     *
     * @param userId  идентификатор пользователя
     * @param request параметры поиска и фильтрации событий
     * @return список кратких DTO событий пользователя
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> getEvents(@PathVariable Long userId, @ModelAttribute @Valid EventSearchRequest request) {
        return eventService.getEvents(userId, request);
    }

    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto getEvent(@PathVariable Long userId, @PathVariable Long eventId) {
        return eventService.getEventsUser(userId, eventId);
    }

    /**
     * Обновляет информацию о событии пользователя.
     *
     * @param userId  идентификатор пользователя
     * @param eventId идентификатор события
     * @param request данные для обновления события
     * @return полное DTO обновленного события
     */
    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto updateEvent(@PathVariable Long userId, @PathVariable Long eventId, @RequestBody @Valid UpdateEventRequest request) {
        return eventService.updateEvent(UpdateEventContext.builder().request(request).userId(userId).eventId(eventId).build());
    }

}
