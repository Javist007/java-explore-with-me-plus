package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.comment.request.*;
import ru.practicum.ewm.dto.comment.response.CommentDto;
import ru.practicum.ewm.exception.model.BadRequestException;
import ru.practicum.ewm.exception.model.ConflictException;
import ru.practicum.ewm.exception.model.NotFoundException;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.model.CommentStatus;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.repository.CommentRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.repository.spec.CommentSpecifications;
import ru.practicum.ewm.service.CommentService;
import ru.practicum.ewm.service.mapper.CommentMapper;

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
    public List<CommentDto> getEventComments(EventCommentsParams params) {
        Event event = eventRepository.findById(params.getEventId())
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Комментарии доступны только для опубликованных событий");
        }

        if (params.getFrom() % params.getSize() != 0) {
            throw new BadRequestException("Parameter 'from' must be a multiple of 'size'");
        }

        Pageable pageable = PageRequest.of(
                params.getFrom() / params.getSize(),
                params.getSize(),
                Sort.by(Sort.Direction.DESC, "createdOn")
        );

        List<Comment> comments = commentRepository.findAllByEventIdAndStatus(
                params.getEventId(),
                CommentStatus.PUBLISHED,
                pageable
        );

        return CommentMapper.toDtoList(comments);
    }

    @Override
    public List<CommentDto> getUserComments(UserCommentsParams params) {
        userRepository.findById(params.getUserId())
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        if (params.getFrom() % params.getSize() != 0) {
            throw new BadRequestException("Parameter 'from' must be a multiple of 'size'");
        }

        Pageable pageable = PageRequest.of(
                params.getFrom() / params.getSize(),
                params.getSize(),
                Sort.by(Sort.Direction.DESC, "createdOn")
        );

        List<Comment> comments = commentRepository.findAllByAuthorId(params.getUserId(), pageable);

        return CommentMapper.toDtoList(comments);
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