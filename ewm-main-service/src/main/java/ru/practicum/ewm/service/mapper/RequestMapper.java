package ru.practicum.ewm.service.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.dto.participation.response.ParticipationRequestDto;
import ru.practicum.ewm.model.ParticipationRequest;

/**
 * Класс для преобразования заявок на участие в DTO.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestMapper {

    /**
     * Преобразует заявку в DTO. Идентификаторы события и пользователя берутся из прокси
     * без дополнительных запросов к БД.
     */
    public static ParticipationRequestDto toDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .event(request.getEvent().getId())
                .requester(request.getRequester().getId())
                .status(request.getStatus())
                .build();
    }
}
