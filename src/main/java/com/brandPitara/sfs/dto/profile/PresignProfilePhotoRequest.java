package com.brandPitara.sfs.dto.profile;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PresignProfilePhotoRequest {

    @NotBlank(message = "contentType is required")
    private String contentType;
}