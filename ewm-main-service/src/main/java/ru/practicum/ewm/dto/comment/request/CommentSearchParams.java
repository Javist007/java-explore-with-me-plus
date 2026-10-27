package ru.practicum.ewm.dto.comment.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import ru.practicum.ewm.common.DateTimeConstants;
import ru.practicum.ewm.model.CommentStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Параметры поиска комментариев администратором. Все фильтры необязательные.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentSearchParams {

    /**
     * Идентификаторы авторов.
     */
    private List<Long> users;

    /**
     * Идентификаторы событий.
     */
    private List<Long> events;

    /**
     * Статус комментария, например PENDING для очереди модерации.
     */
    private CommentStatus status;

    /**
     * Подстрока в тексте комментария, без учёта регистра.
     */
    private String text;

    /**
     * Комментарии, созданные не раньше этой даты.
     */
    @DateTimeFormat(pattern = DateTimeConstants.PATTERN)
    private LocalDateTime rangeStart;

    /**
     * Комментарии, созданные не позже этой даты.
     */
    @DateTimeFormat(pattern = DateTimeConstants.PATTERN)
    private LocalDateTime rangeEnd;

    /**
     * Сколько элементов пропустить.
     */
    @PositiveOrZero
    @Builder.Default
    private int from = 0;

    /**
     * Количество элементов в ответе.
     */
    @Positive
    @Builder.Default
    private int size = 10;
}
