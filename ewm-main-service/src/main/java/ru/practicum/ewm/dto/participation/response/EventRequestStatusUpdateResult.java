package ru.practicum.ewm.dto.participation.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Результат подтверждения/отклонения заявок на участие в событии.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateResult {

    @Builder.Default
    private List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();

    @Builder.Default
    private List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();
}
