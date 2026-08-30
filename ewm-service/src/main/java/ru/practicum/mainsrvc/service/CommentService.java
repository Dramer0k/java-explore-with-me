package ru.practicum.mainsrvc.service;

import org.springframework.data.domain.Page;
import ru.practicum.mainsrvc.dto.comment.CommentAdminDto;
import ru.practicum.mainsrvc.dto.comment.CommentDto;
import ru.practicum.mainsrvc.dto.comment.NewCommentDto;
import ru.practicum.mainsrvc.dto.comment.UpdateCommentDto;
import ru.practicum.mainsrvc.entity.CommentStatus;

public interface CommentService {

    Page<CommentDto> getCommentsByEvent(Long eventId, int from, int size);

    CommentDto getCommentById(Long eventId, Long commentId);

    CommentDto createComment(Long userId, Long eventId, NewCommentDto dto);

    CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto dto);

    void deleteCommentByUser(Long userId, Long commentId);

    Page<CommentDto> getUserComments(Long userId, int from, int size);

    Page<CommentDto> getCommentsForModeration(Long adminId, CommentStatus status, int from, int size);

    CommentDto moderateComment(Long adminId, Long commentId, CommentAdminDto dto);

    void deleteCommentByAdmin(Long adminId, Long commentId);




}