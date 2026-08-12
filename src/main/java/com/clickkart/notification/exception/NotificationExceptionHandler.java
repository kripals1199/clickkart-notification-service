// src/main/java/com/clickkart/notification/exception/NotificationExceptionHandler.java
package com.clickkart.notification.exception;

import com.clickkart.notification.constant.MdcKeys;
import com.clickkart.notification.dto.ApiResponse;
import com.clickkart.notification.dto.ErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Central mapping to the standard {@link ApiResponse} envelope (Rule 12) - own copy of the
 * pattern established in {@code clickkart-auth-service}'s {@code AuthExceptionHandler}, not a
 * shared library (Rule 4).
 */
@Slf4j
@RestControllerAdvice
public class NotificationExceptionHandler {

    private static final String DEFAULT_FIELD_ERROR_MESSAGE = "invalid value";

    @ExceptionHandler(MissingCorrelationIdException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingCorrelationId(MissingCorrelationIdException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, ErrorCode.MISSING_CORRELATION_ID, ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("DATA_INTEGRITY_VIOLATION path={} cause={}", request.getRequestURI(), ex.getMostSpecificCause().toString());
        return respond(HttpStatus.CONFLICT, ErrorCode.DATA_CONFLICT, "This request conflicts with existing data", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError -> fieldErrors.put(
                fieldError.getField(), fieldError.getDefaultMessage() == null ? DEFAULT_FIELD_ERROR_MESSAGE : fieldError.getDefaultMessage()));
        ErrorDetail errorDetail = ErrorDetail.withFieldErrors(ErrorCode.VALIDATION_FAILED, fieldErrors);
        return respond(HttpStatus.BAD_REQUEST, errorDetail, "One or more fields failed validation", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put(ex.getName(), DEFAULT_FIELD_ERROR_MESSAGE);
        ErrorDetail errorDetail = ErrorDetail.withFieldErrors(ErrorCode.VALIDATION_FAILED, fieldErrors);
        return respond(HttpStatus.BAD_REQUEST, errorDetail, "One or more fields failed validation", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedRequestBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Request body is missing or malformed", request);
    }

    /** {@code channel} not matching a populated recipient field - a cross-field consistency check Bean Validation doesn't express here. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Never leak internal exception details/stack traces to the client (Rule 14) - full
        // detail goes to the ERROR log appender instead, keyed by the same correlation id.
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "An unexpected error occurred", request);
    }

    private ResponseEntity<ApiResponse<Void>> respond(HttpStatus status, String code, String message, HttpServletRequest request) {
        return respond(status, ErrorDetail.of(code), message, request);
    }

    private ResponseEntity<ApiResponse<Void>> respond(HttpStatus status, ErrorDetail errorDetail, String message, HttpServletRequest request) {
        String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
        ApiResponse<Void> body = ApiResponse.error(status.value(), errorDetail, message, request.getRequestURI(), correlationId);
        return ResponseEntity.status(status).body(body);
    }

    private static final class ErrorCode {
        private ErrorCode() {}

        static final String MISSING_CORRELATION_ID = "MISSING_CORRELATION_ID";
        static final String DATA_CONFLICT = "DATA_CONFLICT";
        static final String VALIDATION_FAILED = "VALIDATION_FAILED";
        static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    }
}
