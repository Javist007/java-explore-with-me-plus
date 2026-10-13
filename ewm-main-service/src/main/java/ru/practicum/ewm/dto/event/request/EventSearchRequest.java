package ru.practicum.ewm.dto.event.request;

import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EventSearchRequest {
    @Min(value = 0, message = "from не может быть меньше 0")
    private Integer from = 0;
    @Min(value = 1, message = "size не может быть меньше 1")
    private Integer size = 10;
}
