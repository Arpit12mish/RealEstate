package com.brandPitara.sfs.security.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "sfs.authentication.identity-cache")
public class AuthenticationIdentityCacheProperties {

    @Valid
    @NotNull
    private Namespace mobile = Namespace.mobileDefaults();

    @Valid
    @NotNull
    private Namespace dashboard = Namespace.dashboardDefaults();

    @Getter
    @Setter
    public static class Namespace {
        private boolean enabled = true;

        @Min(1)
        @Max(1_000_000)
        private long maximumSize;

        @NotNull
        private Duration expireAfterWrite;

        static Namespace mobileDefaults() {
            Namespace namespace = new Namespace();
            namespace.maximumSize = 50_000;
            namespace.expireAfterWrite = Duration.ofSeconds(30);
            return namespace;
        }

        static Namespace dashboardDefaults() {
            Namespace namespace = new Namespace();
            namespace.maximumSize = 5_000;
            namespace.expireAfterWrite = Duration.ofSeconds(15);
            return namespace;
        }

        @AssertTrue(message = "expireAfterWrite must be between 1 millisecond and 10 minutes")
        public boolean isExpireAfterWriteValid() {
            return expireAfterWrite != null
                    && expireAfterWrite.compareTo(Duration.ofMillis(1)) >= 0
                    && expireAfterWrite.compareTo(Duration.ofMinutes(10)) <= 0;
        }
    }
}
