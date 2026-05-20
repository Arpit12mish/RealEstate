package com.brandPitara.sfs.dashboard.scraping.candidate.dto;

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
public class SaveScrapeCandidateRequest {

    @NotNull(message = "sourceCode must not be null.")
    private ReraSourceCode sourceCode;

    @NotBlank(message = "reraNumber must not be blank.")
    @Size(max = 100, message = "reraNumber must not exceed 100 characters.")
    private String reraNumber;

    private boolean saveEvidence = true;

    /** Optional: supply when re-submitting after a CAPTCHA_REQUIRED response. */
    private String captchaText;
}
