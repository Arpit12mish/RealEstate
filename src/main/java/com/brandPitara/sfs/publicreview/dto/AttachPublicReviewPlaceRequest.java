package com.brandPitara.sfs.publicreview.dto;

import com.brandPitara.sfs.publicreview.enums.PublicReviewPlaceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachPublicReviewPlaceRequest {

    @NotBlank
    @Size(max = 255)
    private String googlePlaceId;

    @NotNull
    private PublicReviewPlaceCategory placeCategory;

    private Boolean active;
}
