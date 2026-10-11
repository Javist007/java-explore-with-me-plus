package ru.practicum.ewm.dto.event.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.dto.event.Location;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventFullDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("annotation")
    private String annotation;

    @JsonProperty("category")
    private CategoryDto category;

    @JsonProperty("confirmedRequests")
    private Long confirmedRequests;

    @JsonProperty("createdOn")
    private String createdOn;

    @JsonProperty("description")
    private String description;

    @JsonProperty("eventDate")
    private String eventDate;

    @JsonProperty("initiator")
    private UserShortDto initiator;

    @JsonProperty("location")
    private Location location;

    @JsonProperty("paid")
    private Boolean paid;

    @JsonProperty("participantLimit")
    private Integer participantLimit;

    @JsonProperty("publishedOn")
    private String publishedOn;

    @JsonProperty("requestModeration")
    private Boolean requestModeration;

    @JsonProperty("state")
    private String state;

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
