package ru.practicum.ewm.dto.event.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventRequestStatusUpdateContext {
    EventRequestStatusUpdateRequest request;
    Long userId;
    Long eventId;
}
