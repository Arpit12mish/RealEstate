package com.brandPitara.sfs.exception;

import com.brandPitara.sfs.observability.LogSanitizer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.CannotCreateTransactionException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void connectionAcquisitionFailureIsReportedAsRetryableServiceUnavailable() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler(new LogSanitizer());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public/home");

        var response = handler.handleCannotCreateTransaction(
                new CannotCreateTransactionException("Connection is not available, request timed out"),
                request
        );

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("1");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(503);
        assertThat(response.getBody().getMessage()).isEqualTo("Database temporarily unavailable");
        assertThat(response.getBody().getMessage()).doesNotContain("Connection is not available");
    }
}
