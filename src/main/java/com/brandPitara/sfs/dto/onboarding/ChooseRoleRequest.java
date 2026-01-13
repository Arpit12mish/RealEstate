package com.brandPitara.sfs.dto.onboarding;

import com.brandPitara.sfs.enums.OnboardingRole;
import jakarta.validation.constraints.NotNull;

public record ChooseRoleRequest(
        @NotNull OnboardingRole role
) {}
