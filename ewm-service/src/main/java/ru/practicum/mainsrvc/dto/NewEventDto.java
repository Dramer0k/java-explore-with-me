package ru.practicum.mainsrvc.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

public class NewEventDto {

    @NotBlank(message = "Заголовок обязателен")
    private String title;

    @NotBlank(message = "Поле 'annotation' обязательно для заполнения и не может состоять только из пробелов")
    private String annotation;

    @NotBlank(message = "Поле 'description' обязательно для заполнения и не может состоять только из пробелов")
    private String description;

    @NotNull(message = "Дата события обязательна")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    private LocalDateTime eventDate;

    @PositiveOrZero(message = "participantLimit должен быть >= 0")
    private Integer participantLimit = 0;

    private Boolean pinned;

    private Boolean paid = false;

    private Boolean requestModeration = true;

    @NotNull(message = "Категория обязательна")
    private Long category;

    @NotNull(message = "Координаты обязательны")
    private LocationDto location;

    public NewEventDto() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public Integer getParticipantLimit() {
        return participantLimit;
    }

    public void setParticipantLimit(Integer participantLimit) {
        this.participantLimit = participantLimit;
    }

    public Boolean getPinned() {
        return pinned;
    }

    public void setPinned(Boolean pinned) {
        this.pinned = pinned;
    }

    public Boolean getPaid() {
        return paid;
    }

    public void setPaid(Boolean paid) {
        this.paid = paid;
    }

    public Boolean getRequestModeration() {
        return requestModeration;
    }

    public void setRequestModeration(Boolean requestModeration) {
        this.requestModeration = requestModeration;
    }

    public Long getCategory() {
        return category;
    }

    public void setCategory(Long category) {
        this.category = category;
    }

    public LocationDto getLocation() {
        return location;
    }

    public void setLocation(LocationDto location) {
        this.location = location;
    }
}