package ru.practicum.ewm.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.common.OffsetPageRequest;
import ru.practicum.ewm.dto.comment.request.EventCommentsParams;
import ru.practicum.ewm.dto.comment.request.UserCommentsParams;
import ru.practicum.ewm.dto.comment.response.CommentDto;
import ru.practicum.ewm.exception.model.NotFoundException;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.CommentRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplPublicTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    private Event publishedEvent;
    private Event pendingEvent;
    private User user;
    private Comment publishedComment;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("Тестовый пользователь").email("test@test.com").build();

        publishedEvent = Event.builder().id(1L).state(EventState.PUBLISHED).build();
        pendingEvent = Event.builder().id(2L).state(EventState.PENDING).build();

        publishedComment = Comment.builder()
                .id(1L)
                .text("Отличное событие!")
                .event(publishedEvent)
                .author(user)
                .status(CommentStatus.PUBLISHED)
                .createdOn(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Должен вернуть список опубликованных комментариев события")
    void getEventComments_ShouldReturnPublishedComments() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(publishedEvent));
        when(commentRepository.findAllByEventIdAndStatus(
                eq(1L), eq(CommentStatus.PUBLISHED), any(OffsetPageRequest.class)))
                .thenReturn(List.of(publishedComment));

        List<CommentDto> result = commentService.getEventComments(new EventCommentsParams(1L, 0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getText()).isEqualTo("Отличное событие!");
        verify(commentRepository).findAllByEventIdAndStatus(
                eq(1L), eq(CommentStatus.PUBLISHED), any(OffsetPageRequest.class));
    }

    @Test
    @DisplayName("Должен выбросить 404, если событие не найдено")
    void getEventComments_ShouldThrowNotFound_WhenEventNotFound() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getEventComments(new EventCommentsParams(99L, 0, 10)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Событие не найдено");
    }

    @Test
    @DisplayName("Должен выбросить 404, если событие не опубликовано")
    void getEventComments_ShouldThrowNotFound_WhenEventNotPublished() {
        when(eventRepository.findById(2L)).thenReturn(Optional.of(pendingEvent));

        assertThatThrownBy(() -> commentService.getEventComments(new EventCommentsParams(2L, 0, 10)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Комментарии доступны только для опубликованных событий");
    }

    @Test
    @DisplayName("Должен вернуть список всех комментариев пользователя")
    void getUserComments_ShouldReturnAllUserComments() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(commentRepository.findAllByAuthorId(eq(1L), any(OffsetPageRequest.class)))
                .thenReturn(List.of(publishedComment));

        List<CommentDto> result = commentService.getUserComments(new UserCommentsParams(1L, 0, 10));

        assertThat(result).hasSize(1);
        verify(userRepository).existsById(1L);
        verify(commentRepository).findAllByAuthorId(eq(1L), any(OffsetPageRequest.class));
    }

    @Test
    @DisplayName("Должен выбросить 404, если пользователь не найден")
    void getUserComments_ShouldThrowNotFound_WhenUserNotFound() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> commentService.getUserComments(new UserCommentsParams(99L, 0, 10)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователь не найден");
    }
}