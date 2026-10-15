package ru.practicum.ewm.dto.event.request;

import lombok.Builder;
import lombok.Data;

/**
 * Контекст для обновления события пользователем.
 * Объединяет запрос на обновление с идентификаторами пользователя и события.
 */
@Data
@Builder
public class UpdateEventContext {
    /**
     * Запрос на обновление события.
     */
    private UpdateEventRequest request;
    /**
     * Идентификатор пользователя, инициирующего обновление.
     */
    private Long userId;
    /**
     * Идентификатор обновляемого события.
     */
    private Long eventId;
}
