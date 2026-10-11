package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.event.request.NewEventRequest;
import ru.practicum.ewm.dto.event.response.EventFullDto;
import ru.practicum.ewm.exception.model.NotFoundException;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.service.EventService;
import ru.practicum.ewm.service.mapper.EventMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EventServicelmpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void createEvent(NewEventRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User не найден"));

        Category category = categoryRepository.findById(request.getCategory())
                .orElseThrow(() -> new NotFoundException("Category не найдена"));

        LocalDateTime eventDate = LocalDateTime.parse(request.getEventDate(),
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new IllegalArgumentException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }

        eventRepository.save(EventMapper.toEntity(request, user, category));
    }

    @Override
    public List<EventFullDto> getEvents(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User не найден"));

        return eventRepository.findByInitiatorId(userId).stream()
                .map(EventMapper::toDto)
                .toList();
    }
}
