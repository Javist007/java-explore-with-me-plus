package ru.practicum.stats.server.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import ru.practicum.stats.dto.StatsDateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class StatsRequestDto {

    @NotNull(message = "Параметр start не может быть пустым")
    @DateTimeFormat(pattern = StatsDateTimeFormat.PATTERN)
    private LocalDateTime start;

    @NotNull(message = "Параметр end не может быть пустым")
    @DateTimeFormat(pattern = StatsDateTimeFormat.PATTERN)
    private LocalDateTime end;

    private List<String> uris;

    private Boolean unique = false;
}