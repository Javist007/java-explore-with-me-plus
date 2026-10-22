package ru.practicum.ewm.dto.compilation.response;

import lombok.*;
import ru.practicum.ewm.dto.event.response.EventShortDto;

import java.util.List;

/**
 * Подборка событий (для ответа клиенту).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompilationDto {
    private Long id;
    private String title;
    private Boolean pinned;
    private List<EventShortDto> events;
}