package com.brandPitara.sfs.dashboard.scraping.candidate.dto;

import com.brandPitara.sfs.dashboard.scraping.candidate.enums.ScrapeCandidateStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateCandidateStatusRequest {

    @NotNull(message = "status must not be null.")
    private ScrapeCandidateStatus status;

    @Size(max = 1000)
    private String remarks;
}
