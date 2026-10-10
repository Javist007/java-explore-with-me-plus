package ru.practicum.ewm.dto.category.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.dto.category.response.CategoryDto;

/**
 * Объект для передачи данных при обновлении существующей категории.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryUpdateRequest {

    private Long catId;
    private CategoryDto categoryDto;
}
