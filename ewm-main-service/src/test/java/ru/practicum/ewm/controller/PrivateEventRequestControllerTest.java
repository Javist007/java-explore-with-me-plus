package ru.practicum.ewm.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.dto.participation.request.RequestStatusUpdateParams;
import ru.practicum.ewm.dto.participation.response.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.participation.response.ParticipationRequestDto;
import ru.practicum.ewm.exception.model.BadRequestException;
import ru.practicum.ewm.exception.model.ConflictException;
import ru.practicum.ewm.exception.model.NotFoundException;
import ru.practicum.ewm.model.RequestStatus;
import ru.practicum.ewm.service.RequestService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrivateEventRequestController.class)
class PrivateEventRequestControllerTest {

    private static final String URL = "/users/1/events/10/requests";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RequestService requestService;

    @Test
    void getEventRequests_returnsRequests() throws Exception {
        when(requestService.getEventRequests(1L, 10L))
                .thenReturn(List.of(dto(5L, RequestStatus.PENDING), dto(6L, RequestStatus.CONFIRMED)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[1].status").value("CONFIRMED"));
    }

    @Test
    void getEventRequests_whenNotInitiator_returns404() throws Exception {
        when(requestService.getEventRequests(1L, 10L)).thenThrow(new NotFoundException("Событие с id=10 не найдено"));

        mockMvc.perform(get(URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void updateRequestStatuses_passesParamsToServiceAndReturnsResult() throws Exception {
        EventRequestStatusUpdateResult result = EventRequestStatusUpdateResult.builder()
                .confirmedRequests(List.of(dto(5L, RequestStatus.CONFIRMED)))
                .rejectedRequests(List.of(dto(6L, RequestStatus.REJECTED)))
                .build();
        when(requestService.updateRequestStatuses(any())).thenReturn(result);

        mockMvc.perform(patch(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestIds\":[5,6],\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests[0].id").value(5))
                .andExpect(jsonPath("$.confirmedRequests[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.rejectedRequests[0].id").value(6))
                .andExpect(jsonPath("$.rejectedRequests[0].status").value("REJECTED"));

        ArgumentCaptor<RequestStatusUpdateParams> captor = ArgumentCaptor.forClass(RequestStatusUpdateParams.class);
        verify(requestService).updateRequestStatuses(captor.capture());
        RequestStatusUpdateParams params = captor.getValue();
        assertThat(params.getUserId()).isEqualTo(1L);
        assertThat(params.getEventId()).isEqualTo(10L);
        assertThat(params.getUpdateRequest().getRequestIds()).containsExactly(5L, 6L);
        assertThat(params.getUpdateRequest().getStatus()).isEqualTo(RequestStatus.CONFIRMED);
    }

    @Test
    void updateRequestStatuses_withEmptyRequestIds_returns400() throws Exception {
        mockMvc.perform(patch(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestIds\":[],\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."));
        verifyNoInteractions(requestService);
    }

    @Test
    void updateRequestStatuses_withoutStatus_returns400() throws Exception {
        mockMvc.perform(patch(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestIds\":[5]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(requestService);
    }

    @Test
    void updateRequestStatuses_withUnknownStatus_returns400() throws Exception {
        mockMvc.perform(patch(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestIds\":[5],\"status\":\"MAYBE\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(requestService);
    }

    @Test
    void updateRequestStatuses_withoutBody_returns400() throws Exception {
        mockMvc.perform(patch(URL).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(requestService);
    }

    @Test
    void updateRequestStatuses_whenServiceRejectsStatus_returns400() throws Exception {
        when(requestService.updateRequestStatuses(any()))
                .thenThrow(new BadRequestException("Новый статус заявок может быть только CONFIRMED или REJECTED"));

        mockMvc.perform(patch(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestIds\":[5],\"status\":\"PENDING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Новый статус заявок может быть только CONFIRMED или REJECTED"));
    }

    @Test
    void updateRequestStatuses_whenLimitReached_returns409() throws Exception {
        when(requestService.updateRequestStatuses(any()))
                .thenThrow(new ConflictException("Достигнут лимит участников события id=10"));

        mockMvc.perform(patch(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestIds\":[5],\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CONFLICT"));
    }

    private static ParticipationRequestDto dto(long id, RequestStatus status) {
        return ParticipationRequestDto.builder()
                .id(id)
                .created(LocalDateTime.of(2026, 10, 1, 12, 0))
                .event(10L)
                .requester(id)
                .status(status)
                .build();
    }
}
