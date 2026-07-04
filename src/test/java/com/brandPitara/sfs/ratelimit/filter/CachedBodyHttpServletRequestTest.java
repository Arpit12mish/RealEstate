package com.brandPitara.sfs.ratelimit.filter;

import com.brandPitara.sfs.ratelimit.exception.RequestBodyTooLargeException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the bounded-read behavior directly, including the case a MockMvc-level
 * test can't reach: a request whose Content-Length header understates (or omits)
 * the real body size, where only the streaming read itself can detect the
 * overflow (RateLimitingFilter's Content-Length pre-check is just a fast path,
 * not the actual safety boundary).
 */
class CachedBodyHttpServletRequestTest {

    private HttpServletRequest requestWithBody(String body) throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ServletInputStream inputStream = asServletInputStream(new ByteArrayInputStream(bytes));
        when(request.getInputStream()).thenReturn(inputStream);
        return request;
    }

    private ServletInputStream asServletInputStream(InputStream delegate) {
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                try {
                    return delegate.available() == 0;
                } catch (IOException e) {
                    return true;
                }
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(jakarta.servlet.ReadListener readListener) {
            }

            @Override
            public int read() throws IOException {
                return delegate.read();
            }
        };
    }

    @Test
    void cachesBodyWithinLimitAndServesItRepeatedly() throws Exception {
        HttpServletRequest request = requestWithBody("{\"phoneNumber\":\"9876543210\"}");

        CachedBodyHttpServletRequest cached = new CachedBodyHttpServletRequest(request, 1024);

        byte[] firstRead = StreamUtils.copyToByteArray(cached.getInputStream());
        byte[] secondRead = StreamUtils.copyToByteArray(cached.getInputStream());

        assertThat(new String(firstRead, StandardCharsets.UTF_8)).contains("9876543210");
        assertThat(secondRead).isEqualTo(firstRead);
    }

    @Test
    void throwsWhenActualStreamedBodyExceedsMaxBytesRegardlessOfDeclaredContentLength() {
        // Simulates a Content-Length header that understates (or is absent for) the
        // real body size - only the bounded streaming read can catch this.
        String oversizedBody = "x".repeat(1000);

        assertThatThrownBy(() -> {
            HttpServletRequest request = requestWithBody(oversizedBody);
            new CachedBodyHttpServletRequest(request, 10);
        }).isInstanceOf(RequestBodyTooLargeException.class);
    }

    @Test
    void bodyExactlyAtTheLimitIsAccepted() throws Exception {
        String body = "x".repeat(10);
        HttpServletRequest request = requestWithBody(body);

        CachedBodyHttpServletRequest cached = new CachedBodyHttpServletRequest(request, 10);

        assertThat(cached.getCachedBody()).hasSize(10);
    }

    @Test
    void emptyBodyIsHandledSafely() throws Exception {
        HttpServletRequest request = requestWithBody("");

        CachedBodyHttpServletRequest cached = new CachedBodyHttpServletRequest(request, 1024);

        assertThat(cached.getCachedBody()).isEmpty();
    }
}
