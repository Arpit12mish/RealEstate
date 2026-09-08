package com.brandPitara.sfs.observability;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLoggingPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void defaultsAreBoundedNonBlockingAndCallerDataFree() {
        RequestLoggingProperties properties = new RequestLoggingProperties();

        assertThat(validator.validate(properties)).isEmpty();
        assertThat(properties.getAsync().getQueueSize()).isEqualTo(2_048);
        assertThat(properties.getAsync().getDiscardingThreshold()).isZero();
        assertThat(properties.getAsync().isNeverBlock()).isTrue();
        assertThat(properties.getAsync().isIncludeCallerData()).isFalse();
        assertThat(properties.getAsync().getMaxFlushTimeMs()).isEqualTo(5_000);
    }

    @Test
    void rejectsUnboundedBlockingAndInvalidThresholdConfigurations() {
        RequestLoggingProperties properties = new RequestLoggingProperties();
        properties.getAsync().setQueueSize(16);
        properties.getAsync().setDiscardingThreshold(16);
        properties.getAsync().setNeverBlock(false);

        assertThat(validator.validate(properties))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("async.discardingThresholdValid", "async.nonBlockingPolicyValid");
    }
}
