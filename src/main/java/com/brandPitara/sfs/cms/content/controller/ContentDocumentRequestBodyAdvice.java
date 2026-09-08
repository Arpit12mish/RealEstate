package com.brandPitara.sfs.cms.content.controller;

import com.brandPitara.sfs.cms.content.document.ContentDocumentLimits;
import com.brandPitara.sfs.cms.content.dto.ContentDocumentUpdateRequest;
import com.brandPitara.sfs.cms.content.exception.CmsContentApiException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

@ControllerAdvice(assignableTypes = ContentDocumentController.class)
public class ContentDocumentRequestBodyAdvice extends RequestBodyAdviceAdapter {

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return targetType == ContentDocumentUpdateRequest.class;
    }

    @Override
    public HttpInputMessage beforeBodyRead(
            HttpInputMessage inputMessage,
            MethodParameter parameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        long declaredLength = inputMessage.getHeaders().getContentLength();
        if (declaredLength > ContentDocumentLimits.MAX_REQUEST_BYTES) {
            throw CmsContentApiException.documentTooLarge(ContentDocumentLimits.MAX_REQUEST_BYTES);
        }
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() throws IOException {
                return new BoundedDocumentInputStream(
                        inputMessage.getBody(), ContentDocumentLimits.MAX_REQUEST_BYTES
                );
            }

            @Override
            public org.springframework.http.HttpHeaders getHeaders() {
                return inputMessage.getHeaders();
            }
        };
    }

    private static final class BoundedDocumentInputStream extends FilterInputStream {
        private final int maximumBytes;
        private int consumedBytes;

        private BoundedDocumentInputStream(InputStream inputStream, int maximumBytes) {
            super(inputStream);
            this.maximumBytes = maximumBytes;
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value >= 0) {
                recordRead(1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int read = super.read(buffer, offset, length);
            if (read > 0) {
                recordRead(read);
            }
            return read;
        }

        private void recordRead(int bytes) {
            consumedBytes += bytes;
            if (consumedBytes > maximumBytes) {
                throw CmsContentApiException.documentTooLarge(maximumBytes);
            }
        }
    }
}
