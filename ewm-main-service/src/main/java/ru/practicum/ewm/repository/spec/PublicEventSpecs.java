package ru.practicum.ewm.repository.spec;

import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Класс спецификаций для публичного поиска событий.
 * Предоставляет статические методы для создания JPA Specification для фильтрации опубликованных событий.
 */
public class PublicEventSpecs {

    /**
     * Создает спецификацию для текстового поиска по аннотации и описанию события.
     * Поиск выполняется без учета регистра.
     *
     * @param text текст для поиска
     * @return спецификация для текстового поиска
     */
    public static Specification<Event> textSearch(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) return cb.conjunction();
            String pattern = "%" + text.toLowerCase() + "%";
            Expression<String> annotation = cb.lower(root.get("annotation"));
            Expression<String> description = cb.lower(root.get("description"));

            return cb.or(
                    cb.like(annotation, pattern),
                    cb.like(description, pattern)
            );
        };
    }

    /**
     * Создает спецификацию для фильтрации только опубликованных событий.
     *
     * @return спецификация для фильтрации по статусу PUBLISHED
     */
    public static Specification<Event> isPublished() {
        return (root, query, cb) ->
                cb.equal(root.get("state"), EventState.PUBLISHED);
    }

    /**
     * Создает спецификацию для фильтрации по платности событий.
     *
     * @param paid флаг платности (null для отключения фильтрации)
     * @return спецификация для фильтрации по платности
     */
    public static Specification<Event> isPaid(Boolean paid) {
        return (root, query, cb) -> paid != null ? cb.equal(root.get("paid"), paid) : cb.conjunction();
    }

    /**
     * Создает спецификацию для фильтрации по диапазону дат проведения событий.
     * Поддерживает фильтрацию по началу, концу или обоим границам диапазона.
     *
     * @param start начало диапазона дат (может быть null)
     * @param end   конец диапазона дат (может быть null)
     * @return спецификация для фильтрации по диапазону дат
     */
    public static Specification<Event> eventDateRange(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start != null && end != null) {
                return cb.between(root.get("eventDate"), start, end);
            } else if (start != null) {
                return cb.greaterThanOrEqualTo(root.get("eventDate"), start);
            } else if (end != null) {
                return cb.lessThanOrEqualTo(root.get("eventDate"), end);
            } else {
                return cb.conjunction();
            }
        };
    }

    /**
     * Создает спецификацию для фильтрации только событий с доступными местами.
     * Событие доступно, если лимит участников равен 0 или количество подтвержденных заявок меньше лимита.
     *
     * @return спецификация для фильтрации по доступности мест
     */
    public static Specification<Event> isAvailable() {
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
            return cb.and(
                    cb.equal(root.get("state"), EventState.PUBLISHED),
                    cb.or(
                            cb.equal(root.get("participantLimit"), 0),
                            cb.lessThan(root.get("confirmedRequests"), root.get("participantLimit"))
                    )
            );
        };
    }

    /**
     * Создает спецификацию для фильтрации по категориям событий.
     *
     * @param categories список идентификаторов категорий (может быть null или пустым)
     * @return спецификация для фильтрации по категориям
     */
    public static Specification<Event> hasCategories(List<Long> categories) {
        return (root, query, cb) -> {
            if (categories != null && !categories.isEmpty()) {
                return root.get("category").get("id").in(categories);
            }

            return cb.conjunction();
        };
    }

    /**
     * Создает спецификацию для фильтрации только будущих событий.
     * Событие считается будущим, если его дата проведения не раньше текущего момента.
     *
     * @return спецификация для фильтрации будущих событий
     */
    public static Specification<Event> isFuture() {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("eventDate"), LocalDateTime.now());
    }

}
