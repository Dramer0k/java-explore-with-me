package ru.practicum.mainsrvc.Mapper;

import org.springframework.stereotype.Component;
import ru.practicum.mainsrvc.dto.CompilationDto;
import ru.practicum.mainsrvc.dto.EventShortDto;
import ru.practicum.mainsrvc.entity.Compilation;
import ru.practicum.mainsrvc.entity.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CompilationDtoMapper {

    public static CompilationDto toCompilationDto(Compilation c, Map<String, Long> hitsMap) {
        CompilationDto dto = new CompilationDto();
        dto.setId(c.getId());
        dto.setPinned(c.getPinned());
        dto.setTitle(c.getTitle());
        dto.setDescription(c.getDescription());

        List<EventShortDto> eventDto = new ArrayList<>();
        if (c.getEvents() != null) {
            for (Event e : c.getEvents()) {
                if (e != null) {
                    eventDto.add(EventDtoMapper.toEventShortDto(e, hitsMap));
                }
            }
        }
        dto.setEvents(eventDto);

        return dto;
    }
}