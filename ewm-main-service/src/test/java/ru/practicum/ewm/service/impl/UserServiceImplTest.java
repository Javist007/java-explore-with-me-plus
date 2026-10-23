package ru.practicum.ewm.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.dto.user.request.NewUserRequest;
import ru.practicum.ewm.dto.user.request.UserSearchRequest;
import ru.practicum.ewm.dto.user.response.UserDto;
import ru.practicum.ewm.exception.model.ConflictException;
import ru.practicum.ewm.exception.model.NotFoundException;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("Ivan").email("ivan@test.ru").build();
    }

    @Test
    @DisplayName("registerUser: должен успешно регистрировать нового пользователя")
    void registerUser_Success() {
        NewUserRequest request = NewUserRequest.builder().name("Ivan").email("ivan@test.ru").build();
        when(userRepository.existsByEmail("ivan@test.ru")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDto result = userService.registerUser(request);

        assertThat(result.getId()).isEqualTo(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("registerUser: должен выбрасывать ConflictException при дублировании email")
    void registerUser_DuplicateEmail_ThrowsConflict() {
        NewUserRequest request = NewUserRequest.builder().name("Ivan").email("ivan@test.ru").build();
        when(userRepository.existsByEmail("ivan@test.ru")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("уже существует");
    }

    @Test
    @DisplayName("getUsers: должен корректно считать страницу и возвращать список")
    void getUsers_Success() {
        UserSearchRequest request = new UserSearchRequest();
        request.setFrom(20);
        request.setSize(10);

        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        List<UserDto> result = userService.getUsers(request);
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("deleteUser: должен выбрасывать NotFoundException, если пользователя нет")
    void deleteUser_NotFound() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(1L))
                .isInstanceOf(NotFoundException.class);
    }
}
