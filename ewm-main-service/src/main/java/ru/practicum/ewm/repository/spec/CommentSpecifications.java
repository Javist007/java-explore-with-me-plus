package ru.practicum.ewm.repository.spec;

import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.model.CommentStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Спецификации для динамической фильтрации комментариев администратором.
 */
public class CommentSpecifications {

    /**
     * Фильтрация по списку идентификаторов авторов.
     */
    public static Specification<Comment> hasUsers(List<Long> ids) {
        return (root, query, cb) -> {
            if (ids == null || ids.isEmpty()) return cb.conjunction();
            return root.get("author").get("id").in(ids);
        };
    }

    /**
     * Фильтрация по списку идентификаторов событий.
     */
    public static Specification<Comment> hasEvents(List<Long> ids) {
        return (root, query, cb) -> {
            if (ids == null || ids.isEmpty()) return cb.conjunction();
            return root.get("event").get("id").in(ids);
        };
    }

    /**
     * Фильтрация по статусу комментария.
     */
    public static Specification<Comment> hasStatus(CommentStatus status) {
        return (root, query, cb) -> {
            if (status == null) return cb.conjunction();
            return cb.equal(root.get("status"), status);
        };
    }

    /**
     * Текстовый поиск по содержанию комментария без учета регистра.
     */
    public static Specification<Comment> textSearch(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) return cb.conjunction();
            String pattern = "%" + text.toLowerCase() + "%";
            Expression<String> content = cb.lower(root.get("text"));
            return cb.like(content, pattern);
        };
    }

    /**
     * Фильтрация по дате создания (начало диапазона).
     */
    public static Specification<Comment> hasRangeStart(LocalDateTime start) {
        return (root, query, cb) -> start != null
                ? cb.greaterThanOrEqualTo(root.get("createdOn"), start)
                : cb.conjunction();
    }

    /**
     * Фильтрация по дате создания (конец диапазона).
     */
    public static Specification<Comment> hasRangeEnd(LocalDateTime end) {
        return (root, query, cb) -> end != null
                ? cb.lessThanOrEqualTo(root.get("createdOn"), end)
                : cb.conjunction();
    }
}
