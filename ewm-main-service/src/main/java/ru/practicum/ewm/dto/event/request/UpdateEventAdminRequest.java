package ru.practicum.ewm.dto.event.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.dto.event.Location;

/**
 * DTO для обновления события администратором.
 * Содержит поля для изменения информации о событии и его статуса.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventAdminRequest {
    /**
     * Краткое описание события (аннотация).
     */
    @Size(min = 20, max = 2000)
    private String annotation;

    /**
     * Идентификатор категории события.
     */
    private Long category;

    /**
     * Полное описание события.
     */
    @Size(min = 20, max = 7000)
    private String description;

    /**
     * Дата и время проведения события в формате ISO 8601.
     */
    private String eventDate;

    /**
     * Географические координаты места проведения события.
     */
    private Location location;

    /**
     * Флаг, указывающий является ли событие платным.
     */
    private Boolean paid;

    /**
     * Лимит количества участников события.
     */
    private Integer participantLimit;

    /**
     * Флаг, указывающий нужна ли модерация заявок на участие.
     */
    private Boolean requestModeration;

    /**
     * Действие администратора для изменения статуса события.
     */
    private StateAction stateAction;

    /**
     * Заголовок события.
     */
    @Size(min = 3, max = 120)
    private String title;

    /**
     * Перечисление действий администратора над событием.
     */
    public enum StateAction {
        PUBLISH_EVENT,
        REJECT_EVENT
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
