package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.category.request.CategorySearchRequest;
import ru.practicum.ewm.dto.category.request.CategoryUpdateRequest;
import ru.practicum.ewm.dto.category.request.NewCategoryDto;
import ru.practicum.ewm.dto.category.response.CategoryDto;

import java.util.List;

/**
 * Интерфейс сервиса для работы с категориями.
 */
public interface CategoryService {
    /**
     * Добавление новой категории
     */
    CategoryDto addCategory(NewCategoryDto dto);

    /**
     * Обновление существующей категории
     */
    CategoryDto updateCategory(CategoryUpdateRequest request);

    /**
     * Удаление категории по ID
     */
    void deleteCategory(Long catId);

    /**
     * Получение списка категорий с пагинацией
     */
    List<CategoryDto> getAllCategories(CategorySearchRequest request);

    /**
     * Получение информации о категории по ID
     */
    CategoryDto getCategoryById(Long catId);
}
