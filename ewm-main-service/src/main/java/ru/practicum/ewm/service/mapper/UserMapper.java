package ru.practicum.ewm.service.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.dto.user.request.NewUserRequest;
import ru.practicum.ewm.dto.user.response.UserDto;
import ru.practicum.ewm.model.User;

/**
 * Класс для преобразования объектов User и UserDto.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserMapper {

    /**
     * Преобразует сущность пользователя в DTO.
     */
    public static UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    /**
     * Преобразует запрос на создание пользователя в сущность.
     */
    public static User toEntity(NewUserRequest request) {
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        return user;
    }

}
