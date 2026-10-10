package ru.practicum.ewm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.category.request.CategorySearchRequest;
import ru.practicum.ewm.dto.category.response.CategoryDto;
import ru.practicum.ewm.service.CategoryService;

import java.util.List;

/**
 * Публичный контроллер для управления категориями.
 */
@Validated
@RestController
@RequestMapping(path = "/categories")
@RequiredArgsConstructor
public class PublicCategoryController {

    private final CategoryService categoryService;

    /**
     * Получение списка категорий.
     */
    @GetMapping
    public List<CategoryDto> getCategories(
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {
        CategorySearchRequest request = new CategorySearchRequest(from, size);
        return categoryService.getAllCategories(request);
    }

    /**
     * Получение категории по ID.
     */
    @GetMapping("/{catId}")
    public CategoryDto getCategoryById(@PathVariable Long catId) {
        return categoryService.getCategoryById(catId);
    }
}
