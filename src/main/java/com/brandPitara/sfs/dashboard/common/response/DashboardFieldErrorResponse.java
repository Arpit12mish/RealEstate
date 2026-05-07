package com.brandPitara.sfs.dashboard.common.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardFieldErrorResponse {

    private String field;
    private Object rejectedValue;
    private String message;
}