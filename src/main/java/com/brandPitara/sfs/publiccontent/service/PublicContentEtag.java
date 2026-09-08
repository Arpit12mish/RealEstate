package com.brandPitara.sfs.publiccontent.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;

@Component
public class PublicContentEtag {

    public String create(Long revisionId, OffsetDateTime publishedAt) {
        String identity = revisionId + ":" + publishedAt.toInstant().toEpochMilli();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(identity.getBytes(StandardCharsets.UTF_8));
            return "\"cms-" + HexFormat.of().formatHex(digest, 0, 16) + "\"";
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public boolean matches(String ifNoneMatch, String etag) {
        if (ifNoneMatch == null || ifNoneMatch.isBlank()) return false;
        for (String candidate : ifNoneMatch.split(",")) {
            String normalized = candidate.trim();
            if ("*".equals(normalized)) return true;
            if (normalized.startsWith("W/")) normalized = normalized.substring(2).trim();
            if (etag.equals(normalized)) return true;
        }
        return false;
    }
}
