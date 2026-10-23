package ru.practicum.ewm.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.practicum.ewm.dto.comment.request.EventCommentsParams;
import ru.practicum.ewm.dto.comment.request.UserCommentsParams;
import ru.practicum.ewm.dto.comment.response.CommentDto;
import ru.practicum.ewm.exception.model.BadRequestException;
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
class CommentServiceImplTest {

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
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("Test User").email("test@test.com").build();

        publishedEvent = Event.builder().id(1L).state(EventState.PUBLISHED).build();
        pendingEvent = Event.builder().id(2L).state(EventState.PENDING).build();

        publishedComment = Comment.builder()
                .id(1L)
                .text("Great event!")
                .event(publishedEvent)
                .author(user)
                .status(CommentStatus.PUBLISHED)
                .createdOn(LocalDateTime.now())
                .build();

        pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdOn"));
    }

    @Test
    @DisplayName("Должен вернуть список PUBLISHED комментариев события")
    void getEventComments_ShouldReturnPublishedComments() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(publishedEvent));
        when(commentRepository.findAllByEventIdAndStatus(eq(1L), eq(CommentStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(List.of(publishedComment));

        List<CommentDto> result = commentService.getEventComments(new EventCommentsParams(1L, 0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getText()).isEqualTo("Great event!");
        verify(commentRepository).findAllByEventIdAndStatus(eq(1L), eq(CommentStatus.PUBLISHED), any(Pageable.class));
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
    @DisplayName("Должен выбросить 400, если from не кратно size")
    void getEventComments_ShouldThrowBadRequest_WhenFromNotMultipleOfSize() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(publishedEvent));

        assertThatThrownBy(() -> commentService.getEventComments(new EventCommentsParams(1L, 5, 10)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Parameter 'from' must be a multiple of 'size'");
    }

    @Test
    @DisplayName("Должен вернуть список всех комментариев пользователя")
    void getUserComments_ShouldReturnAllUserComments() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(commentRepository.findAllByAuthorId(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(publishedComment));

        List<CommentDto> result = commentService.getUserComments(new UserCommentsParams(1L, 0, 10));

        assertThat(result).hasSize(1);
        verify(commentRepository).findAllByAuthorId(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("Должен выбросить 404, если пользователь не найден")
    void getUserComments_ShouldThrowNotFound_WhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getUserComments(new UserCommentsParams(99L, 0, 10)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователь не найден");
    }

    @Test
    @DisplayName("Должен выбросить 400, если from не кратно size в getUserComments")
    void getUserComments_ShouldThrowBadRequest_WhenFromNotMultipleOfSize() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> commentService.getUserComments(new UserCommentsParams(1L, 3, 10)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Parameter 'from' must be a multiple of 'size'");
    }
}
