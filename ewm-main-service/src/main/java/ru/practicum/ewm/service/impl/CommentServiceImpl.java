package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.comment.request.*;
import ru.practicum.ewm.dto.comment.response.CommentDto;
import ru.practicum.ewm.exception.model.ConflictException;
import ru.practicum.ewm.exception.model.NotFoundException;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.CommentRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.service.CommentService;
import ru.practicum.ewm.service.mapper.CommentMapper;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

    /**
     * Создает новый комментарий к событию.
     *
     * @param params параметры для создания комментария
     * @return созданный комментарий
     * @throws NotFoundException если событие или пользователь не найдены
     * @throws ConflictException если событие не опубликовано
     */
    @Override
    @Transactional
    public CommentDto createComment(NewCommentParams params) {
        Event event = eventRepository.findById(params.getEventId())
                .orElseThrow(() -> new NotFoundException("Event не найден"));
        User user = userRepository.findById(params.getUserId())
                .orElseThrow(() -> new NotFoundException("User не найден"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Комментировать можно только опубликованные события");
        }

        Comment comment = Comment.builder().text(params.getRequest().getText())
                .event(event)
                .author(user)
                .status(CommentStatus.PENDING)
                .createdOn(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .updatedOn(null).build();

        return CommentMapper.toDto(commentRepository.save(comment));
    }

    /**
     * Обновляет текст существующего комментария.
     * Обновить комментарий может только его автор.
     *
     * @param params параметры для обновления комментария
     * @return обновленный комментарий
     * @throws NotFoundException если комментарий не найден или принадлежит другому автору
     */
    @Override
    @Transactional
    public CommentDto updateComment(UpdateCommentParams params) {
        Comment comment = commentRepository.findByIdAndAuthorId(params.getCommentId(), params.getUserId())
                .orElseThrow(() -> new NotFoundException("Comment не найден или он принадлежит другому автору"));

        comment.setText(params.getRequest().getText());
        comment.setUpdatedOn(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        comment.setStatus(CommentStatus.PENDING);

        return CommentMapper.toDto(commentRepository.save(comment));
    }

    /**
     * Удаляет комментарий.
     * Удалить комментарий может только его автор.
     *
     * @param userId идентификатор пользователя-автора
     * @param commentId идентификатор комментария
     * @throws NotFoundException если комментарий не найден или принадлежит другому автору
     */
    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Comment не найден или он принадлежит другому автору"));

        commentRepository.delete(comment);
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
