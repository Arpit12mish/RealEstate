package com.brandPitara.sfs.instagram.service.impl;

import com.brandPitara.sfs.instagram.config.AppInstagramProperties;
import com.brandPitara.sfs.instagram.dto.InstagramCachedAsset;
import com.brandPitara.sfs.instagram.service.InstagramAssetCacheService;
import com.brandPitara.sfs.media.config.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstagramAssetCacheServiceImpl implements InstagramAssetCacheService {

    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    private final S3Client s3Client;
    private final S3Properties s3Properties;
    private final AppInstagramProperties appInstagramProperties;

    @Override
    public InstagramCachedAsset cacheThumbnail(String instagramMediaId, String sourceThumbnailUrl) {
        String mediaId = sanitizeMediaId(instagramMediaId);
        String sourceUrl = cleanRequired(sourceThumbnailUrl, "sourceThumbnailUrl is required");

        URI sourceUri = URI.create(sourceUrl);
        DownloadedImage image = downloadImage(sourceUri);
        String extension = extensionForContentType(image.contentType());
        String storageKey = "instagram/reels/" + mediaId + "/thumb." + extension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(cleanRequired(s3Properties.getBucket(), "S3 bucket is required"))
            .key(storageKey)
            .contentType(image.contentType())
            .cacheControl(CACHE_CONTROL)
            .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(image.bytes()));

        return new InstagramCachedAsset(buildPublicUrl(storageKey), storageKey);
    }

    private DownloadedImage downloadImage(URI sourceUri) {
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(appInstagramProperties.effectiveAssetDownloadTimeoutSeconds()))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

        HttpRequest request = HttpRequest.newBuilder(sourceUri)
            .timeout(Duration.ofSeconds(appInstagramProperties.effectiveAssetDownloadTimeoutSeconds()))
            .GET()
            .build();

        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("thumbnail download failed with HTTP status " + status);
            }

            String contentType = response.headers()
                .firstValue("content-type")
                .map(this::normalizeContentType)
                .orElseThrow(() -> new IllegalStateException("thumbnail response is missing content type"));
            if (!contentType.startsWith("image/")) {
                throw new IllegalStateException("thumbnail response is not an image");
            }

            long contentLength = response.headers()
                .firstValueAsLong("content-length")
                .orElse(-1L);
            long maxBytes = appInstagramProperties.effectiveMaxThumbnailBytes();
            if (contentLength > maxBytes) {
                throw new IllegalStateException("thumbnail response is too large");
            }

            byte[] bytes = readBounded(response.body(), maxBytes);
            if (bytes.length == 0) {
                throw new IllegalStateException("thumbnail response is empty");
            }
            return new DownloadedImage(bytes, contentType);
        } catch (IOException ex) {
            log.warn("Instagram thumbnail download failed uri={}", safeUri(sourceUri));
            throw new IllegalStateException("thumbnail download failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Instagram thumbnail download interrupted uri={}", safeUri(sourceUri));
            throw new IllegalStateException("thumbnail download interrupted", ex);
        } catch (RuntimeException ex) {
            log.warn("Instagram thumbnail cache rejected uri={} reason={}", safeUri(sourceUri), ex.getMessage());
            throw ex;
        }
    }

    private byte[] readBounded(InputStream inputStream, long maxBytes) throws IOException {
        try (InputStream in = inputStream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            long total = 0L;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new IllegalStateException("thumbnail response exceeded max size");
                }
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    private String normalizeContentType(String contentType) {
        int semicolon = contentType.indexOf(';');
        String normalized = semicolon >= 0 ? contentType.substring(0, semicolon) : contentType;
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    private String extensionForContentType(String contentType) {
        return switch (contentType) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> throw new IllegalStateException("unsupported thumbnail image type");
        };
    }

    private String buildPublicUrl(String storageKey) {
        String base = s3Properties.getPublicBaseUrl();
        if (StringUtils.hasText(base)) {
            String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
            return normalized + "/" + storageKey;
        }
        return "https://" + s3Properties.getBucket() + ".s3." + s3Properties.getRegion() + ".amazonaws.com/" + storageKey;
    }

    private String sanitizeMediaId(String instagramMediaId) {
        String cleaned = cleanRequired(instagramMediaId, "instagramMediaId is required")
            .replaceAll("[^A-Za-z0-9_-]", "");
        if (!StringUtils.hasText(cleaned)) {
            throw new IllegalArgumentException("instagramMediaId is invalid");
        }
        return cleaned;
    }

    private String cleanRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String safeUri(URI uri) {
        if (uri == null) {
            return null;
        }
        String path = uri.getPath() == null ? "" : uri.getPath();
        return uri.getScheme() + "://" + uri.getHost() + path;
    }

    private record DownloadedImage(byte[] bytes, String contentType) {}
}
