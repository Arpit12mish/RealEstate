package com.brandPitara.sfs.ratelimit.filter;

import com.brandPitara.sfs.ratelimit.exception.RequestBodyTooLargeException;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Buffers the request body once so it can be read both by RateLimitingFilter
 * (to extract a phone number / token / installationId for keying) and, again
 * afterwards, by the real controller's {@code @RequestBody} binding. Unlike
 * Spring's ContentCachingRequestWrapper (which only exposes the cached bytes
 * after something else has already consumed the stream), this wrapper serves
 * a fresh stream backed by the cached bytes on every call.
 * <p>
 * The read is bounded by {@code maxBodyBytes}: a body larger than that throws
 * {@link RequestBodyTooLargeException} as soon as the overflow is detected,
 * so an oversized/malicious body is never fully buffered in memory.
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private static final int READ_CHUNK_SIZE = 8192;

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request, long maxBodyBytes) throws IOException {
        super(request);
        this.cachedBody = readBounded(request.getInputStream(), maxBodyBytes);
    }

    private static byte[] readBounded(InputStream in, long maxBodyBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(READ_CHUNK_SIZE);
        byte[] chunk = new byte[READ_CHUNK_SIZE];
        long total = 0;
        int read;
        while ((read = in.read(chunk)) != -1) {
            total += read;
            if (total > maxBodyBytes) {
                throw new RequestBodyTooLargeException(maxBodyBytes);
            }
            out.write(chunk, 0, read);
        }
        return out.toByteArray();
    }

    public byte[] getCachedBody() {
        return cachedBody;
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedServletInputStream(cachedBody);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    private static class CachedServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream buffer;

        CachedServletInputStream(byte[] cachedBody) {
            this.buffer = new ByteArrayInputStream(cachedBody);
        }

        @Override
        public boolean isFinished() {
            return buffer.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            // Synchronous use only; no async I/O notifications needed for this wrapper.
        }

        @Override
        public int read() {
            return buffer.read();
        }
    }
}
