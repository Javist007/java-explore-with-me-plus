package ru.practicum.ewm.dto.event.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventShortDto {

    @JsonProperty("annotation")
    private String annotation;

    @JsonProperty("category")
    private CategoryDto category;

    @JsonProperty("confirmedRequests")
    private Long confirmedRequests;

    @JsonProperty("eventDate")
    private String eventDate;

    @JsonProperty("initiator")
    private UserShortDto initiator;

    @JsonProperty("paid")
    private Boolean paid;

    @JsonProperty("title")
    private String title;

    @JsonProperty("views")
    private Long views;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDto {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("name")
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserShortDto {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("name")
        private String name;
    }
}
