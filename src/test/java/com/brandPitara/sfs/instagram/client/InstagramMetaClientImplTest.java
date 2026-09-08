package com.brandPitara.sfs.instagram.client;

import com.brandPitara.sfs.instagram.config.InstagramMetaProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;

class InstagramMetaClientImplTest {

    private final InstagramMetaClientImpl client = new InstagramMetaClientImpl(new InstagramMetaProperties());

    @Test
    void redactsAccessTokenFromResourceAccessExceptionMessage() {
        ResourceAccessException ex = new ResourceAccessException(
            "I/O error on GET request for \"https://graph.facebook.com/v25.0/123/media?fields=id"
                + "&limit=25&access_token=EAASECRETVALUETHATMUSTNEVERAPPEARINLOGS\": timeout",
            new java.net.SocketTimeoutException("timeout")
        );

        RestClientException redacted = client.redact(ex);

        assertThat(redacted.getMessage())
            .doesNotContain("EAASECRETVALUETHATMUSTNEVERAPPEARINLOGS")
            .contains("access_token=REDACTED");
    }

    @Test
    void redactsAccessTokenFromPagingNextUrlInExceptionMessage() {
        ResourceAccessException ex = new ResourceAccessException(
            "I/O error on GET request for \"https://graph.facebook.com/v25.0/123/media?after=abc"
                + "&access_token=EAAPAGINGTOKENSECRET\": connection refused"
        );

        RestClientException redacted = client.redact(ex);

        assertThat(redacted.getMessage()).doesNotContain("EAAPAGINGTOKENSECRET");
    }

    @Test
    void leavesMessageUntouchedWhenNoAccessTokenPresent() {
        RestClientException ex = new RestClientException("400 Bad Request: [{\"error\":\"invalid\"}]");

        RestClientException redacted = client.redact(ex);

        assertThat(redacted).isSameAs(ex);
    }
}
