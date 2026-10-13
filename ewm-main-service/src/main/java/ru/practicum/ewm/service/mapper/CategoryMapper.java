package ru.practicum.ewm.service.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.dto.category.request.NewCategoryDto;
import ru.practicum.ewm.dto.category.response.CategoryDto;
import ru.practicum.ewm.model.Category;

/**
 * Класс для преобразования объектов Category и CategoryDto.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CategoryMapper {
    /**
     * Преобразует сущность категории в DTO.
     */
    public static CategoryDto toDto(Category category) {
        if (category == null) {
            return null;
        }
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }

    /**
     * Преобразует запрос на создание категории в сущность.
     */
    public static Category toEntity(NewCategoryDto dto) {
        return Category.builder()
                .name(dto.getName())
                .build();
    }
}
