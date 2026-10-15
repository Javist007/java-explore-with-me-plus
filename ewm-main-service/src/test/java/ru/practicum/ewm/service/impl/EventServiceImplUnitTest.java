package ru.practicum.ewm.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.dto.event.Location;
import ru.practicum.ewm.dto.event.request.AdminParamSearch;
import ru.practicum.ewm.dto.event.request.EventSearchRequest;
import ru.practicum.ewm.dto.event.request.NewEventRequest;
import ru.practicum.ewm.dto.event.request.PublicParamSearch;
import ru.practicum.ewm.dto.event.request.UpdateEventAdminRequest;
import ru.practicum.ewm.dto.event.request.UpdateEventContext;
import ru.practicum.ewm.dto.event.request.UpdateEventRequest;
import ru.practicum.ewm.dto.event.response.EventFullDto;
import ru.practicum.ewm.dto.event.response.EventShortDto;
import ru.practicum.ewm.exception.model.BadRequestException;
import ru.practicum.ewm.exception.model.ConflictException;
import ru.practicum.ewm.exception.model.NotFoundException;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.service.RequestService;
import ru.practicum.stats.client.StatClient;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Юнит-тесты сервиса событий: бизнес-правила, валидация дат,
 * подстановка confirmedRequests из RequestService и views из StatClient.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceImplUnitTest {

    private static final Long USER_ID = 1L;
    private static final Long EVENT_ID = 10L;
    private static final Long CATEGORY_ID = 5L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private StatClient statClient;
    @Mock
    private RequestService requestService;
    @Mock
    private HttpServletRequest servletRequest;

    @InjectMocks
    private EventServiceImpl eventService;

    private User user;
    private Category category;

    @BeforeEach
    void setUp() {
        user = User.builder().id(USER_ID).name("Иван").email("ivan@test.ru").build();
        category = Category.builder().id(CATEGORY_ID).name("Кино").build();
    }

    private Event event(Long id, EventState state) {
        return Event.builder()
                .id(id)
                .annotation("Аннотация события для тестов, достаточной длины")
                .category(category)
                .confirmedRequests(0L)
                .createdOn(LocalDateTime.of(2026, 10, 1, 12, 0))
                .description("Описание события для тестов, достаточной длины")
                .eventDate(LocalDateTime.now().plusDays(3))
                .initiator(user)
                .location(Location.builder().lat(55.75f).lon(37.61f).build())
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state(state)
                .title("Тестовое событие")
                .views(0L)
                .build();
    }

    private NewEventRequest newEventRequest(String date) {
        return NewEventRequest.builder()
                .annotation("Аннотация события для тестов, достаточной длины")
                .category(CATEGORY_ID)
                .description("Описание события для тестов, достаточной длины")
                .eventDate(date)
                .location(Location.builder().lat(55.75f).lon(37.61f).build())
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .title("Тестовое событие")
                .build();
    }

    // ==================== createEvent ====================

    @Nested
    @DisplayName("POST /users/{userId}/events — создание события")
    class CreateEvent {

        @Test
        @DisplayName("Успешное создание: возвращает EventFullDto со статусом PENDING")
        void shouldCreateEvent() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(eventRepository.save(any(Event.class)))
                    .thenAnswer(inv -> {
                        Event e = inv.getArgument(0);
                        e.setId(EVENT_ID);
                        return e;
                    });

            String futureDate = LocalDateTime.now().plusDays(1)
                    .format(FMT);

            EventFullDto dto = eventService.createEvent(newEventRequest(futureDate), USER_ID);

            assertThat(dto.getId()).isEqualTo(EVENT_ID);
            assertThat(dto.getState()).isEqualTo("PENDING");
            assertThat(dto.getTitle()).isEqualTo("Тестовое событие");
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("Пользователь не найден -> 404 NotFoundException")
        void shouldThrowNotFoundWhenUserMissing() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.createEvent(newEventRequest("2026-12-31 20:00:00"), USER_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("User");

            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Категория не найдена -> 404 NotFoundException")
        void shouldThrowNotFoundWhenCategoryMissing() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.createEvent(newEventRequest("2026-12-31 20:00:00"), USER_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Category");
        }

        @Test
        @DisplayName("Неверный формат даты -> 400 BadRequestException (не 500!)")
        void shouldThrowBadRequestOnWrongDateFormat() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

            assertThatThrownBy(() -> eventService.createEvent(newEventRequest("31.12.2026 20:00"), USER_ID))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Неверный формат даты");

            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Дата раньше чем через 2 часа -> 409 ConflictException")
        void shouldThrowConflictWhenDateTooSoon() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

            String soonDate = LocalDateTime.now().plusMinutes(30)
                    .format(FMT);

            assertThatThrownBy(() -> eventService.createEvent(newEventRequest(soonDate), USER_ID))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("2 часа");

            verify(eventRepository, never()).save(any());
        }
    }

    // ==================== getEvents (приватный список) ====================

    @Nested
    @DisplayName("GET /users/{userId}/events — список своих событий")
    class GetPrivateEvents {

        @Test
        @DisplayName("confirmedRequests берётся из requestService, а не из колонки events.confirmed_requests")
        void shouldUseConfirmedCountFromRequestService() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            Event e1 = event(1L, EventState.PUBLISHED);
            Event e2 = event(2L, EventState.PENDING);
            e1.setConfirmedRequests(99L); // намеренно «рассинхронизированное» денормализованное поле
            when(eventRepository.findAllByInitiatorIdWithPagination(eq(USER_ID), eq(0), eq(10)))
                    .thenReturn(List.of(e1, e2));
            when(requestService.getConfirmedRequestsCount(anyCollection()))
                    .thenReturn(Map.of(1L, 3L)); // у второго события ключа нет -> 0

            List<EventShortDto> result = eventService.getEvents(USER_ID, new EventSearchRequest());

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getConfirmedRequests()).isEqualTo(3L);   // из RequestService, не 99
            assertThat(result.get(1).getConfirmedRequests()).isEqualTo(0L);   // getOrDefault
            verify(requestService).getConfirmedRequestsCount(List.of(1L, 2L));
        }

        @Test
        @DisplayName("size <= 0 -> 400 BadRequestException")
        void shouldRejectNonPositiveSize() {
            EventSearchRequest request = new EventSearchRequest();
            request.setSize(0);

            assertThatThrownBy(() -> eventService.getEvents(USER_ID, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("size");
        }

        @Test
        @DisplayName("size = null -> 400 BadRequestException (NPE-защита)")
        void shouldRejectNullSize() {
            EventSearchRequest request = new EventSearchRequest();
            request.setSize(null);

            assertThatThrownBy(() -> eventService.getEvents(USER_ID, request))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Отрицательный from нормализуется в 0")
        void shouldNormalizeNegativeFrom() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(eventRepository.findAllByInitiatorIdWithPagination(eq(USER_ID), eq(0), eq(10)))
                    .thenReturn(List.of());

            EventSearchRequest request = new EventSearchRequest();
            request.setFrom(-5);
            request.setSize(10);

            assertThat(eventService.getEvents(USER_ID, request)).isEmpty();
            verify(eventRepository).findAllByInitiatorIdWithPagination(USER_ID, 0, 10);
        }

        @Test
        @DisplayName("Пустой список событий: requestService вызывается с пустым списком id")
        void shouldHandleEmptyPage() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(eventRepository.findAllByInitiatorIdWithPagination(USER_ID, 0, 10)).thenReturn(List.of());
            when(requestService.getConfirmedRequestsCount(anyCollection())).thenReturn(Map.of());

            assertThat(eventService.getEvents(USER_ID, new EventSearchRequest())).isEmpty();
        }
    }

    // ==================== getEventsUser ====================

    @Nested
    @DisplayName("GET /users/{userId}/events/{eventId}")
    class GetEventUser {

        @Test
        @DisplayName("Событие найдено по владельцу, confirmedRequests из RequestService")
        void shouldReturnOwnEvent() {
            Event e = event(EVENT_ID, EventState.PENDING);
            e.setConfirmedRequests(7L); // игнорируется
            when(eventRepository.findByInitiatorIdAndId(USER_ID, EVENT_ID)).thenReturn(Optional.of(e));
            when(requestService.getConfirmedRequestsCount(List.of(EVENT_ID))).thenReturn(Map.of(EVENT_ID, 2L));

            EventFullDto dto = eventService.getEventsUser(USER_ID, EVENT_ID);

            assertThat(dto.getId()).isEqualTo(EVENT_ID);
            assertThat(dto.getConfirmedRequests()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Чужое/несуществующее событие -> 404 (не NPE!)")
        void shouldReturnNotFoundForForeignEvent() {
            when(eventRepository.findByInitiatorIdAndId(USER_ID, EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.getEventsUser(USER_ID, EVENT_ID))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ==================== updateEvent ====================

    @Nested
    @DisplayName("PATCH /users/{userId}/events/{eventId}")
    class UpdateEvent {

        @Test
        @DisplayName("Опубликованное событие редактировать нельзя -> 409")
        void shouldForbidEditingPublished() {
            Event e = event(EVENT_ID, EventState.PUBLISHED);
            when(eventRepository.findByInitiatorIdAndId(USER_ID, EVENT_ID)).thenReturn(Optional.of(e));

            UpdateEventRequest req = UpdateEventRequest.builder().title("Новый заголовок").build();

            assertThatThrownBy(() -> eventService.updateEvent(
                    UpdateEventContext.builder().request(req).userId(USER_ID).eventId(EVENT_ID).build()))
                    .isInstanceOf(ConflictException.class);

            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("SEND_TO_REVIEW переводит PENDING-событие обратно в PENDING без ошибок")
        void shouldApplySendToReview() {
            Event e = event(EVENT_ID, EventState.CANCELED);
            when(eventRepository.findByInitiatorIdAndId(USER_ID, EVENT_ID)).thenReturn(Optional.of(e));

            UpdateEventRequest req = UpdateEventRequest.builder()
                    .stateAction(UpdateEventRequest.StateAction.SEND_TO_REVIEW)
                    .build();

            EventFullDto dto = eventService.updateEvent(
                    UpdateEventContext.builder().request(req).userId(USER_ID).eventId(EVENT_ID).build());

            assertThat(dto.getState()).isEqualTo("PENDING");
            verify(eventRepository).save(e);
        }

        @Test
        @DisplayName("CANCEL_REVIEW отменяет событие")
        void shouldApplyCancelReview() {
            Event e = event(EVENT_ID, EventState.PENDING);
            when(eventRepository.findByInitiatorIdAndId(USER_ID, EVENT_ID)).thenReturn(Optional.of(e));

            UpdateEventRequest req = UpdateEventRequest.builder()
                    .stateAction(UpdateEventRequest.StateAction.CANCEL_REVIEW)
                    .build();

            EventFullDto dto = eventService.updateEvent(
                    UpdateEventContext.builder().request(req).userId(USER_ID).eventId(EVENT_ID).build());

            assertThat(dto.getState()).isEqualTo("CANCELED");
        }

        @Test
        @DisplayName("Изменение eventDate на слишком близкую дату -> 409")
        void shouldRejectTooSoonDate() {
            Event e = event(EVENT_ID, EventState.PENDING);
            when(eventRepository.findByInitiatorIdAndId(USER_ID, EVENT_ID)).thenReturn(Optional.of(e));

            String soon = LocalDateTime.now().plusMinutes(10)
                    .format(FMT);
            UpdateEventRequest req = UpdateEventRequest.builder().eventDate(soon).build();

            assertThatThrownBy(() -> eventService.updateEvent(
                    UpdateEventContext.builder().request(req).userId(USER_ID).eventId(EVENT_ID).build()))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("2 часа");
        }

        @Test
        @DisplayName("Неверный формат eventDate при PATCH -> 400 BadRequestException")
        void shouldRejectBadDateFormatOnPatch() {
            Event e = event(EVENT_ID, EventState.PENDING);
            when(eventRepository.findByInitiatorIdAndId(USER_ID, EVENT_ID)).thenReturn(Optional.of(e));

            UpdateEventRequest req = UpdateEventRequest.builder().eventDate("not-a-date").build();

            assertThatThrownBy(() -> eventService.updateEvent(
                    UpdateEventContext.builder().request(req).userId(USER_ID).eventId(EVENT_ID).build()))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Смена категории: несуществующая категория -> 404")
        void shouldRejectUnknownCategory() {
            Event e = event(EVENT_ID, EventState.PENDING);
            when(eventRepository.findByInitiatorIdAndId(USER_ID, EVENT_ID)).thenReturn(Optional.of(e));
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            UpdateEventRequest req = UpdateEventRequest.builder().category(999L).build();

            assertThatThrownBy(() -> eventService.updateEvent(
                    UpdateEventContext.builder().request(req).userId(USER_ID).eventId(EVENT_ID).build()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Category");
        }
    }

    // ==================== adminGetEvents ====================

    @Nested
    @DisplayName("GET /admin/events")
    class AdminGetEvents {

        @Test
        @DisplayName("Без фильтров: пустая спецификация допустима, confirmedRequests из RequestService")
        void shouldReturnEventsWithConfirmedCounts() {
            Event e = event(EVENT_ID, EventState.PUBLISHED);
            Page<Event> page = new PageImpl<>(List.of(e));
            when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(requestService.getConfirmedRequestsCount(anyCollection()))
                    .thenReturn(Map.of(EVENT_ID, 4L));

            AdminParamSearch search = AdminParamSearch.builder().from(0).size(10).build();

            List<EventFullDto> result = eventService.adminGetEvents(search);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getConfirmedRequests()).isEqualTo(4L);
        }

        @Test
        @DisplayName("Пустая страница -> пустой список, requestService не дёргается")
        void shouldShortCircuitOnEmptyPage() {
            when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(Page.empty());

            AdminParamSearch search = AdminParamSearch.builder().from(0).size(10).build();

            assertThat(eventService.adminGetEvents(search)).isEmpty();
            verify(requestService, never()).getConfirmedRequestsCount(anyCollection());
        }

        @Test
        @DisplayName("Неверный формат rangeStart -> 400 BadRequestException")
        void shouldRejectBadRangeDate() {
            AdminParamSearch search = AdminParamSearch.builder()
                    .rangeStart("2026/01/01").from(0).size(10).build();

            assertThatThrownBy(() -> eventService.adminGetEvents(search))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Неверный формат даты");
        }

        @Test
        @DisplayName("size <= 0 -> 400")
        void shouldRejectZeroSize() {
            AdminParamSearch search = AdminParamSearch.builder().from(0).size(0).build();

            assertThatThrownBy(() -> eventService.adminGetEvents(search))
                    .isInstanceOf(BadRequestException.class);
        }
    }


    @Nested
    @DisplayName("PATCH /admin/events/{eventId}")
    class AdminUpdateEvent {

        @Test
        @DisplayName("PUBLISH_EVENT из PENDING: статус PUBLISHED + publishedOn заполнен")
        void shouldPublishPendingEvent() {
            Event e = event(EVENT_ID, EventState.PENDING);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(e));

            UpdateEventAdminRequest req = UpdateEventAdminRequest.builder()
                    .stateAction(UpdateEventAdminRequest.StateAction.PUBLISH_EVENT)
                    .build();

            EventFullDto dto = eventService.adminUpdateEvent(EVENT_ID, req);

            assertThat(dto.getState()).isEqualTo("PUBLISHED");
            assertThat(dto.getPublishedOn()).isNotBlank();
            verify(eventRepository).save(e);
        }

        @Test
        @DisplayName("PUBLISH_EVENT уже опубликованного события -> 409")
        void shouldRejectPublishOfPublished() {
            Event e = event(EVENT_ID, EventState.PUBLISHED);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(e));

            UpdateEventAdminRequest req = UpdateEventAdminRequest.builder()
                    .stateAction(UpdateEventAdminRequest.StateAction.PUBLISH_EVENT)
                    .build();

            assertThatThrownBy(() -> eventService.adminUpdateEvent(EVENT_ID, req))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("REJECT_EVENT опубликованного события -> 409")
        void shouldRejectRejectOfPublished() {
            Event e = event(EVENT_ID, EventState.PUBLISHED);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(e));

            UpdateEventAdminRequest req = UpdateEventAdminRequest.builder()
                    .stateAction(UpdateEventAdminRequest.StateAction.REJECT_EVENT)
                    .build();

            assertThatThrownBy(() -> eventService.adminUpdateEvent(EVENT_ID, req))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("REJECT_EVENT из PENDING -> CANCELED")
        void shouldRejectPendingEvent() {
            Event e = event(EVENT_ID, EventState.PENDING);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(e));

            UpdateEventAdminRequest req = UpdateEventAdminRequest.builder()
                    .stateAction(UpdateEventAdminRequest.StateAction.REJECT_EVENT)
                    .build();

            assertThat(eventService.adminUpdateEvent(EVENT_ID, req).getState()).isEqualTo("CANCELED");
        }

        @Test
        @DisplayName("Событие не найдено -> 404")
        void shouldReturnNotFound() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

            UpdateEventAdminRequest req = UpdateEventAdminRequest.builder().title("Что-то").build();

            assertThatThrownBy(() -> eventService.adminUpdateEvent(EVENT_ID, req))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Смена даты PUBLISHED-события раньше publishedOn+1h -> 409")
        void shouldEnforceOneHourRuleForPublished() {
            Event e = event(EVENT_ID, EventState.PUBLISHED);
            e.setPublishedOn(LocalDateTime.of(2026, 10, 10, 10, 0));
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(e));

            UpdateEventAdminRequest req = UpdateEventAdminRequest.builder()
                    .eventDate("2026-10-10 10:30:00")
                    .build();

            assertThatThrownBy(() -> eventService.adminUpdateEvent(EVENT_ID, req))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("час");
        }

        @Test
        @DisplayName("publishedOn == null (PENDING-событие): проверки даты не падают NPE, используется createdOn")
        void shouldNotThrowNpeWhenPublishedOnNull() {
            Event e = event(EVENT_ID, EventState.PENDING); // publishedOn = null, createdOn = 2026-10-01T12:00
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(e));

            UpdateEventAdminRequest req = UpdateEventAdminRequest.builder()
                    .eventDate("2026-10-01 12:30:00") // раньше createdOn + 1h
                    .build();

            assertThatThrownBy(() -> eventService.adminUpdateEvent(EVENT_ID, req))
                    .isInstanceOf(ConflictException.class); // 409, а не NPE!
        }
    }

    // ==================== getPublicEvents ====================

    @Nested
    @DisplayName("GET /events — публичный поиск")
    class PublicSearch {

        @Test
        @DisplayName("views и confirmedRequests обогащаются из StatClient и RequestService")
        void shouldEnrichDtosWithViewsAndConfirmed() {
            Event e1 = event(1L, EventState.PUBLISHED);
            Event e2 = event(2L, EventState.PUBLISHED);
            when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(e1, e2)));
            when(statClient.getStats(any(), any(), anyList(), anyBoolean()))
                    .thenReturn(List.of(
                            ViewStatsDto.builder().uri("/events/1").hits(100L).build(),
                            ViewStatsDto.builder().uri("/events/2").hits(50L).build()));
            when(requestService.getConfirmedRequestsCount(anyCollection()))
                    .thenReturn(Map.of(1L, 1L));

            PublicParamSearch search = PublicParamSearch.builder().from(0).size(10).build();

            List<EventShortDto> result = eventService.getPublicEvents(search);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getViews()).isEqualTo(100L);
            assertThat(result.get(0).getConfirmedRequests()).isEqualTo(1L);
            assertThat(result.get(1).getViews()).isEqualTo(50L);
            assertThat(result.get(1).getConfirmedRequests()).isZero();
        }

        @Test
        @DisplayName("sort=VIEWS сортирует обогащённый список по убыванию просмотров")
        void shouldSortByViewsDescending() {
            Event e1 = event(1L, EventState.PUBLISHED);
            Event e2 = event(2L, EventState.PUBLISHED);
            when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(e1, e2)));
            when(statClient.getStats(any(), any(), anyList(), anyBoolean()))
                    .thenReturn(List.of(
                            ViewStatsDto.builder().uri("/events/1").hits(10L).build(),
                            ViewStatsDto.builder().uri("/events/2").hits(999L).build()));
            when(requestService.getConfirmedRequestsCount(anyCollection())).thenReturn(Map.of());

            PublicParamSearch search = PublicParamSearch.builder()
                    .from(0).size(10).sort(PublicParamSearch.Sort.VIEWS).build();

            List<EventShortDto> result = eventService.getPublicEvents(search);

            assertThat(result.get(0).getId()).isEqualTo(2L); // больше просмотров — выше
            assertThat(result.get(0).getViews()).isEqualTo(999L);
        }

        @Test
        @DisplayName("Дубли uri в ответе статистики схлопываются суммированием (merge Long::sum)")
        void shouldMergeDuplicateStatUris() {
            Event e1 = event(1L, EventState.PUBLISHED);
            when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(e1)));
            when(statClient.getStats(any(), any(), anyList(), anyBoolean()))
                    .thenReturn(List.of(
                            ViewStatsDto.builder().app("a").uri("/events/1").hits(5L).build(),
                            ViewStatsDto.builder().app("b").uri("/events/1").hits(7L).build()));
            when(requestService.getConfirmedRequestsCount(anyCollection())).thenReturn(Map.of());

            List<EventShortDto> result = eventService.getPublicEvents(
                    PublicParamSearch.builder().from(0).size(10).build());

            assertThat(result.get(0).getViews()).isEqualTo(12L); // IllegalStateException бы упал без merge
        }

        @Test
        @DisplayName("Пустая выдача -> пустой список без обращения к статистике")
        void shouldSkipStatsOnEmptyResult() {
            when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(Page.empty());

            assertThat(eventService.getPublicEvents(
                    PublicParamSearch.builder().from(0).size(10).build())).isEmpty();

            verify(statClient, never()).getStats(any(), any(), anyList(), anyBoolean());
        }

        @Test
        @DisplayName("Неверный формат rangeStart -> 400 BadRequestException")
        void shouldRejectBadRangeFormat() {
            PublicParamSearch search = PublicParamSearch.builder()
                    .rangeStart("01.01.2026").from(0).size(10).build();

            assertThatThrownBy(() -> eventService.getPublicEvents(search))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("size <= 0 -> 400")
        void shouldRejectZeroSize() {
            PublicParamSearch search = PublicParamSearch.builder().from(0).size(0).build();

            assertThatThrownBy(() -> eventService.getPublicEvents(search))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    // ==================== getPublicEventId ====================

    @Nested
    @DisplayName("GET /events/{id} — публичные детали")
    class PublicDetails {

        @Test
        @DisplayName("Хит сохраняется, views и confirmedRequests попадают в DTO")
        void shouldSaveHitAndFillViews() {
            Event e = event(EVENT_ID, EventState.PUBLISHED);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(e));
            when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
            when(statClient.getStats(any(), any(), eq(List.of("/events/" + EVENT_ID)), eq(false)))
                    .thenReturn(List.of(ViewStatsDto.builder().uri("/events/" + EVENT_ID).hits(42L).build()));
            when(requestService.getConfirmedRequestsCount(List.of(EVENT_ID)))
                    .thenReturn(Map.of(EVENT_ID, 8L));

            EventFullDto dto = eventService.getPublicEventId(EVENT_ID, servletRequest);

            assertThat(dto.getViews()).isEqualTo(42L);
            assertThat(dto.getConfirmedRequests()).isEqualTo(8L);

            ArgumentCaptor<EndpointHitDto> hitCaptor = ArgumentCaptor.forClass(EndpointHitDto.class);
            verify(statClient).saveHit(hitCaptor.capture());
            assertThat(hitCaptor.getValue().getUri()).isEqualTo("/events/" + EVENT_ID);
            assertThat(hitCaptor.getValue().getIp()).isEqualTo("127.0.0.1");
        }

        @Test
        @DisplayName("Пустая статистика -> views = 0, без IndexOutOfBounds")
        void shouldDefaultViewsToZero() {
            Event e = event(EVENT_ID, EventState.PUBLISHED);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(e));
            when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
            when(statClient.getStats(any(), any(), anyList(), anyBoolean())).thenReturn(List.of());
            when(requestService.getConfirmedRequestsCount(anyCollection())).thenReturn(Map.of());

            assertThat(eventService.getPublicEventId(EVENT_ID, servletRequest).getViews()).isZero();
        }

        @Test
        @DisplayName("Неопубликованное событие -> 404")
        void shouldReturnNotFoundForPending() {
            Event e = event(EVENT_ID, EventState.PENDING);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(e));

            assertThatThrownBy(() -> eventService.getPublicEventId(EVENT_ID, servletRequest))
                    .isInstanceOf(NotFoundException.class);

            verify(statClient, never()).saveHit(any());
        }

        @Test
        @DisplayName("Событие не найдено -> 404")
        void shouldReturnNotFoundForMissing() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.getPublicEventId(EVENT_ID, servletRequest))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
