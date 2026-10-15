package ru.practicum.ewm.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для хранения географических координат местоположения.
 * Используется как встраиваемый объект для хранения широты и долготы.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Location {

    /**
     * Широта местоположения.
     */
    @JsonProperty("lat")
    private Float lat;

    /**
     * Долгота местоположения.
     */
    @JsonProperty("lon")
    private Float lon;
}
