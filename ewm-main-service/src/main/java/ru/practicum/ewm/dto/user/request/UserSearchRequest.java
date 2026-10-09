package ru.practicum.ewm.dto.user.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Объект параметров поиска пользователей с поддержкой пагинации.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSearchRequest {

    private List<Long> ids;
    private int from;
    private int size;
}
