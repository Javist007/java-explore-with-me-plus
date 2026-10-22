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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
@ActiveProfiles("h2")
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

    /**
     * Инициализирует тестовые данные перед выполнением каждого теста.
     * Создает тестового инициатора и категорию.
     */
    @BeforeEach
    void setUp() {
        initiator = User.builder().name("Инициатор").email("init@test.ru").build();
        category = Category.builder().name("Театр").build();
    }

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    /**
     * Создает и сохраняет событие с указанными параметрами.
     * При необходимости сохраняет инициатора и категорию в базе данных.
     *
     * @param state     статус события
     * @param eventDate дата проведения события
     * @param title     заголовок события
     * @return сохраненная сущность события
     */
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

    /**
     * Тестирует сохранение хита и обогащение ответа статистикой просмотров.
     * Проверяет, что при запросе опубликованного события сохраняется хит в статистике
     * и количество просмотров из статистики попадает в ответ.
     */
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

    /**
     * Тестирует скрытие неопубликованных событий.
     * Проверяет, что событие в статусе PENDING недоступно публично
     * и хит не сохраняется в статистике.
     */
    @Test
    @DisplayName("GET /events/{id}: PENDING-событие недоступно публично -> 404, хит не пишется")
    void publicDetailsHidesPending() throws Exception {
        Event e = persistEvent(EventState.PENDING, LocalDateTime.now().plusDays(2), "Скрытое событие");

        mockMvc.perform(get("/events/" + e.getId()))
                .andExpect(status().isNotFound());

        verify(statClient, never()).saveHit(any());
    }

    /**
     * Тестирует обработку несуществующего события.
     * Проверяет, что запрос несуществующего события возвращает 404.
     */
    @Test
    @DisplayName("GET /events/{id}: несуществующее событие -> 404")
    void publicDetailsNotFound() throws Exception {
        mockMvc.perform(get("/events/987654"))
                .andExpect(status().isNotFound());
    }


    /**
     * Тестирует фильтрацию только опубликованных событий в публичном поиске.
     * Проверяет, что в выдачу попадают только события в статусе PUBLISHED
     * и количество просмотров подставляется из статистики.
     */
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

    /**
     * Тестирует сортировку событий по количеству просмотров.
     * Проверяет, что параметр sort=VIEWS упорядочивает события
     * по убыванию количества просмотров из статистики.
     */
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

    /**
     * Тестирует скрытие прошедших событий по умолчанию.
     * Проверяет, что прошедшие события скрыты в публичном поиске
     * при отсутствии параметров rangeStart/rangeEnd.
     */
    @Test
    @DisplayName("GET /events: прошедшее событие скрыто без rangeStart/rangeEnd (фильтр isFuture)")
    void publicSearchHidesPastEvents() throws Exception {
        persistEvent(EventState.PUBLISHED, LocalDateTime.now().minusDays(5), "Уже прошло");

        mockMvc.perform(get("/events").param("text", "Уже прошло").param("from", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    /**
     * Тестирует отображение прошедших событий при указанном диапазоне дат.
     * Проверяет, что явное указание диапазона дат позволяет увидеть прошедшие события.
     */
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
                .andExpect(jsonPath("$[0].id").value(past.getId()));
    }

    /**
     * Тестирует обработку неверного формата даты.
     * Проверяет, что неверный формат параметра rangeStart возвращает 400.
     */
    @Test
    @DisplayName("GET /events: неверный формат rangeStart -> 400")
    void publicSearchBadDateFormatReturns400() throws Exception {
        mockMvc.perform(get("/events").param("rangeStart", "2026-31-12 00:00:00"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Тестирует валидацию параметра size.
     * Проверяет, что size=0 возвращает ошибку 400 на уровне Bean Validation.
     */
    @Test
    @DisplayName("GET /events: size=0 -> 400 (Bean Validation @Min(1))")
    void publicSearchZeroSizeReturns400() throws Exception {
        mockMvc.perform(get("/events").param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Тестирует публикацию события администратором.
     * Проверяет, что публикация переводит событие из PENDING в PUBLISHED
     * и оно становится доступно в публичном поиске.
     */
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
                .andExpect(jsonPath("$[0].id").value(e.getId()));
    }

    /**
     * Тестирует отклонение опубликованного события.
     * Проверяет, что попытка отклонить уже опубликованное событие
     * возвращает ошибку 409 Conflict.
     */
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

    /**
     * Тестирует фильтрацию событий по статусу администратором.
     * Проверяет, что фильтр states=PUBLISHED возвращает только опубликованные события.
     */
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

    /**
     * Тестирует обработку несуществующего статуса.
     * Проверяет, что несуществующий статус возвращает ошибку 400.
     */
    @Test
    @DisplayName("GET /admin/events: несуществующий state -> 400 BadRequestException из спецификации")
    void adminSearchInvalidStateReturns400() throws Exception {
        mockMvc.perform(get("/admin/events").param("states", "NOT_A_STATE"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Тестирует сквозную валидацию DTO на уровне контроллера.
     * Проверяет, что невалидное тело запроса возвращает ошибку 400.
     */
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

    /**
     * Тестирует фильтр по инициаторам в административном поиске.
     * Раньше фильтр сравнивал сущность User с id и падал с 500.
     */
    @Test
    @DisplayName("GET /admin/events?users=...: фильтр по id инициатора работает, а не падает 500")
    void adminSearchFiltersByUsers() throws Exception {
        Event event = persistEvent(EventState.PUBLISHED, LocalDateTime.now().plusDays(1), "Событие инициатора");
        when(statClient.getStats(any(), any(), anyList(), anyBoolean())).thenReturn(List.of());

        mockMvc.perform(get("/admin/events")
                        .param("users", String.valueOf(initiator.getId()))
                        .param("from", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + event.getId() + ")]").exists());

        mockMvc.perform(get("/admin/events")
                        .param("users", "987654")
                        .param("from", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * Тестирует проверку диапазона дат в публичном поиске.
     * Проверяет, что rangeStart позже rangeEnd даёт 400, а запрос не попадает в статистику.
     */
    @Test
    @DisplayName("GET /events: rangeStart позже rangeEnd -> 400, хит не пишется")
    void publicSearchRangeStartAfterRangeEndReturns400() throws Exception {
        mockMvc.perform(get("/events")
                        .param("rangeStart", "2035-01-01 00:00:00")
                        .param("rangeEnd", "2020-01-01 00:00:00"))
                .andExpect(status().isBadRequest());

        verify(statClient, never()).saveHit(any());
    }

    /**
     * Тестирует запись обращения к списку событий в сервис статистики.
     * Проверяет, что GET /events сохраняет хит с uri "/events".
     */
    @Test
    @DisplayName("GET /events: запрос списка сохраняется в статистике с uri /events")
    void publicSearchSavesHit() throws Exception {
        mockMvc.perform(get("/events").param("from", "0").param("size", "10"))
                .andExpect(status().isOk());

        verify(statClient).saveHit(argThat(hit -> "/events".equals(hit.getUri())
                && "ewm-main-service".equals(hit.getApp())));
    }
}
