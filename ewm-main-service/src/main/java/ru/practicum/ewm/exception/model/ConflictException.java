package ru.practicum.ewm.exception.model;

/**
 * Нарушены условия выполнения операции (409 CONFLICT).
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
