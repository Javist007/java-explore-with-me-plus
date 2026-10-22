package ru.practicum.ewm.dto.comment.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.model.CommentStatus;

/**
 * Решение администратора по комментарию: PUBLISHED (подтвердить) или REJECTED (отклонить).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCommentStatusRequest {

    @NotNull
    private CommentStatus status;
}
