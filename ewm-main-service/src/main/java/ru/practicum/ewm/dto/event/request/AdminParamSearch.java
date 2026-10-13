package ru.practicum.ewm.dto.event.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminParamSearch {
    private List<Long> users;
    private List<String> states;
    private List<Long> categories;
    private String rangeStart;
    private String rangeEnd;
    @Min(value = 0, message = "from не может быть меньше 0")
    private Integer from = 0;
    @Min(value = 1, message = "size не может быть меньше 1")
    private Integer size = 10;
}
