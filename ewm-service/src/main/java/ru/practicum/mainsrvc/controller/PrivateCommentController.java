package ru.practicum.mainsrvc.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ru.practicum.mainsrvc.dto.comment.CommentDto;
import ru.practicum.mainsrvc.dto.comment.NewCommentDto;
import ru.practicum.mainsrvc.dto.comment.UpdateCommentDto;
import ru.practicum.mainsrvc.service.CommentService;

@Controller
@RequestMapping("/users/{userId}/comments")
public class PrivateCommentController {

    private final CommentService commentService;

    public PrivateCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/events/{eventId}")
    public ResponseEntity<CommentDto> createComment(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody NewCommentDto dto) {

        CommentDto comment = commentService.createComment(userId, eventId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentDto> updateComment(
            @PathVariable Long userId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentDto dto) {

        CommentDto comment = commentService.updateComment(userId, commentId, dto);
        return ResponseEntity.ok(comment);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long userId,
            @PathVariable Long commentId) {

        commentService.deleteCommentByUser(userId, commentId);
        return ResponseEntity.noContent().build();
    }
}