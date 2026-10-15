package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.event.request.AdminParamSearch;
import ru.practicum.ewm.dto.event.request.UpdateEventAdminRequest;
import ru.practicum.ewm.dto.event.response.EventFullDto;
import ru.practicum.ewm.service.EventService;

import java.util.List;

/**
 * Контроллер для администраторов по управлению событиями.
 * Предоставляет эндпоинты для поиска и обновления событий.
 */
@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
@Validated
public class AdminEventController {

    private final EventService eventService;

    /**
     * Получает список событий с возможностью фильтрации по параметрам администратора.
     *
     * @param request параметры поиска и фильтрации событий
     * @return список полных DTO событий
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventFullDto> getEvents(@ModelAttribute @Valid AdminParamSearch request) {
        return eventService.adminGetEvents(request);
    }

    /**
     * Обновляет информацию о событии от имени администратора.
     *
     * @param eventId идентификатор события
     * @param request данные для обновления события
     * @return полное DTO обновленного события
     */
    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto updateEvent(@PathVariable Long eventId, @RequestBody @Valid UpdateEventAdminRequest request) {
        return eventService.adminUpdateEvent(eventId, request);
    }
}
