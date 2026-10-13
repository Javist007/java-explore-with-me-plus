package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.event.request.AdminParamSearch;
import ru.practicum.ewm.dto.event.request.UpdateEventAdminRequest;
import ru.practicum.ewm.dto.event.response.EventFullDto;
import ru.practicum.ewm.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
@Validated
public class AdminEventController {

    private  final EventService eventService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventFullDto> getEvents(@ModelAttribute AdminParamSearch request) {
        return eventService.adminGetEvents(request);
    }

    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto updateEvent(@PathVariable Long eventId, @RequestBody UpdateEventAdminRequest request) {
        return eventService.adminUpdateEvent(eventId, request);
    }
}
