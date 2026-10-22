package ru.practicum.ewm.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import ru.practicum.ewm.dto.event.Location;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.spec.CommentSpecifications;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты репозитория комментариев.
 */
@DataJpaTest
class CommentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CommentRepository commentRepository;

    private User user;
    private Event event;
    private Comment comment;

    @BeforeEach
    void setUp() {
        user = entityManager.persist(User.builder()
                .name("Test User")
                .email("test@example.com")
                .build());

        Category category = entityManager.persist(Category.builder()
                .name("Test Category")
                .build());

        event = entityManager.persist(Event.builder()
                .annotation("Аннотация события для тестов, достаточной длины")
                .category(category)
                .confirmedRequests(0L)
                .createdOn(LocalDateTime.of(2026, 10, 1, 12, 0))
                .description("Описание события для тестов, достаточной длины")
                .eventDate(LocalDateTime.now().plusDays(3))
                .initiator(user)
                .location(Location.builder().lat(55.75f).lon(37.61f).build())
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .title("Тестовое событие")
                .views(0L)
                .build());

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Должен находить комментарий по ID с загрузкой автора и события")
    void findWithAuthorAndEventById_Success() {
        comment = entityManager.persist(Comment.builder()
                .text("Test content")
                .author(user)
                .event(event)
                .status(CommentStatus.PENDING)
                .createdOn(LocalDateTime.now())
                .build());

        entityManager.flush();

        Optional<Comment> found = commentRepository.findWithAuthorAndEventById(comment.getId());
        assertTrue(found.isPresent());
        assertNotNull(found.get().getAuthor());
        assertNotNull(found.get().getEvent());
    }

    @Test
    @DisplayName("Должен возвращать пустой результат, если комментарий не найден")
    void findWithAuthorAndEventById_NotFound() {
        Optional<Comment> found = commentRepository.findWithAuthorAndEventById(999L);
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Должен находить комментарии по спецификации поиска")
    void findAll_WithSpecification() {
        comment = entityManager.persist(Comment.builder()
                .text("Searchable text")
                .author(user)
                .event(event)
                .status(CommentStatus.PUBLISHED)
                .createdOn(LocalDateTime.now())
                .build());

        entityManager.flush();

        Page<Comment> page = commentRepository.findAll(
                CommentSpecifications.textSearch("Searchable"),
                PageRequest.of(0, 10));

        List<Comment> results = page.getContent();

        assertEquals(1, results.size());
        assertEquals("Searchable text", results.getFirst().getText());
    }
}
