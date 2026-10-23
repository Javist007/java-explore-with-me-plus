package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.comment.request.*;
import ru.practicum.ewm.dto.comment.response.CommentDto;
import ru.practicum.ewm.exception.model.BadRequestException;
import ru.practicum.ewm.exception.model.ConflictException;
import ru.practicum.ewm.exception.model.NotFoundException;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.CommentRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.repository.spec.CommentSpecifications;
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
        if (params.getRangeStart() != null && params.getRangeEnd() != null) {
            if (params.getRangeStart().isAfter(params.getRangeEnd())) {
                throw new BadRequestException("Дата начала не может быть позже даты конца");
            }
        }

        Specification<Comment> spec = Specification.where(null);

        if (params.getUsers() != null) {
            spec = spec.and(CommentSpecifications.hasUsers(params.getUsers()));
        }
        if (params.getEvents() != null) {
            spec = spec.and(CommentSpecifications.hasEvents(params.getEvents()));
        }
        if (params.getStatus() != null) {
            spec = spec.and(CommentSpecifications.hasStatus(params.getStatus()));
        }
        if (params.getText() != null) {
            spec = spec.and(CommentSpecifications.textSearch(params.getText()));
        }

        if (params.getRangeStart() != null) {
            spec = spec.and(CommentSpecifications.hasRangeStart(params.getRangeStart()));
        }
        if (params.getRangeEnd() != null) {
            spec = spec.and(CommentSpecifications.hasRangeEnd(params.getRangeEnd()));
        }

        int pageIndex = Math.max(0, params.getFrom() / params.getSize());
        PageRequest pageable = PageRequest.of(
                pageIndex, params.getSize(), Sort.by("createdOn").descending());

        Page<Comment> page = commentRepository.findAll(spec, pageable);

        return page.getContent().stream()
                .map(CommentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public CommentDto updateCommentStatus(Long commentId, UpdateCommentStatusRequest request) {
        Comment comment = getComment(commentId);

        CommentStatus nextStatus = request.getStatus();

        if (nextStatus == CommentStatus.PUBLISHED) {
            if (comment.getStatus() != CommentStatus.PENDING) {
                throw new ConflictException("Только комментарии в ожидании (PENDING) могут быть одобрены");
            }
        } else if (nextStatus == CommentStatus.REJECTED) {
            if (comment.getStatus() != CommentStatus.PENDING && comment.getStatus() != CommentStatus.PUBLISHED) {
                throw new ConflictException("Только комментарии в ожидании (PENDING) или одобренные (PUBLISHED) могут быть отклонены");
            }
        } else {
            throw new BadRequestException("Некорректный статус для обновления");
        }

        comment.setStatus(nextStatus);
        return CommentMapper.toDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        Comment comment = getComment(commentId);
        commentRepository.delete(comment);
    }

    private Comment getComment(Long commentId) {
        return commentRepository.findWithAuthorAndEventById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден"));
    }
}
