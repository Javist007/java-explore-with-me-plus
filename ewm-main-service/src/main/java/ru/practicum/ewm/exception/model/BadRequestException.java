package ru.practicum.ewm.exception.model;

/**
 * Запрос составлен некорректно (400 BAD_REQUEST).
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
