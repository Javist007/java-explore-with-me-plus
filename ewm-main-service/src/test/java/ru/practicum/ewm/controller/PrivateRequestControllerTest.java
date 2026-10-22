package ru.practicum.ewm.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.dto.participation.response.ParticipationRequestDto;
import ru.practicum.ewm.exception.model.ConflictException;
import ru.practicum.ewm.exception.model.NotFoundException;
import ru.practicum.ewm.model.RequestStatus;
import ru.practicum.ewm.service.RequestService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrivateRequestController.class)
class PrivateRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RequestService requestService;

    private final ParticipationRequestDto dto = ParticipationRequestDto.builder()
            .id(5L)
            .created(LocalDateTime.of(2026, 10, 1, 12, 0, 30))
            .event(10L)
            .requester(2L)
            .status(RequestStatus.PENDING)
            .build();

    @Test
    void getUserRequests_returnsRequests() throws Exception {
        when(requestService.getUserRequests(2L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/users/2/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].created").value("2026-10-01T12:00:30"))
                .andExpect(jsonPath("$[0].event").value(10))
                .andExpect(jsonPath("$[0].requester").value(2))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void getUserRequests_whenUserNotFound_returns404() throws Exception {
        when(requestService.getUserRequests(999L)).thenThrow(new NotFoundException("Пользователь с id=999 не найден"));

        mockMvc.perform(get("/users/999/requests"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.reason").value("The required object was not found."))
                .andExpect(jsonPath("$.message").value("Пользователь с id=999 не найден"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void addRequest_returns201() throws Exception {
        when(requestService.addRequest(2L, 10L)).thenReturn(dto);

        mockMvc.perform(post("/users/2/requests").param("eventId", "10"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void addRequest_withoutEventId_returns400() throws Exception {
        mockMvc.perform(post("/users/2/requests"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
        verifyNoInteractions(requestService);
    }

    @Test
    void addRequest_withNonNumericEventId_returns400() throws Exception {
        mockMvc.perform(post("/users/2/requests").param("eventId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
        verifyNoInteractions(requestService);
    }

    @Test
    void addRequest_whenConflict_returns409() throws Exception {
        when(requestService.addRequest(2L, 10L)).thenThrow(new ConflictException("Достигнут лимит участников"));

        mockMvc.perform(post("/users/2/requests").param("eventId", "10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CONFLICT"))
                .andExpect(jsonPath("$.reason").value("For the requested operation the conditions are not met."))
                .andExpect(jsonPath("$.message").value("Достигнут лимит участников"));
    }

    @Test
    void cancelRequest_returnsCanceledRequest() throws Exception {
        ParticipationRequestDto canceled = ParticipationRequestDto.builder()
                .id(5L)
                .event(10L)
                .requester(2L)
                .status(RequestStatus.CANCELED)
                .build();
        when(requestService.cancelRequest(2L, 5L)).thenReturn(canceled);

        mockMvc.perform(patch("/users/2/requests/5/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void cancelRequest_whenNotFound_returns404() throws Exception {
        when(requestService.cancelRequest(3L, 5L)).thenThrow(new NotFoundException("Заявка с id=5 не найдена"));

        mockMvc.perform(patch("/users/3/requests/5/cancel"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }
}
