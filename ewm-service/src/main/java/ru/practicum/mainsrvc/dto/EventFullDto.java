package ru.practicum.mainsrvc.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.practicum.mainsrvc.entity.EventStatus;

import java.time.LocalDateTime;

public class EventFullDto {
    private Long id;

    @Size(min = 1, max = 255)
    private String title;

    private String annotation;

    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    private LocalDateTime eventDate;

    private Integer participantLimit;

    @NotNull
    private Boolean pinned;

    @NotNull
    private Boolean paid;

    @NotNull
    private Boolean requestModeration;

    private Long views;

    private CategoryDto category;

    private UserShortDto initiator;

    private Long confirmedRequests;

    private EventStatus state;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    private LocalDateTime createdOn;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    private LocalDateTime publishedOn;

    private LocationDto location;

    public EventFullDto(Long id, String title, String annotation, String description, LocalDateTime eventDate, Integer participantLimit, Boolean pinned, Boolean paid, Boolean requestModeration, Long views, CategoryDto category, UserShortDto initiator, Long confirmedRequests, EventStatus state, LocalDateTime createdOn, LocalDateTime publishedOn, LocationDto location) {
        this.id = id;
        this.title = title;
        this.annotation = annotation;
        this.description = description;
        this.eventDate = eventDate;
        this.participantLimit = participantLimit;
        this.pinned = pinned;
        this.paid = paid;
        this.requestModeration = requestModeration;
        this.views = views;
        this.category = category;
        this.initiator = initiator;
        this.confirmedRequests = confirmedRequests;
        this.state = state;
        this.createdOn = createdOn;
        this.publishedOn = publishedOn;
        this.location = location;
    }

    public EventFullDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getViews() {
        return views;
    }

    public void setViews(Long views) {
        this.views = views;
    }

    public CategoryDto getCategory() {
        return category;
    }

    public void setCategory(CategoryDto category) {
        this.category = category;
    }

    public UserShortDto getInitiator() {
        return initiator;
    }

    public void setInitiator(UserShortDto initiator) {
        this.initiator = initiator;
    }

    public Long getConfirmedRequests() {
        return confirmedRequests;
    }

    public void setConfirmedRequests(Long confirmedRequests) {
        this.confirmedRequests = confirmedRequests;
    }

    public EventStatus getState() {
        return state;
    }

    public void setState(EventStatus state) {
        this.state = state;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }

    public LocalDateTime getPublishedOn() {
        return publishedOn;
    }

    public void setPublishedOn(LocalDateTime publishedOn) {
        this.publishedOn = publishedOn;
    }

    public LocationDto getLocation() {
        return location;
    }

    public void setLocation(LocationDto location) {
        this.location = location;
    }
}
