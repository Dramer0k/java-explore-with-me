package ru.practicum.mainsrvc.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.mainsrvc.Mapper.UserDtoMapper;
import ru.practicum.mainsrvc.dto.UserFullDto;
import ru.practicum.mainsrvc.dto.UserShortDto;
import ru.practicum.mainsrvc.entity.User;
import ru.practicum.mainsrvc.exception.ConflictException;
import ru.practicum.mainsrvc.exception.NotFoundException;
import ru.practicum.mainsrvc.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserDtoMapper userDtoMapper;

    public UserServiceImpl(UserRepository userRepository, UserDtoMapper userDtoMapper) {
        this.userRepository = userRepository;
        this.userDtoMapper = userDtoMapper;
    }

    public UserFullDto createUser(UserFullDto dto) {
        log.debug("Creating user: email={}", dto.getEmail());

        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("Имя обязательно");
        }
        if (dto.getName().length() < 2) {
            throw new IllegalArgumentException("Имя должно содержать не менее 2 символов");
        }
        if (dto.getName().length() > 255) {
            throw new IllegalArgumentException("Имя должно содержать не более 255 символов");
        }

        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email обязателен");
        }

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ConflictException("Пользователь с таким email уже существует");
        }

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setActive(true);

        user = userRepository.save(user);
        log.info("Created user: id={}, email={}", user.getId(), user.getEmail());

        return userDtoMapper.toFullDto(user);
    }

    @Transactional(readOnly = true)
    public List<UserShortDto> getAllUsers(int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        Page<User> page = userRepository.findAll(pageable);
        return page.getContent().stream()
                .map(userDtoMapper::toShortDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserShortDto> getUsersByIds(List<Long> ids) {
        List<User> users = userRepository.findAllById(ids);

        return users.stream()
                .map(userDtoMapper::toShortDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserShortDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + id));
        return userDtoMapper.toShortDto(user);
    }

    public UserShortDto activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + userId));
        user.setActive(true);
        user = userRepository.save(user);
        log.info("Activated user: id={}", userId);
        return userDtoMapper.toShortDto(user);
    }

    public void deleteUser(Long userId) {
        log.debug("Deleting user: id={}", userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь не найден: " + userId);
        }

        userRepository.deleteById(userId);
        log.info("Deleted user: id={}", userId);
    }

    public void deleteUsers(List<Long> ids) {
        log.debug("Deleting users: ids={}", ids);

        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("Список ID пользователей не может быть пустым");
        }

        List<User> users = userRepository.findAllById(ids);

        if (users.size() != ids.size()) {
            List<Long> foundIds = users.stream().map(User::getId).collect(Collectors.toList());
            List<Long> notFound = ids.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());
            throw new NotFoundException("Пользователи не найдены: " + notFound);
        }

        userRepository.deleteAll(users);
        log.info("Deleted users: ids={}", ids);
    }


}