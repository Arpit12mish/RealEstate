package com.brandPitara.sfs.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendOtpRequest {
    @NotBlank
    private String phoneNumber;   // e.g. +918368046868
}

