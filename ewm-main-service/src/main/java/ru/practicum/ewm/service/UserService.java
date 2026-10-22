package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.user.request.NewUserRequest;
import ru.practicum.ewm.dto.user.request.UserSearchRequest;
import ru.practicum.ewm.dto.user.response.UserDto;

import java.util.List;

/**
 * Интерфейс сервиса для работы с пользователями.
 */
public interface UserService {

    /**
     * Регистрация нового пользователя
     */
    UserDto registerUser(NewUserRequest request);

    /**
     * Получение списка пользователей с фильтрацией и пагинацией
     */
    List<UserDto> getUsers(UserSearchRequest request);

    /**
     * Удаление пользователя по ID
     */
    void deleteUser(Long userId);
}