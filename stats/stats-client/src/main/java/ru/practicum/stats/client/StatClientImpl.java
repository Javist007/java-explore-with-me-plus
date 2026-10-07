package ru.practicum.stats.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stats.client.exception.StatServiceClientException;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.StatsDateTimeFormat;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatClientImpl implements StatClient {

    private final RestClient restClient;

    @Override
    public void saveHit(EndpointHitDto hit) {
        try {
            restClient.post()
                    .uri("/hit")
                    .body(hit)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Ошибка при сохранении данных о просмотре: app={}, uri={}, error={}",
                    hit.getApp(), hit.getUri(), e.getMessage());
            throw new StatServiceClientException("Failed to save hit to stats service", e);
        }
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        try {
            String startStr = start.format(StatsDateTimeFormat.FORMATTER);
            String endStr = end.format(StatsDateTimeFormat.FORMATTER);

            UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/stats")
                    .queryParam("start", startStr)
                    .queryParam("end", endStr)
                    .queryParam("unique", unique != null ? unique : false);

            if (uris != null && !uris.isEmpty()) {
                builder.queryParam("uris", String.join(",", uris));
            }

            List<ViewStatsDto> result = restClient.get()
                    .uri(builder.build().toUriString())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ViewStatsDto>>() {
                    });

            return result != null ? result : List.of();

        } catch (Exception e) {
            log.error("Ошибка при получении статистики: {}", e.getMessage());
            throw new StatServiceClientException("Failed to fetch stats from remote service", e);
        }
    }
}