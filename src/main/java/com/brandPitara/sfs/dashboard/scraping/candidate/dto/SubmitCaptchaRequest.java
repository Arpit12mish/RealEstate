package com.brandPitara.sfs.dashboard.scraping.candidate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubmitCaptchaRequest {

    /** Live browser session ID returned by the initial CAPTCHA_REQUIRED response. */
    private String captchaSessionId;

    @NotBlank(message = "captchaText must not be blank.")
    @Size(max = 30, message = "captchaText must not exceed 30 characters.")
    private String captchaText;
}
