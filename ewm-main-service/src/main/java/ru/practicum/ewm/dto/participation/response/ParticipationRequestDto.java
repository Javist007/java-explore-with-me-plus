package ru.practicum.ewm.dto.participation.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.model.RequestStatus;

import java.time.LocalDateTime;

/**
 * Заявка на участие в событии.
 * Поле created отдаётся в ISO-формате, как в примере спецификации ("2022-09-06T21:10:05.432").
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationRequestDto {

    private Long id;
    private LocalDateTime created;
    private Long event;
    private Long requester;
    private RequestStatus status;
}
