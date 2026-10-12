package ru.practicum.ewm.dto.event.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateEventContext {
    private UpdateEventRequest request;
    private Long userId;
    private Long eventId;
}
