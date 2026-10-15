package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.Event;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с сущностями событий.
 * Предоставляет методы для поиска, фильтрации и проверки существования событий.
 */
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    /**
     * Находит все события указанного инициатора с пагинацией.
     *
     * @param userId идентификатор пользователя-инициатора
     * @param from   индекс первого элемента для пагинации
     * @param size   количество элементов на странице
     * @return список событий инициатора
     */
    @Query("SELECT e FROM Event e " +
            "WHERE e.initiator.id = :userId " +
            "ORDER BY e.id DESC " +
            "LIMIT :size OFFSET :from")
    List<Event> findAllByInitiatorIdWithPagination(
            @Param("userId") Long userId,
            @Param("from") int from,
            @Param("size") int size);

    /**
     * Находит событие по идентификатору инициатора и идентификатору события.
     *
     * @param initiatorId идентификатор инициатора события
     * @param eventId     идентификатор события
     * @return Optional с найденным событием или пустой, если не найдено
     */
    Optional<Event> findByInitiatorIdAndId(Long initiatorId, Long eventId);


    /**
     * Поиск события по идентификатору среди событий указанного инициатора (используется в заявках).
     */
    Optional<Event> findByIdAndInitiatorId(Long eventId, Long initiatorId);

    /**
     * Проверка, что событие существует и принадлежит указанному инициатору (используется в заявках).
     */
    boolean existsByIdAndInitiatorId(Long eventId, Long initiatorId);

    /**
     * Проверка, существуют ли события в указанной категории.
     */
    boolean existsByCategoryId(Long categoryId);

}
