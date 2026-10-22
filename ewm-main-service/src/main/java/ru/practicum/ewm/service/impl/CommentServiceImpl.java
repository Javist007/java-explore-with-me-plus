package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.comment.request.CommentSearchParams;
import ru.practicum.ewm.dto.comment.request.EventCommentsParams;
import ru.practicum.ewm.dto.comment.request.NewCommentParams;
import ru.practicum.ewm.dto.comment.request.UpdateCommentParams;
import ru.practicum.ewm.dto.comment.request.UpdateCommentStatusRequest;
import ru.practicum.ewm.dto.comment.request.UserCommentsParams;
import ru.practicum.ewm.dto.comment.response.CommentDto;
import ru.practicum.ewm.repository.CommentRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.service.CommentService;

import java.util.List;

/**
 * Реализация сервиса комментариев.
 * Каркас: каждый участник заполняет только свои методы, остальные пока бросают UnsupportedOperationException.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private static final String NOT_IMPLEMENTED = "Метод ещё не реализован";

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    // ---------- Private API (Максим, кроме getUserComments) ----------

    @Override
    @Transactional
    public CommentDto createComment(NewCommentParams params) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    @Transactional
    public CommentDto updateComment(UpdateCommentParams params) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    // ---------- Списки (Даша) ----------

    @Override
    public List<CommentDto> getUserComments(UserCommentsParams params) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public List<CommentDto> getEventComments(EventCommentsParams params) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    // ---------- Admin API (Влад) ----------

    @Override
    public List<CommentDto> searchComments(CommentSearchParams params) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    @Transactional
    public CommentDto updateCommentStatus(Long commentId, UpdateCommentStatusRequest request) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }
}
