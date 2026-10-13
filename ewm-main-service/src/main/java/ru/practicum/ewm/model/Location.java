package ru.practicum.ewm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * Широта и долгота места проведения события. Хранится в колонках lat и lon таблицы events.
 */
@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    @Column(nullable = false)
    private Float lat;

    @Column(nullable = false)
    private Float lon;
}
