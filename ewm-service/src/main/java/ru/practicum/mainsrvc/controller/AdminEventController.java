package ru.practicum.mainsrvc.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.mainsrvc.dto.EventFullDto;
import ru.practicum.mainsrvc.dto.UpdateEventRequestDto;
import ru.practicum.mainsrvc.entity.EventStatus;
import ru.practicum.mainsrvc.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/events")
public class AdminEventController {

    private final EventService eventService;

    public AdminEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<EventFullDto>> getAdminEvents(
            @RequestParam(required = false) List<EventStatus> states,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<Long> categories) {

        List<EventFullDto> result = eventService.getAdminEventsWithFilters(
                states, rangeStart, rangeEnd, from, size, users, categories);

        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventFullDto> updateEventAdmin(
            @PathVariable Long eventId,
            @RequestBody UpdateEventRequestDto dto) {

        EventFullDto result = eventService.updateEventByAdmin(eventId, dto);
        return ResponseEntity.ok(result);
    }
}