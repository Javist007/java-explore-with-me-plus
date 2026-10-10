package ru.practicum.ewm.dto.category.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO для передачи информации о категории в API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CategoryDto {

    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank(message = "Название не должно быть пустым")
    @Size(min = 1, max = 50, message = "Длинна названия должна быть в диапазоне от 1 до 50")
    private String name;
}
