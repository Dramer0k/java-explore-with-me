package ru.practicum.stats.exceptions;

import lombok.Data;

@Data
public class ErrorResponse {
    private String error;
    private int status;

    public ErrorResponse(String error, int status) {
        this.error = error;
        this.status = status;
    }
}