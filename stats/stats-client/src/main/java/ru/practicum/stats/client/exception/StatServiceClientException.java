package ru.practicum.stats.client.exception;

public class StatServiceClientException extends RuntimeException {

    public StatServiceClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public StatServiceClientException(String message) {
        super(message);
    }
}
