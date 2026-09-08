package com.brandPitara.sfs.exception;

import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.observability.LoggingConstants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Renders OtpRequestException as {success:false, code, message,
 * retryAfterSeconds} - the structured contract the mobile client's OTP resend
 * timer/messaging depends on, distinct from GlobalExceptionHandler's generic
 * ApiError shape used everywhere else.
 * <p>
 * OtpRequestException extends ResponseStatusException, and GlobalExceptionHandler
 * already has a handler for that supertype. Spring's ExceptionHandlerExceptionResolver
 * picks the first applicable @ExceptionHandler by advice-bean iteration order, NOT the
 * most-specific type across beans (confirmed empirically in
 * AuthControllerExceptionHandlingTest - without this @Order, GlobalExceptionHandler's
 * generic handler won and this class's structured contract never fired). Highest
 * precedence here - mirroring MobileUpdatePolicyExceptionHandler's identical need -
 * guarantees this handler is checked before GlobalExceptionHandler's.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class OtpRequestExceptionHandler {

    private static final Logger SECURITY_LOG = LoggerFactory.getLogger(LoggingConstants.LOGGER_SECURITY);

    private final LogSanitizer logSanitizer;

    @ExceptionHandler(OtpRequestException.class)
    public ResponseEntity<OtpErrorResponse> handleOtpRequestException(
            OtpRequestException ex, HttpServletRequest request) {

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("event", "OTP_REQUEST_REJECTED");
        fields.put("code", ex.getCode());
        fields.put("path", logSanitizer.sanitizePath(request.getRequestURI()));
        SECURITY_LOG.warn("{}", StructuredArguments.entries(fields));

        OtpErrorResponse body = OtpErrorResponse.builder()
                .success(false)
                .code(ex.getCode())
                .message(ex.getReason())
                .retryAfterSeconds(ex.getRetryAfterSeconds())
                .build();

        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        return ResponseEntity.status(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
