package ru.practicum.ewm.dto.category.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Объект параметров получения списка категорий с поддержкой пагинации.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategorySearchRequest {

    private int from;
    private int size;
}
