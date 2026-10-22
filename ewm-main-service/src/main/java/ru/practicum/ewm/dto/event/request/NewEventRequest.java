package ru.practicum.ewm.dto.event.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.dto.event.Location;

/**
 * DTO для создания нового события.
 * Содержит все необходимые данные для инициализации события пользователем.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewEventRequest {

    /**
     * Краткое описание события (аннотация).
     */
    @NotBlank
    @Size(min = 20, max = 2000)
    @JsonProperty("annotation")
    private String annotation;

    /**
     * Идентификатор категории события.
     */
    @NotNull
    @JsonProperty("category")
    private Long category;

    /**
     * Полное описание события.
     */
    @NotBlank
    @Size(min = 20, max = 7000)
    @JsonProperty("description")
    private String description;

    /**
     * Дата и время проведения события в формате ISO 8601.
     */
    @NotBlank
    @JsonProperty("eventDate")
    private String eventDate;

    /**
     * Географические координаты места проведения события.
     */
    @NotNull
    @JsonProperty("location")
    private Location location;

    /**
     * Флаг, указывающий является ли событие платным.
     */
    @JsonProperty("paid")
    private Boolean paid = false;

    /**
     * Лимит количества участников события.
     */
    @PositiveOrZero
    @JsonProperty("participantLimit")
    private Integer participantLimit = 0;

    /**
     * Флаг, указывающий нужна ли модерация заявок на участие.
     */
    @JsonProperty("requestModeration")
    private Boolean requestModeration = true;

    /**
     * Заголовок события.
     */
    @NotBlank
    @Size(min = 3, max = 120)
    @JsonProperty("title")
    private String title;

}
