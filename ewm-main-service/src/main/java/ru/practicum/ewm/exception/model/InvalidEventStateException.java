package ru.practicum.ewm.exception.model;

/**
 * Некорректный тип действия над статусом (400 BAD_REQUEST).
 */
public class InvalidEventStateException extends RuntimeException {
    public InvalidEventStateException(String message) {
        super(message);
    }
}
