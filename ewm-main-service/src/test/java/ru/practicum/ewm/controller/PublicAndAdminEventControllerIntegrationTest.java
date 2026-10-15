package ru.practicum.ewm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.event.Location;
import ru.practicum.ewm.dto.event.request.NewEventRequest;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.stats.client.StatClient;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты публичного и административного API событий.
 * StatClient полностью замокан: проверяем обогащение views, сортировку по просмотрам,
 * фильтрацию только опубликованных событий и жизненный цикл публикации админом.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class PublicAndAdminEventControllerIntegrationTest {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @MockBean
    private StatClient statClient;

    private User initiator;
    private Category category;

    @BeforeEach
    void setUp() {
        initiator = User.builder().name("Инициатор").email("init@test.ru").build();
        category = Category.builder().name("Театр").build();
        // сохраняем через репозиторий пользователей/категорий нельзя (их не инжектим) —
        // сохраним через EntityManager-зависимость EventRepository не подходит, используем JPA напрямую
    }

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    private Event persistEvent(EventState state, LocalDateTime eventDate, String title) {
        if (initiator.getId() == null) {
            em.persist(initiator);
            em.persist(category);
            em.flush();
        }
        Event e = Event.builder()
                .annotation("Аннотация интеграционного теста достаточной длины")
                .category(category)
                .confirmedRequests(0L)
                .createdOn(LocalDateTime.now().minusDays(1))
                .description("Описание интеграционного теста достаточной длины")
                .eventDate(eventDate)
                .initiator(initiator)
                .location(Location.builder().lat(55.75f).lon(37.61f).build())
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state(state)
                .title(title)
                .views(0L)
                .build();
        if (state == EventState.PUBLISHED) {
            e.setPublishedOn(LocalDateTime.now().minusHours(1));
        }
        return eventRepository.save(e);
    }

    @Test
    @DisplayName("GET /events/{id}: сохранён хит и views из статистики попали в ответ")
    void publicDetailsSaveHitAndFillViews() throws Exception {
        Event e = persistEvent(EventState.PUBLISHED, LocalDateTime.now().plusDays(2), "Публика с просмотрами");

        when(statClient.getStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of(ViewStatsDto.builder().uri("/events/" + e.getId()).hits(77L).build()));

        mockMvc.perform(get("/events/" + e.getId()).header("X-Forwarded-For", "10.0.0.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(e.getId()))
                .andExpect(jsonPath("$.views").value(77))
                .andExpect(jsonPath("$.confirmedRequests").value(0))
                .andExpect(jsonPath("$.state").value("PUBLISHED"));

        verify(statClient).saveHit(any());
    }

    @Test
    @DisplayName("GET /events/{id}: PENDING-событие недоступно публично -> 404, хит не пишется")
    void publicDetailsHidesPending() throws Exception {
        Event e = persistEvent(EventState.PENDING, LocalDateTime.now().plusDays(2), "Скрытое событие");

        mockMvc.perform(get("/events/" + e.getId()))
                .andExpect(status().isNotFound());

        verify(statClient, never()).saveHit(any());
    }

    @Test
    @DisplayName("GET /events/{id}: несуществующее событие -> 404")
    void publicDetailsNotFound() throws Exception {
        mockMvc.perform(get("/events/987654"))
                .andExpect(status().isNotFound());
    }


    @Test
    @DisplayName("GET /events: в выдачу попадают только PUBLISHED, views подставляются из статистики")
    void publicSearchReturnsOnlyPublishedWithViews() throws Exception {
        Event published = persistEvent(EventState.PUBLISHED, LocalDateTime.now().plusDays(1), "Видимое событие номер раз");
        persistEvent(EventState.PENDING, LocalDateTime.now().plusDays(1), "Невидимое событие номер два");

        when(statClient.getStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of(ViewStatsDto.builder().uri("/events/" + published.getId()).hits(5L).build()));

        mockMvc.perform(get("/events")
                        .param("text", "интеграционного")
                        .param("from", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + published.getId() + ")].views").value(5))
                .andExpect(jsonPath("$[?(@.title == 'Невидимое событие номер два')]").doesNotExist());
    }

    @Test
    @DisplayName("GET /events: sort=VIEWS упорядочивает по убыванию просмотров из статистики")
    void publicSearchSortsByViews() throws Exception {
        Event low = persistEvent(EventState.PUBLISHED, LocalDateTime.now().plusDays(1), "Мало смотрят");
        Event high = persistEvent(EventState.PUBLISHED, LocalDateTime.now().plusDays(2), "Много смотрят");

        when(statClient.getStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of(
                        ViewStatsDto.builder().uri("/events/" + low.getId()).hits(1L).build(),
                        ViewStatsDto.builder().uri("/events/" + high.getId()).hits(1000L).build()));

        mockMvc.perform(get("/events")
                        .param("sort", "VIEWS").param("from", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(high.getId()))
                .andExpect(jsonPath("$[0].views").value(1000))
                .andExpect(jsonPath("$[1].id").value(low.getId()));
    }

    @Test
    @DisplayName("GET /events: прошедшее событие скрыто без rangeStart/rangeEnd (фильтр isFuture)")
    void publicSearchHidesPastEvents() throws Exception {
        persistEvent(EventState.PUBLISHED, LocalDateTime.now().minusDays(5), "Уже прошло");

        mockMvc.perform(get("/events").param("text", "Уже прошло").param("from", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /events: тот же диапазон дат явно разрешает прошедшие события")
    void publicSearchRangeOverridesFutureFilter() throws Exception {
        Event past = persistEvent(EventState.PUBLISHED, LocalDateTime.now().minusDays(5), "Прошлое для диапазона");

        when(statClient.getStats(any(), any(), anyList(), anyBoolean())).thenReturn(List.of());

        mockMvc.perform(get("/events")
                        .param("rangeStart", LocalDateTime.now().minusDays(10).format(FMT))
                        .param("rangeEnd", LocalDateTime.now().format(FMT))
                        .param("text", "интеграционного")
                        .param("from", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(past.getId()));    }

    @Test
    @DisplayName("GET /events: неверный формат rangeStart -> 400")
    void publicSearchBadDateFormatReturns400() throws Exception {
        mockMvc.perform(get("/events").param("rangeStart", "2026-31-12 00:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /events: size=0 -> 400 (Bean Validation @Min(1))")
    void publicSearchZeroSizeReturns400() throws Exception {
        mockMvc.perform(get("/events").param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /admin/events/{id}: публикация переводит PENDING -> PUBLISHED и событие становится видно публично")
    void adminPublishMakesEventVisible() throws Exception {
        Event e = persistEvent(EventState.PENDING, LocalDateTime.now().plusDays(3), "Ждёт публикации");

        String body = objectMapper.writeValueAsString(
                java.util.Map.of("stateAction", "PUBLISH_EVENT"));

        mockMvc.perform(patch("/admin/events/" + e.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedOn").exists());

        when(statClient.getStats(any(), any(), anyList(), anyBoolean())).thenReturn(List.of());

        mockMvc.perform(get("/events")
                        .param("text", "интеграционного")
                        .param("from", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(e.getId()));    }

    @Test
    @DisplayName("PATCH /admin/events/{id}: отклонение уже опубликованного события -> 409")
    void adminRejectPublishedReturns409() throws Exception {
        Event e = persistEvent(EventState.PUBLISHED, LocalDateTime.now().plusDays(3), "Опубликовано давно");

        String body = objectMapper.writeValueAsString(
                java.util.Map.of("stateAction", "REJECT_EVENT"));

        mockMvc.perform(patch("/admin/events/" + e.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /admin/events: фильтр states=PUBLISHED работает, чужие статусы отсекаются")
    void adminSearchFiltersByState() throws Exception {
        Event published = persistEvent(EventState.PUBLISHED, LocalDateTime.now().plusDays(1), "Админ видит меня");
        Event canceled = persistEvent(EventState.CANCELED, LocalDateTime.now().plusDays(1), "Админ не видит меня");

        when(statClient.getStats(any(), any(), anyList(), anyBoolean())).thenReturn(List.of());

        mockMvc.perform(get("/admin/events")
                        .param("states", "PUBLISHED")
                        .param("from", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + published.getId() + ")].state").value("PUBLISHED"))
                .andExpect(jsonPath("$[?(@.id == " + canceled.getId() + ")]").doesNotExist());
    }

    @Test
    @DisplayName("GET /admin/events: несуществующий state -> 400 BadRequestException из спецификации")
    void adminSearchInvalidStateReturns400() throws Exception {
        mockMvc.perform(get("/admin/events").param("states", "NOT_A_STATE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users/{id}/events c валидным телом проходит сквозную валидацию DTO (@Valid на контроллере)")
    void createEventValidationWorksThroughController() throws Exception {
        NewEventRequest req = NewEventRequest.builder()
                .annotation("коротко")
                .category(null)
                .description("тоже коротко")
                .eventDate("bad-date")
                .location(null)
                .title("аб")
                .build();

        mockMvc.perform(post("/users/1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
