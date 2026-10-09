package ru.practicum.ewm.exception;

/**
 * Запрос составлен некорректно (400 BAD_REQUEST).
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
