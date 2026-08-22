package ru.practicum.mainsrvc.Mapper;

import org.springframework.stereotype.Component;
import ru.practicum.mainsrvc.dto.*;
import ru.practicum.mainsrvc.entity.Event;

import java.util.Map;

@Component
public class EventDtoMapper {

    public EventShortDto toEventShortDto(Event e, Map<String, Long> hitsMap, Long confirmedRequests) {
        EventShortDto dto = new EventShortDto();
        dto.setId(e.getId());
        dto.setAnnotation(e.getAnnotation());
        dto.setTitle(e.getTitle());
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

        dto.setConfirmedRequests(confirmedRequests);

        return dto;
    }

    public static EventShortDto toEventShortDto(Event e, Map<String, Long> hitsMap) {
        EventShortDto dto = new EventShortDto();
        dto.setId(e.getId());
        dto.setAnnotation(e.getAnnotation());
        dto.setTitle(e.getTitle());
        dto.setPinned(e.isPinned());
        dto.setPaid(e.isPaid());
        dto.setEventDate(e.getEventDate());

        String uri = "/events/" + e.getId();
        Long views = hitsMap != null ? hitsMap.getOrDefault(uri, 0L) : 0L;
        dto.setViews(views);

        return dto;
    }

    public EventFullDto toEventFullDto(Event e, Map<String, Long> hitsMap, Long confirmedRequests) {
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

        dto.setConfirmedRequests(confirmedRequests);

        return dto;
    }
}