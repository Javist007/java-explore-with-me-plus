package ru.practicum.ewm.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.model.Compilation;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompilationRepository extends JpaRepository<Compilation, Long> {

    @Query("SELECT c.id FROM Compilation c WHERE c.pinned = :pinned ORDER BY c.id")
    List<Long> findIdsByPinned(@Param("pinned") Boolean pinned, Pageable pageable);

    @Query("SELECT c.id FROM Compilation c ORDER BY c.id")
    List<Long> findAllIds(Pageable pageable);

    @Query("SELECT DISTINCT c FROM Compilation c " +
            "LEFT JOIN FETCH c.events e " +
            "LEFT JOIN FETCH e.category " +
            "LEFT JOIN FETCH e.initiator " +
            "WHERE c.id IN :ids")
    List<Compilation> findByIdsWithEvents(@Param("ids") List<Long> ids);

    @Query("SELECT c FROM Compilation c " +
            "LEFT JOIN FETCH c.events e " +
            "LEFT JOIN FETCH e.category " +
            "LEFT JOIN FETCH e.initiator " +
            "WHERE c.id = :id")
    Optional<Compilation> findByIdWithEvents(@Param("id") Long id);
}