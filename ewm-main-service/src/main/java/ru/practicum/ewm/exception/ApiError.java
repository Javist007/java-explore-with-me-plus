package ru.practicum.ewm.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.common.DateTimeConstants;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Сведения об ошибке в формате спецификации (схема ApiError).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    @Builder.Default
    private List<String> errors = List.of();

    private String message;

    private String reason;

    private String status;

    @JsonFormat(pattern = DateTimeConstants.PATTERN)
    private LocalDateTime timestamp;
}
