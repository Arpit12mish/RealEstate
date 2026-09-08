package com.brandPitara.sfs.cms.media.validation;

import com.brandPitara.sfs.cms.media.config.CmsMediaProperties;
import com.brandPitara.sfs.cms.media.domain.CmsMediaType;
import com.brandPitara.sfs.cms.media.exception.CmsMediaApiException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

@Component
public class CmsMediaValidator {
    private static final Set<String> IMAGES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> VIDEOS = Set.of("video/mp4");
    private static final Set<String> MP4_BRANDS = Set.of("isom", "iso2", "mp41", "mp42", "avc1", "M4V ", "MSNV");
    private final CmsMediaProperties properties;

    public CmsMediaValidator(CmsMediaProperties properties) {
        this.properties = properties;
    }

    public String validateDeclaration(CmsMediaType type, String filename, String contentType, long size) {
        String normalizedType = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        if (type == null || !(type == CmsMediaType.IMAGE ? IMAGES : VIDEOS).contains(normalizedType)) {
            throw CmsMediaApiException.invalidType("Unsupported media type or content type.");
        }
        if (size <= 0) throw CmsMediaApiException.invalidType("Media size must be positive.");
        long limit = limit(type);
        if (size > limit) throw CmsMediaApiException.tooLarge(limit);
        validateFilename(filename, normalizedType);
        return normalizedType;
    }

    public CmsMediaValidationResult validateObject(CmsMediaType type, String contentType, byte[] prefix) {
        if (type == CmsMediaType.VIDEO) {
            if (!"video/mp4".equals(contentType) || !isMp4(prefix)) throw CmsMediaApiException.validationFailed();
            return new CmsMediaValidationResult(null, null, null);
        }
        int[] dimensions = switch (contentType) {
            case "image/png" -> pngDimensions(prefix);
            case "image/jpeg" -> jpegDimensions(prefix);
            case "image/webp" -> webpDimensions(prefix);
            default -> null;
        };
        if (dimensions == null || dimensions[0] <= 0 || dimensions[1] <= 0
                || dimensions[0] > 30_000 || dimensions[1] > 30_000
                || (long) dimensions[0] * dimensions[1] > 100_000_000L) {
            throw CmsMediaApiException.validationFailed();
        }
        return new CmsMediaValidationResult(dimensions[0], dimensions[1], null);
    }

    public long limit(CmsMediaType type) {
        return type == CmsMediaType.IMAGE ? properties.getMaxImageBytes() : properties.getMaxVideoBytes();
    }

    private void validateFilename(String filename, String contentType) {
        if (filename == null) throw CmsMediaApiException.invalidType("Filename is required.");
        String value = filename.trim();
        if (value.isEmpty() || value.length() > 255 || value.contains("/") || value.contains("\\")
                || value.equals(".") || value.equals("..") || value.chars().anyMatch(ch -> Character.isISOControl(ch))) {
            throw CmsMediaApiException.invalidType("Filename is unsafe.");
        }
        String lower = value.toLowerCase(Locale.ROOT);
        boolean extensionMatches = switch (contentType) {
            case "image/jpeg" -> lower.endsWith(".jpg") || lower.endsWith(".jpeg");
            case "image/png" -> lower.endsWith(".png");
            case "image/webp" -> lower.endsWith(".webp");
            case "video/mp4" -> lower.endsWith(".mp4");
            default -> false;
        };
        if (!extensionMatches) throw CmsMediaApiException.invalidType("Filename extension does not match content type.");
    }

    private int[] pngDimensions(byte[] b) {
        byte[] sig = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        if (!starts(b, sig) || b.length < 24 || !ascii(b, 12, "IHDR")) return null;
        return new int[]{be32(b, 16), be32(b, 20)};
    }

    private int[] jpegDimensions(byte[] b) {
        if (b.length < 4 || u(b[0]) != 0xff || u(b[1]) != 0xd8 || u(b[2]) != 0xff) return null;
        int p = 2;
        while (p + 9 < b.length) {
            if (u(b[p]) != 0xff) { p++; continue; }
            while (p < b.length && u(b[p]) == 0xff) p++;
            if (p >= b.length) return null;
            int marker = u(b[p++]);
            if (marker == 0xd8 || marker == 0xd9) continue;
            if (p + 1 >= b.length) return null;
            int length = (u(b[p]) << 8) | u(b[p + 1]);
            if (length < 2 || p + length > b.length) return null;
            if ((marker >= 0xc0 && marker <= 0xc3) || (marker >= 0xc5 && marker <= 0xc7)
                    || (marker >= 0xc9 && marker <= 0xcb) || (marker >= 0xcd && marker <= 0xcf)) {
                if (length < 7) return null;
                return new int[]{(u(b[p + 5]) << 8) | u(b[p + 6]), (u(b[p + 3]) << 8) | u(b[p + 4])};
            }
            p += length;
        }
        return null;
    }

    private int[] webpDimensions(byte[] b) {
        if (b.length < 30 || !ascii(b, 0, "RIFF") || !ascii(b, 8, "WEBP")) return null;
        if (ascii(b, 12, "VP8X")) return new int[]{le24(b, 24) + 1, le24(b, 27) + 1};
        if (ascii(b, 12, "VP8 ") && u(b[23]) == 0x9d && u(b[24]) == 0x01 && u(b[25]) == 0x2a) {
            return new int[]{le16(b, 26) & 0x3fff, le16(b, 28) & 0x3fff};
        }
        if (ascii(b, 12, "VP8L") && u(b[20]) == 0x2f && b.length >= 25) {
            int bits = u(b[21]) | u(b[22]) << 8 | u(b[23]) << 16 | u(b[24]) << 24;
            return new int[]{(bits & 0x3fff) + 1, ((bits >>> 14) & 0x3fff) + 1};
        }
        return null;
    }

    private boolean isMp4(byte[] b) {
        if (b.length < 12 || !ascii(b, 4, "ftyp")) return false;
        int boxSize = be32(b, 0);
        if (boxSize < 16 || boxSize > b.length) return false;
        String majorBrand = new String(b, 8, 4, StandardCharsets.US_ASCII);
        if (MP4_BRANDS.contains(majorBrand)) return true;
        for (int offset = 16; offset + 4 <= boxSize; offset += 4) {
            if (MP4_BRANDS.contains(new String(b, offset, 4, StandardCharsets.US_ASCII))) return true;
        }
        return false;
    }

    private boolean starts(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) if (data[i] != prefix[i]) return false;
        return true;
    }
    private boolean ascii(byte[] b, int offset, String value) {
        if (offset + value.length() > b.length) return false;
        byte[] expected = value.getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < expected.length; i++) if (b[offset + i] != expected[i]) return false;
        return true;
    }
    private int u(byte b) { return b & 0xff; }
    private int be32(byte[] b, int p) { return u(b[p]) << 24 | u(b[p + 1]) << 16 | u(b[p + 2]) << 8 | u(b[p + 3]); }
    private int le16(byte[] b, int p) { return u(b[p]) | u(b[p + 1]) << 8; }
    private int le24(byte[] b, int p) { return u(b[p]) | u(b[p + 1]) << 8 | u(b[p + 2]) << 16; }
}
