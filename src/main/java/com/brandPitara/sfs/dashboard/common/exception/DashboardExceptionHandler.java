package com.brandPitara.sfs.dashboard.common.exception;

import com.brandPitara.sfs.dashboard.common.response.DashboardApiErrorResponse;
import com.brandPitara.sfs.dashboard.common.response.DashboardFieldErrorResponse;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.observability.LogEvents;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.observability.LoggingConstants;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.brandPitara.sfs.dashboard")
@RequiredArgsConstructor
public class DashboardExceptionHandler {

    private static final Logger ERROR_LOG = LoggerFactory.getLogger(DashboardExceptionHandler.class);

    private final LogSanitizer logSanitizer;

    // ── 400 Validation ───────────────────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<DashboardApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<DashboardFieldErrorResponse> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .toList();

        logStructured("warn", LogEvents.VALIDATION_FAILED, request, null,
                Map.of("fieldCount", fieldErrors.size()));

        DashboardApiErrorResponse response = build(
                HttpStatus.BAD_REQUEST,
                "DASHBOARD_VALIDATION_FAILED",
                "Validation failed. Please check the submitted fields.",
                request
        );
        response.setFieldErrors(fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<DashboardApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        List<DashboardFieldErrorResponse> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(v -> DashboardFieldErrorResponse.builder()
                        .field(v.getPropertyPath() != null ? v.getPropertyPath().toString() : null)
                        .rejectedValue(v.getInvalidValue())
                        .message(v.getMessage())
                        .build())
                .toList();

        DashboardApiErrorResponse response = build(
                HttpStatus.BAD_REQUEST,
                "DASHBOARD_VALIDATION_FAILED",
                "Validation failed. Please check the submitted fields.",
                request
        );
        response.setFieldErrors(fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<DashboardApiErrorResponse> handleBadRequest(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                build(HttpStatus.BAD_REQUEST, "DASHBOARD_INVALID_WORKFLOW",
                        cleanMessage(ex.getMessage(), "Invalid dashboard request."), request)
        );
    }

    // ── 401 Auth ─────────────────────────────────────────────────────────────
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<DashboardApiErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        // Login failure is already logged by DashboardAuthServiceImpl via loginAuditService.
        // Adding a file log here for the security log as well.
        logStructured("warn", LogEvents.DASHBOARD_LOGIN_FAILED, request, null, Map.of());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                build(HttpStatus.UNAUTHORIZED, "DASHBOARD_AUTHENTICATION_FAILED",
                        cleanMessage(ex.getMessage(), "Dashboard authentication failed."), request)
        );
    }

    // ── 403 Access denied ────────────────────────────────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<DashboardApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                build(HttpStatus.FORBIDDEN, "DASHBOARD_ACCESS_DENIED",
                        cleanMessage(ex.getMessage(), "You do not have permission to perform this dashboard action."),
                        request)
        );
    }

    // ── 404 Not found ────────────────────────────────────────────────────────
    @ExceptionHandler({NotFoundException.class, EntityNotFoundException.class})
    public ResponseEntity<DashboardApiErrorResponse> handleNotFound(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                build(HttpStatus.NOT_FOUND, "DASHBOARD_RESOURCE_NOT_FOUND",
                        cleanMessage(ex.getMessage(), "Requested dashboard resource was not found."), request)
        );
    }

    // ── 404 Endpoint not found ────────────────────────────────────────────────
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<DashboardApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                build(HttpStatus.NOT_FOUND, "DASHBOARD_ENDPOINT_NOT_FOUND",
                        "Dashboard endpoint not found: " + request.getMethod() + " " + request.getRequestURI(),
                        request)
        );
    }

    // ── 409 Conflict ─────────────────────────────────────────────────────────
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<DashboardApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        logStructured("warn", LogEvents.DATABASE_ERROR, request, null,
                Map.of("message", "Data integrity violation"));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                build(HttpStatus.CONFLICT, "DASHBOARD_DATA_CONFLICT",
                        "Data conflict occurred. Duplicate or invalid relational data may already exist.", request)
        );
    }

    // ── 400 Parse / format errors ─────────────────────────────────────────────
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<DashboardApiErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                build(HttpStatus.BAD_REQUEST, "DASHBOARD_INVALID_JSON",
                        "Invalid request body. Please check JSON format and enum values.", request)
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<DashboardApiErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                build(HttpStatus.BAD_REQUEST, "DASHBOARD_MISSING_PARAMETER",
                        "Missing required parameter: " + ex.getParameterName(), request)
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<DashboardApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String message = "Invalid value for parameter: " + ex.getName();
        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            message = "Invalid value for parameter '" + ex.getName()
                    + "'. Allowed values: "
                    + String.join(", ", enumValues(ex.getRequiredType()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                build(HttpStatus.BAD_REQUEST, "DASHBOARD_INVALID_PARAMETER", message, request)
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<DashboardApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
                build(HttpStatus.METHOD_NOT_ALLOWED, "DASHBOARD_METHOD_NOT_ALLOWED",
                        "HTTP method is not allowed for this dashboard endpoint.", request)
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<DashboardApiErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(
                build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "DASHBOARD_UNSUPPORTED_MEDIA_TYPE",
                        "Unsupported content type. Use application/json.", request)
        );
    }

    // ── 500 Unexpected ───────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<DashboardApiErrorResponse> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {
        logStructured("error", LogEvents.SERVER_ERROR, request, ex, Map.of());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                build(HttpStatus.INTERNAL_SERVER_ERROR, "DASHBOARD_INTERNAL_ERROR",
                        "Something went wrong while processing the dashboard request.", request)
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DashboardApiErrorResponse build(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) {
        return DashboardApiErrorResponse.of(
                status.value(),
                status.name(),
                code,
                message,
                request.getRequestURI(),
                request.getMethod(),
                resolveRequestId(request)
        );
    }

    private void logStructured(
            String level,
            String event,
            HttpServletRequest request,
            Exception ex,
            Map<String, Object> extra
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("event", event);
        fields.put("path",  logSanitizer.sanitizePath(request.getRequestURI()));
        if (ex != null) {
            fields.put("exceptionClass", ex.getClass().getSimpleName());
            fields.put("message", logSanitizer.sanitizeMessage(ex.getMessage()));
        }
        fields.putAll(extra);

        switch (level) {
            case "error" -> {
                if (ex != null) {
                    ERROR_LOG.error("{}", StructuredArguments.entries(fields), ex);
                } else {
                    ERROR_LOG.error("{}", StructuredArguments.entries(fields));
                }
            }
            case "warn"  -> ERROR_LOG.warn("{}", StructuredArguments.entries(fields));
            default      -> ERROR_LOG.info("{}", StructuredArguments.entries(fields));
        }
    }

    private DashboardFieldErrorResponse toFieldError(FieldError error) {
        Object rejectedValue = error.getRejectedValue();
        if (rejectedValue instanceof String value && value.length() > 120) {
            rejectedValue = value.substring(0, 120) + "...";
        }
        return DashboardFieldErrorResponse.builder()
                .field(error.getField())
                .rejectedValue(rejectedValue)
                .message(error.getDefaultMessage())
                .build();
    }

    private String cleanMessage(String message, String fallback) {
        if (message == null || message.isBlank()) return fallback;
        return message.trim();
    }

    private String resolveRequestId(HttpServletRequest request) {
        Object attr = request.getAttribute(LoggingConstants.ATTR_REQUEST_ID);
        if (attr instanceof String id && !id.isBlank()) return id;

        String header = request.getHeader(LoggingConstants.HEADER_REQUEST_ID);
        if (header != null && !header.isBlank()) return header.trim();

        String mdc = MDC.get(LoggingConstants.MDC_REQUEST_ID);
        return (mdc != null && !mdc.isBlank()) ? mdc : null;
    }

    private List<String> enumValues(Class<?> enumClass) {
        Object[] constants = enumClass.getEnumConstants();
        if (constants == null) return List.of();
        return Arrays.stream(constants).map(Object::toString).toList();
    }
}
