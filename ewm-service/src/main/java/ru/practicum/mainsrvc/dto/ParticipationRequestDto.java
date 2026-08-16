package ru.practicum.mainsrvc.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;
import java.util.Objects;

@JsonPropertyOrder({"id", "created", "status", "event", "requester"})
public class ParticipationRequestDto {

    private Long id;
    private LocalDateTime created;
    private String status;
    private Long event;
    private Long requester;

    public Long getId() {
        return id;
    }

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS", timezone = "UTC")
    public LocalDateTime getCreated() {
        return created;
    }

    public String getStatus() {
        return status;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getEvent() {
        return event;
    }

    public void setEvent(Long event) {
        this.event = event;
    }

    public Long getRequester() {
        return requester;
    }

    public void setRequester(Long requester) {
        this.requester = requester;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParticipationRequestDto that = (ParticipationRequestDto) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(created, that.created) &&
                Objects.equals(status, that.status) &&
                Objects.equals(event, that.event) &&
                Objects.equals(requester, that.requester);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, created, status, event, requester);
    }

    @Override
    public String toString() {
        return "ParticipationRequestDto{" +
                "id=" + id +
                ", created=" + created +
                ", status='" + status + '\'' +
                ", event=" + event +
                ", requester=" + requester +
                '}';
    }
}