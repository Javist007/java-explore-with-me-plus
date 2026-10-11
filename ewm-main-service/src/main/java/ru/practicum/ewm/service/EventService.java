package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.event.request.NewEventRequest;
import ru.practicum.ewm.dto.event.response.EventFullDto;
import ru.practicum.ewm.dto.event.response.EventShortDto;

import java.util.List;

public interface EventService {
    void createEvent(NewEventRequest request, Long userId);

    List<EventShortDto> getEvents(Long userId);

    EventFullDto getEventsUser(Long userId, Long eventId);
}
