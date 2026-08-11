package ru.practicum.stats.model.mapper;

import ru.practicum.dto.StatDto;
import ru.practicum.stats.model.Stat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StatMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    public static Stat toStat(StatDto statDto) {
        Stat stat = new Stat();
        stat.setIp(statDto.getIp());
        stat.setUri(statDto.getUri());
        stat.setApp(statDto.getApp());
        stat.setTimestamp(LocalDateTime.parse(statDto.getTimestamp(), FORMATTER));

        return stat;
    }

    public static StatDto toDto(Stat stat) {
        return new StatDto(
                stat.getApp(),
                stat.getUri(),
                stat.getIp(),
                stat.getTimestamp().toString()
        );
    }
}