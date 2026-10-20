package ru.practicum.ewm.exception.model;

public class InvalidEventStateException extends RuntimeException {
    public InvalidEventStateException(String message) {
        super(message);
    }
}
