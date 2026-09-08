package com.brandPitara.sfs.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OtpErrorResponse {
    private boolean success;
    private String code;
    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long retryAfterSeconds;
}
