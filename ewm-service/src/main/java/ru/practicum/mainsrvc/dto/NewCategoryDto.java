package ru.practicum.mainsrvc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class NewCategoryDto {
    @NotBlank(message = "Название категории не может быть пустым")
    @Size(min = 1, max = 255, message = "Длина имени должна быть от 1 до 255 символов")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
