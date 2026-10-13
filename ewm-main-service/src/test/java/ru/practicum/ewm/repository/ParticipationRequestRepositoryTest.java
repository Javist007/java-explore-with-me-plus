package ru.practicum.ewm.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.model.Location;
import ru.practicum.ewm.model.ParticipationRequest;
import ru.practicum.ewm.model.RequestStatus;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.projection.EventRequestCount;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Проверка собственных запросов репозиториев заявок и событий на схеме из schema.sql.
 */
@DataJpaTest
class ParticipationRequestRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ParticipationRequestRepository requestRepository;

    @Autowired
    private EventRepository eventRepository;

    private User initiator;
    private User alice;
    private User bob;
    private Event concert;
    private Event lecture;
    private Event empty;
    private ParticipationRequest aliceConcert;
    private ParticipationRequest bobConcert;
    private ParticipationRequest aliceLecture;
    private ParticipationRequest bobLecture;

    @BeforeEach
    void setUp() {
        initiator = em.persist(User.builder().name("initiator").email("initiator@mail.ru").build());
        alice = em.persist(User.builder().name("alice").email("alice@mail.ru").build());
        bob = em.persist(User.builder().name("bob").email("bob@mail.ru").build());
        Category category = em.persist(Category.builder().name("Концерты").build());

        concert = em.persist(event("Концерт", category));
        lecture = em.persist(event("Лекция", category));
        empty = em.persist(event("Без заявок", category));

        aliceConcert = em.persist(request(concert, alice, RequestStatus.CONFIRMED));
        bobConcert = em.persist(request(concert, bob, RequestStatus.PENDING));
        aliceLecture = em.persist(request(lecture, alice, RequestStatus.CONFIRMED));
        bobLecture = em.persist(request(lecture, bob, RequestStatus.CONFIRMED));
        em.flush();
        em.clear();
    }

    @Test
    void findAllByRequesterId_returnsOnlyRequestsOfUser() {
        assertThat(requestRepository.findAllByRequesterId(alice.getId()))
                .extracting(ParticipationRequest::getId)
                .containsExactlyInAnyOrder(aliceConcert.getId(), aliceLecture.getId());
    }

    @Test
    void findAllByEventId_returnsOnlyRequestsOfEvent() {
        assertThat(requestRepository.findAllByEventId(concert.getId()))
                .extracting(ParticipationRequest::getId)
                .containsExactlyInAnyOrder(aliceConcert.getId(), bobConcert.getId());
    }

    @Test
    void findAllByIdInAndEventId_skipsRequestsOfOtherEvents() {
        List<ParticipationRequest> found = requestRepository.findAllByIdInAndEventId(
                List.of(aliceConcert.getId(), aliceLecture.getId()), concert.getId());

        assertThat(found).extracting(ParticipationRequest::getId).containsExactly(aliceConcert.getId());
    }

    @Test
    void findByIdAndRequesterId_findsOnlyOwnRequest() {
        assertThat(requestRepository.findByIdAndRequesterId(aliceConcert.getId(), alice.getId())).isPresent();
        assertThat(requestRepository.findByIdAndRequesterId(aliceConcert.getId(), bob.getId())).isEmpty();
    }

    @Test
    void existsByEventIdAndRequesterId_detectsExistingRequest() {
        assertThat(requestRepository.existsByEventIdAndRequesterId(concert.getId(), alice.getId())).isTrue();
        assertThat(requestRepository.existsByEventIdAndRequesterId(concert.getId(), initiator.getId())).isFalse();
    }

    @Test
    void countByEventIdAndStatus_countsOnlyGivenStatus() {
        assertThat(requestRepository.countByEventIdAndStatus(concert.getId(), RequestStatus.CONFIRMED)).isEqualTo(1);
        assertThat(requestRepository.countByEventIdAndStatus(concert.getId(), RequestStatus.PENDING)).isEqualTo(1);
        assertThat(requestRepository.countByEventIdAndStatus(empty.getId(), RequestStatus.CONFIRMED)).isZero();
    }

    @Test
    void countByEventIdsAndStatus_groupsByEventAndSkipsEventsWithoutRequests() {
        Map<Long, Long> counts = requestRepository
                .countByEventIdsAndStatus(List.of(concert.getId(), lecture.getId(), empty.getId()),
                        RequestStatus.CONFIRMED).stream()
                .collect(Collectors.toMap(EventRequestCount::getEventId, EventRequestCount::getRequestCount));

        assertThat(counts).containsExactlyInAnyOrderEntriesOf(Map.of(concert.getId(), 1L, lecture.getId(), 2L));
    }

    @Test
    void updateStatusByEventId_changesOnlyMatchingRequestsOfEvent() {
        em.persist(request(lecture, initiator, RequestStatus.PENDING));
        em.flush();

        int updated = requestRepository.updateStatusByEventId(concert.getId(),
                RequestStatus.PENDING, RequestStatus.REJECTED);

        assertThat(updated).isEqualTo(1);
        assertThat(statusOf(bobConcert)).isEqualTo(RequestStatus.REJECTED);
        assertThat(statusOf(aliceConcert)).isEqualTo(RequestStatus.CONFIRMED);
        assertThat(requestRepository.countByEventIdAndStatus(lecture.getId(), RequestStatus.PENDING)).isEqualTo(1);
    }

    @Test
    void save_duplicateRequestOfSameUserForSameEvent_violatesUniqueConstraint() {
        ParticipationRequest duplicate = request(em.find(Event.class, concert.getId()),
                em.find(User.class, alice.getId()), RequestStatus.PENDING);

        assertThatThrownBy(() -> requestRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void eventRepository_findsEventOnlyForItsInitiator() {
        assertThat(eventRepository.findByIdAndInitiatorId(concert.getId(), initiator.getId())).isPresent();
        assertThat(eventRepository.findByIdAndInitiatorId(concert.getId(), alice.getId())).isEmpty();
        assertThat(eventRepository.existsByIdAndInitiatorId(concert.getId(), initiator.getId())).isTrue();
        assertThat(eventRepository.existsByIdAndInitiatorId(concert.getId(), alice.getId())).isFalse();
    }

    private RequestStatus statusOf(ParticipationRequest request) {
        return requestRepository.findById(request.getId()).orElseThrow().getStatus();
    }

    private Event event(String title, Category category) {
        return Event.builder()
                .title(title)
                .annotation("Аннотация события " + title)
                .description("Описание события " + title)
                .category(category)
                .initiator(initiator)
                .eventDate(LocalDateTime.now().plusDays(5))
                .createdOn(LocalDateTime.now())
                .publishedOn(LocalDateTime.now())
                .location(new Location(55.75f, 37.62f))
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .build();
    }

    private static ParticipationRequest request(Event event, User requester, RequestStatus status) {
        return ParticipationRequest.builder()
                .event(event)
                .requester(requester)
                .created(LocalDateTime.now())
                .status(status)
                .build();
    }
}
