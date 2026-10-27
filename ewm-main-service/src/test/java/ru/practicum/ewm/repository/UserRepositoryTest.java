package ru.practicum.ewm.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import ru.practicum.ewm.model.User;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("existsByEmail должен возвращать true, если пользователь есть")
    void existsByEmail_True() {
        User user = User.builder().name("Test").email("test@test.ru").build();
        userRepository.save(user);

        assertThat(userRepository.existsByEmail("test@test.ru")).isTrue();
    }

    @Test
    @DisplayName("Сохранение пользователя с существующим email должен вызвать DataIntegrityViolationException")
    void saveDuplicateEmail_ThrowsException() {
        User user1 = User.builder().name("User1").email("dup@test.ru").build();
        userRepository.save(user1);

        User user2 = User.builder().name("User2").email("dup@test.ru").build();
        assertThatThrownBy(() -> userRepository.save(user2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
