package ru.practicum.mainsrvc.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.mainsrvc.dto.EventFullDto;
import ru.practicum.mainsrvc.dto.EventShortDto;
import ru.practicum.mainsrvc.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/events")
public class PublicEventController {

    private static final Logger log = LoggerFactory.getLogger(PublicEventController.class);
    private final EventService eventService;

    public PublicEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<EventShortDto>> getPublicEvents(
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) String text,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        log.info("Get /events {START: {}, END: {}}", rangeStart, rangeEnd);

        if (text != null && text.isBlank()) {
            text = null;
        }

        if (rangeStart != null && rangeEnd != null && rangeEnd.isBefore(rangeStart)) {
            throw new IllegalArgumentException("rangeEnd не может быть раньше rangeStart");
        }

        if (from < 0) {
            throw new IllegalArgumentException("from не может быть отрицательным");
        }
        if (size <= 0 || size > 100000) {
            throw new IllegalArgumentException("size должен быть от 1 до 100000");
        }

        String clientIp = request.getRemoteAddr();

        List<EventShortDto> result = eventService.getPublicEvents(
                categories, paid, text, rangeStart, rangeEnd, from, size, clientIp);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventFullDto> getEventById(
            @PathVariable Long id,
            HttpServletRequest request) {

        log.info("Get /events/{} {request: {}}", id, request.getRemoteAddr());

        String clientIp = request.getRemoteAddr();
        EventFullDto event = eventService.getEventFullByIdForPublicWithStats(id, clientIp);
        return ResponseEntity.ok(event);
    }
}