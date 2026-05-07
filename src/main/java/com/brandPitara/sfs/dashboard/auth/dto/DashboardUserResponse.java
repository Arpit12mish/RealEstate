package com.brandPitara.sfs.dashboard.auth.dto;

import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardUserResponse {

    private Long id;
    private String name;
    private String email;
    private DashboardRole role;
    private Boolean active;

    public static DashboardUserResponse from(DashboardUserEntity user) {
        if (user == null) {
            return null;
        }

        return DashboardUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.getActive())
                .build();
    }
}