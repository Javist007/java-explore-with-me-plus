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

    /**
     * Формирует ответ об ошибке.
     * @param error тип ошибки
     * @param message сообщение об ошибке
     * @param status HTTP статус
     * @param request HTTP запрос
     * @return ErrorResponse с деталями ошибки
     */
    private ErrorResponse buildResponse(String error, String message, HttpStatus status, HttpServletRequest request) {
        return new ErrorResponse(
                error,
                message,
                status.name(),
                OffsetDateTime.now(),
                request.getRequestURI()
        );
    }

    /**
     * Обрабатывает ошибки валидации аргументов метода.
     * @param e исключение валидации
     * @param request HTTP запрос
     * @return ErrorResponse с деталями ошибки
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValid(final MethodArgumentNotValidException e, HttpServletRequest request) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("%s: %s", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining(", "));

        log.error("{}: {}", VALIDATION_ERROR, errorMessage);
        return buildResponse(VALIDATION_ERROR, errorMessage, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Обрабатывает нарушения ограничений (constraint violations).
     * @param e исключение нарушения ограничений
     * @param request HTTP запрос
     * @return ErrorResponse с деталями ошибки
     */
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

    /**
     * Обрабатывает исключения неверных аргументов.
     * @param e исключение неверного аргумента
     * @param request HTTP запрос
     * @return ErrorResponse с деталями ошибки
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(final IllegalArgumentException e, HttpServletRequest request) {
        log.error("Ошибка аргументов: {}", e.getMessage());
        return buildResponse(BAD_REQUEST_ERROR, e.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Обрабатывает отсутствие обязательных параметров запроса.
     * @param e исключение отсутствующего параметра
     * @param request HTTP запрос
     * @return ErrorResponse с деталями ошибки
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingParams(final MissingServletRequestParameterException e, HttpServletRequest request) {
        log.error("Пропущен параметр: {}", e.getMessage());
        return buildResponse("Пропущен обязательный параметр", e.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Обрабатывает несоответствие типов аргументов метода.
     * @param e исключение несоответствия типов
     * @param request HTTP запрос
     * @return ErrorResponse с деталями ошибки
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMismatch(final MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.error("Несовпадение типов: {}", e.getMessage());
        return buildResponse("Неверный формат параметра", e.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Обрабатывает ошибки чтения HTTP сообщения (неверный формат JSON).
     * @param e исключение чтения сообщения
     * @param request HTTP запрос
     * @return ErrorResponse с деталями ошибки
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.error("Неверный формат JSON: {}", e.getMessage());
        return buildResponse("Неверный формат запроса", e.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Обрабатывает все остальные непредвиденные исключения.
     * @param e исключение
     * @param request HTTP запрос
     * @return ErrorResponse с деталями ошибки
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAllExceptions(final Exception e, HttpServletRequest request) {
        log.error("Unexpected error occurred: ", e);
        return buildResponse(INTERNAL_SERVER_ERROR, "Произошла непредвиденная ошибка",
                HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}