package com.brandPitara.sfs.dashboard.city.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardCityCoverImageUpdateRequest {

    @NotBlank
    @Size(max = 500)
    @Pattern(regexp = "^https?://\\S+$", message = "coverImageUrl must be a valid http or https URL")
    private String coverImageUrl;
}
