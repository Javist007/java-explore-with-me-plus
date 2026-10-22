package ru.practicum.ewm.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.practicum.ewm.model.Category;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("findById должен возвращать Optional с категорией")
    void findById_Success() {
        Category cat = Category.builder().name("Music").build();
        categoryRepository.save(cat);

        Optional<Category> found = categoryRepository.findById(cat.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Music");
    }
}