package ru.practicum.ewm.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.dto.category.request.*;
import ru.practicum.ewm.dto.category.response.CategoryDto;
import ru.practicum.ewm.exception.model.ConflictException;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private EventRepository eventRepository;
    @InjectMocks private CategoryServiceImpl categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = Category.builder().id(1L).name("Sports").build();
    }

    @Test
    @DisplayName("addCategory: должен сохранять и возвращать DTO")
    void addCategory_Success() {
        NewCategoryDto dto = NewCategoryDto.builder().name("Sports").build();
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryDto result = categoryService.addCategory(dto);

        assertThat(result.getName()).isEqualTo("Sports");
    }

    @Test
    @DisplayName("updateCategory: должен обновлять существующую категорию")
    void updateCategory_Success() {
        CategoryUpdateRequest request = new CategoryUpdateRequest(1L, CategoryDto.builder().name("New Name").build());
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryDto result = categoryService.updateCategory(request);

        assertThat(result.getName()).isEqualTo("New Name");
    }

    @Test
    @DisplayName("deleteCategory: должен выбрасывать ConflictException, если в категории есть события")
    void deleteCategory_ConflictWithEvents() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        // Имитируем наличие событий
        when(eventRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("не может быть удалена, так как в ней есть события");
    }

    @Test
    @DisplayName("deleteCategory: должен удалять категорию, если событий нет")
    void deleteCategory_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(eventRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.deleteCategory(1L);

        verify(categoryRepository).delete(category);
    }

    @Test
    @DisplayName("getAllCategories: должен корректно считать страницу")
    void getAllCategories_Pagination() {
        CategorySearchRequest request = new CategorySearchRequest();
        request.setFrom(10);
        request.setSize(10);

        when(categoryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(category)));

        List<CategoryDto> result = categoryService.getAllCategories(request);
        assertThat(result).hasSize(1);
    }
}
