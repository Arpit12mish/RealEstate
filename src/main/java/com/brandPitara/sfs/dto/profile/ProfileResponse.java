package com.brandPitara.sfs.dto.profile;

import com.brandPitara.sfs.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class ProfileResponse {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private boolean verified;
    private Role role;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastLoginAt;
}
