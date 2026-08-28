package ru.practicum.mainsrvc.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.mainsrvc.entity.CommentStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentAdminDto {

    private CommentStatus status;
    private String moderatorComment;
}