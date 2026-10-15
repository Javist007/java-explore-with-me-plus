package ru.practicum.ewm.dto.event.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Краткое DTO события.
 * Содержит основную информацию о событии для отображения в списках.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventShortDto {

    /**
     * Идентификатор события.
     */
    @JsonProperty("id")
    private Long id;

    /**
     * Краткое описание события (аннотация).
     */
    @JsonProperty("annotation")
    private String annotation;

    /**
     * Категория события.
     */
    @JsonProperty("category")
    private CategoryDto category;

    /**
     * Количество подтвержденных заявок на участие.
     */
    @JsonProperty("confirmedRequests")
    private Long confirmedRequests;

    /**
     * Дата и время проведения события.
     */
    @JsonProperty("eventDate")
    private String eventDate;

    /**
     * Инициатор события (пользователь).
     */
    @JsonProperty("initiator")
    private UserShortDto initiator;

    /**
     * Флаг, указывающий является ли событие платным.
     */
    @JsonProperty("paid")
    private Boolean paid;

    /**
     * Заголовок события.
     */
    @JsonProperty("title")
    private String title;

    /**
     * Количество просмотров события.
     */
    @JsonProperty("views")
    private Long views;

    /**
     * DTO категории события.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDto {
        /**
         * Идентификатор категории.
         */
        @JsonProperty("id")
        private Long id;

        /**
         * Название категории.
         */
        @JsonProperty("name")
        private String name;
    }

    /**
     * Краткое DTO пользователя.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserShortDto {
        /**
         * Идентификатор пользователя.
         */
        @JsonProperty("id")
        private Long id;

        /**
         * Имя пользователя.
         */
        @JsonProperty("name")
        private String name;
    }
}
