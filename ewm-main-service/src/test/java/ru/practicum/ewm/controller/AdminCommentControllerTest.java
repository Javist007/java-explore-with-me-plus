package ru.practicum.ewm.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.dto.comment.request.*;
import ru.practicum.ewm.dto.comment.response.CommentDto;
import ru.practicum.ewm.model.CommentStatus;
import ru.practicum.ewm.service.CommentService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты контроллера административных комментариев.
 */
@WebMvcTest(AdminCommentController.class)
public class AdminCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    CommentService commentService;

    @Test
    @DisplayName("GET /admin/comments - Должен возвращать список комментариев (200 OK)")
    void searchComments_Success() throws Exception {
        CommentDto dto = CommentDto.builder()
                .id(1L)
                .text("Test")
                .status(CommentStatus.PENDING)
                .build();

        when(commentService.searchComments(any(CommentSearchParams.class)))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/admin/comments")
                        .param("text", "test")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].text").value("Test"));
    }

    @Test
    @DisplayName("GET /admin/comments - Должен возвращать 400 при некорректном диапазоне дат")
    void searchComments_InvalidDateRange_BadRequest() throws Exception {
        String rangeStart = "2025-12-31%2023:59:59";
        String rangeEnd = "2025-01-01%2000:00:00";
        mockMvc.perform(get("/admin/comments?rangeStart=" + rangeStart + "&rangeEnd=" + rangeEnd)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /admin/comments/{id} - Должен обновлять статус (200 OK)")
    void updateCommentStatus_Success() throws Exception {
        CommentDto dto = CommentDto.builder().id(1L).status(CommentStatus.PUBLISHED).build();

        when(commentService.updateCommentStatus(eq(1L), any(UpdateCommentStatusRequest.class)))
                .thenReturn(dto);

        mockMvc.perform(patch("/admin/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"PUBLISHED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("DELETE /admin/comments/{id} - Должен возвращать 204 No Content")
    void deleteComment_Success() throws Exception {
        doNothing().when(commentService).deleteCommentByAdmin(1L);

        mockMvc.perform(delete("/admin/comments/1"))
                .andExpect(status().isNoContent());
    }
}
