package com.brandPitara.sfs.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSendResult {
    private String status;
    private String message;
    private long resendAfterSeconds;
}