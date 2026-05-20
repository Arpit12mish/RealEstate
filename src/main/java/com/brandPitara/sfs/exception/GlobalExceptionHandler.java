package com.brandPitara.sfs.exception;

import com.brandPitara.sfs.observability.LogEvents;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.observability.LoggingConstants;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final Logger ERROR_LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final LogSanitizer logSanitizer;

    // ── 404 Missing route ────────────────────────────────────────────────────
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    // ── 405 ──────────────────────────────────────────────────────────────────
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest req
    ) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), req);
    }

    // ── 400 Validation ───────────────────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest req
    ) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }

        logStructured(ERROR_LOG, "warn", LogEvents.VALIDATION_FAILED, req, null,
                Map.of("fieldCount", errors.size()));

        ApiError body = ApiError.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .path(req.getRequestURI())
                .requestId(resolveRequestId(req))
                .validationErrors(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ── 400 Illegal arg ──────────────────────────────────────────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest req
    ) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    // ── 403 Forbidden ────────────────────────────────────────────────────────
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest req
    ) {
        return build(HttpStatus.FORBIDDEN, "Access denied", req);
    }

    // ── 401 Auth ─────────────────────────────────────────────────────────────
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", req);
    }

    // ── 404 Not found ────────────────────────────────────────────────────────
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleJpaNotFound(EntityNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    // ── 409 Conflict ─────────────────────────────────────────────────────────
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(
            DataIntegrityViolationException ex,
            HttpServletRequest req
    ) {
        logStructured(ERROR_LOG, "warn", LogEvents.DATABASE_ERROR, req, null,
                Map.of("message", "Data integrity violation"));
        return build(HttpStatus.CONFLICT, "Constraint violation", req);
    }

    // ── 500 Unexpected ───────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        logStructured(ERROR_LOG, "error", LogEvents.SERVER_ERROR, req, ex, Map.of());

        return ResponseEntity.status(500).body(
                ApiError.builder()
                        .timestamp(OffsetDateTime.now())
                        .status(500)
                        .error("Internal Server Error")
                        .message("Internal server error")
                        .path(req.getRequestURI())
                        .requestId(resolveRequestId(req))
                        .build()
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest req) {
        ApiError body = ApiError.builder()
                .timestamp(OffsetDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(req.getRequestURI())
                .requestId(resolveRequestId(req))
                .build();
        return ResponseEntity.status(status).body(body);
    }

    private void logStructured(
            Logger log,
            String level,
            String event,
            HttpServletRequest req,
            Exception ex,
            Map<String, Object> extra
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("event", event);
        fields.put("path",  logSanitizer.sanitizePath(req.getRequestURI()));
        if (ex != null) {
            fields.put("exceptionClass", ex.getClass().getSimpleName());
            fields.put("message", logSanitizer.sanitizeMessage(ex.getMessage()));
        }
        fields.putAll(extra);

        switch (level) {
            case "error" -> {
                if (ex != null) {
                    log.error("{}", StructuredArguments.entries(fields), ex);
                } else {
                    log.error("{}", StructuredArguments.entries(fields));
                }
            }
            case "warn"  -> log.warn("{}", StructuredArguments.entries(fields));
            default      -> log.info("{}", StructuredArguments.entries(fields));
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        Object attr = request.getAttribute(LoggingConstants.ATTR_REQUEST_ID);
        if (attr instanceof String id && !id.isBlank()) return id;

        String header = request.getHeader(LoggingConstants.HEADER_REQUEST_ID);
        if (header != null && !header.isBlank()) return header.trim();

        String mdc = MDC.get(LoggingConstants.MDC_REQUEST_ID);
        return (mdc != null && !mdc.isBlank()) ? mdc : null;
    }
}
