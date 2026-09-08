package com.brandPitara.sfs.publiccontent.media;

import com.brandPitara.sfs.cms.media.domain.CmsMediaStatus;
import com.brandPitara.sfs.cms.media.domain.CmsMediaType;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * Production/staging/test path. Never active under the "local" profile -
 * see LocalPresignedPublicMediaUrlResolver, which stands in for CloudFront
 * there without touching (or bypassing) the S3-host rejection below.
 */
@Component
@Profile("!local")
@RequiredArgsConstructor
public class CloudFrontPublicMediaUrlResolver implements PublicMediaUrlResolver {
    private static final Map<CmsMediaType, String> MANAGED_PREFIXES = Map.of(
            CmsMediaType.IMAGE, "cms/images/",
            CmsMediaType.VIDEO, "cms/videos/"
    );
    private static final Map<CmsMediaType, Set<String>> CONTENT_TYPES = Map.of(
            CmsMediaType.IMAGE, Set.of("image/jpeg", "image/png", "image/webp"),
            CmsMediaType.VIDEO, Set.of("video/mp4")
    );

    private final PublicMediaDeliveryProperties properties;
    private final Environment environment;
    private final PublicMediaDeliveryMetrics metrics;
    private URI baseUri;

    @PostConstruct
    void validateConfiguration() {
        String configured = properties.getBaseUrl();
        boolean production = environment.acceptsProfiles(Profiles.of("prod"));
        if (configured == null || configured.isBlank()) {
            if (production) {
                throw new IllegalStateException("CMS_MEDIA_PUBLIC_BASE_URL is required in production");
            }
            baseUri = null;
            return;
        }
        try {
            URI candidate = URI.create(configured.trim());
            boolean validScheme = "https".equalsIgnoreCase(candidate.getScheme())
                    || (!production && "http".equalsIgnoreCase(candidate.getScheme()));
            if (!candidate.isAbsolute() || !validScheme || candidate.getHost() == null
                    || candidate.getUserInfo() != null || candidate.getQuery() != null
                    || candidate.getFragment() != null || isS3Endpoint(candidate.getHost())) {
                throw new IllegalArgumentException("invalid public media base URL");
            }
            String normalized = candidate.toString();
            while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
            baseUri = URI.create(normalized);
        } catch (IllegalArgumentException invalid) {
            throw new IllegalStateException("CMS_MEDIA_PUBLIC_BASE_URL must be an absolute HTTPS URL without credentials, query, or fragment", invalid);
        }
    }

    private boolean isS3Endpoint(String host) {
        String normalized = host.toLowerCase(java.util.Locale.ROOT);
        return normalized.endsWith(".amazonaws.com")
                && (normalized.startsWith("s3.")
                || normalized.startsWith("s3-")
                || normalized.contains(".s3.")
                || normalized.contains(".s3-")
                || normalized.contains("s3-website"));
    }

    @Override
    public String resolve(CmsMediaAssetEntity asset) {
        if (asset == null || asset.getStatus() != CmsMediaStatus.READY) {
            throw failure(asset, "Only READY CMS media can be delivered publicly");
        }
        if (baseUri == null) {
            throw failure(asset, "Public CMS media delivery is not configured");
        }
        CmsMediaType type = asset.getMediaType();
        String key = asset.getStorageKey();
        String prefix = MANAGED_PREFIXES.get(type);
        if (prefix == null || key == null || !key.startsWith(prefix) || !isSafeManagedKey(key)) {
            throw failure(asset, "CMS media storage key is not publicly deliverable");
        }
        if (!CONTENT_TYPES.get(type).contains(asset.getContentType())) {
            throw failure(asset, "CMS media content type is not publicly deliverable");
        }
        String[] segments = key.split("/", -1);
        String resolved = UriComponentsBuilder.fromUri(baseUri)
                .pathSegment(Arrays.copyOf(segments, segments.length))
                .build()
                .encode()
                .toUriString();
        metrics.resolved(type, "success");
        return resolved;
    }

    private PublicMediaDeliveryException failure(CmsMediaAssetEntity asset, String message) {
        metrics.resolved(asset == null ? null : asset.getMediaType(), "failure");
        return new PublicMediaDeliveryException(message);
    }

    private boolean isSafeManagedKey(String key) {
        if (key.startsWith("/") || key.contains("\\") || key.contains("?") || key.contains("#")) return false;
        return Arrays.stream(key.split("/", -1))
                .allMatch(segment -> !segment.isBlank()
                        && !segment.equals(".")
                        && !segment.equals("..")
                        && segment.chars().noneMatch(Character::isISOControl));
    }
}
