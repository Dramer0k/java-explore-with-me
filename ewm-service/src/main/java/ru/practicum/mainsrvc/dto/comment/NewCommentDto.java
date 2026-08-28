package ru.practicum.mainsrvc.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewCommentDto {

    @NotBlank(message = "Комментарий не может быть пустым")
    @Size(min = 1, max = 2000, message = "Комментарий должен быть от 1 до 2000 символов")
    private String text;
}