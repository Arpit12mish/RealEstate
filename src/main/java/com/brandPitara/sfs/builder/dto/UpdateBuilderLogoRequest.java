package com.brandPitara.sfs.builder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateBuilderLogoRequest {

    @NotBlank
    @Size(max = 500)
    @Pattern(regexp = "^https?://\\S+$", message = "logoUrl must be a valid http or https URL")
    private String logoUrl;
}
