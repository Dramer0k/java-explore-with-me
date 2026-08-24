package ru.practicum.mainsrvc.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.mainsrvc.Mapper.EventDtoMapper;
import ru.practicum.mainsrvc.dto.*;
import ru.practicum.mainsrvc.entity.*;
import ru.practicum.mainsrvc.exception.ConflictException;
import ru.practicum.mainsrvc.exception.ForbiddenException;
import ru.practicum.mainsrvc.exception.NotFoundException;
import ru.practicum.mainsrvc.repository.CategoryRepository;
import ru.practicum.mainsrvc.repository.EventRepository;
import ru.practicum.mainsrvc.repository.RequestRepository;
import ru.practicum.mainsrvc.repository.UserRepository;
import ru.practicum.stat_clt.client.StatClient;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final StatClient statClient;
    private final EventDtoMapper eventDtoMapper;

    public EventServiceImpl(EventRepository eventRepository,
                            RequestRepository requestRepository,
                            CategoryRepository categoryRepository,
                            UserRepository userRepository,
                            StatClient statClient, EventDtoMapper eventDtoMapper) {
        this.eventRepository = eventRepository;
        this.requestRepository = requestRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.statClient = statClient;
        this.eventDtoMapper = eventDtoMapper;
    }

    @Transactional(readOnly = true)
    public List<EventShortDto> getPublicEvents(PublicEventSearchRequest searchRequest,
                                               String clientIp) {

        validatePagination(searchRequest.getFrom(), searchRequest.getSize());

        try {
            statClient.hit("/events", "ewm-service", clientIp);
            log.debug("Отправлен просмотр для /events с IP {}", clientIp);
        } catch (Exception ex) {
            log.warn("Не удалось отправить статистику для /events", ex);
        }

        if (searchRequest.getRangeStart() == null) {
            searchRequest.setRangeStart(LocalDateTime.now());
        }
        if (searchRequest.getRangeEnd() == null) {
            searchRequest.setRangeEnd(LocalDateTime.now().plusYears(100));
        }

        validateRangeDates(searchRequest.getRangeStart(), searchRequest.getRangeEnd());

        Sort sort = Sort.by("eventDate").ascending();
        Pageable pageable = PageRequest.of(searchRequest.getFrom() / searchRequest.getSize(),
                searchRequest.getSize(), sort);

        Page<Event> pageResult = findPublicEvents(searchRequest, pageable);

        List<Event> events = pageResult.getContent();

        Map<String, Long> hitsMap = getHitsMapForEvents(events);
        Map<Long, Long> confirmedMap = getConfirmedCountsForEvents(events);

        List<EventShortDto> result = new ArrayList<>(events.size());
        for (Event e : events) {
            long confirmed = confirmedMap.getOrDefault(e.getId(), 0L);
            result.add(eventDtoMapper.toEventShortDto(e, hitsMap, confirmed));
        }

        return result;
    }

    private Map<Long, Long> getConfirmedCountsForEvents(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        List<Object[]> rows = requestRepository.countConfirmedByEventIds(
                eventIds, RequestStatus.CONFIRMED);

        Map<Long, Long> map = new HashMap<>(rows.size());
        for (Object[] row : rows) {
            Long eventId = (Long) row[0];
            Long count = (Long) row[1];
            map.put(eventId, count);
        }
        return map;
    }



    @Transactional(readOnly = true)
    public EventShortDto getEventShortById(Long eventId, String clientIp) {
        Event event = eventRepository.findByIdWithDetails(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено или не опубликовано"));

        if (event.getState() != EventStatus.PUBLISHED) {
            throw new NotFoundException("Событие ещё не опубликовано");
        }

        try {
            statClient.hit("/events/" + eventId, "ewm-service", clientIp);
        } catch (Exception ex) {
            log.warn("Не удалось отправить статистику для события id={}", eventId, ex);
        }

        Map<String, Long> hitsMap = getHitsMapForEvent(eventId);

        Long confirmedRequests = requestRepository.countConfirmedByEventId(event.getId());

        return eventDtoMapper.toEventShortDto(event, hitsMap, confirmedRequests);
    }

    @Transactional(readOnly = true)
    public EventFullDto getEventFullByIdForPublicWithStats(Long eventId, String clientIp) {
        Event event = eventRepository.findByIdWithDetails(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (event.getState() != EventStatus.PUBLISHED) {
            throw new NotFoundException("Событие ещё не опубликовано");
        }

        String uri = "/events/" + event.getId();

        try {
            statClient.hit(uri, "ewm-service", clientIp);
            log.debug("Отправлен просмотр для события {} с IP {}", eventId, clientIp);
        } catch (Exception ex) {
            log.warn("Не удалось отправить статистику просмотров для события id={}", eventId, ex);
            Map<String, Long> hitsMap = getHitsMapForEvent(eventId);
            return eventDtoMapper.toEventFullDto(event, hitsMap, requestRepository.countConfirmedByEventId(event.getId()));
        }

        Map<String, Long> updatedHitsMap = getHitsMapForEvent(eventId);
        return eventDtoMapper.toEventFullDto(event, updatedHitsMap,
                requestRepository.countConfirmedByEventId(event.getId()));
    }

    public EventFullDto createEvent(NewEventDto dto, Long initiatorId) {
        validateNewEvent(dto);

        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория не найдена"));

        User initiator = userRepository.findById(initiatorId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Event event = new Event();
        event.setTitle(dto.getTitle());
        event.setAnnotation(dto.getAnnotation());
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());
        event.setPaid(dto.getPaid() != null ? dto.getPaid() : false);
        event.setParticipantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0);
        event.setRequestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true);
        event.setPinned(dto.getPinned() != null ? dto.getPinned() : false);
        event.setCategory(category);
        event.setInitiator(initiator);
        event.setState(EventStatus.PENDING);

        if (dto.getLocation() != null) {
            event.setLocationLat(dto.getLocation().getLat());
            event.setLocationLon(dto.getLocation().getLon());
        }

        event = eventRepository.save(event);
        log.info("Событие создано: id={}, title={}, initiatorId={}", event.getId(), event.getTitle(), initiatorId);

        Map<String, Long> hitsMap = getHitsMapForEvent(event.getId());
        return eventDtoMapper.toEventFullDto(event, hitsMap,
                requestRepository.countConfirmedByEventId(event.getId()));
    }

    public EventFullDto updateEvent(Long eventId, UpdateEventRequestDto dto, Long initiatorId) {
        Event event = eventRepository.findByIdAndInitiator(eventId, initiatorId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (event.getState() == EventStatus.PUBLISHED) {
            throw new ConflictException("Нельзя редактировать опубликованное событие");
        }

        if (dto.getStateAction() != null && !dto.getStateAction().isEmpty()) {
            throw new ValidationException("Изменение статуса доступно только через отдельный эндпоинт");
        }

        validateUpdateEvent(dto, event);
        updateEventFields(event, dto);

        event = eventRepository.save(event);
        log.info("Событие id={} обновлено пользователем id={}", eventId, initiatorId);

        Map<String, Long> hitsMap = getHitsMapForEvent(event.getId());
        return eventDtoMapper.toEventFullDto(event, hitsMap,
                requestRepository.countConfirmedByEventId(event.getId()));
    }

    public EventFullDto updateEventState(Long userId, Long eventId, StateActionDto dto) {
        Event event = eventRepository.findByIdWithDetails(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException("Пользователь не является инициатором события");
        }

        EventAction action = dto.getStateAction();

        switch (action) {
            case SEND_TO_REVIEW:
                if (event.getState() == EventStatus.PENDING) {
                    log.debug("Событие id={} уже на модерации", eventId);
                } else if (event.getState() == EventStatus.CANCELED) {
                    event.setState(EventStatus.PENDING);
                    log.info("Событие id={} повторно отправлено на модерацию пользователем id={}", eventId, userId);
                } else if (event.getState() == EventStatus.PUBLISHED) {
                    throw new ConflictException("Нельзя отправить опубликованное событие на модерацию");
                } else {
                    throw new ConflictException(
                            "Нельзя отправить событие на модерацию. Текущий статус: " + event.getState()
                    );
                }
                break;

            case CANCEL_REVIEW:
                if (event.getState() != EventStatus.PENDING) {
                    throw new ConflictException(
                            "Отменить можно только событие в состоянии PENDING. Текущий статус: " + event.getState()
                    );
                }
                event.setState(EventStatus.CANCELED);
                log.info("Событие id={} отменено пользователем id={}", eventId, userId);
                break;

            default:
                throw new IllegalArgumentException("Неизвестное действие: " + action);
        }

        event = eventRepository.save(event);
        Map<String, Long> hitsMap = getHitsMapForEvent(event.getId());
        return eventDtoMapper.toEventFullDto(event, hitsMap,
                requestRepository.countConfirmedByEventId(event.getId()));
    }

    @Transactional(readOnly = true)
    public EventFullDto getEventFullByIdForUser(Long eventId, Long userId) {
        Event event = eventRepository.findByIdWithDetails(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new EntityNotFoundException("Не хватает прав на просмотр страницы");
        }

        Map<String, Long> hitsMap = getHitsMapForEvent(eventId);

        return eventDtoMapper.toEventFullDto(event, hitsMap,
                requestRepository.countConfirmedByEventId(event.getId()));
    }

    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        validatePagination(from, size);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь не найден");
        }

        Pageable pageable = PageRequest.of(from / size, size);
        Page<Event> eventsPage = eventRepository.findAllByInitiatorId(userId, pageable);

        List<Event> events = eventsPage.getContent();
        Map<String, Long> hitsMap = getHitsMapForEvents(events);
        Map<Long, Long> confirmedMap = getConfirmedCountsForEvents(events);

        return events.stream()
                .map(e -> {
                    long confirmed = confirmedMap.getOrDefault(e.getId(), 0L);
                    return eventDtoMapper.toEventShortDto(e, hitsMap, confirmed);
                })
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public List<EventFullDto> getAdminEventsWithFilters(AdminEventSearchRequest request) {
        validatePagination(request.getFrom(), request.getSize());

        if (request.getRangeStart() == null) {
            request.setRangeStart(LocalDateTime.now().minusYears(100));
        }
        if (request.getRangeEnd() == null) {
            request.setRangeEnd(LocalDateTime.now().plusYears(100));
        }

        Pageable pageable = PageRequest.of(request.getFrom() / request.getSize(), request.getSize());

        List<EventStatus> states = (request.getStates() != null && !request.getStates().isEmpty())
                ? request.getStates()
                : null;

        List<Long> users = (request.getUsers() != null && !request.getUsers().isEmpty())
                ? request.getUsers()
                : null;

        List<Long> categories = (request.getCategories() != null && !request.getCategories().isEmpty())
                ? request.getCategories()
                : null;

        Page<Event> page = eventRepository.findAdminAll(
                states,
                request.getRangeStart(),
                request.getRangeEnd(),
                users,
                categories,
                pageable
        );

        List<Event> events = page.getContent();
        log.debug("Найдено событий: {}, всего: {}", events.size(), page.getTotalElements());

        Map<String, Long> statsMap;
        if (!events.isEmpty()) {
            List<String> uris = events.stream()
                    .map(e -> "/events/" + e.getId())
                    .toList();

            List<ViewStatsDto> viewStats = statClient.getStats(
                    request.getRangeStart(),
                    request.getRangeEnd(),
                    uris,
                    false
            );

            statsMap = viewStats.stream()
                    .collect(Collectors.toMap(
                            ViewStatsDto::getUri,
                            ViewStatsDto::getHits,
                            (v1, v2) -> v1
                    ));
        } else {
            statsMap = Collections.emptyMap();
        }

        Map<Long, Long> confirmedMap = getConfirmedCountsForEvents(events);

        return events.stream()
                .map(e -> {
                    long confirmed = confirmedMap.getOrDefault(e.getId(), 0L);
                    return eventDtoMapper.toEventFullDto(e, statsMap, confirmed);
                })
                .collect(Collectors.toList());
    }

    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventRequestDto dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        String stateActionStr = dto.getStateAction();

        if (stateActionStr != null && !stateActionStr.isEmpty()) {
            EventAction action;
            try {
                action = EventAction.valueOf(stateActionStr);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Неизвестное действие: " + stateActionStr);
            }

            switch (action) {
                case PUBLISH_EVENT:
                    if (event.getState() == EventStatus.PUBLISHED) {
                        throw new ConflictException("Событие уже опубликовано");
                    }
                    if (event.getState() != EventStatus.PENDING) {
                        throw new ConflictException(
                                "Нельзя опубликовать событие: текущий статус — " + event.getState() +
                                        ". Публикация разрешена только из состояния PENDING."
                        );
                    }
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime minEventDate = now.plusHours(1);
                    if (event.getEventDate().isBefore(minEventDate)) {
                        throw new IllegalArgumentException(
                                "Дата события должна быть не ранее чем через 1 час от текущего времени"
                        );
                    }
                    break;

                case REJECT_EVENT:
                    if (event.getState() == EventStatus.PUBLISHED) {
                        throw new ConflictException("Нельзя отклонить уже опубликованное событие");
                    }
                    if (event.getState() != EventStatus.PENDING) {
                        throw new ConflictException(
                                "Нельзя отклонить событие: текущий статус — " + event.getState() +
                                        ". Отклонение разрешено только из состояния PENDING."
                        );
                    }
                    break;

                default:
                    throw new IllegalArgumentException("Неизвестное действие: " + action);
            }
        }

        if (dto.getTitle() != null) {
            String title = dto.getTitle();
            if (title.length() < 3 || title.length() > 120) {
                throw new ValidationException("Заголовок должен содержать от 3 до 120 символов");
            }
            event.setTitle(title);
        }

        if (dto.getAnnotation() != null) {
            String annotation = dto.getAnnotation();
            if (annotation.length() < 20 || annotation.length() > 2000) {
                throw new ValidationException("Аннотация должна содержать от 20 до 2000 символов");
            }
            event.setAnnotation(annotation);
        }

        if (dto.getDescription() != null) {
            String description = dto.getDescription();
            if (description.length() < 20 || description.length() > 7000) {
                throw new ValidationException("Описание должно содержать от 20 до 7000 символов");
            }
            event.setDescription(description);
        }

        if (dto.getEventDate() != null) {
            LocalDateTime newEventDate = dto.getEventDate();
            LocalDateTime now = LocalDateTime.now();
            if (newEventDate.isBefore(now)) {
                throw new ValidationException("Дата события не может быть в прошлом");
            }
            event.setEventDate(newEventDate);
        }

        if (dto.getParticipantLimit() != null) {
            if (dto.getParticipantLimit() < 0) {
                throw new ValidationException("participantLimit не может быть отрицательным");
            }
            event.setParticipantLimit(dto.getParticipantLimit());
        }

        if (dto.getPinned() != null) {
            event.setPinned(dto.getPinned());
        }

        if (dto.getPaid() != null) {
            event.setPaid(dto.getPaid());
        }

        if (dto.getRequestModeration() != null) {
            event.setRequestModeration(dto.getRequestModeration());
        }

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Категория не найдена"));
            event.setCategory(category);
        }

        if (dto.getLocationLat() != null) {
            event.setLocationLat(dto.getLocationLat());
        }

        if (dto.getLocationLon() != null) {
            event.setLocationLon(dto.getLocationLon());
        }

        if (stateActionStr != null && !stateActionStr.isEmpty()) {
            EventAction action = EventAction.valueOf(stateActionStr);
            switch (action) {
                case PUBLISH_EVENT:
                    event.setState(EventStatus.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    log.info("Событие id={} успешно опубликовано администратором", eventId);
                    break;
                case REJECT_EVENT:
                    event.setState(EventStatus.CANCELED);
                    log.info("Событие id={} успешно отклонено администратором", eventId);
                    break;
                default:
                    throw new IllegalArgumentException("Неизвестное действие: " + action);
            }
        }

        event = eventRepository.save(event);

        Map<String, Long> hitsMap = getHitsMapForEvent(event.getId());
        return eventDtoMapper.toEventFullDto(event, hitsMap,
                requestRepository.countConfirmedByEventId(event.getId()));
    }

    public EventFullDto publishEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (event.getState() != EventStatus.PENDING) {
            throw new ConflictException(
                    "Нельзя опубликовать событие: текущий статус — " + event.getState() +
                            ". Публикация разрешена только из состояния PENDING."
            );
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minEventDate = now.plusHours(1);
        if (event.getEventDate().isBefore(minEventDate)) {
            throw new IllegalArgumentException(
                    "Дата события должна быть не ранее чем через 1 час от текущего времени"
            );
        }

        event.setState(EventStatus.PUBLISHED);
        event.setPublishedOn(now);
        event = eventRepository.save(event);

        log.info("Событие id={} успешно опубликовано", eventId);

        Map<String, Long> hitsMap = getHitsMapForEvent(event.getId());
        return eventDtoMapper.toEventFullDto(event, hitsMap,
                requestRepository.countConfirmedByEventId(event.getId()));
    }

    @Transactional(readOnly = true)
    public List<EventFullDto> getAdminEventsList(int from, int size) {
        validatePagination(from, size);

        Pageable pageable = PageRequest.of(from / size, size);
        Page<Event> pageResult = eventRepository.findAll(pageable);

        return pageResult.getContent().stream()
                .map(e -> eventDtoMapper.toEventFullDto(e, Collections.emptyMap(),
                        requestRepository.countConfirmedByEventId(e.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено: " + eventId));
    }

    private Page<Event> findPublicEvents(
            PublicEventSearchRequest searchRequest,
            Pageable pageable) {

        boolean hasCategories = searchRequest.getCategories() != null && !searchRequest.getCategories().isEmpty();
        boolean hasPaid = searchRequest.getPaid() != null;
        boolean hasText = searchRequest.getText() != null && !searchRequest.getText().isBlank();

        if (hasCategories) {
            if (hasPaid && hasText) {
                return eventRepository.findPublicWithCategoriesAll(searchRequest.getCategories(), searchRequest.getPaid(), searchRequest.getText(), searchRequest.getRangeStart(), searchRequest.getRangeEnd(), pageable);
            } else if (hasPaid) {
                return eventRepository.findPublicWithCategoriesPaidOnly(searchRequest.getCategories(), searchRequest.getPaid(), searchRequest.getRangeStart(), searchRequest.getRangeEnd(), pageable);
            } else if (hasText) {
                return eventRepository.findPublicWithCategoriesTextOnly(searchRequest.getCategories(), searchRequest.getText(), searchRequest.getRangeStart(), searchRequest.getRangeEnd(), pageable);
            } else {
                return eventRepository.findPublicWithCategoriesBasic(searchRequest.getCategories(), searchRequest.getRangeStart(), searchRequest.getRangeEnd(), pageable);
            }
        } else {
            if (hasPaid && hasText) {
                return eventRepository.findPublicWithoutCategoriesAll(searchRequest.getPaid(), searchRequest.getText(), searchRequest.getRangeStart(), searchRequest.getRangeEnd(), pageable);
            } else if (hasPaid) {
                return eventRepository.findPublicWithoutCategoriesPaidOnly(searchRequest.getPaid(), searchRequest.getRangeStart(), searchRequest.getRangeEnd(), pageable);
            } else if (hasText) {
                return eventRepository.findPublicWithoutCategoriesTextOnly(searchRequest.getText(), searchRequest.getRangeStart(), searchRequest.getRangeEnd(), pageable);
            } else {
                return eventRepository.findPublicWithoutCategoriesBasic(searchRequest.getRangeStart(), searchRequest.getRangeEnd(), pageable);
            }
        }
    }

    private Page<Event> findAdminEvents(
            List<String> states,
            AdminEventSearchRequest searchRequest,
            Pageable pageable) {

        boolean hasUsers = searchRequest.getUsers() != null && !searchRequest.getUsers().isEmpty();
        boolean hasCategories = searchRequest.getCategories() != null && !searchRequest.getCategories().isEmpty();

        if (hasUsers && hasCategories) {
            return eventRepository.findAllAdmin(states, searchRequest.getRangeStart(), searchRequest.getRangeEnd(), searchRequest.getUsers(), searchRequest.getCategories(), pageable);
        } else if (hasUsers) {
            return eventRepository.findAdminWithUsers(states, searchRequest.getRangeStart(), searchRequest.getRangeEnd(), searchRequest.getUsers(), pageable);
        } else if (hasCategories) {
            return eventRepository.findAdminWithCategories(states, searchRequest.getRangeStart(), searchRequest.getRangeEnd(), searchRequest.getCategories(), pageable);
        } else {
            return eventRepository.findAdminBasic(states, searchRequest.getRangeStart(), searchRequest.getRangeEnd(), pageable);
        }
    }

    private Map<String, Long> getHitsMapForEvents(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> uris = events.stream()
                .map(e -> "/events/" + e.getId())
                .collect(Collectors.toList());

        return getHitsMap(uris);
    }

    private Map<String, Long> getHitsMapForEvent(Long eventId) {
        String uri = "/events/" + eventId;
        return getHitsMap(Collections.singletonList(uri));
    }

    private Map<String, Long> getHitsMap(List<String> uris) {
        try {
            LocalDateTime start = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
            LocalDateTime end = LocalDateTime.now();

            List<ViewStatsDto> stats = statClient.getStats(start, end, uris, true);
            return stats.stream()
                    .collect(Collectors.toMap(
                            ViewStatsDto::getUri,
                            ViewStatsDto::getHits,
                            (v1, v2) -> v1
                    ));
        } catch (Exception e) {
            log.warn("Ошибка получения статистики: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private void validatePagination(int from, int size) {
        if (from < 0) {
            throw new IllegalArgumentException("from не может быть отрицательным");
        }
        if (size <= 0 || size > 1000) {
            throw new IllegalArgumentException("size должен быть от 1 до 1000");
        }
    }

    private void validateRangeDates(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeStart.isAfter(rangeEnd)) {
            throw new IllegalArgumentException("rangeEnd не может быть раньше rangeStart");
        }
    }

    private void validateNewEvent(NewEventDto dto) {
        LocalDateTime eventDate = dto.getEventDate();
        if (eventDate == null) {
            throw new IllegalArgumentException("Дата события обязательна");
        }

        LocalDateTime minDate = LocalDateTime.now().plusHours(2);
        if (eventDate.isBefore(minDate)) {
            throw new IllegalArgumentException(
                    "Дата события должна быть не ранее чем через 2 часа от текущего времени"
            );
        }

        String description = dto.getDescription();
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Описание события обязательно");
        }
        if (description.length() < 20) {
            throw new IllegalArgumentException("Описание события должно содержать не менее 20 символов");
        }
        if (description.length() > 7000) {
            throw new IllegalArgumentException("Описание события не должно превышать 7000 символов");
        }

        String annotation = dto.getAnnotation();
        if (annotation == null || annotation.trim().isEmpty()) {
            throw new IllegalArgumentException("Аннотация события обязательна");
        }
        if (annotation.length() < 20) {
            throw new IllegalArgumentException("Аннотация события должна содержать не менее 20 символов");
        }
        if (annotation.length() > 2000) {
            throw new IllegalArgumentException("Аннотация события не должна превышать 2000 символов");
        }

        String title = dto.getTitle();
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Заголовок события обязателен");
        }
        if (title.length() < 3) {
            throw new IllegalArgumentException("Заголовок события должен содержать не менее 3 символов");
        }
        if (title.length() > 120) {
            throw new IllegalArgumentException("Заголовок события не должен превышать 120 символов");
        }

        if (dto.getParticipantLimit() != null && dto.getParticipantLimit() < 0) {
            throw new IllegalArgumentException("participantLimit не может быть отрицательным");
        }
    }

    private void validateUpdateEvent(UpdateEventRequestDto dto, Event event) {
        String title = dto.getTitle();
        if (title != null) {
            if (title.trim().isEmpty()) {
                throw new ValidationException("Заголовок не может быть пустым");
            }
            if (title.length() < 3 || title.length() > 120) {
                throw new ValidationException("Заголовок должен содержать от 3 до 120 символов");
            }
        }

        String description = dto.getDescription();
        if (description != null) {
            if (description.trim().isEmpty()) {
                throw new ValidationException("Описание не может быть пустым");
            }
            if (description.length() < 20 || description.length() > 7000) {
                throw new ValidationException("Описание должно содержать от 20 до 7000 символов");
            }
        }

        String annotation = dto.getAnnotation();
        if (annotation != null) {
            if (annotation.trim().isEmpty()) {
                throw new ValidationException("Аннотация не может быть пустой");
            }
            if (annotation.length() < 20 || annotation.length() > 2000) {
                throw new ValidationException("Аннотация должна содержать от 20 до 2000 символов");
            }
        }

        Integer participantLimit = dto.getParticipantLimit();
        if (participantLimit != null && participantLimit < 0) {
            throw new ValidationException("participantLimit не может быть отрицательным");
        }

        LocalDateTime newEventDate = dto.getEventDate();
        if (newEventDate != null) {
            LocalDateTime now = LocalDateTime.now();
            if (newEventDate.isBefore(now)) {
                throw new ValidationException("Дата события не может быть в прошлом");
            }

            if (event.getState() == EventStatus.PENDING) {
                LocalDateTime minDate = now.plusHours(2);
                if (newEventDate.isBefore(minDate)) {
                    throw new ValidationException(
                            "Дата события должна быть не ранее чем через 2 часа от текущего времени"
                    );
                }
            }
        }
    }

    private void updateEventFields(Event event, UpdateEventRequestDto dto) {
        if (dto.getTitle() != null) {
            event.setTitle(dto.getTitle());
        }
        if (dto.getAnnotation() != null) {
            event.setAnnotation(dto.getAnnotation());
        }
        if (dto.getDescription() != null) {
            event.setDescription(dto.getDescription());
        }
        if (dto.getEventDate() != null) {
            event.setEventDate(dto.getEventDate());
        }
        if (dto.getParticipantLimit() != null) {
            event.setParticipantLimit(dto.getParticipantLimit());
        }
        if (dto.getPinned() != null) {
            event.setPinned(dto.getPinned());
        }
        if (dto.getPaid() != null) {
            event.setPaid(dto.getPaid());
        }
        if (dto.getRequestModeration() != null) {
            event.setRequestModeration(dto.getRequestModeration());
        }
        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Категория не найдена"));
            event.setCategory(category);
        }
        if (dto.getLocationLat() != null) {
            event.setLocationLat(dto.getLocationLat());
        }
        if (dto.getLocationLon() != null) {
            event.setLocationLon(dto.getLocationLon());
        }
    }

}