package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.event.request.*;
import ru.practicum.ewm.dto.event.response.EventFullDto;
import ru.practicum.ewm.dto.event.response.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.event.response.EventShortDto;
import ru.practicum.ewm.dto.event.response.ParticipationRequestDto;
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

    public EventFullDto createEvent(@PathVariable Long userId, @RequestBody @Valid NewEventRequest request) {
        return eventService.createEvent(request, userId);
    }

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

    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto updateEvent(@PathVariable Long userId, @PathVariable Long eventId, @RequestBody @Valid UpdateEventRequest request) {
        return eventService.updateEvent(UpdateEventContext.builder().request(request).userId(userId).eventId(eventId).build());
    }

    @GetMapping("/{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getEventRequests(@PathVariable Long userId, @PathVariable Long eventId) {
        return eventService.getEventRequest(userId, eventId);
    }

    @PatchMapping("/{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public EventRequestStatusUpdateResult updateEventRequests(@RequestBody @Valid EventRequestStatusUpdateRequest request, @PathVariable Long userId, @PathVariable Long eventId) {
        return eventService.updateEventRequests(EventRequestStatusUpdateContext.builder().request(request).userId(userId).eventId(eventId).build());
    }

}
