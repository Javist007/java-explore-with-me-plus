package ru.practicum.ewm.repository.spec;

import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.common.DateTimeConstants;
import ru.practicum.ewm.dto.event.request.AdminParamSearch;
import ru.practicum.ewm.model.Event;

import java.time.LocalDateTime;
import java.util.List;

public class AdminEventSpecs {
    public static Specification<Event> hasUser(AdminParamSearch search) {
        return (root, query, cb) ->
                search.getUsers() != null ? root.get("initiator").in(search.getUsers()) : null;
    }

    public static Specification<Event> hasStates(AdminParamSearch search) {
        return (root, query, cb) ->
                search.getStates() != null ? root.get("state").in(search.getStates()) : null;
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
