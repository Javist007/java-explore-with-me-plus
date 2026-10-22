package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.comment.request.NewCommentDto;
import ru.practicum.ewm.dto.comment.request.NewCommentParams;
import ru.practicum.ewm.dto.comment.request.UpdateCommentDto;
import ru.practicum.ewm.dto.comment.request.UpdateCommentParams;
import ru.practicum.ewm.dto.comment.request.UserCommentsParams;
import ru.practicum.ewm.dto.comment.response.CommentDto;
import ru.practicum.ewm.service.CommentService;

import java.util.List;

/**
 * Закрытый API комментариев: только для зарегистрированных пользователей.
 */
@Validated
@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
public class PrivateCommentController {

    private final CommentService commentService;

    /**
     * Создание комментария к опубликованному событию.
     */
    @PostMapping("/events/{eventId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(@PathVariable Long userId,
                                    @PathVariable Long eventId,
                                    @RequestBody @Valid NewCommentDto request) {
        return commentService.createComment(new NewCommentParams(userId, eventId, request));
    }

    /**
     * Изменение своего комментария.
     */
    @PatchMapping("/comments/{commentId}")
    public CommentDto updateComment(@PathVariable Long userId,
                                    @PathVariable Long commentId,
                                    @RequestBody @Valid UpdateCommentDto request) {
        return commentService.updateComment(new UpdateCommentParams(userId, commentId, request));
    }

    /**
     * Удаление своего комментария.
     */
    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long userId, @PathVariable Long commentId) {
        commentService.deleteComment(userId, commentId);
    }

    /**
     * Свои комментарии в любом статусе.
     */
    @GetMapping("/comments")
    public List<CommentDto> getUserComments(@PathVariable Long userId,
                                            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                            @RequestParam(defaultValue = "10") @Positive int size) {
        return commentService.getUserComments(new UserCommentsParams(userId, from, size));
    }
}
