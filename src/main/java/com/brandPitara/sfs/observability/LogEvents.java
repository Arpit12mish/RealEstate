package com.brandPitara.sfs.observability;

public final class LogEvents {

    // API request events
    public static final String API_REQUEST_COMPLETED = "api_request_completed";
    public static final String API_REQUEST_FAILED    = "api_request_failed";
    public static final String SLOW_API              = "slow_api";

    // JWT / mobile auth events
    public static final String JWT_MISSING           = "jwt_missing";
    public static final String JWT_EXPIRED           = "jwt_expired";
    public static final String JWT_INVALID           = "jwt_invalid";
    public static final String JWT_MALFORMED         = "jwt_malformed";
    public static final String JWT_SIGNATURE_INVALID = "jwt_signature_invalid";
    public static final String JWT_AUTH_FAILED       = "jwt_auth_failed";

    // Refresh token events
    public static final String REFRESH_TOKEN_INVALID = "refresh_token_invalid";
    public static final String REFRESH_SUCCESS       = "refresh_success";

    // OTP events
    public static final String OTP_VERIFY_FAILED     = "otp_verify_failed";

    // Guest session events
    public static final String GUEST_SESSION_EXPIRED  = "guest_session_expired";

    // Dashboard auth events
    public static final String DASHBOARD_LOGIN_FAILED  = "dashboard_login_failed";
    public static final String DASHBOARD_JWT_EXPIRED   = "dashboard_jwt_expired";
    public static final String DASHBOARD_JWT_INVALID   = "dashboard_jwt_invalid";

    // Access control events
    public static final String ACCESS_DENIED_ROLE    = "access_denied_role";
    public static final String ACCESS_DENIED         = "access_denied";
    public static final String AUTH_REQUIRED         = "auth_required";

    // Session events
    public static final String LOGOUT_SUCCESS        = "logout_success";

    // Error events
    public static final String SERVER_ERROR          = "server_error";
    public static final String VALIDATION_FAILED     = "validation_failed";
    public static final String RESOURCE_NOT_FOUND    = "resource_not_found";
    public static final String CONFLICT              = "conflict";
    public static final String DATABASE_ERROR        = "database_error";

    // Audit events
    public static final String AUDIT_ACTION          = "audit_action";

    private LogEvents() {}
}
