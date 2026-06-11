package com.brandPitara.sfs.publicreview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GooglePlaceSearchRequest {

    @NotBlank
    private String query;
}
