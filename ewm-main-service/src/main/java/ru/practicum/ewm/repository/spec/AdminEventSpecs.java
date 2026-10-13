package ru.practicum.ewm.repository.spec;

import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.common.DateTimeConstants;
import ru.practicum.ewm.dto.event.request.AdminParamSearch;
import ru.practicum.ewm.exception.model.BadRequestException;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

public class AdminEventSpecs {
    public static Specification<Event> hasUser(AdminParamSearch search) {
        return (root, query, cb) ->
                (search.getUsers() != null && !search.getUsers().isEmpty())
                        ? root.get("initiator").in(search.getUsers())
                        : cb.conjunction();
    }

    public static Specification<Event> hasStates(AdminParamSearch search) {
        return (root, query, cb) -> {
            if (search.getStates() == null || search.getStates().isEmpty()) return cb.conjunction();
            List<EventState> converted = search.getStates().stream()
                    .map(s -> {
                        try {
                            return EventState.valueOf(s.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            throw new BadRequestException(
                                    "Недопустимый статус события: " + s);
                        }
                    })
                    .toList();
            return root.get("state").in(converted);
        };
    }

    public static Specification<Event> hasCategories(AdminParamSearch search) {
        return (root, query, cb) -> {
            List<Long> categories = search.getCategories();

            if (categories != null && !categories.isEmpty()) {
                return root.get("category").get("id").in(categories);
            }

            return cb.conjunction();
        };
    }


    public static Specification<Event> hasRangeStart(LocalDateTime start) {
        return (root, query, cb) -> start != null
                ? cb.greaterThanOrEqualTo(root.get("eventDate"), start)
                : cb.conjunction();
    }

    public static Specification<Event> hasRangeEnd(LocalDateTime end) {
        return (root, query, cb) -> end != null
                ? cb.lessThanOrEqualTo(root.get("eventDate"), end)
                : cb.conjunction();
    }

}
