package ru.practicum.ewm.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Location {

    @JsonProperty("lat")
    private Float lat;

    @JsonProperty("lon")
    private Float lon;
}
