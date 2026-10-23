package ru.practicum.ewm.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.dto.comment.request.*;
import ru.practicum.ewm.dto.comment.response.CommentDto;
import ru.practicum.ewm.exception.model.*;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplAdminTest {

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    private Comment comment;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(1L).name("User").build();
        Event event = Event.builder().id(10L).initiator(user).build();
        comment = Comment.builder()
                .id(100L)
                .text("Test text")
                .author(user)
                .event(event)
                .status(CommentStatus.PENDING)
                .createdOn(java.time.LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("deleteCommentByAdmin должен удалять комментарий")
    void deleteCommentByAdmin_Success() {
        when(commentRepository.findWithAuthorAndEventById(100L)).thenReturn(Optional.of(comment));

        commentService.deleteCommentByAdmin(100L);

        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("deleteCommentByAdmin должен выбрасывать 404 если комментарий не найден")
    void deleteCommentByAdmin_NotFound() {
        when(commentRepository.findWithAuthorAndEventById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteCommentByAdmin(999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("updateCommentStatus: PENDING -> PUBLISHED (Успех)")
    void updateCommentStatus_PendingToPublished_Success() {
        when(commentRepository.findWithAuthorAndEventById(100L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any())).thenReturn(comment);

        CommentDto result = commentService.updateCommentStatus(100L,
                new UpdateCommentStatusRequest(CommentStatus.PUBLISHED));

        assertThat(result.getStatus()).isEqualTo(CommentStatus.PUBLISHED);
        verify(commentRepository).save(comment);
    }

    @Test
    @DisplayName("updateCommentStatus: PUBLISHED -> PUBLISHED (Конфликт)")
    void updateCommentStatus_PublishedToPublished_Conflict() {
        comment.setStatus(CommentStatus.PUBLISHED);
        when(commentRepository.findWithAuthorAndEventById(100L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.updateCommentStatus(100L,
                new UpdateCommentStatusRequest(CommentStatus.PUBLISHED)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Только комментарии в ожидании");
    }

    @Test
    @DisplayName("updateCommentStatus: PENDING -> REJECTED (Успех)")
    void updateCommentStatus_PendingToRejected_Success() {
        when(commentRepository.findWithAuthorAndEventById(100L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any())).thenReturn(comment);

        CommentDto result = commentService.updateCommentStatus(100L,
                new UpdateCommentStatusRequest(CommentStatus.REJECTED));

        assertThat(result.getStatus()).isEqualTo(CommentStatus.REJECTED);
    }

    @Test
    @DisplayName("searchComments должен возвращать результаты при валидных фильтрах")
    void searchComments_ValidFilters() {
        CommentSearchParams params = CommentSearchParams.builder()
                .status(CommentStatus.PENDING)
                .text("test")
                .build();

        when(commentRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(comment)));

        var result = commentService.searchComments(params);
        assertThat(result).hasSize(1);
    }
}
