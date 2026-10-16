package ru.practicum.ewm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.model.Compilation;

import java.util.Optional;

@Repository
public interface CompilationRepository extends JpaRepository<Compilation, Long> {

    @Query(value = "SELECT c FROM Compilation c " +
            "LEFT JOIN FETCH c.events e " +
            "LEFT JOIN FETCH e.category " +
            "LEFT JOIN FETCH e.initiator " +
            "WHERE c.pinned = :pinned",
            countQuery = "SELECT COUNT(c) FROM Compilation c WHERE c.pinned = :pinned")
    Page<Compilation> findAllByPinnedWithEvents(@Param("pinned") Boolean pinned, Pageable pageable);

    @Query(value = "SELECT c FROM Compilation c " +
            "LEFT JOIN FETCH c.events e " +
            "LEFT JOIN FETCH e.category " +
            "LEFT JOIN FETCH e.initiator",
            countQuery = "SELECT COUNT(c) FROM Compilation c")
    Page<Compilation> findAllWithEvents(Pageable pageable);

    @Query("SELECT c FROM Compilation c " +
            "LEFT JOIN FETCH c.events e " +
            "LEFT JOIN FETCH e.category " +
            "LEFT JOIN FETCH e.initiator " +
            "WHERE c.id = :id")
    Optional<Compilation> findByIdWithEvents(@Param("id") Long id);
}