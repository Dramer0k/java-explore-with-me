package ru.practicum.stats.service;

import jakarta.validation.Valid;
import ru.practicum.dto.StatDto;
import ru.practicum.dto.ViewStatsDto;

import java.util.List;

public interface StatService {
    void createHit(@Valid StatDto statDto);

    List<ViewStatsDto> getStats(List<String> uris, String startDate, String endDate, boolean unique);
}