package com.brandPitara.sfs.cms.media.service;

import com.brandPitara.sfs.cms.media.domain.CmsMediaType;
import io.micrometer.core.instrument.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CmsMediaMetrics {
    private final MeterRegistry registry;

    public void uploadRequested(CmsMediaType type, String result) { increment("cms.media.upload.requested", type, result); }
    public void uploadCompleted(CmsMediaType type, String result) { increment("cms.media.upload.completed", type, result); }
    public void validationFailed(CmsMediaType type) { increment("cms.media.validation.failed", type, "failed"); }
    public void cleanupDeleted(CmsMediaType type, String result) { increment("cms.media.cleanup.deleted", type, result); }

    private void increment(String name, CmsMediaType type, String result) {
        Counter.builder(name).tag("mediaType", type.name()).tag("result", result).register(registry).increment();
    }
}
