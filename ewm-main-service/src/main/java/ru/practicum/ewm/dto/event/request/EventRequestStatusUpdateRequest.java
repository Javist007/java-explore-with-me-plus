package ru.practicum.ewm.dto.event.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateRequest {

    @NotEmpty
    @JsonProperty("requestIds")
    private List<Long> requestIds;

    @NotNull
    @JsonProperty("status")
    private String status;

    public enum Status {
        CONFIRMED,
        REJECTED
    }
}
