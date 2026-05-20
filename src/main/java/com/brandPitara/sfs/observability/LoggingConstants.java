package com.brandPitara.sfs.observability;

public final class LoggingConstants {

    // MDC keys
    public static final String MDC_REQUEST_ID  = "requestId";
    public static final String MDC_USER_ID     = "userId";
    public static final String MDC_ROLE        = "role";

    // Request-attribute key (set by CorrelationIdFilter so downstream handlers can read it)
    public static final String ATTR_REQUEST_ID = "sfs.requestId";

    // Incoming / outgoing header
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    // Logger names — must match logback-spring.xml logger declarations
    public static final String LOGGER_API      = "com.brandPitara.sfs.observability.api";
    public static final String LOGGER_SECURITY = "com.brandPitara.sfs.observability.security";
    public static final String LOGGER_AUDIT    = "com.brandPitara.sfs.observability.audit";

    // Paths that should be skipped or minimised in API logs
    public static final String PATH_ACTUATOR_HEALTH = "/actuator/health";
    public static final String PATH_FAVICON          = "/favicon.ico";

    private LoggingConstants() {}
}
