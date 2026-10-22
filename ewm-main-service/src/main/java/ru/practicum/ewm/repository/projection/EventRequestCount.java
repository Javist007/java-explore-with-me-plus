package ru.practicum.ewm.repository.projection;

/**
 * Количество заявок одного события (результат группировки по событию).
 */
public interface EventRequestCount {
    Long getEventId();

    Long getRequestCount();
}
