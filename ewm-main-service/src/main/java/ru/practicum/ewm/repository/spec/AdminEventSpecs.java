package ru.practicum.ewm.repository.spec;

import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.common.DateTimeConstants;
import ru.practicum.ewm.dto.event.request.AdminParamSearch;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

public class AdminEventSpecs {
    public static Specification<Event> hasUser(AdminParamSearch search) {
        return (root, query, cb) ->{
            return search.getUsers() != null && !search.getUsers().isEmpty()
                    ? root.get("initiator").get("id").in(search.getUsers())
                    : null;
        };
    }

    public static Specification<Event> hasStates(AdminParamSearch search) {
        return (root, query, cb) -> {
            if (search.getStates() == null || search.getStates().isEmpty()) return null;
            List<EventState> converted = search.getStates().stream()
                    .map(s -> {
                        try {
                            return EventState.valueOf(s.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            throw new ru.practicum.ewm.exception.model.BadRequestException(
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

            return null;
        };
    }


    public static Specification<Event> hasRangeStart(AdminParamSearch search) {
        return (root, query, cb) -> {
            if (search.getRangeStart() != null) {
                LocalDateTime start = LocalDateTime.parse(search.getRangeStart(), DateTimeConstants.FORMATTER);
                return cb.greaterThanOrEqualTo(root.get("eventDate"), start);
            }
            return null;
        };
    }

    public static Specification<Event> hasRangeEnd(AdminParamSearch search) {
        return (root, query, cb) -> {
            if (search.getRangeEnd() != null) {
                LocalDateTime end = LocalDateTime.parse(search.getRangeEnd(), DateTimeConstants.FORMATTER);
                return cb.lessThanOrEqualTo(root.get("eventDate"), end);
            }
            return null;
        };

    }

}
