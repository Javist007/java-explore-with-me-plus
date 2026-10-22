package ru.practicum.ewm.dto.comment.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Параметры получения своих комментариев: автор и пагинация (from = сколько пропустить).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCommentsParams {

    private Long userId;
    private int from;
    private int size;
}
