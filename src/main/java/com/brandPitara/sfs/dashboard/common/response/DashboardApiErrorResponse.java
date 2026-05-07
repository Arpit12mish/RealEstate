package com.brandPitara.sfs.dashboard.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardApiErrorResponse {

    @Builder.Default
    private Boolean success = false;

    private Integer status;

    /**
     * HTTP error name.
     * Example: BAD_REQUEST, UNAUTHORIZED, FORBIDDEN, NOT_FOUND.
     */
    private String error;

    /**
     * Frontend-friendly stable error code.
     * Example: DASHBOARD_INVALID_WORKFLOW, DASHBOARD_ACCESS_DENIED.
     */
    private String code;

    /**
     * User/frontend readable message.
     */
    private String message;

    private String path;
    private String method;

    private String requestId;

    private OffsetDateTime timestamp;

    private List<DashboardFieldErrorResponse> fieldErrors;

    public static DashboardApiErrorResponse of(
            Integer status,
            String error,
            String code,
            String message,
            String path,
            String method,
            String requestId
    ) {
        return DashboardApiErrorResponse.builder()
                .success(false)
                .status(status)
                .error(error)
                .code(code)
                .message(message)
                .path(path)
                .method(method)
                .requestId(requestId)
                .timestamp(OffsetDateTime.now())
                .build();
    }
}