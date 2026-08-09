package ru.practicum.stats.model.mapper;

import ru.practicum.dto.StatDto;
import ru.practicum.stats.model.Stat;

public class StatMapper {

    public static Stat toStat(StatDto statDto) {
        Stat stat = new Stat();
        stat.setIp(statDto.getIp());
        stat.setUri(statDto.getUri());
        stat.setApp(statDto.getApp());
        stat.setTimestamp(statDto.getTimestamp());

        return stat;
    }

    public static StatDto toDto(Stat stat) {
        return new StatDto(
                stat.getApp(),
                stat.getUri(),
                stat.getIp(),
                stat.getTimestamp()
        );
    }
}