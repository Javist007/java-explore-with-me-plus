package ru.practicum.ewm.dto.event.request;

import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для параметров поиска событий пользователя.
 * Используется для пагинации при получении списка событий пользователя.
 */
@Data
@NoArgsConstructor
public class EventSearchRequest {
    /**
     * Индекс первого элемента для пагинации.
     */
    @Min(value = 0, message = "from не может быть меньше 0")
    private Integer from = 0;
    /**
     * Количество элементов на странице для пагинации.
     */
    @Min(value = 1, message = "size не может быть меньше 1")
    private Integer size = 10;
}
