package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.category.request.CategorySearchRequest;
import ru.practicum.ewm.dto.category.request.CategoryUpdateRequest;
import ru.practicum.ewm.dto.category.request.NewCategoryDto;
import ru.practicum.ewm.dto.category.response.CategoryDto;
import ru.practicum.ewm.exception.model.NotFoundException;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.service.CategoryService;
import ru.practicum.ewm.service.mapper.CategoryMapper;

import java.util.List;

/**
 * Реализация сервиса категорий.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private static final String CATEGORY_NOT_FOUND_EXCEPTION = "Категория не найдена id=";

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryDto addCategory(NewCategoryDto dto) {
        Category category = CategoryMapper.toEntity(dto);
        Category saved = categoryRepository.save(category);
        return CategoryMapper.toDto(saved);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(CategoryUpdateRequest request) {
        Category category = getCategory(request.getCatId());

        category.setName(request.getCategoryDto().getName());
        Category saved = categoryRepository.save(category);
        return CategoryMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        Category category = getCategory(catId);

        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories(CategorySearchRequest request) {
        int page = request.getFrom() / request.getSize();
        PageRequest pageable = PageRequest.of(page, request.getSize());

        Specification<Category> spec = Specification.where(null);

        return categoryRepository.findAll(spec, pageable).getContent().stream()
                .map(CategoryMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long catId) {
        Category category = getCategory(catId);
        return CategoryMapper.toDto(category);
    }

    private Category getCategory(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException(CATEGORY_NOT_FOUND_EXCEPTION + catId));
    }
}
