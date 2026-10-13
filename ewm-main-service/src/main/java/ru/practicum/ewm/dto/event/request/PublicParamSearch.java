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
public class PublicParamSearch {
    private String text;
    private List<Long> categories;
    private Boolean paid;
    private String rangeStart;
    private String rangeEnd;
    private Boolean onlyAvailable;
    private Sort sort;
    @Min(value = 0, message = "from не может быть меньше 0")
    private Integer from = 0;
    @Min(value = 1, message = "size не может быть меньше 1")
    private Integer size = 10;

    public enum Sort {
        EVENT_DATE,
        VIEWS
    }
}
