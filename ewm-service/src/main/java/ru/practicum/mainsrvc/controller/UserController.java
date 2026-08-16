package ru.practicum.mainsrvc.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.mainsrvc.dto.*;
import ru.practicum.mainsrvc.entity.Event;
import ru.practicum.mainsrvc.entity.EventAction;
import ru.practicum.mainsrvc.exception.ForbiddenException;
import ru.practicum.mainsrvc.service.EventService;
import ru.practicum.mainsrvc.service.ParticipationRequestService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {

    private final ParticipationRequestService participationRequestService;
    private final EventService eventService;

    public UserController(
            ParticipationRequestService participationRequestService,
            EventService eventService) {
        this.participationRequestService = participationRequestService;
        this.eventService = eventService;
    }

    @GetMapping("/{userId}/requests")
    public ResponseEntity<List<ParticipationRequestDto>> getUserRequests(
            @PathVariable Long userId) {

        List<ParticipationRequestDto> result = participationRequestService.getUserRequestsAsList(userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{userId}/events/{eventId}/requests")
    public ResponseEntity<List<ParticipationRequestDto>> getRequestsForUserAndEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId) {

        Event event = eventService.getEventById(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException("Пользователь не является инициатором события");
        }

        List<ParticipationRequestDto> result = participationRequestService.getEventRequestsAsList(eventId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{userId}/requests")
    public ResponseEntity<ParticipationRequestDto> createRequest(
            @PathVariable Long userId,
            @RequestParam Long eventId) {

        ParticipationRequestDto result = participationRequestService.createRequest(userId, eventId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PatchMapping("/{userId}/requests/{requestId}/cancel")
    public ResponseEntity<ParticipationRequestDto> cancelRequest(
            @PathVariable Long userId,
            @PathVariable Long requestId) {

        ParticipationRequestDto result = participationRequestService.cancelRequest(userId, requestId);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{userId}/events/{eventId}/requests")
    public ResponseEntity<EventRequestStatusUpdateResult> approveOrRejectRequest(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestBody ParticipationRequestStatusDto dto) {

        if (dto == null) {
            throw new IllegalArgumentException("requestIds не может быть null");
        }

        if (dto.getRequestIds() == null || dto.getRequestIds().isEmpty()) {
            throw new IllegalArgumentException("requestIds не может быть пустым");
        }

        if (dto.getStatus() == null) {
            throw new IllegalArgumentException("status не может быть null");
        }

        List<ParticipationRequestDto> result = participationRequestService.processRequestStatus(
                userId, eventId, dto);

        List<ParticipationRequestDto> confirmed = result.stream()
                .filter(r -> "CONFIRMED".equals(r.getStatus()))
                .collect(Collectors.toList());

        List<ParticipationRequestDto> rejected = result.stream()
                .filter(r -> "REJECTED".equals(r.getStatus()))
                .collect(Collectors.toList());

        EventRequestStatusUpdateResult response = new EventRequestStatusUpdateResult();
        response.setConfirmedRequests(confirmed);
        response.setRejectedRequests(rejected);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/events")
    public ResponseEntity<EventFullDto> createEventForUser(
            @PathVariable Long userId,
            @Valid @RequestBody NewEventDto dto) {

        EventFullDto full = eventService.createEvent(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(full);
    }

    @GetMapping("/{userId}/events")
    public ResponseEntity<List<EventShortDto>> getUserEvents(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {

        validatePaginationParams(from, size);

        List<EventShortDto> events = eventService.getUserEvents(userId, from, size);
        return ResponseEntity.ok(events);
    }

    @PatchMapping("/{userId}/events/{eventId}")
    public ResponseEntity<EventFullDto> updateEventUser(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestBody UpdateEventRequestDto dto) {

        if (dto.getStateAction() != null && !dto.getStateAction().isEmpty()) {
            try {
                EventAction action = EventAction.valueOf(dto.getStateAction());
                StateActionDto stateActionDto = new StateActionDto();
                stateActionDto.setStateAction(action);

                EventFullDto result = eventService.updateEventState(userId, eventId, stateActionDto);
                return ResponseEntity.ok(result);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Недопустимое значение stateAction: " + dto.getStateAction());
            }
        }

        EventFullDto result = eventService.updateEvent(eventId, dto, userId);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{userId}/events/{eventId}/state")
    public ResponseEntity<EventFullDto> updateEventState(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestBody StateActionDto stateActionDto) {

        if (stateActionDto == null || stateActionDto.getStateAction() == null) {
            throw new IllegalArgumentException("stateAction не может быть null");
        }

        EventFullDto result = eventService.updateEventState(userId, eventId, stateActionDto);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{userId}/events/{eventId}")
    public ResponseEntity<EventFullDto> getEventFullById(
            @PathVariable Long userId,
            @PathVariable Long eventId) {

        EventFullDto result = eventService.getEventFullByIdForUser(eventId, userId);
        return ResponseEntity.ok(result);
    }

    private void validatePaginationParams(int from, int size) {
        if (from < 0) {
            throw new IllegalArgumentException("Параметр 'from' должен быть >= 0");
        }
        if (size <= 0 || size > 1000) {
            throw new IllegalArgumentException("Параметр 'size' должен быть в диапазоне (0, 1000]");
        }
    }
}