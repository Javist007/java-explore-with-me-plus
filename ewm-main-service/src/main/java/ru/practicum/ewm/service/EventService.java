package ru.practicum.ewm.service;

import jakarta.servlet.http.HttpServletRequest;

import ru.practicum.ewm.dto.event.request.*;
import ru.practicum.ewm.dto.event.response.EventFullDto;
import ru.practicum.ewm.dto.event.response.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.event.response.EventShortDto;
import ru.practicum.ewm.dto.participation.response.ParticipationRequestDto;

import java.util.List;

public interface EventService {
    EventFullDto createEvent(NewEventRequest request, Long userId);

    List<EventShortDto> getEvents(Long userId, EventSearchRequest request);

    EventFullDto getEventsUser(Long userId, Long eventId);

    EventFullDto updateEvent(UpdateEventContext context);

    List<ParticipationRequestDto> getEventRequest(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateEventRequests(EventRequestStatusUpdateContext context);

    List<EventFullDto> adminGetEvents(AdminParamSearch request);

    EventFullDto adminUpdateEvent(Long eventId, UpdateEventAdminRequest request);

    List<EventShortDto> getPublicEvents(PublicParamSearch request);

    EventFullDto getPublicEventId(Long eventId, HttpServletRequest servletRequest);

}
