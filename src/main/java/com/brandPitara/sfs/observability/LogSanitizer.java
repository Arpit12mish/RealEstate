package com.brandPitara.sfs.observability;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Sanitizes values before they are written to log files.
 * Prevents sensitive data (tokens, OTPs, passwords, PII) from appearing in logs.
 * Also defends against log-injection via newline stripping.
 */
@Component
public class LogSanitizer {

    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int MAX_VALUE_LENGTH   = 200;

    private static final Pattern SAFE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-_]{1,80}$");

    private static final Set<String> SENSITIVE_QUERY_KEYS = Set.of(
            "authorization", "accesstoken", "refreshtoken", "token", "jwt",
            "password", "otp", "code", "pin", "secret", "clientsecret",
            "payment", "card", "cvv", "upi", "address", "phone", "email"
    );

    // ── Public API ────────────────────────────────────────────────────────────

    public String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return phone.substring(0, 2) + "******" + phone.substring(phone.length() - 2);
    }

    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "****";
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) return "**@" + domain;
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + domain;
    }

    public String maskToken(String token) {
        if (token == null || token.isBlank()) return "****";
        return "****";
    }

    public String maskAuthorizationHeader(String header) {
        if (header == null || header.isBlank()) return null;
        if (header.toLowerCase().startsWith("bearer ")) return "Bearer ****";
        return "****";
    }

    /**
     * Removes sensitive query params and masks their values.
     * Strips newlines to prevent log injection.
     */
    public String sanitizeQueryString(String queryString) {
        if (queryString == null || queryString.isBlank()) return null;

        String[] pairs = queryString.split("&");
        StringBuilder sb = new StringBuilder();

        for (String pair : pairs) {
            if (sb.length() > 0) sb.append("&");
            int eq = pair.indexOf('=');
            if (eq < 0) {
                sb.append(stripNewlines(pair));
                continue;
            }
            String key = pair.substring(0, eq);
            String val = pair.substring(eq + 1);
            if (SENSITIVE_QUERY_KEYS.contains(key.toLowerCase())) {
                sb.append(stripNewlines(key)).append("=****");
            } else {
                sb.append(stripNewlines(key)).append("=").append(truncate(stripNewlines(val), 80));
            }
        }

        return sb.length() > MAX_VALUE_LENGTH ? sb.substring(0, MAX_VALUE_LENGTH) + "..." : sb.toString();
    }

    public String sanitizePath(String path) {
        if (path == null) return "/";
        return stripNewlines(truncate(path, 200));
    }

    public String sanitizeMessage(String message) {
        if (message == null) return null;
        return truncate(stripNewlines(message), MAX_MESSAGE_LENGTH);
    }

    public String simplifyUserAgent(String ua) {
        if (ua == null || ua.isBlank()) return "unknown";
        String lower = ua.toLowerCase();
        if (lower.contains("android"))    return "Android";
        if (lower.contains("iphone"))     return "iPhone";
        if (lower.contains("ipad"))       return "iPad";
        if (lower.contains("postman"))    return "Postman";
        if (lower.contains("okhttp"))     return "OkHttp";
        if (lower.contains("retrofit"))   return "Retrofit";
        if (lower.contains("curl"))       return "curl";
        if (lower.contains("insomnia"))   return "Insomnia";
        if (lower.contains("mozilla"))    return "Browser";
        return truncate(stripNewlines(ua), 40);
    }

    /**
     * Masks the last two octets of an IPv4 address.
     * Returns "masked" for IPv6 or unknown formats.
     */
    public String maskIp(String ip) {
        if (ip == null || ip.isBlank()) return "unknown";
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".*.* ";
        }
        return "masked";
    }

    public String sanitizeMap(Map<String, String[]> paramMap) {
        if (paramMap == null || paramMap.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            String key = entry.getKey();
            if (SENSITIVE_QUERY_KEYS.contains(key.toLowerCase())) {
                sb.append(stripNewlines(key)).append("=****");
            } else {
                String val = entry.getValue() != null && entry.getValue().length > 0
                        ? entry.getValue()[0] : "";
                sb.append(stripNewlines(key)).append("=").append(truncate(stripNewlines(val), 80));
            }
        }
        return sb.length() > MAX_VALUE_LENGTH ? sb.substring(0, MAX_VALUE_LENGTH) + "..." : sb.toString();
    }

    /**
     * Validates that a correlation ID is safe to log (no injection risk).
     */
    public boolean isSafeCorrelationId(String id) {
        return id != null && SAFE_ID_PATTERN.matcher(id).matches();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static String stripNewlines(String value) {
        if (value == null) return null;
        return value.replace('\n', '_').replace('\r', '_').replace('\t', ' ');
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) + "..." : value;
    }
}
