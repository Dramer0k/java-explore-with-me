package ru.practicum.mainsrvc.dto;

import java.util.List;

public class CompilationDto {

    private Long id;
    private boolean pinned;
    private String title;
    private String description;
    private List<EventShortDto> events;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
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

    public List<EventShortDto> getEvents() {
        return events;
    }

    public void setEvents(List<EventShortDto> events) {
        this.events = events;
    }
}