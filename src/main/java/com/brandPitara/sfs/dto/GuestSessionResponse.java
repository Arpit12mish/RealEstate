package com.brandPitara.sfs.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GuestSessionResponse {

    private String accessToken;
    private Long guestSessionId;
    private String role;
    private String installationId;
}