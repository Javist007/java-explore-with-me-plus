package ru.practicum.ewm.dto.event.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для параметров поиска событий администратором.
 * Позволяет фильтровать события по пользователям, состояниям, категориям и датам.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminParamSearch {
    /**
     * Список идентификаторов пользователей-инициаторов событий.
     */
    private List<Long> users;
    /**
     * Список состояний событий для фильтрации.
     */
    private List<String> states;
    /**
     * Список идентификаторов категорий для фильтрации.
     */
    private List<Long> categories;
    /**
     * Начало диапазона дат событий.
     */
    private String rangeStart;
    /**
     * Конец диапазона дат событий.
     */
    private String rangeEnd;
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
