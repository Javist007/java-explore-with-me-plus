package ru.practicum.stats.server.exception;

public class ValidationExceptionHandler extends RuntimeException {
    public ValidationExceptionHandler(String message) {
        super(message);
    }
}
