package ru.practicum.mainsrvc.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.mainsrvc.dto.EventFullDto;
import ru.practicum.mainsrvc.dto.EventShortDto;
import ru.practicum.mainsrvc.dto.PublicEventSearchRequest;
import ru.practicum.mainsrvc.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/events")
public class PublicEventController {
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

        PublicEventSearchRequest searchRequest = new PublicEventSearchRequest();
        searchRequest.setPaid(paid);
        searchRequest.setSize(size);
        searchRequest.setFrom(from);
        searchRequest.setText(text);
        searchRequest.setCategories(categories);
        searchRequest.setRangeEnd(rangeEnd);
        searchRequest.setRangeStart(rangeStart);

        log.info("Request: {}", searchRequest);

        List<EventShortDto> result = eventService.getPublicEvents(searchRequest, clientIp);

        log.info("RESULT: {}", result);

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