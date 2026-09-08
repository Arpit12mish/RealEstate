package com.brandPitara.sfs.cdn.service;

import com.brandPitara.sfs.cdn.event.ProjectCacheEvictionReason;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectPublicCacheEvictionMetrics {

    private final MeterRegistry meterRegistry;

    public void requested(ProjectCacheEvictionReason reason) { increment("requested", reason); }
    public void success(ProjectCacheEvictionReason reason) { increment("success", reason); }
    public void failure(ProjectCacheEvictionReason reason) { increment("failure", reason); }
    public void skipped(ProjectCacheEvictionReason reason) { increment("skipped", reason); }

    private void increment(String outcome, ProjectCacheEvictionReason reason) {
        Counter.builder("sfs.cdn.invalidation." + outcome)
                .tag("reason", reason.name())
                .tag("provider", "cloudfront")
                .register(meterRegistry)
                .increment();
    }
}
