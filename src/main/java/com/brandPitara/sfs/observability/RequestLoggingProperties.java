package com.brandPitara.sfs.observability;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "sfs.logging.request")
public class RequestLoggingProperties {

    @Valid
    private Async async = new Async();

    @Getter
    @Setter
    public static class Async {

        @Min(16)
        @Max(1_000_000)
        private int queueSize = 2_048;

        @Min(0)
        @Max(999_999)
        private int discardingThreshold = 0;

        private boolean neverBlock = true;

        private boolean includeCallerData = false;

        @Min(100)
        @Max(30_000)
        private int maxFlushTimeMs = 5_000;

        @AssertTrue(message = "discarding-threshold must be smaller than queue-size")
        public boolean isDiscardingThresholdValid() {
            return discardingThreshold < queueSize;
        }

        @AssertTrue(message = "request logging must remain non-blocking")
        public boolean isNonBlockingPolicyValid() {
            return neverBlock;
        }
    }
}
