package ru.practicum.mainsrvc.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "annotation", columnDefinition = "TEXT")
    private String annotation;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;

    @Column(name = "paid", nullable = false)
    private boolean paid = false;

    @Column(name = "participant_limit")
    private Integer participantLimit = 0;

    @Column(name = "pinned", nullable = false)
    private boolean pinned = false;

    @Column(name = "is_request_moderation", nullable = false)
    private boolean requestModeration = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private EventStatus state = EventStatus.PENDING;

    @ManyToMany(mappedBy = "events", fetch = FetchType.LAZY)
    private List<Compilation> compilations = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_on", updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    @Column(name = "location_lat")
    private Double locationLat;

    @Column(name = "location_lon")
    private Double locationLon;

    public Event() {
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAnnotation() {
        return annotation;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public Category getCategory() {
        return category;
    }

    public User getInitiator() {
        return initiator;
    }

    public boolean isPaid() {
        return paid;
    }

    public Integer getParticipantLimit() {
        return participantLimit;
    }

    public boolean isPinned() {
        return pinned;
    }

    public boolean isRequestModeration() {
        return requestModeration;
    }

    public EventStatus getState() {
        return state;
    }

    public List<Compilation> getCompilations() {
        return compilations;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public LocalDateTime getPublishedOn() {
        return publishedOn;
    }

    public Double getLocationLat() {
        return locationLat;
    }

    public Double getLocationLon() {
        return locationLon;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void setInitiator(User initiator) {
        this.initiator = initiator;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public void setParticipantLimit(Integer participantLimit) {
        this.participantLimit = participantLimit;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public void setRequestModeration(boolean requestModeration) {
        this.requestModeration = requestModeration;
    }

    public void setState(EventStatus state) {
        this.state = state;
    }

    public void setCompilations(List<Compilation> compilations) {
        this.compilations = compilations;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }

    public void setPublishedOn(LocalDateTime publishedOn) {
        this.publishedOn = publishedOn;
    }

    public void setLocationLat(Double locationLat) {
        this.locationLat = locationLat;
    }

    public void setLocationLon(Double locationLon) {
        this.locationLon = locationLon;
    }
}