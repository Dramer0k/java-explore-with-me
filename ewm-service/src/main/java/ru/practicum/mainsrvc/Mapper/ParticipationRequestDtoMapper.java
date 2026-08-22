package ru.practicum.mainsrvc.Mapper;

import org.springframework.stereotype.Component;
import ru.practicum.mainsrvc.dto.ParticipationRequestDto;
import ru.practicum.mainsrvc.entity.ParticipationRequest;

@Component
public class ParticipationRequestDtoMapper {

    public ParticipationRequestDto toDto(ParticipationRequest request) {
        ParticipationRequestDto dto = new ParticipationRequestDto();
        dto.setId(request.getId());
        dto.setCreated(request.getCreated());
        dto.setStatus(request.getStatus().name());

        if (request.getEvent() != null) {
            dto.setEvent(request.getEvent().getId());
        }

        if (request.getRequester() != null) {
            dto.setRequester(request.getRequester().getId());
        }

        return dto;
    }
}