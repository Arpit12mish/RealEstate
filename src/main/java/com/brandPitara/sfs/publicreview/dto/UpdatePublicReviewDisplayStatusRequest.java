package com.brandPitara.sfs.publicreview.dto;

import com.brandPitara.sfs.publicreview.enums.PublicReviewDisplayStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePublicReviewDisplayStatusRequest {

    @NotNull
    private PublicReviewDisplayStatus displayStatus;
}
