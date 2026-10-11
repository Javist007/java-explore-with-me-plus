package ru.practicum.ewm.dto.event.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.dto.event.Location;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewEventRequest {

    @NotBlank
    @Size(min = 20, max = 2000)
    @JsonProperty("annotation")
    private String annotation;

    @NotNull
    @JsonProperty("category")
    private Long category;

    @NotBlank
    @Size(min = 20, max = 7000)
    @JsonProperty("description")
    private String description;

    @NotBlank
    @JsonProperty("eventDate")
    private String eventDate;

    @NotNull
    @JsonProperty("location")
    private Location location;

    @JsonProperty("paid")
    private Boolean paid = false;

    @JsonProperty("participantLimit")
    private Integer participantLimit = 0;

    @JsonProperty("requestModeration")
    private Boolean requestModeration = true;

    @NotBlank
    @Size(min = 3, max = 120)
    @JsonProperty("title")
    private String title;
}
