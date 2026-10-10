package ru.practicum.ewm.repository.spec;

import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.model.Category;

/**
 * Набор спецификаций для динамической фильтрации категорий.
 */
public class CategorySpecifications {

    /**
     * Спецификация фильтрации по идентификатору.
     */
    public static Specification<Category> hasId(Long id) {
        return (root, query, cb) -> {
            if (id == null) return null;
            return cb.equal(root.get("id"), id);
        };
    }
}

