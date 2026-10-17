package ru.practicum.ewm.dto.compilation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewCompilationDto {
    @NotBlank
    @Size(min = 1, max = 50)
    private String title;

    @Builder.Default
    private Boolean pinned = false;

    private List<Long> events;
}