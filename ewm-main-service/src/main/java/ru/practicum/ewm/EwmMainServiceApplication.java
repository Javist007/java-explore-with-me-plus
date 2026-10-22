package ru.practicum.ewm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Основной сервис Explore With Me.
 *
 * <p>scanBasePackages расширен до ru.practicum, чтобы Spring нашёл бины клиента статистики
 * из пакета ru.practicum.stats.client.
 */
@SpringBootApplication(scanBasePackages = "ru.practicum")
public class EwmMainServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EwmMainServiceApplication.class, args);
    }
}
