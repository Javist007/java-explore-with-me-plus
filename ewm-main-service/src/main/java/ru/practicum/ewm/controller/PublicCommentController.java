package ru.practicum.ewm.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.comment.request.EventCommentsParams;
import ru.practicum.ewm.dto.comment.response.CommentDto;
import ru.practicum.ewm.service.CommentService;

import java.util.List;

/**
 * Публичный API комментариев: доступен всем.
 */
@Validated
@RestController
@RequestMapping("/events/{eventId}/comments")
@RequiredArgsConstructor
public class PublicCommentController {

    private final CommentService commentService;

    /**
     * Подтверждённые комментарии опубликованного события.
     */
    @GetMapping
    public List<CommentDto> getEventComments(@PathVariable Long eventId,
                                             @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                             @RequestParam(defaultValue = "10") @Positive int size) {
        return commentService.getEventComments(new EventCommentsParams(eventId, from, size));
    }
}
