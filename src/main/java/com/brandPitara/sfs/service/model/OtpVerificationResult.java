package com.brandPitara.sfs.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class OtpVerificationResult {
    private final boolean approved;
    private final String normalizedPhoneNumber;
}
