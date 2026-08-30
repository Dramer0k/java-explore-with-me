package ru.practicum.mainsrvc.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ru.practicum.mainsrvc.dto.comment.CommentAdminDto;
import ru.practicum.mainsrvc.dto.comment.CommentDto;
import ru.practicum.mainsrvc.entity.CommentStatus;
import ru.practicum.mainsrvc.service.CommentServiceImpl;

@Controller
@RequestMapping("/admin/comments")
public class AdminCommentController {

    private final CommentServiceImpl commentService;

    public AdminCommentController(CommentServiceImpl commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public ResponseEntity<Page<CommentDto>> getCommentsForModeration(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Long adminId) {

        CommentStatus commentStatus = null;
        if (status != null) {
            commentStatus = CommentStatus.valueOf(status.toUpperCase());
        }

        Page<CommentDto> comments = commentService.getCommentsForModeration(
                adminId, commentStatus, from, size);
        return ResponseEntity.ok(comments);
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentDto> moderateComment(
            @PathVariable Long commentId,
            @RequestParam Long adminId,
            @RequestBody CommentAdminDto dto) {

        CommentDto comment = commentService.moderateComment(adminId, commentId, dto);
        return ResponseEntity.ok(comment);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @RequestParam Long adminId) {

        commentService.deleteCommentByAdmin(adminId, commentId);
        return ResponseEntity.noContent().build();
    }
}