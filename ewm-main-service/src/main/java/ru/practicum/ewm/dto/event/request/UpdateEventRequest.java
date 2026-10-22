package ru.practicum.ewm.dto.event.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.dto.event.Location;

/**
 * DTO для обновления события пользователем.
 * Содержит поля для изменения информации о событии и его статуса.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventRequest {

    /**
     * Краткое описание события (аннотация).
     */
    @Size(min = 20, max = 2000)
    @JsonProperty("annotation")
    private String annotation;

    /**
     * Идентификатор категории события.
     */
    @JsonProperty("category")
    private Long category;

    /**
     * Полное описание события.
     */
    @Size(min = 20, max = 7000)
    @JsonProperty("description")
    private String description;

    /**
     * Дата и время проведения события в формате ISO 8601.
     */
    @JsonProperty("eventDate")
    private String eventDate;

    /**
     * Географические координаты места проведения события.
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
    @PositiveOrZero
    @JsonProperty("participantLimit")
    private Integer participantLimit;

    /**
     * Флаг, указывающий нужна ли модерация заявок на участие.
     */
    @JsonProperty("requestModeration")
    private Boolean requestModeration;

    /**
     * Действие пользователя для изменения статуса события.
     */
    @JsonProperty("stateAction")
    private StateAction stateAction;

    /**
     * Заголовок события.
     */
    @Size(min = 3, max = 120)
    @JsonProperty("title")
    private String title;

    /**
     * Перечисление действий пользователя над событием.
     */
    public enum StateAction {
        SEND_TO_REVIEW,
        CANCEL_REVIEW
    }

    /**
     * Проверяет наличие аннотации в запросе.
     *
     * @return true если аннотация задана, иначе false
     */
    public boolean hasAnnotation() {
        return annotation != null;
    }

    /**
     * Проверяет наличие категории в запросе.
     *
     * @return true если категория задана, иначе false
     */
    public boolean hasCategory() {
        return category != null;
    }

    /**
     * Проверяет наличие описания в запросе.
     *
     * @return true если описание задано, иначе false
     */
    public boolean hasDescription() {
        return description != null;
    }

    /**
     * Проверяет наличие даты события в запросе.
     *
     * @return true если дата задана, иначе false
     */
    public boolean hasEventDate() {
        return eventDate != null;
    }

    /**
     * Проверяет наличие местоположения в запросе.
     *
     * @return true если местоположение задано, иначе false
     */
    public boolean hasLocation() {
        return location != null;
    }

    /**
     * Проверяет наличие флага платности в запросе.
     *
     * @return true если флаг задан, иначе false
     */
    public boolean hasPaid() {
        return paid != null;
    }

    /**
     * Проверяет наличие лимита участников в запросе.
     *
     * @return true если лимит задан, иначе false
     */
    public boolean hasParticipantLimit() {
        return participantLimit != null;
    }

    /**
     * Проверяет наличие флага модерации в запросе.
     *
     * @return true если флаг задан, иначе false
     */
    public boolean hasRequestModeration() {
        return requestModeration != null;
    }

    /**
     * Проверяет наличие действия над статусом в запросе.
     *
     * @return true если действие задано, иначе false
     */
    public boolean hasStateAction() {
        return stateAction != null;
    }

    /**
     * Проверяет наличие заголовка в запросе.
     *
     * @return true если заголовок задан, иначе false
     */
    public boolean hasTitle() {
        return title != null;
    }
}
