package ru.practicum.ewm.dto.comment.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Параметры создания комментария: кто пишет, к какому событию и что.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewCommentParams {

    private Long userId;
    private Long eventId;
    private NewCommentDto request;
}
