package ru.practicum.stats.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.StatDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.stats.service.StatService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping
@AllArgsConstructor
public class StatController {
    private final StatService service;



    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void createHit(@Valid @RequestBody StatDto statDto) {
        log.debug("POST /hit: {app={}, uri={}, ip={}}", statDto.getApp(), statDto.getUri(), statDto.getIp());
        service.createHit(statDto);
    }

    @GetMapping("/stats")
    public List<ViewStatsDto> getStats(
            @RequestParam(required = false) List<String> uris,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "false") boolean unique) {

        return service.getStats(uris, start, end, unique);
    }
}