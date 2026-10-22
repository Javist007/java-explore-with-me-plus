package ru.practicum.ewm.dto.comment.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.common.DateTimeConstants;
import ru.practicum.ewm.dto.user.response.UserShortDto;
import ru.practicum.ewm.model.CommentStatus;

import java.time.LocalDateTime;

/**
 * Комментарий к событию.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {

    private Long id;
    private String text;
    private Long eventId;
    private UserShortDto author;
    private CommentStatus status;

    @JsonFormat(pattern = DateTimeConstants.PATTERN)
    private LocalDateTime createdOn;

    @JsonFormat(pattern = DateTimeConstants.PATTERN)
    private LocalDateTime updatedOn;
}
