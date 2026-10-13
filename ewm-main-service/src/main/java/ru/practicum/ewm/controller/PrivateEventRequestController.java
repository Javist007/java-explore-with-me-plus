//package ru.practicum.ewm.controller;
//
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.*;
//import ru.practicum.ewm.dto.participation.request.EventRequestStatusUpdateRequest;
//import ru.practicum.ewm.dto.participation.request.RequestStatusUpdateParams;
//import ru.practicum.ewm.dto.participation.response.EventRequestStatusUpdateResult;
//import ru.practicum.ewm.dto.participation.response.ParticipationRequestDto;
//import ru.practicum.ewm.service.RequestService;
//
//import java.util.List;
//
///**
// * Закрытый API для работы инициатора события с заявками на участие в нём.
// */
//@RestController
//@RequestMapping("/users/{userId}/events/{eventId}/requests")
//@RequiredArgsConstructor
//public class PrivateEventRequestController {
//
//    private final RequestService requestService;
//
//    /**
//     * Получение заявок на участие в событии текущего пользователя.
//     */
//    @GetMapping
//    public List<ParticipationRequestDto> getEventRequests(@PathVariable Long userId, @PathVariable Long eventId) {
//        return requestService.getEventRequests(userId, eventId);
//    }
//
//    /**
//     * Подтверждение или отклонение заявок на участие в событии текущего пользователя.
//     */
//    @PatchMapping("/cancel")
//    public EventRequestStatusUpdateResult updateRequestStatuses(
//            @PathVariable Long userId,
//            @PathVariable Long eventId,
//            @RequestBody @Valid EventRequestStatusUpdateRequest updateRequest) {
//        RequestStatusUpdateParams params = new RequestStatusUpdateParams(userId, eventId, updateRequest);
//        return requestService.updateRequestStatuses(params);
//    }
//}
