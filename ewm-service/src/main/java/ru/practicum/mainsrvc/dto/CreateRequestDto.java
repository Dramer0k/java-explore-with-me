package ru.practicum.mainsrvc.dto;

public class CreateRequestDto {
    private String comment;

    public CreateRequestDto(String comment) {
        this.comment = comment;
    }

    public CreateRequestDto() {
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
