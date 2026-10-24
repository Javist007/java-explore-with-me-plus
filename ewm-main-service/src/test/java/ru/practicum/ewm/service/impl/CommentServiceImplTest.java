package ru.practicum.ewm.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.dto.comment.request.NewCommentDto;
import ru.practicum.ewm.dto.comment.request.NewCommentParams;
import ru.practicum.ewm.dto.comment.request.UpdateCommentDto;
import ru.practicum.ewm.dto.comment.request.UpdateCommentParams;
import ru.practicum.ewm.dto.comment.response.CommentDto;
import ru.practicum.ewm.exception.model.ConflictException;
import ru.practicum.ewm.exception.model.NotFoundException;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.CommentRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Юнит-тесты реализованной части CommentServiceImpl:
 * создание / изменение / удаление комментария (Private API).
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User user(Long id) {
        User u = new User();
        u.setId(id);
        u.setName("Автор " + id);
        return u;
    }

    private Event event(Long id, EventState state) {
        Event e = new Event();
        e.setId(id);
        e.setState(state);
        return e;
    }

    private Comment comment(Long id, User author, CommentStatus status) {
        return Comment.builder()
                .id(id)
                .text("Исходный текст")
                .author(author)
                .event(event(1L, EventState.PUBLISHED))
                .status(status)
                .createdOn(LocalDateTime.now().minusDays(1))
                .updatedOn(null)
                .build();
    }

    // ---------- createComment ----------

    @Nested
    @DisplayName("Создание комментария")
    class Create {

        private NewCommentParams params(Long userId, Long eventId, String text) {
            return new NewCommentParams(userId, eventId, NewCommentDto.builder().text(text).build());
        }

        @Test
        @DisplayName("201-сценарий: сохраняется PENDING с createdOn и пустым updatedOn")
        void shouldCreatePendingComment() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(event(1L, EventState.PUBLISHED)));
            when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L)));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setId(100L);
                return c;
            });

            CommentDto result = commentService.createComment(params(7L, 1L, "Отличный концерт!"));

            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentRepository).save(captor.capture());
            Comment saved = captor.getValue();

            assertThat(saved.getStatus()).isEqualTo(CommentStatus.PENDING);
            assertThat(saved.getText()).isEqualTo("Отличный концерт!");
            assertThat(saved.getAuthor().getId()).isEqualTo(7L);
            assertThat(saved.getEvent().getId()).isEqualTo(1L);
            assertThat(saved.getCreatedOn()).isNotNull();
            assertThat(saved.getUpdatedOn()).isNull();

            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getStatus()).isEqualTo(CommentStatus.PENDING);
            assertThat(result.getEventId()).isEqualTo(1L);
            assertThat(result.getAuthor().getId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("Событие не найдено -> 404, ничего не сохраняется")
        void shouldThrowNotFoundWhenEventMissing() {
            when(eventRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.createComment(params(7L, 999L, "текст")))
                    .isInstanceOf(NotFoundException.class);
            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Пользователь не найден -> 404")
        void shouldThrowNotFoundWhenUserMissing() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(event(1L, EventState.PUBLISHED)));
            when(userRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.createComment(params(404L, 1L, "текст")))
                    .isInstanceOf(NotFoundException.class);
            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Событие не опубликовано (PENDING) -> 409")
        void shouldThrowConflictWhenEventNotPublished() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(event(1L, EventState.PENDING)));
            when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L)));

            assertThatThrownBy(() -> commentService.createComment(params(7L, 1L, "текст")))
                    .isInstanceOf(ConflictException.class);
            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Событие отменено (CANCELED) -> 409")
        void shouldThrowConflictWhenEventCanceled() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(event(1L, EventState.CANCELED)));
            when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L)));

            assertThatThrownBy(() -> commentService.createComment(params(7L, 1L, "текст")))
                    .isInstanceOf(ConflictException.class);
        }
    }

    // ---------- updateComment ----------

    @Nested
    @DisplayName("Изменение своего комментария")
    class Update {

        private UpdateCommentParams params(Long userId, Long commentId, String text) {
            return new UpdateCommentParams(userId, commentId, UpdateCommentDto.builder().text(text).build());
        }

        @Test
        @DisplayName("Текст меняется, updatedOn ставится, статус возвращается на PENDING")
        void shouldUpdateTextAndResetStatusToPending() {
            Comment c = comment(10L, user(7L), CommentStatus.PUBLISHED);
            when(commentRepository.findByIdAndAuthorId(10L, 7L)).thenReturn(Optional.of(c));

            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));


            CommentDto result = commentService.updateComment(params(7L, 10L, "Новый текст"));

            assertThat(c.getText()).isEqualTo("Новый текст");
            assertThat(c.getStatus()).isEqualTo(CommentStatus.PENDING);
            assertThat(c.getUpdatedOn()).isNotNull();
            assertThat(result.getText()).isEqualTo("Новый текст");
            assertThat(result.getStatus()).isEqualTo(CommentStatus.PENDING);
        }

        @Test
        @DisplayName("Чужой комментарий -> 404 (поиск сразу по автору)")
        void shouldReturnNotFoundForForeignComment() {
            when(commentRepository.findByIdAndAuthorId(10L, 8L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.updateComment(params(8L, 10L, "хакну")))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Комментарий не найден -> 404")
        void shouldReturnNotFoundWhenCommentMissing() {
            when(commentRepository.findByIdAndAuthorId(404L, 7L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.updateComment(params(7L, 404L, "текст")))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ---------- deleteComment ----------

    @Nested
    @DisplayName("Удаление своего комментария")
    class Delete {

        @Test
        @DisplayName("Свой комментарий удаляется")
        void shouldDeleteOwnComment() {
            Comment c = comment(10L, user(7L), CommentStatus.PUBLISHED);
            when(commentRepository.findByIdAndAuthorId(10L, 7L)).thenReturn(Optional.of(c));

            commentService.deleteComment(7L, 10L);

            verify(commentRepository).delete(c);
        }

        @Test
        @DisplayName("Чужой комментарий -> 404, delete не вызывается")
        void shouldNotDeleteForeignComment() {
            when(commentRepository.findByIdAndAuthorId(10L, 8L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.deleteComment(8L, 10L))
                    .isInstanceOf(NotFoundException.class);
            verify(commentRepository, never()).delete(any(Comment.class));
        }

        @Test
        @DisplayName("Несуществующий комментарий -> 404")
        void shouldReturnNotFoundWhenMissing() {
            when(commentRepository.findByIdAndAuthorId(anyLong(), anyLong())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.deleteComment(7L, 404L))
                    .isInstanceOf(NotFoundException.class);
            verify(commentRepository, never()).delete(any(Comment.class));
        }
    }
}