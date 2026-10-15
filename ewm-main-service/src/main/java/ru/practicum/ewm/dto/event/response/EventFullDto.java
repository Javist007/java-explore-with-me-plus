package ru.practicum.ewm.dto.event.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.dto.event.Location;

/**
 * Полное DTO события.
 * Содержит всю информацию о событии включая статистику просмотров и подтвержденные заявки.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventFullDto {

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
     * Дата и время создания события.
     */
    @JsonProperty("createdOn")
    private String createdOn;

    /**
     * Полное описание события.
     */
    @JsonProperty("description")
    private String description;

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
     * Место проведения события.
     */
    @JsonProperty("location")
    private Location location;

    /**
     * Флаг, указывающий является ли событие платным.
     */
    @JsonProperty("paid")
    private Boolean paid;

    /**
     * Лимит количества участников события.
     */
    @JsonProperty("participantLimit")
    private Integer participantLimit;

    /**
     * Дата и время публикации события.
     */
    @JsonProperty("publishedOn")
    private String publishedOn;

    /**
     * Флаг, указывающий нужна ли модерация заявок на участие.
     */
    @JsonProperty("requestModeration")
    private Boolean requestModeration;

    /**
     * Статус события.
     */
    @JsonProperty("state")
    private String state;

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
