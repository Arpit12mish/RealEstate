package com.brandPitara.sfs.dto.profile;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmProfilePhotoRequest {

    @NotBlank(message = "url is required")
    private String url;

    @NotBlank(message = "storageKey is required")
    private String storageKey;
}