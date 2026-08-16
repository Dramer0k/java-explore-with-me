package ru.practicum.mainsrvc.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.mainsrvc.dto.EventFullDto;
import ru.practicum.mainsrvc.dto.NewEventDto;
import ru.practicum.mainsrvc.dto.UpdateEventRequestDto;
import ru.practicum.mainsrvc.service.EventService;

@RestController
@RequestMapping("/private/events")
public class PrivateEventController {

    private static final Logger log = LoggerFactory.getLogger(PrivateEventController.class);
    private final EventService eventService;

    public PrivateEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<EventFullDto> createEvent(
            @Valid @RequestBody NewEventDto dto,
            @RequestAttribute(name = "initiatorId") Long initiatorId) {
        log.info("Post /private/events {initiatorId: {}}", initiatorId);
        return ResponseEntity.status(201).body(eventService.createEvent(dto, initiatorId));
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventFullDto> updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventRequestDto dto,
            @RequestAttribute(name = "initiatorId") Long initiatorId) {
        log.info("Patch /private/events/{} {eventId: {}, initiatorId: {}, dto: {}}", eventId, eventId, initiatorId, dto);
        return ResponseEntity.ok(eventService.updateEvent(eventId, dto, initiatorId));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventFullDto> getEventFullById(
            @PathVariable Long eventId,
            @RequestAttribute(name = "userId") Long userId) {
        log.info("Get /private/events/{} {userId: {}}", eventId, userId);
        return ResponseEntity.ok(eventService.getEventFullByIdForUser(eventId, userId));
    }
}