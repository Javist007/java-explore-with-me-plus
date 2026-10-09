package ru.practicum.ewm.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Общий обработчик ошибок основного сервиса. Возвращает ответ в формате ApiError.
 */
@Slf4j
@RestControllerAdvice
public class ErrorHandler {
    private static final String BAD_REQUEST_REASON = "Incorrectly made request.";
    private static final String NOT_FOUND_REASON = "The required object was not found.";
    private static final String CONFLICT_REASON = "For the requested operation the conditions are not met.";
    private static final String INTEGRITY_REASON = "Integrity constraint has been violated.";
    private static final String INTERNAL_REASON = "Internal server error.";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("Field: %s. Error: %s. Value: %s",
                        error.getField(), error.getDefaultMessage(), error.getRejectedValue()))
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, BAD_REQUEST_REASON, message);
    }

    @ExceptionHandler({
            BadRequestException.class,
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequest(Exception e) {
        return build(HttpStatus.BAD_REQUEST, BAD_REQUEST_REASON, e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(NotFoundException e) {
        return build(HttpStatus.NOT_FOUND, NOT_FOUND_REASON, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(ConflictException e) {
        return build(HttpStatus.CONFLICT, CONFLICT_REASON, e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataIntegrityViolation(DataIntegrityViolationException e) {
        return build(HttpStatus.CONFLICT, INTEGRITY_REASON, e.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception e) {
        // Стандартные исключения Spring со своим статусом (404 на неизвестный адрес, 405, ResponseStatusException)
        if (e instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
            return ResponseEntity.status(status).body(build(status, status.getReasonPhrase(), e.getMessage()));
        }
        log.error("Непредвиденная ошибка", e);
        return ResponseEntity.internalServerError()
                .body(build(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_REASON, e.getMessage()));
    }

    private ApiError build(HttpStatus status, String reason, String message) {
        log.warn("{}: {}", status, message);
        return ApiError.builder()
                .status(status.name())
                .reason(reason)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
