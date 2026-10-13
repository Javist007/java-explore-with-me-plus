package ru.practicum.ewm.dto.participation.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Параметры изменения статуса заявок: кто меняет, у какого события и на что.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestStatusUpdateParams {

    private Long userId;
    private Long eventId;
    private EventRequestStatusUpdateRequest updateRequest;
}
