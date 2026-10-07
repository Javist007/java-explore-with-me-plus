package ru.practicum.stats.server.exception;

public class ValidationExceptionHandler extends RuntimeException {
    /**
     * Создает исключение с сообщением об ошибке.
     * @param message сообщение об ошибке
     */
    public ValidationExceptionHandler(String message) {
        super(message);
    }
}
