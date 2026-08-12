package ru.practicum.stats.service;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.StatDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.stats.model.mapper.StatMapper;
import ru.practicum.stats.repository.StatRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@AllArgsConstructor
public class StatServiceImpl implements StatService {
    private StatRepository repository;
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}" +
                    "(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)$"
    );
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public void createHit(StatDto statDto) {

        log.info("Post /hit {}", statDto);

        if (statDto.getUri().isBlank() || statDto.getApp().isBlank()) {
            throw new IllegalArgumentException("Введены неверные данные");
        }

        if (!IPV4_PATTERN.matcher(statDto.getIp()).matches()) {
            throw new IllegalArgumentException("Введен неверный IP адрес");
        }

        if (statDto.getTimestamp() == null) {
            throw new IllegalArgumentException("timestamp не может быть пустым");
        }

        repository.save(StatMapper.toStat(statDto));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViewStatsDto> getStats(List<String> uris, String start, String end, boolean unique) {

        log.info("Get /stats uris={}, start={}, end={}, unique={}", uris, start, end, unique);

        List<String> filterUris = (uris == null || uris.isEmpty()) ? null : uris;

        if (start.isBlank()) {
            throw new IllegalArgumentException("Параметр 'start' обязателен");
        }
        if (end.isBlank()) {
            throw new IllegalArgumentException("Параметр 'end' обязателен");
        }

        LocalDateTime startDate = LocalDateTime.parse(start, FORMATTER);
        LocalDateTime endDate = LocalDateTime.parse(end, FORMATTER);

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Параметр 'start' не может быть позже 'end'");
        }

        List<ViewStatsDto> result;
        if (unique) {
            result = repository.findUniqueStats(filterUris, startDate, endDate);
        } else {
            result = repository.findAllStats(filterUris, startDate, endDate);
        }

        return result;
    }


}