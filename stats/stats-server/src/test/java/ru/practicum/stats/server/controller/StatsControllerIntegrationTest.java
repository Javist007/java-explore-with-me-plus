package ru.practicum.stats.server.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционный тест сервиса статистики на реальном контексте Spring и H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StatsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Сохранённые хиты попадают в статистику, уникальные считаются по ip.
     */
    @Test
    @DisplayName("POST /hit -> 201, GET /stats считает все и уникальные просмотры")
    void saveHitAndGetStats() throws Exception {
        for (String ip : new String[]{"10.0.0.1", "10.0.0.1", "10.0.0.2"}) {
            mockMvc.perform(post("/hit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"app\":\"ewm-main-service\",\"uri\":\"/events/1\",\"ip\":\"" + ip
                                    + "\",\"timestamp\":\"2026-10-01 12:00:00\"}"))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/stats")
                        .param("start", "2026-01-01 00:00:00")
                        .param("end", "2027-01-01 00:00:00")
                        .param("uris", "/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uri").value("/events/1"))
                .andExpect(jsonPath("$[0].hits").value(3));

        mockMvc.perform(get("/stats")
                        .param("start", "2026-01-01 00:00:00")
                        .param("end", "2027-01-01 00:00:00")
                        .param("uris", "/events/1")
                        .param("unique", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hits").value(2));
    }

    /**
     * Дата начала позже даты конца — некорректный запрос, а не внутренняя ошибка сервера.
     */
    @Test
    @DisplayName("GET /stats: start позже end -> 400, а не 500")
    void startAfterEndReturns400() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", "2035-01-01 00:00:00")
                        .param("end", "2020-01-01 00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }
}
