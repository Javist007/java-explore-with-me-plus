package ru.practicum.ewm.dto.participation.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.model.RequestStatus;

import java.util.List;

/**
 * Изменение статуса заявок на участие в событии текущего пользователя.
 * Допустимые значения status: CONFIRMED и REJECTED.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateRequest {

    @NotEmpty
    private List<Long> requestIds;

    @NotNull
    private RequestStatus status;
}
