package com.brandPitara.sfs.exception;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
public class ApiError {
    private OffsetDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    // optional (only for validation errors)
    private Map<String, String> validationErrors;
}
