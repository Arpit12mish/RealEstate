package com.brandPitara.sfs.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GuestSessionRequest {

    @NotBlank
    private String installationId;

    private String deviceModel;
    private String deviceName;
    private String platform;
    private String osVersion;
    private String appVersion;
}