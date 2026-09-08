package com.brandPitara.sfs.cdn.config;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "sfs.cdn")
public class CdnProperties {

    private boolean enabled;
    private String provider = "cloudfront";
    private String distributionId;

    @AssertTrue(message = "sfs.cdn.distribution-id is required when sfs.cdn.enabled=true")
    public boolean isConfigurationValid() {
        return !enabled || ("cloudfront".equalsIgnoreCase(provider)
                && distributionId != null
                && !distributionId.isBlank());
    }
}
