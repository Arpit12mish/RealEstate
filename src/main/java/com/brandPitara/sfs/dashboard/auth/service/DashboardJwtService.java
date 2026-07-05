package com.brandPitara.sfs.dashboard.auth.service;

import com.brandPitara.sfs.dashboard.auth.security.DashboardUserDetails;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class DashboardJwtService {

    private static final String PRINCIPAL_TYPE = "DASHBOARD";
    private static final String TOKEN_TYPE_ACCESS = "DASHBOARD_ACCESS";

    @Value("${dashboard.jwt.secret}")
    private String dashboardJwtSecret;

    @Value("${dashboard.jwt.access-expiration-ms}")
    private long accessExpirationMs;

    /**
     * dashboard.jwt.secret is only otherwise read lazily (on first token
     * generate/parse), so a blank value would previously pass health checks
     * and only surface when a dashboard user tried to log in. Fail fast.
     */
    @PostConstruct
    void validateConfig() {
        if (dashboardJwtSecret == null || dashboardJwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "dashboard.jwt.secret is not configured. Set the DASHBOARD_JWT_SECRET environment variable before starting this profile."
            );
        }
    }

    public String generateAccessToken(DashboardUserEntity user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("principalType", PRINCIPAL_TYPE);
        claims.put("tokenType", TOKEN_TYPE_ACCESS);
        claims.put("dashboardUserId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessExpirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateAccessToken(String token, DashboardUserDetails userDetails) {
        String email = getSubject(token);
        String principalType = getPrincipalType(token);
        String tokenType = getTokenType(token);

        return email.equalsIgnoreCase(userDetails.getUsername())
                && PRINCIPAL_TYPE.equals(principalType)
                && TOKEN_TYPE_ACCESS.equals(tokenType)
                && !isTokenExpired(token)
                && userDetails.isEnabled();
    }

    public String getSubject(String token) {
        return getClaim(token, Claims::getSubject);
    }

    public String getPrincipalType(String token) {
        return getClaim(token, claims -> claims.get("principalType", String.class));
    }

    public String getTokenType(String token) {
        return getClaim(token, claims -> claims.get("tokenType", String.class));
    }

    public Long getDashboardUserId(String token) {
        return getClaim(token, claims -> {
            Object value = claims.get("dashboardUserId");
            return value == null ? null : Long.valueOf(value.toString());
        });
    }

    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }

    private boolean isTokenExpired(String token) {
        return getClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T getClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return resolver.apply(claims);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = dashboardJwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}