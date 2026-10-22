package ru.practicum.ewm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.participation.response.ParticipationRequestDto;
import ru.practicum.ewm.service.RequestService;

import java.util.List;

/**
 * Закрытый API для работы с заявками текущего пользователя на участие в чужих событиях.
 */
@RestController
@RequestMapping("/users/{userId}/requests")
@RequiredArgsConstructor
public class PrivateRequestController {

    private final RequestService requestService;

    /**
     * Получение заявок текущего пользователя на участие в чужих событиях.
     */
    @GetMapping
    public List<ParticipationRequestDto> getUserRequests(@PathVariable Long userId) {
        return requestService.getUserRequests(userId);
    }

    /**
     * Добавление заявки текущего пользователя на участие в событии.
     */
    @PostMapping
    public ResponseEntity<ParticipationRequestDto> addRequest(@PathVariable Long userId,
                                                              @RequestParam Long eventId) {
        return new ResponseEntity<>(requestService.addRequest(userId, eventId), HttpStatus.CREATED);
    }

    /**
     * Отмена своей заявки на участие в событии.
     */
    @PatchMapping("/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable Long userId, @PathVariable Long requestId) {
        return requestService.cancelRequest(userId, requestId);
    }
}
