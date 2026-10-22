package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.comment.request.CommentSearchParams;
import ru.practicum.ewm.dto.comment.request.EventCommentsParams;
import ru.practicum.ewm.dto.comment.request.NewCommentParams;
import ru.practicum.ewm.dto.comment.request.UpdateCommentParams;
import ru.practicum.ewm.dto.comment.request.UpdateCommentStatusRequest;
import ru.practicum.ewm.dto.comment.request.UserCommentsParams;
import ru.practicum.ewm.dto.comment.response.CommentDto;

import java.util.List;

/**
 * Сервис комментариев к событиям с премодерацией.
 */
public interface CommentService {

    // ---------- Private API: автор комментария ----------

    /**
     * Создаёт комментарий к опубликованному событию. Новый комментарий получает статус PENDING.
     * Нет пользователя или события: 404. Событие не опубликовано: 409.
     */
    CommentDto createComment(NewCommentParams params);

    /**
     * Изменяет текст своего комментария, ставит updatedOn и возвращает статус PENDING.
     * Нет комментария или он чужой: 404.
     */
    CommentDto updateComment(UpdateCommentParams params);

    /**
     * Удаляет свой комментарий. Нет комментария или он чужой: 404.
     */
    void deleteComment(Long userId, Long commentId);

    /**
     * Все свои комментарии в любом статусе. Нет пользователя: 404.
     */
    List<CommentDto> getUserComments(UserCommentsParams params);

    // ---------- Public API ----------

    /**
     * Подтверждённые (PUBLISHED) комментарии опубликованного события, сортировка по createdOn.
     * Нет события или оно не опубликовано: 404.
     */
    List<CommentDto> getEventComments(EventCommentsParams params);

    // ---------- Admin API: модерация ----------

    /**
     * Поиск комментариев по необязательным фильтрам. rangeStart позже rangeEnd: 400.
     */
    List<CommentDto> searchComments(CommentSearchParams params);

    /**
     * Подтверждает (PUBLISHED) или отклоняет (REJECTED) комментарий.
     * Подтвердить можно только PENDING, отклонить PENDING или PUBLISHED, иначе 409. Нет комментария: 404.
     */
    CommentDto updateCommentStatus(Long commentId, UpdateCommentStatusRequest request);

    /**
     * Удаляет любой комментарий. Нет комментария: 404.
     */
    void deleteCommentByAdmin(Long commentId);
}
