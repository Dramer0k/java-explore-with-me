package ru.practicum.mainsrvc.dto;

import jakarta.validation.constraints.NotNull;

public class LocationDto {

    @NotNull(message = "Широта обязательна")
    private Double lat;

    @NotNull(message = "Долгота обязательна")
    private Double lon;

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }
}