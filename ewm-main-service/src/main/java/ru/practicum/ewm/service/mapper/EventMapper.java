package ru.practicum.ewm.service.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.dto.event.response.EventFullDto;
import ru.practicum.ewm.dto.event.request.NewEventRequest;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.model.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Класс для преобразования объектов Event и EventDto.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Преобразует запрос на создание события в сущность.
     */
    public static Event toEntity(NewEventRequest request, User initiator, Category category) {
        return Event.builder()
                .annotation(request.getAnnotation())
                .category(category)
                .confirmedRequests(0L)
                .createdOn(LocalDateTime.now())
                .description(request.getDescription())
                .eventDate(LocalDateTime.parse(request.getEventDate(), FORMATTER))
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
     * Преобразует сущность в DTO.
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
                .createdOn(event.getCreatedOn().format(FORMATTER))
                .description(event.getDescription())
                .eventDate(event.getEventDate().format(FORMATTER))
                .initiator(EventFullDto.UserShortDto.builder()
                        .id(event.getInitiator().getId())
                        .name(event.getInitiator().getName())
                        .build())
                .location(event.getLocation())
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn() != null ? event.getPublishedOn().format(FORMATTER) : null)
                .requestModeration(event.getRequestModeration())
                .state(event.getState().name())
                .title(event.getTitle())
                .views(event.getViews())
                .build();
    }

}
