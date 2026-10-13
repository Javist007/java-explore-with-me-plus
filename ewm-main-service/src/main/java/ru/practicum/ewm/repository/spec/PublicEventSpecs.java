package ru.practicum.ewm.repository.spec;

import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;

import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

public class PublicEventSpecs {

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

    public static Specification<Event> isPublished() {
        return (root, query, cb) ->
                cb.equal(root.get("state"), EventState.PUBLISHED);
    }

    public static Specification<Event> isPaid(Boolean paid) {
        return (root, query, cb) -> paid != null ? cb.equal(root.get("paid"), paid) : cb.conjunction();
    }

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

    public static Specification<Event> hasCategories(List<Long> categories) {
        return (root, query, cb) -> {
            if (categories != null && !categories.isEmpty()) {
                return root.get("category").get("id").in(categories);
            }

            return cb.conjunction();
        };
    }

    public static Specification<Event> isFuture() {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("eventDate"), LocalDateTime.now());
    }

}
