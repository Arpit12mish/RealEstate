package com.brandPitara.sfs.dto.onboarding;

import com.brandPitara.sfs.enums.Role;
import jakarta.validation.constraints.NotNull;

public record ChooseRoleRequest(
        @NotNull Role role
) {}
