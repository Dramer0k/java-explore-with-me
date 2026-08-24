package ru.practicum.mainsrvc.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class UpdateEventRequestDto {

    private String title;
    private String annotation;
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    private Long categoryId;
    private Boolean paid;
    private Integer participantLimit;
    private Boolean pinned;
    private Boolean requestModeration;
    private Double locationLat;
    private Double locationLon;

    private String stateAction;

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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Boolean getPaid() {
        return paid;
    }

    public void setPaid(Boolean paid) {
        this.paid = paid;
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

    public Boolean getRequestModeration() {
        return requestModeration;
    }

    public void setRequestModeration(Boolean requestModeration) {
        this.requestModeration = requestModeration;
    }

    public Double getLocationLat() {
        return locationLat;
    }

    public void setLocationLat(Double locationLat) {
        this.locationLat = locationLat;
    }

    public Double getLocationLon() {
        return locationLon;
    }

    public void setLocationLon(Double locationLon) {
        this.locationLon = locationLon;
    }

    public String getStateAction() {
        return stateAction;
    }

    public void setStateAction(String stateAction) {
        this.stateAction = stateAction;
    }
}