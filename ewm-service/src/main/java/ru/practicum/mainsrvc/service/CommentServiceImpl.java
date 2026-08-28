package ru.practicum.mainsrvc.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.mainsrvc.Mapper.CommentDtoMapper;
import ru.practicum.mainsrvc.dto.comment.CommentAdminDto;
import ru.practicum.mainsrvc.dto.comment.CommentDto;
import ru.practicum.mainsrvc.dto.comment.NewCommentDto;
import ru.practicum.mainsrvc.dto.comment.UpdateCommentDto;
import ru.practicum.mainsrvc.entity.*;
import ru.practicum.mainsrvc.exception.ConflictException;
import ru.practicum.mainsrvc.exception.ForbiddenException;
import ru.practicum.mainsrvc.exception.NotFoundException;
import ru.practicum.mainsrvc.repository.CommentRepository;
import ru.practicum.mainsrvc.repository.EventRepository;
import ru.practicum.mainsrvc.repository.UserRepository;

import java.time.LocalDateTime;

@Service
@Transactional
@Slf4j
@AllArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CommentDtoMapper commentDtoMapper;

    private static final int MAX_EDIT_HOURS = 24;


    @Transactional(readOnly = true)
    public Page<CommentDto> getCommentsByEvent(Long eventId, int from, int size) {
        log.debug("Получение комментариев для события id={}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (event.getState() != EventStatus.PUBLISHED) {
            throw new NotFoundException("Событие ещё не опубликовано");
        }

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("created").descending());

        Page<Comment> comments = commentRepository.findByEventIdAndStatusAndDeletedFalse(
                eventId, CommentStatus.APPROVED, pageable);

        return comments.map(commentDtoMapper::toDto);
    }

    @Transactional(readOnly = true)
    public CommentDto getCommentById(Long eventId, Long commentId) {
        log.debug("Получение комментария id={}", commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден"));

        if (comment.isDeleted()) {
            throw new NotFoundException("Комментарий был удалён");
        }

        if (comment.getStatus() != CommentStatus.APPROVED) {
            throw new NotFoundException("Комментарий ещё не одобрен");
        }

        return commentDtoMapper.toDto(comment);
    }

    public CommentDto createComment(Long userId, Long eventId, NewCommentDto dto) {
        log.debug("Создание комментария: userId={}, eventId={}", userId, eventId);

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (event.getState() != EventStatus.PUBLISHED) {
            throw new ConflictException("Нельзя комментировать неопубликованное событие");
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может оставлять комментарии к своему событию");
        }

        Comment comment = new Comment();
        comment.setText(dto.getText());
        comment.setEvent(event);
        comment.setAuthor(author);
        comment.setCreated(LocalDateTime.now());
        comment.setStatus(CommentStatus.PENDING);
        comment.setDeleted(false);

        comment = commentRepository.save(comment);
        log.info("Создан комментарий id={} для события id={}", comment.getId(), eventId);

        return commentDtoMapper.toDto(comment);
    }

    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto dto) {
        log.debug("Обновление комментария: userId={}, commentId={}", userId, commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден"));

        if (comment.isDeleted()) {
            throw new NotFoundException("Комментарий был удалён");
        }

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("Только автор может редактировать комментарий");
        }

        LocalDateTime editDeadline = comment.getCreated().plusHours(MAX_EDIT_HOURS);
        if (editDeadline.isBefore(LocalDateTime.now())) {
            throw new ConflictException("Комментарий можно редактировать только в течение "
                    + MAX_EDIT_HOURS + " часов после создания");
        }

        comment.setText(dto.getText());
        comment.setUpdated(LocalDateTime.now());

        if (comment.getStatus() != CommentStatus.PENDING) {
            comment.setStatus(CommentStatus.PENDING);
        }

        comment = commentRepository.save(comment);
        log.info("Обновлён комментарий id={} пользователем id={}", commentId, userId);

        return commentDtoMapper.toDto(comment);
    }

    public void deleteCommentByUser(Long userId, Long commentId) {
        log.debug("Удаление комментария пользователем: userId={}, commentId={}", userId, commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден"));

        if (comment.isDeleted()) {
            throw new NotFoundException("Комментарий уже удалён");
        }

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("Только автор может удалить комментарий");
        }

        comment.setDeleted(true);
        comment.setUpdated(LocalDateTime.now());

        commentRepository.save(comment);
        log.info("Комментарий id={} помечен как удалён пользователем id={}", commentId, userId);
    }

    @Transactional(readOnly = true)
    public Page<CommentDto> getUserComments(Long userId, int from, int size) {
        log.debug("Получение комментариев пользователя: userId={}", userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("created").descending());

        Page<Comment> comments = commentRepository.findByAuthorIdAndDeletedFalse(userId, pageable);

        return comments.map(commentDtoMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<CommentDto> getCommentsForModeration(Long adminId, CommentStatus status, int from, int size) {
        log.debug("Получение комментариев на модерацию: adminId={}, status={}", adminId, status);

        userRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("created").ascending());

        CommentStatus filterStatus = (status == null) ? CommentStatus.PENDING : status;

        Page<Comment> comments = commentRepository.findByStatusAndDeletedFalse(filterStatus, pageable);

        return comments.map(commentDtoMapper::toDto);
    }

    public CommentDto moderateComment(Long adminId, Long commentId, CommentAdminDto dto) {
        log.debug("Модерация комментария: adminId={}, commentId={}, status={}",
                adminId, commentId, dto.getStatus());

        User moderator = userRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Администратор не найден"));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден"));

        if (comment.isDeleted()) {
            throw new ConflictException("Невозможно модерировать удалённый комментарий");
        }

        if (comment.getStatus() != CommentStatus.PENDING) {
            throw new ConflictException("Комментарий уже прошел модерацию. Текущий статус: " + comment.getStatus());
        }

        if (dto.getStatus() != CommentStatus.APPROVED && dto.getStatus() != CommentStatus.REJECTED) {
            throw new IllegalArgumentException("Допустимые статусы модерации: APPROVED, REJECTED");
        }

        comment.setStatus(dto.getStatus());
        comment.setModeratorComment(dto.getModeratorComment());
        comment.setModerator(moderator);
        comment.setModeratedAt(LocalDateTime.now());

        log.info("Комментарий id={} прошел модерацию администратором id={}. Новый статус: {}",
                commentId, adminId, dto.getStatus());

        return commentDtoMapper.toDto(comment);
    }

    public void deleteCommentByAdmin(Long adminId, Long commentId) {
        log.debug("Удаление комментария администратором: adminId={}, commentId={}", adminId, commentId);

        userRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Администратор не найден"));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден"));

        if (comment.isDeleted()) {
            throw new NotFoundException("Комментарий уже удалён");
        }

        comment.setDeleted(true);
        comment.setUpdated(LocalDateTime.now());

        commentRepository.save(comment);
        log.info("Комментарий id={} удалён администратором id={}", commentId, adminId);
    }

}