package ru.practicum.ewm.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.practicum.ewm.dto.compilation.request.NewCompilationDto;
import ru.practicum.ewm.dto.compilation.request.UpdateCompilationRequest;
import ru.practicum.ewm.dto.compilation.response.CompilationDto;
import ru.practicum.ewm.exception.model.NotFoundException;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.CompilationRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.service.RequestService;
import ru.practicum.stats.client.StatClient;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompilationServiceImplTest {

    @Mock
    private CompilationRepository compilationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RequestService requestService;

    @Mock
    private StatClient statClient;

    @InjectMocks
    private CompilationServiceImpl compilationService;

    private Compilation compilation;
    private NewCompilationDto newCompilationDto;
    private UpdateCompilationRequest updateRequest;
    private Event event;
    private ViewStatsDto viewStatsDto;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        Category category = Category.builder()
                .id(1L)
                .name("Test Category")
                .build();

        User initiator = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .build();

        event = Event.builder()
                .id(1L)
                .title("Test Event")
                .annotation("Test annotation")
                .category(category)
                .initiator(initiator)
                .eventDate(LocalDateTime.of(2025, 12, 31, 15, 0))
                .createdOn(LocalDateTime.now())
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state(EventState.PENDING)
                .build();

        compilation = Compilation.builder()
                .id(1L)
                .title("Test Compilation")
                .pinned(false)
                .events(Set.of(event))
                .build();

        newCompilationDto = NewCompilationDto.builder()
                .title("New Compilation")
                .pinned(true)
                .events(List.of(1L))
                .build();

        updateRequest = UpdateCompilationRequest.builder()
                .title("Updated Compilation")
                .pinned(true)
                .build();

        viewStatsDto = ViewStatsDto.builder()
                .uri("/events/1")
                .hits(100L)
                .build();

        pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
    }

    private void mockStatsServices() {
        when(requestService.getConfirmedRequestsCount(anyList())).thenReturn(Map.of(1L, 5L));
        when(statClient.getStats(any(LocalDateTime.class), any(LocalDateTime.class), anyList(), anyBoolean()))
                .thenReturn(List.of(viewStatsDto));
    }

    @Test
    @DisplayName("Должен создать подборку успешно")
    void save_ShouldCreateCompilationSuccessfully() {
        when(eventRepository.findAllById(List.of(1L))).thenReturn(List.of(event));
        when(compilationRepository.save(any(Compilation.class))).thenReturn(compilation);
        when(compilationRepository.findByIdWithEvents(1L)).thenReturn(Optional.of(compilation));
        mockStatsServices();

        CompilationDto result = compilationService.save(newCompilationDto);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Compilation");
        assertThat(result.getEvents()).hasSize(1);
        assertThat(result.getEvents().get(0).getViews()).isEqualTo(100L);
        assertThat(result.getEvents().get(0).getConfirmedRequests()).isEqualTo(5L);

        verify(eventRepository).findAllById(List.of(1L));
        verify(compilationRepository).save(any(Compilation.class));
        verify(compilationRepository).findByIdWithEvents(1L);
    }

    @Test
    @DisplayName("Должен создать подборку без событий")
    void save_ShouldCreateCompilationWithoutEvents() {
        NewCompilationDto dtoWithoutEvents = NewCompilationDto.builder()
                .title("Empty Compilation")
                .pinned(false)
                .build();

        Compilation emptyCompilation = Compilation.builder()
                .id(2L)
                .title("Empty Compilation")
                .pinned(false)
                .events(Set.of())
                .build();

        when(compilationRepository.save(any(Compilation.class))).thenReturn(emptyCompilation);
        when(compilationRepository.findByIdWithEvents(2L)).thenReturn(Optional.of(emptyCompilation));

        CompilationDto result = compilationService.save(dtoWithoutEvents);

        assertThat(result).isNotNull();
        assertThat(result.getEvents()).isEmpty();
        verify(eventRepository, never()).findAllById(anyList());
        verify(compilationRepository).findByIdWithEvents(2L);
    }

    @Test
    @DisplayName("Должен обновить подборку успешно")
    void update_ShouldUpdateCompilationSuccessfully() {
        when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));
        when(compilationRepository.save(any(Compilation.class))).thenReturn(compilation);
        when(compilationRepository.findByIdWithEvents(1L)).thenReturn(Optional.of(compilation));
        mockStatsServices();

        CompilationDto result = compilationService.update(1L, updateRequest);

        assertThat(result).isNotNull();
        verify(compilationRepository).findById(1L);
        verify(compilationRepository).save(any(Compilation.class));
        verify(compilationRepository).findByIdWithEvents(1L);
    }

    @Test
    @DisplayName("Должен выбросить NotFoundException при обновлении несуществующей подборки")
    void update_ShouldThrowNotFoundException_WhenCompilationNotFound() {
        when(compilationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compilationService.update(999L, updateRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Compilation with id=999 was not found");

        verify(compilationRepository).findById(999L);
        verify(compilationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Должен выбросить NotFoundException при обновлении с несуществующими событиями")
    void update_ShouldThrowNotFoundException_WhenEventsNotFound() {
        when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));
        when(eventRepository.findAllById(List.of(999L))).thenReturn(List.of());

        UpdateCompilationRequest requestWithMissingEvents = UpdateCompilationRequest.builder()
                .events(List.of(999L))
                .build();

        assertThatThrownBy(() -> compilationService.update(1L, requestWithMissingEvents))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Events with ids [999] were not found");

        verify(eventRepository).findAllById(List.of(999L));
        verify(compilationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Должен удалить подборку успешно")
    void delete_ShouldDeleteCompilationSuccessfully() {
        when(compilationRepository.existsById(1L)).thenReturn(true);

        compilationService.delete(1L);

        verify(compilationRepository).existsById(1L);
        verify(compilationRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Должен выбросить NotFoundException при удалении несуществующей подборки")
    void delete_ShouldThrowNotFoundException_WhenCompilationNotFound() {
        when(compilationRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> compilationService.delete(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Compilation with id=999 was not found");

        verify(compilationRepository).existsById(999L);
        verify(compilationRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Должен получить список подборок с фильтрацией по pinned")
    void getAll_ShouldReturnFilteredCompilations_WhenPinnedIsNotNull() {
        when(compilationRepository.findAllByPinnedWithEvents(eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(compilation)));
        mockStatsServices();

        List<CompilationDto> result = compilationService.getAll(true, pageable);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test Compilation");
        assertThat(result.get(0).getEvents()).hasSize(1);

        verify(compilationRepository).findAllByPinnedWithEvents(eq(true), any(Pageable.class));
    }

    @Test
    @DisplayName("Должен получить все подборки когда pinned равен null")
    void getAll_ShouldReturnAllCompilations_WhenPinnedIsNull() {
        when(compilationRepository.findAllWithEvents(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(compilation)));
        mockStatsServices();

        List<CompilationDto> result = compilationService.getAll(null, pageable);

        assertThat(result).hasSize(1);
        verify(compilationRepository).findAllWithEvents(any(Pageable.class));
        verify(compilationRepository, never()).findAllByPinnedWithEvents(any(), any());
    }

    @Test
    @DisplayName("Должен получить подборку по ID")
    void getById_ShouldReturnCompilation_WhenExists() {
        when(compilationRepository.findByIdWithEvents(1L)).thenReturn(Optional.of(compilation));
        mockStatsServices();

        CompilationDto result = compilationService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Compilation");
        assertThat(result.getEvents()).hasSize(1);
        assertThat(result.getEvents().get(0).getViews()).isEqualTo(100L);
        assertThat(result.getEvents().get(0).getConfirmedRequests()).isEqualTo(5L);

        verify(compilationRepository).findByIdWithEvents(1L);
    }

    @Test
    @DisplayName("Должен выбросить NotFoundException при получении несуществующей подборки")
    void getById_ShouldThrowNotFoundException_WhenCompilationNotFound() {
        when(compilationRepository.findByIdWithEvents(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compilationService.getById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Compilation with id=999 was not found");

        verify(compilationRepository).findByIdWithEvents(999L);
    }

    @Test
    @DisplayName("Должен создать подборку с пустым списком событий")
    void save_ShouldCreateCompilationWithEmptyEventsList() {
        NewCompilationDto dtoWithEmptyEvents = NewCompilationDto.builder()
                .title("Empty List Compilation")
                .pinned(false)
                .events(List.of())
                .build();

        Compilation emptyCompilation = Compilation.builder()
                .id(3L)
                .title("Empty List Compilation")
                .pinned(false)
                .events(Set.of())
                .build();

        when(compilationRepository.save(any(Compilation.class))).thenReturn(emptyCompilation);
        when(compilationRepository.findByIdWithEvents(3L)).thenReturn(Optional.of(emptyCompilation));

        CompilationDto result = compilationService.save(dtoWithEmptyEvents);

        assertThat(result).isNotNull();
        assertThat(result.getEvents()).isEmpty();
        verify(eventRepository, never()).findAllById(anyList());
        verify(compilationRepository).findByIdWithEvents(3L);
    }

    @Test
    @DisplayName("Должен выбросить NotFoundException при создании с несуществующими событиями")
    void save_ShouldThrowNotFoundException_WhenEventsNotFound() {
        when(eventRepository.findAllById(List.of(999L))).thenReturn(List.of());

        NewCompilationDto dtoWithMissingEvents = NewCompilationDto.builder()
                .title("Compilation with missing events")
                .events(List.of(999L))
                .build();

        assertThatThrownBy(() -> compilationService.save(dtoWithMissingEvents))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Events with ids [999] were not found");

        verify(eventRepository).findAllById(List.of(999L));
        verify(compilationRepository, never()).save(any());
    }
}