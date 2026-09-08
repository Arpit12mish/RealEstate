package com.brandPitara.sfs.ratelimit.resolver;

import com.brandPitara.sfs.ratelimit.config.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Resolves the client IP for rate-limiting purposes. X-Forwarded-For is only
 * trusted when the direct TCP peer is a configured trusted proxy (default:
 * loopback, matching a single-EC2-instance deployment with a local nginx in
 * front of the app) - otherwise it falls back to request.getRemoteAddr(), so
 * a request cannot spoof its rate-limit identity by sending an arbitrary
 * X-Forwarded-For header directly to the app.
 */
@Component
public class ClientIpResolver {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    /**
     * Strict RFC 4291 IPv6 literal syntax (zone IDs excluded - already filtered out earlier by
     * the "%" check). Matched BEFORE {@link InetAddress#getByName} is ever called: that method
     * only skips DNS for a string its own internal literal-address check accepts, so a
     * charset-valid-but-structurally-invalid candidate (e.g. "aaaa:bbbb" - two groups, no "::")
     * previously fell through to a real, blocking, attacker-triggerable hostname lookup on the
     * request thread. Rejecting anything that fails this check first means getByName is only
     * ever invoked on an already-confirmed-literal string, so it can never reach the
     * nameservice - closing that thread-exhaustion path without weakening what's accepted as a
     * valid address.
     */
    private static final Pattern IPV6_LITERAL = Pattern.compile(
            "^("
                    + "([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}"
                    + "|([0-9a-fA-F]{1,4}:){1,7}:"
                    + "|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}"
                    + "|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}"
                    + "|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}"
                    + "|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}"
                    + "|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}"
                    + "|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})"
                    + "|:((:[0-9a-fA-F]{1,4}){1,7}|:)"
                    + "|::(ffff(:0{1,4})?:)?((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])"
                    + "|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])"
                    + ")$"
    );

    private final Set<String> trustedProxies;

    public ClientIpResolver(RateLimitProperties properties) {
        this.trustedProxies = properties.getTrustedProxies().stream()
                .map(this::normalize)
                .filter(value -> !"unknown".equals(value))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddr = normalize(request.getRemoteAddr());

        if (isTrustedProxy(remoteAddr)) {
            List<String> forwarded = normalizedHops(request.getHeader(FORWARDED_FOR_HEADER));
            for (int index = forwarded.size() - 1; index >= 0; index--) {
                String candidate = forwarded.get(index);
                if (!isTrustedProxy(candidate)) return candidate;
            }
        }

        return remoteAddr;
    }

    private boolean isTrustedProxy(String remoteAddr) {
        if ("unknown".equals(remoteAddr)) return false;
        return trustedProxies.contains(remoteAddr);
    }

    /**
     * Parses only numeric addresses. The right-most untrusted hop is the client;
     * this prevents a client-supplied left-most XFF value from winning when a
     * trusted nginx appends instead of overwriting the header.
     */
    private List<String> normalizedHops(String forwardedFor) {
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return List.of();
        }
        List<String> hops = new ArrayList<>();
        for (String candidate : forwardedFor.split(",", -1)) {
            String normalized = normalize(candidate);
            if (!"unknown".equals(normalized)) hops.add(normalized);
        }
        return hops;
    }

    String normalize(String rawAddress) {
        if (rawAddress == null) return "unknown";
        String candidate = rawAddress.trim().toLowerCase(Locale.ROOT);
        if (candidate.isEmpty() || candidate.contains("%")) return "unknown";
        if (candidate.startsWith("[") && candidate.endsWith("]")) {
            candidate = candidate.substring(1, candidate.length() - 1);
        }

        if (candidate.indexOf(':') < 0) return normalizeIpv4(candidate);
        // Must be a syntactically valid IPv6 literal BEFORE getByName ever sees it - see
        // IPV6_LITERAL's doc comment for why this ordering is what keeps this call
        // non-blocking against attacker-controlled input.
        if (!IPV6_LITERAL.matcher(candidate).matches()) return "unknown";
        try {
            InetAddress parsed = InetAddress.getByName(candidate);
            if (parsed instanceof Inet4Address) return parsed.getHostAddress();
            if (parsed instanceof Inet6Address) return parsed.getHostAddress().toLowerCase(Locale.ROOT);
            return "unknown";
        } catch (UnknownHostException ignored) {
            return "unknown";
        }
    }

    private String normalizeIpv4(String candidate) {
        String[] octets = candidate.split("\\.", -1);
        if (octets.length != 4) return "unknown";
        int[] parsed = new int[4];
        for (int index = 0; index < octets.length; index++) {
            if (!octets[index].matches("[0-9]{1,3}")) return "unknown";
            try {
                parsed[index] = Integer.parseInt(octets[index]);
            } catch (NumberFormatException ignored) {
                return "unknown";
            }
            if (parsed[index] > 255) return "unknown";
        }
        return parsed[0] + "." + parsed[1] + "." + parsed[2] + "." + parsed[3];
    }
}
