package ru.practicum.mainsrvc.dto;

import ru.practicum.mainsrvc.entity.Event;

public interface CompilationEventProjection {
    Long getCompilationId();
    Event getEvent();
}