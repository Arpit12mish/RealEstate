package com.brandPitara.sfs.dto;

import jakarta.validation.constraints.NotBlank;
// import lombok.AllArgsConstructor;
import lombok.Data;
// import lombok.NoArgsConstructor;

@Data
public class VerifyOtpRequest {
    @NotBlank
    private String phoneNumber;

    @NotBlank
    private String code;

    private String deviceId;
    private String fcmToken;
}
