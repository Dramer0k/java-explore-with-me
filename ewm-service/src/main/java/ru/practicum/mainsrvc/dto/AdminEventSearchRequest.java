package ru.practicum.mainsrvc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.mainsrvc.entity.EventStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class  AdminEventSearchRequest {
    private List<EventStatus> states;
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
    private int from;
    private int size;
    private List<Long> users;
    private List<Long> categories;
}