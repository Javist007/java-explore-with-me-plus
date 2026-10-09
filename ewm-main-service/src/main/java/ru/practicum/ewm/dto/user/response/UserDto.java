package ru.practicum.ewm.dto.user.response;

import lombok.*;

/**
 * DTO для передачи информации о пользователе в API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserDto {

    @EqualsAndHashCode.Include
    private Long id;
    private String name;
    private String email;
}
