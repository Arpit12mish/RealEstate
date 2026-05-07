package com.brandPitara.sfs.dashboard.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardLogoutRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}