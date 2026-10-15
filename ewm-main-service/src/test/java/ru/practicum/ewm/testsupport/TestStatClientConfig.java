package ru.practicum.ewm.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import ru.practicum.stats.client.StatClient;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Заглушка StatClient для интеграционных тестов: не делает реальных HTTP-вызовов,
 * хранит сохранённые хиты и позволяет подменить ответ getStats().
 */
@TestConfiguration
public class TestStatClientConfig {

    @Bean
    @Primary
    public StatClient testStatClient() {
        return new StubStatClient();
    }

    public static class StubStatClient implements StatClient {

        private final List<EndpointHitDto> savedHits = new ArrayList<>();
        private List<ViewStatsDto> statsResponse = List.of();

        @Override
        public void saveHit(EndpointHitDto hit) {
            savedHits.add(hit);
        }

        @Override
        public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                           List<String> uris, Boolean unique) {
            return statsResponse;
        }

        public List<EndpointHitDto> getSavedHits() {
            return savedHits;
        }

        public void setStatsResponse(List<ViewStatsDto> statsResponse) {
            this.statsResponse = statsResponse;
        }
    }
}
