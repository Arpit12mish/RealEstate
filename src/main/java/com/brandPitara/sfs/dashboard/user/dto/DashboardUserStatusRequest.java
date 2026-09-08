package com.brandPitara.sfs.dashboard.user.dto;

import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public record DashboardUserStatusRequest(@NotNull Boolean active) {
    @JsonAnySetter
    public void rejectUnknownField(String field, Object ignoredValue) {
        throw new IllegalArgumentException("Unknown account-status field: " + field);
    }
}
