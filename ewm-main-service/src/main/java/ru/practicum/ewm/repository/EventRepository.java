package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.model.Event;

import java.util.Optional;

/**
 * Репозиторий для работы с сущностью события.
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Поиск события по идентификатору среди событий указанного инициатора.
     */
    Optional<Event> findByIdAndInitiatorId(Long eventId, Long initiatorId);

    /**
     * Проверка, что событие существует и принадлежит указанному инициатору.
     */
    boolean existsByIdAndInitiatorId(Long eventId, Long initiatorId);
}
