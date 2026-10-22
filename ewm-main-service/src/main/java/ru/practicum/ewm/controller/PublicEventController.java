package ru.practicum.ewm.controller;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.event.request.PublicParamSearch;
import ru.practicum.ewm.dto.event.response.EventFullDto;
import ru.practicum.ewm.dto.event.response.EventShortDto;
import ru.practicum.ewm.service.EventService;

import java.util.List;

/**
 * Контроллер для публичного доступа к событиям.
 * Предоставляет эндпоинты для поиска событий и просмотра подробной информации о событиях.
 */
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final EventService eventService;


    /**
     * Получает список опубликованных событий с возможностью фильтрации.
     *
     * @param request параметры поиска и фильтрации событий
     * @return список кратких DTO событий
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> getEvents(@ModelAttribute @Valid PublicParamSearch request, HttpServletRequest servletRequest) {
        return eventService.getPublicEvents(request, servletRequest);
    }

    /**
     * Получает полную информацию о конкретном опубликованном событии.
     *
     * @param eventId        идентификатор события
     * @param servletRequest HTTP запрос для статистики просмотров
     * @return полное DTO события
     */
    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto getEvent(@PathVariable Long eventId, HttpServletRequest servletRequest) {
        return eventService.getPublicEventId(eventId, servletRequest);
    }

}
