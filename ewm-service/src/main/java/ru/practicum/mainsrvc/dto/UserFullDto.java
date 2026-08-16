package ru.practicum.mainsrvc.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class UserFullDto {

    private Long id;

    @NotBlank(message = "Имя обязательно")
    @Size(min = 2, max = 250, message = "Имя должно содержать от 2 до 250 символов")
    private String name;

    @NotBlank(message = "Email обязателен")
    @Size(min = 6, max = 254, message = "Email должен содержать от 6 до 254 символов")
    @Email(message = "Email должен быть корректным")
    private String email;

    private Boolean active;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime created;

    public UserFullDto(Long id, String name, String email, Boolean active) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.active = active;
    }

    public UserFullDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }
}
