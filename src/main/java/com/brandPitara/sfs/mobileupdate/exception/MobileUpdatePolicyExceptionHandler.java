package com.brandPitara.sfs.mobileupdate.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataAccessException;

import java.time.OffsetDateTime;

@RestControllerAdvice(basePackages = "com.brandPitara.sfs.mobileupdate")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MobileUpdatePolicyExceptionHandler {

    @ExceptionHandler(MobileUpdatePolicyException.class)
    public ResponseEntity<MobileUpdatePolicyErrorResponse> handlePolicy(
            MobileUpdatePolicyException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<MobileUpdatePolicyErrorResponse> handleInvalidQuery(
            Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_UPDATE_POLICY_REQUEST",
                "platform and currentBuild must be valid; currentBuild must be zero or greater", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MobileUpdatePolicyErrorResponse> handleInvalidBody(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_UPDATE_POLICY_CONFIGURATION",
                "Update policy validation failed", request);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<MobileUpdatePolicyErrorResponse> handleDatabaseUnavailable(
            DataAccessException ex, HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "UPDATE_POLICY_UNAVAILABLE",
                "Update policy is temporarily unavailable", request);
    }

    private ResponseEntity<MobileUpdatePolicyErrorResponse> build(
            HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(new MobileUpdatePolicyErrorResponse(
                OffsetDateTime.now(), status.value(), code, message, request.getRequestURI()));
    }
}
