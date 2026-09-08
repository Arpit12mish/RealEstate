package com.brandPitara.sfs.dashboard.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public record DashboardUserPasswordRequest(
        @NotBlank @Size(min = 12, max = 128) String newPassword
) {
    @JsonAnySetter
    public void rejectUnknownField(String field, Object ignoredValue) {
        throw new IllegalArgumentException("Unknown password-reset field: " + field);
    }
}
