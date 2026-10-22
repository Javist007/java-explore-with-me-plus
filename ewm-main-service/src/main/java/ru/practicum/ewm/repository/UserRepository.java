package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.model.User;


/**
 * Репозиторий для работы с сущностью пользователя с поддержкой Specification.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * Проверка наличия пользователя по email.
     */
    boolean existsByEmail(String email);
}
