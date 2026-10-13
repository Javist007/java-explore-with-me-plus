package ru.practicum.ewm.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.dto.participation.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.dto.participation.request.RequestStatusUpdateParams;
import ru.practicum.ewm.dto.participation.response.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.participation.response.ParticipationRequestDto;
import ru.practicum.ewm.exception.model.BadRequestException;
import ru.practicum.ewm.exception.model.ConflictException;
import ru.practicum.ewm.exception.model.NotFoundException;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.model.ParticipationRequest;
import ru.practicum.ewm.model.RequestStatus;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.repository.projection.EventRequestCount;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

    private static final long INITIATOR_ID = 1L;
    private static final long USER_ID = 2L;
    private static final long EVENT_ID = 10L;

    @Mock
    private ParticipationRequestRepository requestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private RequestServiceImpl requestService;

    private final User initiator = user(INITIATOR_ID);
    private final User requester = user(USER_ID);

    // ---------- getUserRequests ----------

    @Test
    void getUserRequests_whenUserNotFound_throwsNotFound() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> requestService.getUserRequests(USER_ID))
                .isInstanceOf(NotFoundException.class);
        verify(requestRepository, never()).findAllByRequesterId(anyLong());
    }

    @Test
    void getUserRequests_returnsMappedRequests() {
        Event event = event(EventState.PUBLISHED, 10, true);
        ParticipationRequest request = request(5L, event, requester, RequestStatus.PENDING);
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(requestRepository.findAllByRequesterId(USER_ID)).thenReturn(List.of(request));

        List<ParticipationRequestDto> result = requestService.getUserRequests(USER_ID);

        assertThat(result).hasSize(1);
        ParticipationRequestDto dto = result.get(0);
        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getEvent()).isEqualTo(EVENT_ID);
        assertThat(dto.getRequester()).isEqualTo(USER_ID);
        assertThat(dto.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(dto.getCreated()).isEqualTo(request.getCreated());
    }

    // ---------- addRequest ----------

    @Test
    void addRequest_whenUserNotFound_throwsNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.addRequest(USER_ID, EVENT_ID))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(eventRepository, requestRepository);
    }

    @Test
    void addRequest_whenEventNotFound_throwsNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(requester));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.addRequest(USER_ID, EVENT_ID))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(requestRepository);
    }

    @Test
    void addRequest_whenUserIsInitiator_throwsConflict() {
        when(userRepository.findById(INITIATOR_ID)).thenReturn(Optional.of(initiator));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(EventState.PUBLISHED, 10, true)));

        assertThatThrownBy(() -> requestService.addRequest(INITIATOR_ID, EVENT_ID))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(requestRepository);
    }

    @Test
    void addRequest_whenEventNotPublished_throwsConflict() {
        stubUserAndEvent(event(EventState.PENDING, 10, true));

        assertThatThrownBy(() -> requestService.addRequest(USER_ID, EVENT_ID))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(requestRepository);
    }

    @Test
    void addRequest_whenRequestAlreadyExists_throwsConflict() {
        stubUserAndEvent(event(EventState.PUBLISHED, 10, true));
        when(requestRepository.existsByEventIdAndRequesterId(EVENT_ID, USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> requestService.addRequest(USER_ID, EVENT_ID))
                .isInstanceOf(ConflictException.class);
        verify(requestRepository, never()).save(any());
    }

    @Test
    void addRequest_whenParticipantLimitReached_throwsConflict() {
        stubUserAndEvent(event(EventState.PUBLISHED, 2, true));
        when(requestRepository.existsByEventIdAndRequesterId(EVENT_ID, USER_ID)).thenReturn(false);
        when(requestRepository.countByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED)).thenReturn(2L);

        assertThatThrownBy(() -> requestService.addRequest(USER_ID, EVENT_ID))
                .isInstanceOf(ConflictException.class);
        verify(requestRepository, never()).save(any());
    }

    @Test
    void addRequest_withModerationAndLimit_savesPendingRequest() {
        Event event = event(EventState.PUBLISHED, 2, true);
        stubUserAndEvent(event);
        when(requestRepository.existsByEventIdAndRequesterId(EVENT_ID, USER_ID)).thenReturn(false);
        when(requestRepository.countByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED)).thenReturn(1L);
        stubSaveWithId(100L);
        LocalDateTime before = LocalDateTime.now();

        ParticipationRequestDto result = requestService.addRequest(USER_ID, EVENT_ID);

        ArgumentCaptor<ParticipationRequest> captor = ArgumentCaptor.forClass(ParticipationRequest.class);
        verify(requestRepository).save(captor.capture());
        ParticipationRequest saved = captor.getValue();
        assertThat(saved.getEvent()).isSameAs(event);
        assertThat(saved.getRequester()).isSameAs(requester);
        assertThat(saved.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(saved.getCreated()).isBetween(before, LocalDateTime.now());
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo(RequestStatus.PENDING);
    }

    @Test
    void addRequest_whenNoParticipantLimit_confirmsWithoutCountingParticipants() {
        stubUserAndEvent(event(EventState.PUBLISHED, 0, true));
        when(requestRepository.existsByEventIdAndRequesterId(EVENT_ID, USER_ID)).thenReturn(false);
        stubSaveWithId(100L);

        ParticipationRequestDto result = requestService.addRequest(USER_ID, EVENT_ID);

        assertThat(result.getStatus()).isEqualTo(RequestStatus.CONFIRMED);
        verify(requestRepository, never()).countByEventIdAndStatus(anyLong(), any());
    }

    @Test
    void addRequest_whenModerationDisabled_confirmsRequest() {
        stubUserAndEvent(event(EventState.PUBLISHED, 5, false));
        when(requestRepository.existsByEventIdAndRequesterId(EVENT_ID, USER_ID)).thenReturn(false);
        when(requestRepository.countByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED)).thenReturn(1L);
        stubSaveWithId(100L);

        ParticipationRequestDto result = requestService.addRequest(USER_ID, EVENT_ID);

        assertThat(result.getStatus()).isEqualTo(RequestStatus.CONFIRMED);
    }

    // ---------- cancelRequest ----------

    @Test
    void cancelRequest_whenRequestNotFound_throwsNotFound() {
        when(requestRepository.findByIdAndRequesterId(5L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.cancelRequest(USER_ID, 5L))
                .isInstanceOf(NotFoundException.class);
        verify(requestRepository, never()).save(any());
    }

    @Test
    void cancelRequest_setsCanceledStatus() {
        ParticipationRequest request = request(5L, event(EventState.PUBLISHED, 10, true), requester,
                RequestStatus.CONFIRMED);
        when(requestRepository.findByIdAndRequesterId(5L, USER_ID)).thenReturn(Optional.of(request));
        when(requestRepository.save(request)).thenReturn(request);

        ParticipationRequestDto result = requestService.cancelRequest(USER_ID, 5L);

        assertThat(result.getStatus()).isEqualTo(RequestStatus.CANCELED);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.CANCELED);
    }

    // ---------- getEventRequests ----------

    @Test
    void getEventRequests_whenUserIsNotInitiator_throwsNotFound() {
        when(eventRepository.existsByIdAndInitiatorId(EVENT_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> requestService.getEventRequests(USER_ID, EVENT_ID))
                .isInstanceOf(NotFoundException.class);
        verify(requestRepository, never()).findAllByEventId(anyLong());
    }

    @Test
    void getEventRequests_returnsMappedRequests() {
        Event event = event(EventState.PUBLISHED, 10, true);
        when(eventRepository.existsByIdAndInitiatorId(EVENT_ID, INITIATOR_ID)).thenReturn(true);
        when(requestRepository.findAllByEventId(EVENT_ID)).thenReturn(List.of(
                request(5L, event, requester, RequestStatus.PENDING),
                request(6L, event, user(3L), RequestStatus.CONFIRMED)));

        List<ParticipationRequestDto> result = requestService.getEventRequests(INITIATOR_ID, EVENT_ID);

        assertThat(result).extracting(ParticipationRequestDto::getId).containsExactly(5L, 6L);
        assertThat(result).extracting(ParticipationRequestDto::getRequester).containsExactly(USER_ID, 3L);
    }

    // ---------- updateRequestStatuses: проверки входных данных ----------

    @Test
    void updateRequestStatuses_whenNewStatusIsPending_throwsBadRequest() {
        assertThatThrownBy(() -> requestService.updateRequestStatuses(params(RequestStatus.PENDING, 5L)))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(eventRepository, requestRepository);
    }

    @Test
    void updateRequestStatuses_whenNewStatusIsCanceled_throwsBadRequest() {
        assertThatThrownBy(() -> requestService.updateRequestStatuses(params(RequestStatus.CANCELED, 5L)))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(eventRepository, requestRepository);
    }

    @Test
    void updateRequestStatuses_whenEventNotFoundOrNotInitiator_throwsNotFound() {
        when(eventRepository.findByIdAndInitiatorId(EVENT_ID, INITIATOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.updateRequestStatuses(params(RequestStatus.CONFIRMED, 5L)))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(requestRepository);
    }

    @Test
    void updateRequestStatuses_whenSomeRequestsNotFound_throwsNotFound() {
        Event event = stubInitiatorEvent(10, true);
        when(requestRepository.findAllByIdInAndEventId(List.of(5L, 6L), EVENT_ID))
                .thenReturn(List.of(request(5L, event, requester, RequestStatus.PENDING)));

        assertThatThrownBy(() -> requestService.updateRequestStatuses(params(RequestStatus.CONFIRMED, 5L, 6L)))
                .isInstanceOf(NotFoundException.class);
        verify(requestRepository, never()).saveAll(any());
    }

    @Test
    void updateRequestStatuses_whenRequestIsNotPending_throwsConflict() {
        Event event = stubInitiatorEvent(10, true);
        when(requestRepository.findAllByIdInAndEventId(List.of(5L), EVENT_ID))
                .thenReturn(List.of(request(5L, event, requester, RequestStatus.CONFIRMED)));

        assertThatThrownBy(() -> requestService.updateRequestStatuses(params(RequestStatus.REJECTED, 5L)))
                .isInstanceOf(ConflictException.class);
        verify(requestRepository, never()).saveAll(any());
    }

    @Test
    void updateRequestStatuses_withDuplicateIds_treatsThemAsOneRequest() {
        Event event = stubInitiatorEvent(10, true);
        when(requestRepository.findAllByIdInAndEventId(List.of(5L, 5L), EVENT_ID))
                .thenReturn(List.of(request(5L, event, requester, RequestStatus.PENDING)));

        EventRequestStatusUpdateResult result =
                requestService.updateRequestStatuses(params(RequestStatus.REJECTED, 5L, 5L));

        assertThat(result.getRejectedRequests()).hasSize(1);
    }

    // ---------- updateRequestStatuses: отклонение ----------

    @Test
    void updateRequestStatuses_reject_rejectsAllRequests() {
        Event event = stubInitiatorEvent(10, true);
        ParticipationRequest first = request(5L, event, requester, RequestStatus.PENDING);
        ParticipationRequest second = request(6L, event, user(3L), RequestStatus.PENDING);
        when(requestRepository.findAllByIdInAndEventId(List.of(5L, 6L), EVENT_ID)).thenReturn(List.of(first, second));

        EventRequestStatusUpdateResult result =
                requestService.updateRequestStatuses(params(RequestStatus.REJECTED, 5L, 6L));

        assertThat(result.getConfirmedRequests()).isEmpty();
        assertThat(result.getRejectedRequests()).extracting(ParticipationRequestDto::getId).containsExactly(5L, 6L);
        assertThat(first.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(second.getStatus()).isEqualTo(RequestStatus.REJECTED);
        verify(requestRepository).saveAll(List.of(first, second));
        verify(requestRepository, never()).countByEventIdAndStatus(anyLong(), any());
        verify(requestRepository, never()).updateStatusByEventId(anyLong(), any(), any());
    }

    // ---------- updateRequestStatuses: подтверждение ----------

    @Test
    void updateRequestStatuses_confirmWithoutLimit_confirmsAllWithoutCounting() {
        Event event = stubInitiatorEvent(0, true);
        ParticipationRequest first = request(5L, event, requester, RequestStatus.PENDING);
        ParticipationRequest second = request(6L, event, user(3L), RequestStatus.PENDING);
        when(requestRepository.findAllByIdInAndEventId(List.of(5L, 6L), EVENT_ID)).thenReturn(List.of(first, second));

        EventRequestStatusUpdateResult result =
                requestService.updateRequestStatuses(params(RequestStatus.CONFIRMED, 5L, 6L));

        assertThat(result.getConfirmedRequests()).extracting(ParticipationRequestDto::getId).containsExactly(5L, 6L);
        assertThat(result.getRejectedRequests()).isEmpty();
        verify(requestRepository).saveAll(List.of(first, second));
        verify(requestRepository, never()).countByEventIdAndStatus(anyLong(), any());
        verify(requestRepository, never()).updateStatusByEventId(anyLong(), any(), any());
    }

    @Test
    void updateRequestStatuses_confirmWhenLimitAlreadyReached_throwsConflict() {
        Event event = stubInitiatorEvent(2, true);
        when(requestRepository.findAllByIdInAndEventId(List.of(5L), EVENT_ID))
                .thenReturn(List.of(request(5L, event, requester, RequestStatus.PENDING)));
        when(requestRepository.countByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED)).thenReturn(2L);

        assertThatThrownBy(() -> requestService.updateRequestStatuses(params(RequestStatus.CONFIRMED, 5L)))
                .isInstanceOf(ConflictException.class);
        verify(requestRepository, never()).saveAll(any());
    }

    @Test
    void updateRequestStatuses_confirmBelowLimit_confirmsAllAndKeepsOtherPendingRequests() {
        Event event = stubInitiatorEvent(5, true);
        ParticipationRequest first = request(5L, event, requester, RequestStatus.PENDING);
        ParticipationRequest second = request(6L, event, user(3L), RequestStatus.PENDING);
        when(requestRepository.findAllByIdInAndEventId(List.of(5L, 6L), EVENT_ID)).thenReturn(List.of(first, second));
        when(requestRepository.countByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED)).thenReturn(0L);

        EventRequestStatusUpdateResult result =
                requestService.updateRequestStatuses(params(RequestStatus.CONFIRMED, 5L, 6L));

        assertThat(result.getConfirmedRequests()).hasSize(2);
        assertThat(result.getRejectedRequests()).isEmpty();
        verify(requestRepository, never()).updateStatusByEventId(anyLong(), any(), any());
    }

    @Test
    void updateRequestStatuses_confirmExactlyUpToLimit_rejectsOtherPendingRequestsOfEvent() {
        Event event = stubInitiatorEvent(2, true);
        ParticipationRequest first = request(5L, event, requester, RequestStatus.PENDING);
        ParticipationRequest second = request(6L, event, user(3L), RequestStatus.PENDING);
        when(requestRepository.findAllByIdInAndEventId(List.of(5L, 6L), EVENT_ID)).thenReturn(List.of(first, second));
        when(requestRepository.countByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED)).thenReturn(0L);

        EventRequestStatusUpdateResult result =
                requestService.updateRequestStatuses(params(RequestStatus.CONFIRMED, 5L, 6L));

        assertThat(result.getConfirmedRequests()).hasSize(2);
        assertThat(result.getRejectedRequests()).isEmpty();
        verify(requestRepository).updateStatusByEventId(EVENT_ID, RequestStatus.PENDING, RequestStatus.REJECTED);
    }

    @Test
    void updateRequestStatuses_confirmOverLimit_confirmsInIdOrderAndRejectsTheRest() {
        Event event = stubInitiatorEvent(2, true);
        ParticipationRequest r5 = request(5L, event, requester, RequestStatus.PENDING);
        ParticipationRequest r6 = request(6L, event, user(3L), RequestStatus.PENDING);
        ParticipationRequest r7 = request(7L, event, user(4L), RequestStatus.PENDING);
        // репозиторий может вернуть заявки в любом порядке
        when(requestRepository.findAllByIdInAndEventId(List.of(7L, 5L, 6L), EVENT_ID)).thenReturn(List.of(r7, r5, r6));
        when(requestRepository.countByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED)).thenReturn(1L);

        EventRequestStatusUpdateResult result =
                requestService.updateRequestStatuses(params(RequestStatus.CONFIRMED, 7L, 5L, 6L));

        assertThat(result.getConfirmedRequests()).extracting(ParticipationRequestDto::getId).containsExactly(5L);
        assertThat(result.getRejectedRequests()).extracting(ParticipationRequestDto::getId).containsExactly(6L, 7L);
        assertThat(r5.getStatus()).isEqualTo(RequestStatus.CONFIRMED);
        assertThat(r6.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(r7.getStatus()).isEqualTo(RequestStatus.REJECTED);
        verify(requestRepository).saveAll(List.of(r5, r6, r7));
        verify(requestRepository).updateStatusByEventId(EVENT_ID, RequestStatus.PENDING, RequestStatus.REJECTED);
    }

    // ---------- getConfirmedRequestsCount ----------

    @Test
    void getConfirmedRequestsCount_whenIdsNull_returnsEmptyMapWithoutQuery() {
        assertThat(requestService.getConfirmedRequestsCount(null)).isEmpty();
        verifyNoInteractions(requestRepository);
    }

    @Test
    void getConfirmedRequestsCount_whenIdsEmpty_returnsEmptyMapWithoutQuery() {
        assertThat(requestService.getConfirmedRequestsCount(List.of())).isEmpty();
        verifyNoInteractions(requestRepository);
    }

    @Test
    void getConfirmedRequestsCount_returnsCountPerEvent() {
        List<Long> eventIds = List.of(1L, 2L, 3L);
        when(requestRepository.countByEventIdsAndStatus(eventIds, RequestStatus.CONFIRMED))
                .thenReturn(List.of(count(1L, 2L), count(3L, 5L)));

        Map<Long, Long> result = requestService.getConfirmedRequestsCount(eventIds);

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(1L, 2L, 3L, 5L));
    }

    // ---------- вспомогательные методы ----------

    private void stubUserAndEvent(Event event) {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(requester));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
    }

    private Event stubInitiatorEvent(int participantLimit, boolean requestModeration) {
        Event event = event(EventState.PUBLISHED, participantLimit, requestModeration);
        when(eventRepository.findByIdAndInitiatorId(EVENT_ID, INITIATOR_ID)).thenReturn(Optional.of(event));
        return event;
    }

    private void stubSaveWithId(long id) {
        when(requestRepository.save(any(ParticipationRequest.class))).thenAnswer(invocation -> {
            ParticipationRequest request = invocation.getArgument(0);
            request.setId(id);
            return request;
        });
    }

    private RequestStatusUpdateParams params(RequestStatus status, Long... requestIds) {
        EventRequestStatusUpdateRequest updateRequest = new EventRequestStatusUpdateRequest(List.of(requestIds), status);
        return new RequestStatusUpdateParams(INITIATOR_ID, EVENT_ID, updateRequest);
    }

    private static User user(long id) {
        return User.builder().id(id).name("user" + id).email("user" + id + "@mail.ru").build();
    }

    private Event event(EventState state, int participantLimit, boolean requestModeration) {
        return Event.builder()
                .id(EVENT_ID)
                .initiator(initiator)
                .state(state)
                .participantLimit(participantLimit)
                .requestModeration(requestModeration)
                .build();
    }

    private static ParticipationRequest request(long id, Event event, User requester, RequestStatus status) {
        return ParticipationRequest.builder()
                .id(id)
                .event(event)
                .requester(requester)
                .created(LocalDateTime.of(2026, 10, 1, 12, 0))
                .status(status)
                .build();
    }

    private static EventRequestCount count(long eventId, long requestCount) {
        return new EventRequestCount() {
            @Override
            public Long getEventId() {
                return eventId;
            }

            @Override
            public Long getRequestCount() {
                return requestCount;
            }
        };
    }
}
