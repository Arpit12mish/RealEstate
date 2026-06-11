package com.brandPitara.sfs.publicreview.dto;

import com.brandPitara.sfs.publicreview.enums.SfsReviewVerificationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SfsReviewUpdateVerificationRequest {

    @NotNull
    private SfsReviewVerificationStatus verificationStatus;

    private String internalNote;
}
