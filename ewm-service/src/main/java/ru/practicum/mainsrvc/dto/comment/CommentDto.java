package ru.practicum.mainsrvc.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.mainsrvc.dto.UserShortDto;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentDto {
    private Long id;
    private String text;
    private LocalDateTime created;
    private LocalDateTime updated;
    private String status;
    private Long eventId;
    private UserShortDto author;
    private String moderatorComment;
}