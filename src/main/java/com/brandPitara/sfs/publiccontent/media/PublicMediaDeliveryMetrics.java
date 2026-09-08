package com.brandPitara.sfs.publiccontent.media;

import com.brandPitara.sfs.cms.media.domain.CmsMediaType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PublicMediaDeliveryMetrics {
    private final MeterRegistry registry;

    public void resolved(CmsMediaType mediaType, String result) {
        Counter.builder("cms.public.media.resolve")
                .tag("mediaType", mediaType == null ? "UNKNOWN" : mediaType.name())
                .tag("result", result)
                .register(registry)
                .increment();
    }
}
