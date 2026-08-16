package ru.practicum.mainsrvc.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            NotFoundException ex,
            HttpServletRequest request) {

        log.warn("Not found [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        String.valueOf(HttpStatus.NOT_FOUND.value()),
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        if (message.isEmpty()) {
            message = "Ошибка валидации входных данных";
        }

        log.warn("Ошибка валидации [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        String.valueOf(HttpStatus.BAD_REQUEST.value()),
                        message,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        String message = "Обязательный параметр запроса '" + ex.getParameterName() + "' отсутствует";
        log.warn("Отсутствует параметр [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getParameterName());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        String.valueOf(HttpStatus.BAD_REQUEST.value()),
                        message,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            EmptyResultDataAccessException ex,
            HttpServletRequest request) {

        String message = ex.getMessage() != null ? ex.getMessage() : "Ресурс не найден";
        log.warn("Не найдено [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), message);

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        String.valueOf(HttpStatus.NOT_FOUND.value()),
                        message,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        log.warn("Illegal argument [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        String.valueOf(HttpStatus.BAD_REQUEST.value()),
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        String causeMsg = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();

        String message = "Нарушение целостности данных: " + causeMsg;
        log.warn("Конфликт данных [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), causeMsg);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        String.valueOf(HttpStatus.CONFLICT.value()),
                        message,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Необработанное исключение [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                        "Внутренняя ошибка сервера",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        log.warn("Forbidden [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        String.valueOf(HttpStatus.FORBIDDEN.value()),
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        String path = request != null ? request.getRequestURI() : "/";
        log.warn("Conflict [{}] {}: {}",
                request != null ? request.getMethod() : "-",
                path,
                ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        String.valueOf(HttpStatus.CONFLICT.value()),
                        ex.getMessage(),
                        path
                ));
    }

    @ExceptionHandler(InvalidDataAccessResourceUsageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDataAccess(
            InvalidDataAccessResourceUsageException ex,
            HttpServletRequest request) {

        Throwable rootCause = ex.getRootCause();
        String message = rootCause != null ? rootCause.getMessage() : ex.getMessage();

        log.error("SQL-ошибка [{}] {}: {}, SQL: {}",
                request.getMethod(),
                request.getRequestURI(),
                message,
                ex.getMessage(),
                ex);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        String.valueOf(HttpStatus.BAD_REQUEST.value()),
                        "Ошибка запроса к базе данных: " + message,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex,
            HttpServletRequest request) {
        log.warn("Ошибка валидации [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        String.valueOf(HttpStatus.BAD_REQUEST.value()),
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }
}