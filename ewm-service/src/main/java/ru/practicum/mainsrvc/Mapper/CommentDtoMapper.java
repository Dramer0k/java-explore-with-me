package ru.practicum.mainsrvc.Mapper;

import org.springframework.stereotype.Component;
import ru.practicum.mainsrvc.dto.UserShortDto;
import ru.practicum.mainsrvc.dto.comment.CommentDto;
import ru.practicum.mainsrvc.entity.Comment;
import ru.practicum.mainsrvc.entity.User;

@Component
public class CommentDtoMapper {

    public CommentDto toDto(Comment comment) {
        CommentDto dto = new CommentDto();
        dto.setId(comment.getId());
        dto.setText(comment.getText());
        dto.setCreated(comment.getCreated());
        dto.setUpdated(comment.getUpdated());

        if (comment.getStatus() != null) {
            dto.setStatus(comment.getStatus().name());
        }

        if (comment.getEvent() != null) {
            dto.setEventId(comment.getEvent().getId());
        }

        dto.setModeratorComment(comment.getModeratorComment());

        if (comment.getAuthor() != null) {
            User author = comment.getAuthor();
            UserShortDto authorDto = new UserShortDto();
            authorDto.setId(author.getId());
            authorDto.setName(author.getName());
            authorDto.setEmail(author.getEmail());
            authorDto.setActive(author.getActive());
            dto.setAuthor(authorDto);
        }

        return dto;
    }
}