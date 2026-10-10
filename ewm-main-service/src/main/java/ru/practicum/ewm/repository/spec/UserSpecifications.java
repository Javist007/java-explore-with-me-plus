package ru.practicum.ewm.repository.spec;

import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.model.User;

import java.util.List;

/**
 * Набор спецификаций для динамической фильтрации пользователей.
 */
public class UserSpecifications {

    /**
     * Спецификация фильтрации по списку идентификаторов.
     * Если список пуст, условие игнорируется (возвращаются все записи).
     */
    public static Specification<User> hasIds(List<Long> ids) {
        return (root, query, cb) -> {
            if (ids == null || ids.isEmpty()) return null;
            return root.get("id").in(ids);
        };
    }
}
