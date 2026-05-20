package com.brandPitara.sfs.dashboard.scraping.candidate.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class RefreshCaptchaResponse {

    private Long candidateId;
    private String screenshotPath;
    private String captchaSessionId;
    private OffsetDateTime captchaExpiresAt;
}
