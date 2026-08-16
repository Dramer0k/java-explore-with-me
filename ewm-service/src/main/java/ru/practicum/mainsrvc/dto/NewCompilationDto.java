package ru.practicum.mainsrvc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class NewCompilationDto {

    @NotBlank(message = "Заголовок не может быть пустым")
    @Size(min = 3, max = 50, message = "Заголовок должен содержать от 3 до 50 символов")
    private String title;

    private String description;

    private boolean pinned = false;

    private List<Long> events;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public List<Long> getEvents() {
        return events;
    }

    public void setEvents(List<Long> events) {
        this.events = events;
    }
}