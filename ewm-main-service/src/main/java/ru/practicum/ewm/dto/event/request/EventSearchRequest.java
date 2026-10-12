package ru.practicum.ewm.dto.event.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EventSearchRequest {
    private int from = 0;
    private int size = 10;
}
