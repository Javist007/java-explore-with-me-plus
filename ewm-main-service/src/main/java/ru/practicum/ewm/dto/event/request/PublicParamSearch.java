package ru.practicum.ewm.dto.event.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для параметров публичного поиска событий.
 * Позволяет фильтровать опубликованные события по различным критериям.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicParamSearch {
    /**
     * Текст для поиска в аннотации и описании события.
     */
    private String text;
    /**
     * Список идентификаторов категорий для фильтрации.
     */
    private List<Long> categories;
    /**
     * Флаг для фильтрации только платных или бесплатных событий.
     */
    private Boolean paid;
    /**
     * Начало диапазона дат событий.
     */
    private String rangeStart;
    /**
     * Конец диапазона дат событий.
     */
    private String rangeEnd;
    /**
     * Флаг для поиска только событий с доступными местами.
     */
    private Boolean onlyAvailable;
    /**
     * Параметр сортировки результатов.
     */
    private Sort sort;
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

    /**
     * Перечисление вариантов сортировки событий.
     */
    public enum Sort {
        EVENT_DATE,
        VIEWS
    }
}
