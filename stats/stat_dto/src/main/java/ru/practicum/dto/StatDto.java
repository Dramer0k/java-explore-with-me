package ru.practicum.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class StatDto {
    private String app;
    private String uri;
    private String ip;
    private LocalDateTime timestamp;
}