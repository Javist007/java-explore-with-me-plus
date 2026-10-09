package ru.practicum.ewm.common;

import java.time.format.DateTimeFormatter;

/**
 * Формат даты и времени, принятый в API основного сервиса.
 */
public final class DateTimeConstants {
    public static final String PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PATTERN);

    private DateTimeConstants() {
    }
}
