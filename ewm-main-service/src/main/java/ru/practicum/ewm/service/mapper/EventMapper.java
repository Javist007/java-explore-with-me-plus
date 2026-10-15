package ru.practicum.ewm.service.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.common.DateTimeConstants;
import ru.practicum.ewm.dto.event.request.NewEventRequest;
import ru.practicum.ewm.dto.event.response.EventFullDto;
import ru.practicum.ewm.dto.event.response.EventShortDto;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.model.User;

import java.time.LocalDateTime;

/**
 * Класс для преобразования объектов Event и EventDto.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventMapper {

    /**
     * Преобразует запрос на создание события в сущность.
     *
     * @param request   запрос на создание события
     * @param initiator пользователь-инициатор события
     * @param category  категория события
     * @return сущность события
     */
    public static Event toEntity(NewEventRequest request, User initiator, Category category) {
        LocalDateTime eventDate = LocalDateTime.parse(request.getEventDate(), DateTimeConstants.FORMATTER);
        return Event.builder()
                .annotation(request.getAnnotation())
                .category(category)
                .confirmedRequests(0L)
                .createdOn(LocalDateTime.now())
                .description(request.getDescription())
                .eventDate(eventDate)
                .initiator(initiator)
                .location(request.getLocation())
                .paid(request.getPaid() != null ? request.getPaid() : false)
                .participantLimit(request.getParticipantLimit() != null ? request.getParticipantLimit() : 0)
                .requestModeration(request.getRequestModeration() != null ? request.getRequestModeration() : true)
                .state(EventState.PENDING)
                .title(request.getTitle())
                .views(0L)
                .build();
    }

    /**
     * Преобразует сущность в полное DTO.
     *
     * @param event сущность события
     * @return полное DTO события
     */
    public static EventFullDto toDto(Event event) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(EventFullDto.CategoryDto.builder()
                        .id(event.getCategory().getId())
                        .name(event.getCategory().getName())
                        .build())
                .confirmedRequests(event.getConfirmedRequests())
                .createdOn(event.getCreatedOn().format(DateTimeConstants.FORMATTER))
                .description(event.getDescription())
                .eventDate(event.getEventDate().format(DateTimeConstants.FORMATTER))
                .initiator(EventFullDto.UserShortDto.builder()
                        .id(event.getInitiator().getId())
                        .name(event.getInitiator().getName())
                        .build())
                .location(event.getLocation())
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn() != null ? event.getPublishedOn().format(DateTimeConstants.FORMATTER) : null)
                .requestModeration(event.getRequestModeration())
                .state(event.getState().name())
                .title(event.getTitle())
                .views(event.getViews())
                .build();
    }

    /**
     * Преобразует сущность в краткое DTO.
     *
     * @param event сущность события
     * @return краткое DTO события
     */
    public static EventShortDto toShortDto(Event event) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(EventShortDto.CategoryDto.builder()
                        .id(event.getCategory().getId())
                        .name(event.getCategory().getName())
                        .build())
                .confirmedRequests(event.getConfirmedRequests())
                .eventDate(event.getEventDate().format(DateTimeConstants.FORMATTER))
                .initiator(EventShortDto.UserShortDto.builder()
                        .id(event.getInitiator().getId())
                        .name(event.getInitiator().getName())
                        .build())
                .paid(event.getPaid())
                .title(event.getTitle())
                .views(event.getViews())
                .build();
    }

}
