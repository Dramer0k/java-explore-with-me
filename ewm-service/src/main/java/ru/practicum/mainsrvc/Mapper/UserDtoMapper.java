package ru.practicum.mainsrvc.Mapper;

import org.springframework.stereotype.Component;
import ru.practicum.mainsrvc.dto.UserFullDto;
import ru.practicum.mainsrvc.dto.UserShortDto;
import ru.practicum.mainsrvc.entity.User;

@Component
public class UserDtoMapper {

    public UserShortDto toShortDto(User user) {
        UserShortDto dto = new UserShortDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setActive(user.getActive());
        return dto;
    }

    public UserFullDto toFullDto(User user) {
        UserFullDto dto = new UserFullDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setActive(user.getActive());
        dto.setCreated(user.getCreated());
        return dto;
    }
}