package ru.practicum.ewm.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.model.Compilation;

import java.util.List;

/**
 * Репозиторий для работы с подборками событий.
 */
@Repository
public interface CompilationRepository extends JpaRepository<Compilation, Long> {

    /**
     * Найти все подборки с фильтрацией по флагу pinned.
     * Если pinned == null, возвращает все подборки.
     */
    List<Compilation> findAllByPinned(Boolean pinned, Pageable pageable);
}