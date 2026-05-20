package com.brandPitara.sfs.dashboard.scraping.dto;

import com.brandPitara.sfs.dashboard.scraping.enums.ReraSourceCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReraNumberSearchRequest {

    @NotNull(message = "Source code must not be null.")
    private ReraSourceCode sourceCode;

    @NotBlank(message = "RERA number must not be blank.")
    @Size(max = 100, message = "RERA number must not exceed 100 characters.")
    private String reraNumber;

    private boolean saveEvidence = true;

    private boolean includeRaw = true;

    /** Optional: existing live-browser session ID for captcha-continuation requests. */
    private String captchaSessionId;

    /** Optional: CAPTCHA answer supplied by the admin for portals that require it. */
    private String captchaText;
}
