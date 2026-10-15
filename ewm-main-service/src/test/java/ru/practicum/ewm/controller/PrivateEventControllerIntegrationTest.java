package ru.practicum.ewm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.event.Location;
import ru.practicum.ewm.dto.event.request.NewEventRequest;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционный тест приватного API событий на реальном контексте Spring:
 * проверяет сквозную работу контроллер -> сервис -> JPA, а также то, что
 * контекст стартует без Ambiguous mapping (конфликт /users/{userId}/events/{eventId}/requests).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("h2")
class PrivateEventControllerIntegrationTest {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    private Long userId;
    private Long categoryId;

    @BeforeEach
    void setUp() {
        User user = User.builder().name("Тест").email("integration@test.ru").build();
        em.persist(user);
        userId = user.getId();

        Category category = Category.builder().name("Интеграционная категория").build();
        em.persist(category);
        categoryId = category.getId();

        em.flush();
    }

    private NewEventRequest validRequest(String date) {
        return NewEventRequest.builder()
                .annotation("Аннотация для интеграционного теста достаточной длины")
                .category(categoryId)
                .description("Описание для интеграционного теста достаточной длины")
                .eventDate(date)
                .location(Location.builder().lat(55.75f).lon(37.61f).build())
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .title("Интеграционное событие")
                .build();
    }

    @Test
    @DisplayName("POST /users/{id}/events возвращает 201 и EventFullDto со state=PENDING")
    void createEventReturnsFullDto() throws Exception {
        String future = LocalDateTime.now().plusDays(2).format(FMT);

        mockMvc.perform(post("/users/" + userId + "/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(future))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.state").value("PENDING"))
                .andExpect(jsonPath("$.title").value("Интеграционное событие"))
                .andExpect(jsonPath("$.initiator.id").value(userId))
                .andExpect(jsonPath("$.category.id").value(categoryId));
    }

    @Test
    @DisplayName("POST с датой раньше 2 часов -> 409 Conflict")
    void createEventTooSoonReturns409() throws Exception {
        String soon = LocalDateTime.now().plusMinutes(30).format(FMT);

        mockMvc.perform(post("/users/" + userId + "/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(soon))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST с неверным форматом eventDate -> 400, а не 500")
    void createEventBadDateFormatReturns400() throws Exception {
        mockMvc.perform(post("/users/" + userId + "/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("31-12-2026 20:00"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST с несуществующим пользователем -> 404")
    void createEventUnknownUserReturns404() throws Exception {
        String future = LocalDateTime.now().plusDays(2).format(FMT);

        mockMvc.perform(post("/users/999999/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(future))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST с короткой аннотацией (<20 символов) -> 400 Bean Validation")
    void createEventShortAnnotationReturns400() throws Exception {
        String future = LocalDateTime.now().plusDays(2).format(FMT);
        NewEventRequest req = validRequest(future);
        req.setAnnotation("Коротко");

        mockMvc.perform(post("/users/" + userId + "/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /users/{id}/events возвращает список своих событий (confirmedRequests=0 из RequestService)")
    void getOwnEventsReturnsList() throws Exception {
        String future = LocalDateTime.now().plusDays(2).format(FMT);
        mockMvc.perform(post("/users/" + userId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest(future))));

        mockMvc.perform(get("/users/" + userId + "/events").param("from", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].confirmedRequests").value(0));
    }

    @Test
    @DisplayName("GET чужого события -> 404 (проверка владельца работает насквозь)")
    void getForeignEventReturns404() throws Exception {
        String future = LocalDateTime.now().plusDays(2).format(FMT);
        String created = mockMvc.perform(post("/users/" + userId + "/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(future))))
                .andReturn().getResponse().getContentAsString();
        long eventId = objectMapper.readTree(created).get("id").asLong();

        User other = User.builder().name("Друг").email("other@test.ru").build();
        em.persist(other);
        em.flush();

        mockMvc.perform(get("/users/" + other.getId() + "/events/" + eventId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /events публичный: созданное PENDING-событие не виден, контекст стартует без конфликта маппингов")
    void publicSearchHidesPendingEvents() throws Exception {
        String future = LocalDateTime.now().plusDays(2).format(FMT);
        mockMvc.perform(post("/users/" + userId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest(future))));

        // Если бы был Ambiguous mapping — контекст вообще не поднялся бы и весь класс упал бы.
        mockMvc.perform(get("/events").param("text", "Интеграционное").param("from", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
