package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.category.request.CategoryUpdateRequest;
import ru.practicum.ewm.dto.category.request.NewCategoryDto;
import ru.practicum.ewm.dto.category.response.CategoryDto;
import ru.practicum.ewm.service.CategoryService;

/**
 * Контроллер для управления категориями (Админ).
 */
@Validated
@RestController
@RequestMapping(path = "/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    /**
     * Добавление новой категории.
     */
    @PostMapping
    public ResponseEntity<CategoryDto> addCategory(@RequestBody @Valid NewCategoryDto dto) {
        return new ResponseEntity<>(categoryService.addCategory(dto), HttpStatus.CREATED);
    }

    /**
     * Удаление категории.
     */
    @DeleteMapping("/{catId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable @Positive Long catId) {
        categoryService.deleteCategory(catId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Обновление категории.
     */
    @PatchMapping("/{catId}")
    public ResponseEntity<CategoryDto> updateCategory(
            @PathVariable @Positive Long catId,
            @RequestBody @Valid CategoryDto dto) {
        CategoryUpdateRequest request = new CategoryUpdateRequest(catId, dto);
        return ResponseEntity.ok(categoryService.updateCategory(request));
    }
}
