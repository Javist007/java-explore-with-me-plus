package ru.practicum.ewm.dto.user.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Краткий DTO для передачи информации о пользователе в API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserShortDto {

    private Long id;
    private String name;
}
