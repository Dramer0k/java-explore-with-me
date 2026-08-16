package ru.practicum.mainsrvc.dto;

import java.util.List;

public class CompilationCreatedDto {
    private Long id;
    private Boolean pinned;
    private String title;
    private String description;
    private List<Long> events;

    public CompilationCreatedDto(Long id, Boolean pinned, String title, String description, List<Long> events) {
        this.id = id;
        this.pinned = pinned;
        this.title = title;
        this.description = description;
        this.events = events;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean isPinned() {
        return pinned;
    }

    public void setPinned(Boolean pinned) {
        this.pinned = pinned;
    }

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

    public List<Long> getEvents() {
        return events;
    }

    public void setEvents(List<Long> events) {
        this.events = events;
    }
}