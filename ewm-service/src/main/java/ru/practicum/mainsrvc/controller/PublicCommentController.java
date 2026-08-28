package ru.practicum.mainsrvc.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.mainsrvc.dto.comment.CommentDto;
import ru.practicum.mainsrvc.service.CommentService;

@Controller
@RequestMapping("/events")
public class PublicCommentController {

    private final CommentService commentService;

    public PublicCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/{eventId}/comments")
    public ResponseEntity<Page<CommentDto>> getCommentsByEvent(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {

        Page<CommentDto> comments = commentService.getCommentsByEvent(eventId, from, size);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/{eventId}/comments/{commentId}")
    public ResponseEntity<CommentDto> getCommentById(
            @PathVariable Long eventId,
            @PathVariable Long commentId) {

        CommentDto comment = commentService.getCommentById(eventId, commentId);
        return ResponseEntity.ok(comment);
    }
}