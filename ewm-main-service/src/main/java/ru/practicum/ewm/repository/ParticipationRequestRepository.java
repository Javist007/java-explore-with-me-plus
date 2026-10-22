package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.model.ParticipationRequest;
import ru.practicum.ewm.model.RequestStatus;
import ru.practicum.ewm.repository.projection.EventRequestCount;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с заявками на участие в событиях.
 */
@Repository
public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    /**
     * Все заявки пользователя на участие в чужих событиях.
     */
    List<ParticipationRequest> findAllByRequesterId(Long requesterId);

    /**
     * Все заявки на участие в событии.
     */
    List<ParticipationRequest> findAllByEventId(Long eventId);

    /**
     * Заявки с указанными идентификаторами, относящиеся к событию.
     */
    List<ParticipationRequest> findAllByIdInAndEventId(Collection<Long> ids, Long eventId);

    /**
     * Заявка по идентификатору среди заявок указанного пользователя.
     */
    Optional<ParticipationRequest> findByIdAndRequesterId(Long id, Long requesterId);

    /**
     * Проверка, подавал ли пользователь заявку на событие.
     */
    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    /**
     * Количество заявок события в указанном статусе.
     */
    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    /**
     * Количество заявок в указанном статусе для каждого из событий одним запросом.
     * События без таких заявок в результат не попадают.
     */
    @Query("SELECT r.event.id AS eventId, COUNT(r.id) AS requestCount " +
            "FROM ParticipationRequest r " +
            "WHERE r.event.id IN :eventIds AND r.status = :status " +
            "GROUP BY r.event.id")
    List<EventRequestCount> countByEventIdsAndStatus(@Param("eventIds") Collection<Long> eventIds,
                                                     @Param("status") RequestStatus status);

    /**
     * Перевод всех заявок события из одного статуса в другой одним запросом.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE ParticipationRequest r SET r.status = :newStatus " +
            "WHERE r.event.id = :eventId AND r.status = :currentStatus")
    int updateStatusByEventId(@Param("eventId") Long eventId,
                              @Param("currentStatus") RequestStatus currentStatus,
                              @Param("newStatus") RequestStatus newStatus);
}
