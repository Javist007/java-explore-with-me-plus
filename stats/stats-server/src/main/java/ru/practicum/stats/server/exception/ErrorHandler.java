package ru.practicum.stats.server.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {

    private static final String VALIDATION_ERROR = "Ошибка валидации";
    private static final String BAD_REQUEST_ERROR = "Неверный запрос";
    private static final String INTERNAL_SERVER_ERROR = "Внутренняя ошибка сервера";

    private ErrorResponse buildResponse(String error, String message, HttpStatus status, HttpServletRequest request) {
        return new ErrorResponse(
                error,
                message,
                status.name(),
                OffsetDateTime.now(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValid(final MethodArgumentNotValidException e, HttpServletRequest request) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("%s: %s", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining(", "));

        log.error("{}: {}", VALIDATION_ERROR, errorMessage);
        return buildResponse(VALIDATION_ERROR, errorMessage, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolation(final ConstraintViolationException e, HttpServletRequest request) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(violation -> String.format("%s: %s",
                        violation.getPropertyPath().toString(),
                        violation.getMessage()))
                .collect(Collectors.joining(", "));

        log.error("{}: {}", VALIDATION_ERROR, errorMessage);
        return buildResponse(VALIDATION_ERROR, errorMessage, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(final IllegalArgumentException e, HttpServletRequest request) {
        log.error("Ошибка аргументов: {}", e.getMessage());
        return buildResponse(BAD_REQUEST_ERROR, e.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingParams(final MissingServletRequestParameterException e, HttpServletRequest request) {
        log.error("Пропущен параметр: {}", e.getMessage());
        return buildResponse("Пропущен обязательный параметр", e.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMismatch(final MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.error("Несовпадение типов: {}", e.getMessage());
        return buildResponse("Неверный формат параметра", e.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.error("Неверный формат JSON: {}", e.getMessage());
        return buildResponse("Неверный формат запроса", e.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAllExceptions(final Exception e, HttpServletRequest request) {
        log.error("Unexpected error occurred: ", e);
        return buildResponse(INTERNAL_SERVER_ERROR, "Произошла непредвиденная ошибка",
                HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}