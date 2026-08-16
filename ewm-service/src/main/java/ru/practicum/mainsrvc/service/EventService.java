package ru.practicum.mainsrvc.service;

import jakarta.persistence.EntityNotFoundException;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final StatClient statClient;

    public EventService(EventRepository eventRepository,
                        RequestRepository requestRepository,
                        CategoryRepository categoryRepository,
                        UserRepository userRepository,
                        StatClient statClient) {
        this.eventRepository = eventRepository;
        this.requestRepository = requestRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.statClient = statClient;
    }

    @Transactional(readOnly = true)
    public List<EventShortDto> getPublicEvents(
            List<Long> categories,
            Boolean paid,
            String text,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            int from,
            int size,
            String clientIp) {

        validatePagination(from, size);

        try {
            statClient.hit("/events", "ewm-service", clientIp);
            log.debug("Отправлен просмотр для /events с IP {}", clientIp);
        } catch (Exception ex) {
            log.warn("Не удалось отправить статистику для /events", ex);
        }

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }
        if (rangeEnd == null) {
            rangeEnd = LocalDateTime.now().plusYears(100);
        }

        validateRangeDates(rangeStart, rangeEnd);

        String searchText = (text != null && !text.isBlank()) ? text.trim() : null;
        List<Long> categoriesList = (categories != null && !categories.isEmpty()) ? categories : null;

        Sort sort = Sort.by("eventDate").ascending();
        Pageable pageable = PageRequest.of(from / size, size, sort);

        Page<Event> pageResult = findPublicEvents(categoriesList, paid, searchText, rangeStart, rangeEnd, pageable);

        List<Event> events = pageResult.getContent();

        Map<String, Long> hitsMap = getHitsMapForEvents(events);

        List<EventShortDto> result = new ArrayList<>(events.size());
        for (Event e : events) {
            result.add(toEventShortDto(e, hitsMap));
        }

        return result;
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

        return toEventShortDto(event, hitsMap);
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
            return toEventFullDto(event, hitsMap);
        }

        Map<String, Long> updatedHitsMap = getHitsMapForEvent(eventId);
        return toEventFullDto(event, updatedHitsMap);
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

        return toEventFullDto(event, Collections.emptyMap());
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

        return toEventFullDto(event, Collections.emptyMap());
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
        return toEventFullDto(event, Collections.emptyMap());
    }

    @Transactional(readOnly = true)
    public EventFullDto getEventFullByIdForUser(Long eventId, Long userId) {
        Event event = eventRepository.findByIdWithDetails(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new EntityNotFoundException("Не хватает прав на просмотр страницы");
        }

        Map<String, Long> hitsMap = getHitsMapForEvent(eventId);

        return toEventFullDto(event, hitsMap);
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

        return events.stream()
                .map(e -> toEventShortDto(e, hitsMap))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventFullDto> getAdminEventsWithFilters(
            List<EventStatus> states,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            int from,
            int size,
            List<Long> users,
            List<Long> categories) {

        validatePagination(from, size);

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now().minusYears(100);
        }
        if (rangeEnd == null) {
            rangeEnd = LocalDateTime.now().plusYears(100);
        }

        Pageable pageable = PageRequest.of(from / size, size);

        List<String> statesStrings = null;
        if (states != null && !states.isEmpty()) {
            statesStrings = states.stream()
                    .map(EventStatus::name)
                    .collect(Collectors.toList());
        }

        List<Long> usersList = (users != null && !users.isEmpty()) ? users : null;
        List<Long> categoriesList = (categories != null && !categories.isEmpty()) ? categories : null;

        Page<Event> page = findAdminEvents(statesStrings, rangeStart, rangeEnd, usersList, categoriesList, pageable);

        log.debug("Найдено событий: {}, всего: {}", page.getContent().size(), page.getTotalElements());

        return page.getContent().stream()
                .map(e -> toEventFullDto(e, Collections.emptyMap()))
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
        return toEventFullDto(event, Collections.emptyMap());
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
        return toEventFullDto(event, Collections.emptyMap());
    }

    public EventFullDto rejectEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (event.getState() != EventStatus.PENDING) {
            throw new ConflictException(
                    "Нельзя отклонить событие: текущий статус — " + event.getState() +
                            ". Отклонение разрешено только из состояния PENDING."
            );
        }

        event.setState(EventStatus.CANCELED);
        event = eventRepository.save(event);

        log.info("Событие id={} успешно отклонено", eventId);
        return toEventFullDto(event, Collections.emptyMap());
    }

    @Transactional(readOnly = true)
    public List<EventFullDto> getAdminEventsList(int from, int size) {
        validatePagination(from, size);

        Pageable pageable = PageRequest.of(from / size, size);
        Page<Event> pageResult = eventRepository.findAll(pageable);

        return pageResult.getContent().stream()
                .map(e -> toEventFullDto(e, Collections.emptyMap()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено: " + eventId));
    }

    private Page<Event> findPublicEvents(
            List<Long> categories,
            Boolean paid,
            String text,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Pageable pageable) {

        boolean hasCategories = categories != null && !categories.isEmpty();
        boolean hasPaid = paid != null;
        boolean hasText = text != null && !text.isBlank();

        if (hasCategories) {
            if (hasPaid && hasText) {
                return eventRepository.findPublicWithCategoriesAll(categories, paid, text, rangeStart, rangeEnd, pageable);
            } else if (hasPaid) {
                return eventRepository.findPublicWithCategoriesPaidOnly(categories, paid, rangeStart, rangeEnd, pageable);
            } else if (hasText) {
                return eventRepository.findPublicWithCategoriesTextOnly(categories, text, rangeStart, rangeEnd, pageable);
            } else {
                return eventRepository.findPublicWithCategoriesBasic(categories, rangeStart, rangeEnd, pageable);
            }
        } else {
            if (hasPaid && hasText) {
                return eventRepository.findPublicWithoutCategoriesAll(paid, text, rangeStart, rangeEnd, pageable);
            } else if (hasPaid) {
                return eventRepository.findPublicWithoutCategoriesPaidOnly(paid, rangeStart, rangeEnd, pageable);
            } else if (hasText) {
                return eventRepository.findPublicWithoutCategoriesTextOnly(text, rangeStart, rangeEnd, pageable);
            } else {
                return eventRepository.findPublicWithoutCategoriesBasic(rangeStart, rangeEnd, pageable);
            }
        }
    }

    private Page<Event> findAdminEvents(
            List<String> states,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            List<Long> users,
            List<Long> categories,
            Pageable pageable) {

        boolean hasUsers = users != null && !users.isEmpty();
        boolean hasCategories = categories != null && !categories.isEmpty();

        if (hasUsers && hasCategories) {
            return eventRepository.findAdminAll(states, rangeStart, rangeEnd, users, categories, pageable);
        } else if (hasUsers) {
            return eventRepository.findAdminWithUsers(states, rangeStart, rangeEnd, users, pageable);
        } else if (hasCategories) {
            return eventRepository.findAdminWithCategories(states, rangeStart, rangeEnd, categories, pageable);
        } else {
            return eventRepository.findAdminBasic(states, rangeStart, rangeEnd, pageable);
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

    private EventShortDto toEventShortDto(Event e, Map<String, Long> hitsMap) {
        EventShortDto dto = new EventShortDto();
        dto.setId(e.getId());
        dto.setTitle(e.getTitle());
        dto.setAnnotation(e.getAnnotation());
        dto.setPinned(e.isPinned());
        dto.setPaid(e.isPaid());
        dto.setEventDate(e.getEventDate());

        if (e.getCategory() != null) {
            CategoryDto categoryDto = new CategoryDto();
            categoryDto.setId(e.getCategory().getId());
            categoryDto.setName(e.getCategory().getName());
            dto.setCategory(categoryDto);
        }

        if (e.getInitiator() != null) {
            UserShortDto initiatorDto = new UserShortDto();
            initiatorDto.setId(e.getInitiator().getId());
            initiatorDto.setName(e.getInitiator().getName());
            initiatorDto.setEmail(e.getInitiator().getEmail());
            dto.setInitiator(initiatorDto);
        }

        String uri = "/events/" + e.getId();
        Long views = hitsMap != null ? hitsMap.getOrDefault(uri, 0L) : 0L;
        dto.setViews(views);

        Long confirmedRequests = requestRepository.countConfirmedByEventId(e.getId());
        dto.setConfirmedRequests(confirmedRequests);

        return dto;
    }

    private EventFullDto toEventFullDto(Event e, Map<String, Long> hitsMap) {
        EventFullDto dto = new EventFullDto();
        dto.setId(e.getId());
        dto.setTitle(e.getTitle());
        dto.setAnnotation(e.getAnnotation());
        dto.setDescription(e.getDescription());
        dto.setEventDate(e.getEventDate());
        dto.setParticipantLimit(e.getParticipantLimit());
        dto.setPinned(e.isPinned());
        dto.setPaid(e.isPaid());
        dto.setRequestModeration(e.isRequestModeration());
        dto.setState(e.getState());
        dto.setCreatedOn(e.getCreatedOn());
        dto.setPublishedOn(e.getPublishedOn());

        if (e.getCategory() != null) {
            CategoryDto categoryDto = new CategoryDto();
            categoryDto.setId(e.getCategory().getId());
            categoryDto.setName(e.getCategory().getName());
            dto.setCategory(categoryDto);
        }

        if (e.getInitiator() != null) {
            UserShortDto initiatorDto = new UserShortDto();
            initiatorDto.setId(e.getInitiator().getId());
            initiatorDto.setName(e.getInitiator().getName());
            initiatorDto.setEmail(e.getInitiator().getEmail());
            initiatorDto.setActive(e.getInitiator().getActive());
            dto.setInitiator(initiatorDto);
        }

        if (e.getLocationLat() != null && e.getLocationLon() != null) {
            LocationDto locationDto = new LocationDto();
            locationDto.setLat(e.getLocationLat());
            locationDto.setLon(e.getLocationLon());
            dto.setLocation(locationDto);
        }

        String uri = "/events/" + e.getId();
        Long views = hitsMap != null ? hitsMap.getOrDefault(uri, 0L) : 0L;
        dto.setViews(views);

        Long confirmedRequests = requestRepository.countConfirmedByEventId(e.getId());
        dto.setConfirmedRequests(confirmedRequests);

        return dto;
    }
}