package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.event.request.NewEventRequest;
import ru.practicum.ewm.dto.event.response.EventFullDto;
import ru.practicum.ewm.service.EventService;

import java.util.List;

@RestController
@Validated
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class PrivateEventController {

    private final EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public void createEvent(@PathVariable Long userId, @RequestBody @Valid NewEventRequest request) {
        eventService.createEvent(request, userId);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Transactional(readOnly = true)
    public List<EventFullDto> getEvents(@PathVariable Long userId) {
        return eventService.getEvents(userId);
    }
}
