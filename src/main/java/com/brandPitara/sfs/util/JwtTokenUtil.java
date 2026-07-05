package com.brandPitara.sfs.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtTokenUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration.ms}")
    private long expirationMs;

    /**
     * jwt.secret is only otherwise read lazily (on first token generate/parse),
     * so a blank value would previously pass health checks and only surface
     * when a real user tried to log in. Fail fast at startup instead.
     */
    @PostConstruct
    void validateConfig() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "jwt.secret is not configured. Set the JWT_SECRET environment variable before starting this profile."
            );
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UserDetails userDetails, Long userId, String phoneNumber, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("principalType", "USER");
        claims.put("userId", userId);
        claims.put("phone", phoneNumber);
        claims.put("role", role);

        return createToken(claims, phoneNumber);
    }

    public String generateGuestToken(Long guestSessionId, String installationId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("principalType", "GUEST");
        claims.put("guestSessionId", guestSessionId);
        claims.put("installationId", installationId);
        claims.put("role", "GUEST");

        return createToken(claims, installationId);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public String getPrincipalTypeFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("principalType", String.class));
    }

    public String getRoleFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("role", String.class));
    }

    public Long getUserIdFromToken(String token) {
        return getClaimFromToken(token, claims -> {
            Object value = claims.get("userId");
            if (value == null) return null;
            return Long.valueOf(value.toString());
        });
    }

    public Long getGuestSessionIdFromToken(String token) {
        return getClaimFromToken(token, claims -> {
            Object value = claims.get("guestSessionId");
            if (value == null) return null;
            return Long.valueOf(value.toString());
        });
    }

    public String getInstallationIdFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("installationId", String.class));
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public boolean validateGuestToken(String token, String installationId) {
        final String subject = getUsernameFromToken(token);
        return (subject.equals(installationId) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }
}