package ru.practicum.mainsrvc.service;

import ru.practicum.mainsrvc.dto.*;
import ru.practicum.mainsrvc.entity.Event;

import java.util.List;

public interface EventService {

    EventShortDto getEventShortById(Long eventId, String clientIp);

    EventFullDto getEventFullByIdForPublicWithStats(Long eventId, String clientIp);

    EventFullDto createEvent(NewEventDto dto, Long initiatorId);

    EventFullDto updateEvent(Long eventId, UpdateEventRequestDto dto, Long initiatorId);

    EventFullDto updateEventState(Long userId, Long eventId, StateActionDto dto);

    EventFullDto getEventFullByIdForUser(Long eventId, Long userId);

    List<EventShortDto> getUserEvents(Long userId, int from, int size);

    List<EventFullDto> getAdminEventsWithFilters(AdminEventSearchRequest request);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventRequestDto dto);

    EventFullDto publishEvent(Long eventId);

    EventFullDto rejectEvent(Long eventId);

    List<EventFullDto> getAdminEventsList(int from, int size);

    Event getEventById(Long eventId);

}