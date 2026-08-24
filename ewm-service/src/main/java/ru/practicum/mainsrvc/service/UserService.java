package ru.practicum.mainsrvc.service;

import ru.practicum.mainsrvc.dto.UserFullDto;
import ru.practicum.mainsrvc.dto.UserShortDto;

import java.util.List;

public interface UserService {

    UserFullDto createUser(UserFullDto dto);

    List<UserShortDto> getAllUsers(int from, int size);

    List<UserShortDto> getUsersByIds(List<Long> ids);

    UserShortDto getUserById(Long id);

    UserShortDto activateUser(Long userId);

    void deleteUser(Long userId);

    void deleteUsers(List<Long> ids);

}