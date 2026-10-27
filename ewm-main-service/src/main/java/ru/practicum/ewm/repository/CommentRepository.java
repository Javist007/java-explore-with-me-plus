package ru.practicum.ewm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.model.CommentStatus;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий комментариев. Все методы загружают автора и событие вместе с комментарием (без N+1).
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, JpaSpecificationExecutor<Comment> {

    /**
     * Комментарий по идентификатору вместе с автором и событием.
     */
    @EntityGraph(attributePaths = {"author", "event"})
    Optional<Comment> findWithAuthorAndEventById(Long commentId);

    /**
     * Комментарий указанного автора (изменение и удаление своего комментария).
     */
    @EntityGraph(attributePaths = {"author", "event"})
    Optional<Comment> findByIdAndAuthorId(Long commentId, Long authorId);

    /**
     * Комментарии события в указанном статусе (публичный список).
     */
    @EntityGraph(attributePaths = {"author", "event"})
    List<Comment> findAllByEventIdAndStatus(Long eventId, CommentStatus status, Pageable pageable);

    /**
     * Все комментарии автора в любом статусе (список своих комментариев).
     */
    @EntityGraph(attributePaths = {"author", "event"})
    List<Comment> findAllByAuthorId(Long authorId, Pageable pageable);

    /**
     * Поиск комментариев администратором по набору необязательных фильтров.
     */
    @Override
    @EntityGraph(attributePaths = {"author", "event"})
    Page<Comment> findAll(Specification<Comment> spec, Pageable pageable);
}
