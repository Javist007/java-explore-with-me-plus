package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.comment.request.CommentSearchParams;
import ru.practicum.ewm.dto.comment.request.UpdateCommentStatusRequest;
import ru.practicum.ewm.dto.comment.response.CommentDto;
import ru.practicum.ewm.service.CommentService;

import java.util.List;

/**
 * Административный API комментариев: поиск и модерация.
 */
@Validated
@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final CommentService commentService;

    /**
     * Поиск комментариев по необязательным фильтрам.
     */
    @GetMapping
    public List<CommentDto> searchComments(@Valid @ModelAttribute CommentSearchParams params) {
        return commentService.searchComments(params);
    }

    /**
     * Подтверждение или отклонение комментария.
     */
    @PatchMapping("/{commentId}")
    public CommentDto updateCommentStatus(@PathVariable Long commentId,
                                          @RequestBody @Valid UpdateCommentStatusRequest request) {
        return commentService.updateCommentStatus(commentId, request);
    }

    /**
     * Удаление любого комментария.
     */
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long commentId) {
        commentService.deleteCommentByAdmin(commentId);
    }
}
