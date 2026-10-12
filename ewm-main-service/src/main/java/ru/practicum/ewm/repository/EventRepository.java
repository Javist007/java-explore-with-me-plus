package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.Event;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    @Query("SELECT e FROM Event e " +
            "WHERE e.initiator.id = :userId " +
            "ORDER BY e.id DESC " +
            "LIMIT :size OFFSET :from")
    List<Event> findAllByInitiatorIdWithPagination(
            @Param("userId") Long userId,
            @Param("from") int from,
            @Param("size") int size);

    Event findByInitiatorIdAndId(Long initiatorId, Long eventId);


}
