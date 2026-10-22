package ru.practicum.ewm.dto.comment.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Параметры получения комментариев события: событие и пагинация (from = сколько пропустить).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventCommentsParams {

    private Long eventId;
    private int from;
    private int size;
}
