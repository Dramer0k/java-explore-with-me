package ru.practicum.stat_clt.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.StatDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class StatClient {
    private final RestTemplate restTemplate;
    private final String serverUrl;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatClient(RestTemplate restTemplate, @Value("${stats.server.url}") String serverUrl) {
        this.restTemplate = restTemplate;
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl : serverUrl + "/";
    }

    public void hit(String url, String app, String ip) {
        String created = LocalDateTime.now().format(FORMATTER);
        StatDto statDto = new StatDto(app, url, ip, created);
        try {
            restTemplate.postForObject(serverUrl + "hit", statDto, Void.class);
        } catch (Exception e) {
            System.err.println("Failed to send hit to stats service: " + e.getMessage());
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        try {
            String startStr = start.format(FORMATTER);
            String endStr = end.format(FORMATTER);

            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(serverUrl + "stats")
                    .queryParam("start", startStr)
                    .queryParam("end", endStr)
                    .queryParam("unique", unique);

            if (uris != null && !uris.isEmpty()) {
                for (String u : uris) {
                    builder.queryParam("uris", u);
                }
            }

            ResponseEntity<List<ViewStatsDto>> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ViewStatsDto>>() {
                    }
            );
            log.info("RESPONSE: {}", response.getBody());
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Failed to get stats from stats service: " + e.getMessage());
            return List.of();
        }
    }
}