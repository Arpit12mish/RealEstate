// PhoneOtpVerifyRequest.java
package com.brandPitara.sfs.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PhoneOtpVerifyRequest {

    @NotBlank
    private String phoneNumber;

    @NotBlank
    private String code;
}
