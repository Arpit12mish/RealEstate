package com.brandPitara.sfs.publiccontent.exception;

import com.brandPitara.sfs.publiccontent.dto.PublicContentErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;

@RestControllerAdvice(basePackages = "com.brandPitara.sfs.publiccontent")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PublicContentExceptionHandler {

    @ExceptionHandler(PublicContentApiException.class)
    ResponseEntity<PublicContentErrorResponse> handle(
            PublicContentApiException exception,
            HttpServletRequest request
    ) {
        PublicContentErrorResponse response = new PublicContentErrorResponse(
                OffsetDateTime.now(), exception.getStatus().value(), exception.getCode(),
                exception.getMessage(), request.getRequestURI()
        );
        return ResponseEntity.status(exception.getStatus())
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<PublicContentErrorResponse> handleInvalidRequest(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        PublicContentErrorResponse response = new PublicContentErrorResponse(
                OffsetDateTime.now(), 400, "CONTENT_INVALID_REQUEST",
                exception.getMessage(), request.getRequestURI()
        );
        return ResponseEntity.badRequest()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<PublicContentErrorResponse> handleInvalidParameter(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        PublicContentErrorResponse response = new PublicContentErrorResponse(
                OffsetDateTime.now(), 400, "CONTENT_INVALID_REQUEST",
                "A public content query parameter is invalid.", request.getRequestURI()
        );
        return ResponseEntity.badRequest()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }
}
