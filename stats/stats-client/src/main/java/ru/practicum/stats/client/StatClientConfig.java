package ru.practicum.stats.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class StatClientConfig {

    @Value("${stat.server.url:http://localhost:9090}")
    private String baseUrl;

    @Bean
    public RestClient statisticsRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
