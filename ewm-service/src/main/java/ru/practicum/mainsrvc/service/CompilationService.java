package ru.practicum.mainsrvc.service;

import jakarta.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.mainsrvc.dto.CompilationDto;
import ru.practicum.mainsrvc.dto.EventShortDto;
import ru.practicum.mainsrvc.dto.NewCompilationDto;
import ru.practicum.mainsrvc.dto.UpdateCompilationDto;
import ru.practicum.mainsrvc.entity.Compilation;
import ru.practicum.mainsrvc.entity.Event;
import ru.practicum.mainsrvc.exception.ConflictException;
import ru.practicum.mainsrvc.exception.NotFoundException;
import ru.practicum.mainsrvc.repository.CompilationRepository;
import ru.practicum.mainsrvc.repository.EventRepository;
import ru.practicum.stat_clt.client.StatClient;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CompilationService {

    private static final Logger log = LoggerFactory.getLogger(CompilationService.class);

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final StatClient statClient;

    public CompilationService(CompilationRepository compilationRepository,
                              EventRepository eventRepository,
                              StatClient statClient) {
        this.compilationRepository = compilationRepository;
        this.eventRepository = eventRepository;
        this.statClient = statClient;
    }

    public CompilationDto createCompilation(NewCompilationDto dto) {
        log.debug("Creating compilation with title: {}", dto.getTitle());

        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new ValidationException("Заголовок подборки не может быть пустым");
        }
        String title = dto.getTitle();
        if (title.length() < 3 || title.length() > 50) {
            throw new ValidationException("Заголовок должен содержать от 3 до 50 символов");
        }

        if (compilationRepository.existsByTitle(title)) {
            throw new ConflictException("Подборка '" + title + "' уже существует");
        }

        Compilation compilation = new Compilation();
        compilation.setTitle(title);
        compilation.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        compilation.setPinned(dto.isPinned());

        List<Event> events = new ArrayList<>();
        if (dto.getEvents() != null && !dto.getEvents().isEmpty()) {
            events = eventRepository.findAllById(dto.getEvents());
            if (events.size() != dto.getEvents().size()) {
                Set<Long> foundIds = events.stream().map(Event::getId).collect(Collectors.toSet());
                List<Long> notFound = dto.getEvents().stream()
                        .filter(id -> !foundIds.contains(id))
                        .collect(Collectors.toList());
                throw new NotFoundException("События не найдены: " + notFound);
            }
            compilation.setEvents(events);
        }

        compilation = compilationRepository.save(compilation);
        log.info("Created compilation: id={}, title={}, eventsCount={}",
                compilation.getId(), compilation.getTitle(), compilation.getEvents().size());

        Map<String, Long> hitsMap = getStatsForCompilation(compilation);

        return toCompilationDto(compilation, hitsMap);
    }

    @Transactional(readOnly = true)
    public List<CompilationDto> getPublicCompilations(Boolean pinned, int from, int size) {
        log.debug("Getting public compilations: pinned={}, from={}, size={}", pinned, from, size);

        validatePagination(from, size);

        Sort sort = pinned != null
                ? Sort.by("pinned").descending().and(Sort.by("id").ascending())
                : Sort.by("id").ascending();

        Pageable pageable = PageRequest.of(from / size, size, sort);
        Page<Compilation> compsPage = compilationRepository.findAllOrByPinned(pinned, pageable);
        List<Compilation> compilations = compsPage.getContent();

        Map<Long, Map<String, Long>> statsMap = collectStatsForCompilations(compilations);

        List<CompilationDto> result = new ArrayList<>();
        for (Compilation c : compilations) {
            Map<String, Long> hitsMap = statsMap.getOrDefault(c.getId(), Collections.emptyMap());
            result.add(toCompilationDto(c, hitsMap));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public CompilationDto getCompilationById(Long id) {
        log.debug("Getting compilation by id: {}", id);

        Compilation compilation = compilationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Подборка не найдена: " + id));

        Map<String, Long> hitsMap = getStatsForCompilation(compilation);

        return toCompilationDto(compilation, hitsMap);
    }

    public CompilationDto updateCompilation(Long compId, UpdateCompilationDto dto) {
        log.debug("Updating compilation: id={}", compId);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка не найдена: " + compId));

        if (dto.getTitle() != null) {
            String newTitle = dto.getTitle();
            if (newTitle.length() < 3 || newTitle.length() > 50) {
                throw new ValidationException("Заголовок должен содержать от 3 до 50 символов");
            }
            if (!newTitle.equals(compilation.getTitle()) && compilationRepository.existsByTitle(newTitle)) {
                throw new ConflictException("Подборка с таким заголовком уже существует");
            }
            compilation.setTitle(newTitle);
        }

        if (dto.getDescription() != null) {
            compilation.setDescription(dto.getDescription().trim());
        }

        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
        }

        if (dto.getEvents() != null) {
            List<Event> events;
            if (dto.getEvents().isEmpty()) {
                events = Collections.emptyList();
            } else {
                events = eventRepository.findAllById(dto.getEvents());
                if (events.size() != dto.getEvents().size()) {
                    Set<Long> foundIds = events.stream().map(Event::getId).collect(Collectors.toSet());
                    List<Long> notFound = dto.getEvents().stream()
                            .filter(id -> !foundIds.contains(id))
                            .collect(Collectors.toList());
                    throw new NotFoundException("События не найдены: " + notFound);
                }
            }
            compilation.setEvents(events);
        }

        compilation = compilationRepository.save(compilation);
        log.info("Updated compilation: id={}, title={}, eventsCount={}",
                compilation.getId(), compilation.getTitle(), compilation.getEvents().size());

        Map<String, Long> hitsMap = getStatsForCompilation(compilation);

        return toCompilationDto(compilation, hitsMap);
    }

    public void deleteCompilation(Long compId) {
        log.debug("Deleting compilation: id={}", compId);

        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Подборка не найдена: " + compId);
        }

        compilationRepository.deleteById(compId);
        log.info("Deleted compilation: id={}", compId);
    }

    private void validatePagination(int from, int size) {
        if (from < 0) {
            throw new IllegalArgumentException("Параметр 'from' не может быть отрицательным");
        }
        if (size <= 0 || size > 1000) {
            throw new IllegalArgumentException("Параметр 'size' должен быть больше 0 и не более 1000");
        }
    }

    private Map<String, Long> getStatsForUris(List<String> uris) {
        if (uris == null || uris.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            LocalDateTime start = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
            LocalDateTime end = LocalDateTime.now();
            List<ViewStatsDto> stats = statClient.getStats(start, end, uris, false);

            Map<String, Long> result = new HashMap<>();
            for (ViewStatsDto stat : stats) {
                result.put(stat.getUri(), stat.getHits());
            }
            return result;
        } catch (Exception e) {
            log.warn("Ошибка получения статистики: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<Long, Map<String, Long>> collectStatsForCompilations(List<Compilation> compilations) {
        List<String> allUris = new ArrayList<>();
        for (Compilation c : compilations) {
            if (c.getEvents() != null) {
                for (Event e : c.getEvents()) {
                    if (e != null) {
                        allUris.add("/events/" + e.getId());
                    }
                }
            }
        }

        if (allUris.isEmpty()) {
            Map<Long, Map<String, Long>> emptyResult = new HashMap<>();
            for (Compilation c : compilations) {
                emptyResult.put(c.getId(), Collections.emptyMap());
            }
            return emptyResult;
        }

        Map<String, Long> allStats = getStatsForUris(allUris);

        Map<Long, Map<String, Long>> result = new HashMap<>();
        for (Compilation c : compilations) {
            Map<String, Long> compilationStats = new HashMap<>();
            if (c.getEvents() != null) {
                for (Event e : c.getEvents()) {
                    if (e != null) {
                        String uri = "/events/" + e.getId();
                        compilationStats.put(uri, allStats.getOrDefault(uri, 0L));
                    }
                }
            }
            result.put(c.getId(), compilationStats);
        }

        return result;
    }

    private Map<String, Long> getStatsForCompilation(Compilation compilation) {
        if (compilation.getEvents() == null || compilation.getEvents().isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> uris = new ArrayList<>();
        for (Event e : compilation.getEvents()) {
            if (e != null) {
                uris.add("/events/" + e.getId());
            }
        }

        return getStatsForUris(uris);
    }

    private EventShortDto toEventShortDto(Event e, Map<String, Long> hitsMap) {
        EventShortDto dto = new EventShortDto();
        dto.setId(e.getId());
        dto.setTitle(e.getTitle());
        dto.setPinned(e.isPinned());
        dto.setPaid(e.isPaid());
        dto.setEventDate(e.getEventDate());

        String uri = "/events/" + e.getId();
        dto.setViews(hitsMap != null ? hitsMap.getOrDefault(uri, 0L) : 0L);

        return dto;
    }

    private CompilationDto toCompilationDto(Compilation c, Map<String, Long> hitsMap) {
        CompilationDto dto = new CompilationDto();
        dto.setId(c.getId());
        dto.setPinned(c.getPinned());
        dto.setTitle(c.getTitle());
        dto.setDescription(c.getDescription());

        List<EventShortDto> eventDtos = new ArrayList<>();
        if (c.getEvents() != null) {
            for (Event e : c.getEvents()) {
                if (e != null) {
                    eventDtos.add(toEventShortDto(e, hitsMap));
                }
            }
        }
        dto.setEvents(eventDtos);

        return dto;
    }
}