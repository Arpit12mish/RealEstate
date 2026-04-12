package com.brandPitara.sfs.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PresignProfilePhotoResponse {
    private String uploadUrl;
    private String publicUrl;
    private String storageKey;
    private int expiresInSeconds;
}