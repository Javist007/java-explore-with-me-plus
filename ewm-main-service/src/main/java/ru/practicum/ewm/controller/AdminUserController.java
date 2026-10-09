package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.user.request.NewUserRequest;
import ru.practicum.ewm.dto.user.request.UserSearchRequest;
import ru.practicum.ewm.dto.user.response.UserDto;
import ru.practicum.ewm.service.UserService;

import java.util.List;

/**
 * Контроллер для управления пользователями.
 */
@Validated
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    /**
     * Получение списка пользователей.
     *
     * @param ids  список ID пользователей для фильтрации
     * @param from количество пропущенных элементов
     * @param size количество элементов в ответе
     */
    @GetMapping
    public List<UserDto> getUsers(
            @RequestParam(required = false) List<Long> ids,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {
        UserSearchRequest request = new UserSearchRequest(ids, from, size);
        return userService.getUsers(request);
    }

    /**
     * Регистрация нового пользователя.
     */
    @PostMapping
    public ResponseEntity<UserDto> registerUser(@RequestBody @Valid NewUserRequest request) {
        return new ResponseEntity<>(userService.registerUser(request), HttpStatus.CREATED);
    }

    /**
     * Удаление пользователя.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
