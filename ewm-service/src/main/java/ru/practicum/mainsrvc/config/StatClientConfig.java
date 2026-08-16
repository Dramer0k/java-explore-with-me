package ru.practicum.mainsrvc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import ru.practicum.stat_clt.client.StatClient;

@Configuration
public class StatClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public StatClient statClient(RestTemplate restTemplate,
                                 org.springframework.core.env.Environment env) {
        String url = env.getProperty("stats.server.url");
        if (url == null) {
            throw new IllegalStateException("Property 'stats.server.url' is required");
        }
        return new StatClient(restTemplate, url);
    }
}