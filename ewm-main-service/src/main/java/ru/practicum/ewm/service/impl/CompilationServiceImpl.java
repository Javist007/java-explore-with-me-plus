package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.compilation.request.NewCompilationDto;
import ru.practicum.ewm.dto.compilation.request.UpdateCompilationRequest;
import ru.practicum.ewm.dto.compilation.response.CompilationDto;
import ru.practicum.ewm.exception.model.NotFoundException;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.repository.CompilationRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.service.CompilationService;
import ru.practicum.ewm.service.mapper.CompilationMapper;

import java.util.List;

/**
 * Реализация сервиса для работы с подборками событий.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CompilationDto save(NewCompilationDto dto) {
        List<Event> events = List.of();
        if (dto.getEvents() != null && !dto.getEvents().isEmpty()) {
            events = eventRepository.findAllById(dto.getEvents());
        }

        Compilation compilation = CompilationMapper.toEntity(dto, events);
        Compilation saved = compilationRepository.save(compilation);

        return CompilationMapper.toDto(saved);
    }

    @Override
    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationRequest request) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Compilation with id=%d was not found", compId)));

        List<Event> newEvents = null;
        if (request.getEvents() != null) {
            newEvents = eventRepository.findAllById(request.getEvents());

            if (newEvents.size() != request.getEvents().size()) {
                throw new NotFoundException("Some events were not found");
            }
        }

        CompilationMapper.updateCompilation(request, compilation, newEvents);
        Compilation updated = compilationRepository.save(compilation);
        return CompilationMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void delete(Long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException(
                    String.format("Compilation with id=%d was not found", compId));
        }
        compilationRepository.deleteById(compId);
    }

    @Override
    public List<CompilationDto> getAll(Boolean pinned, Integer from, Integer size) {
        Pageable pageable = createPageable(from, size);

        List<Compilation> compilations;
        if (pinned != null) {
            compilations = compilationRepository.findAllByPinned(pinned, pageable);
        } else {
            compilations = compilationRepository.findAll(pageable).getContent();
        }

        return CompilationMapper.toDtoList(compilations);
    }

    @Override
    public CompilationDto getById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Compilation with id=%d was not found", compId)));

        return CompilationMapper.toDto(compilation);
    }

    /**
     * Создаёт Pageable из параметров from и size.
     * from — количество элементов, которые нужно пропустить
     * size — размер страницы
     */
    private Pageable createPageable(Integer from, Integer size) {
        int fromValue = from != null ? from : 0;
        int sizeValue = size != null ? size : 10;
        return PageRequest.of(fromValue / sizeValue, sizeValue);
    }
}