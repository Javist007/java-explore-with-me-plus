package ru.practicum.ewm.dto.comment.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Параметры изменения своего комментария: кто меняет, какой комментарий и на что.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCommentParams {

    private Long userId;
    private Long commentId;
    private UpdateCommentDto request;
}
