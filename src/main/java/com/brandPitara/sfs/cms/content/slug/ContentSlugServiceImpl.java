package com.brandPitara.sfs.cms.content.slug;

import com.brandPitara.sfs.cms.content.domain.ContentValidation;
import com.brandPitara.sfs.cms.content.exception.CmsContentApiException;
import com.brandPitara.sfs.cms.content.repository.ContentPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ContentSlugServiceImpl implements ContentSlugService {

    private static final Set<String> RESERVED = Set.of(
            "admin", "api", "dashboard", "login", "search", "category",
            "tag", "preview", "feed", "rss", "sitemap"
    );
    private static final int MAX_COLLISION_ATTEMPTS = 10_000;

    private final ContentPostRepository contentPostRepository;

    @Override
    public String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        String normalized = ascii
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-+|-+$)", "");
        return trimToLength(normalized, ContentValidation.SLUG_MAX);
    }

    @Override
    public String resolveForCreate(String requestedSlug, String title) {
        if (StringUtils.hasText(requestedSlug)) {
            String normalized = requireAllowed(normalize(requestedSlug));
            if (contentPostRepository.existsBySlug(normalized)) {
                throw CmsContentApiException.slugConflict(normalized);
            }
            return normalized;
        }

        String base = normalize(title);
        if (base.isBlank()) {
            base = "content";
        }
        String candidate = base;
        for (int suffix = 2; suffix <= MAX_COLLISION_ATTEMPTS; suffix++) {
            if (!isReserved(candidate) && !contentPostRepository.existsBySlug(candidate)) {
                return candidate;
            }
            candidate = withSuffix(base, suffix);
        }
        throw CmsContentApiException.slugConflict(base);
    }

    @Override
    public String resolveForUpdate(String requestedSlug, Long contentId) {
        String normalized = requireAllowed(normalize(requestedSlug));
        if (contentPostRepository.existsBySlugAndIdNot(normalized, contentId)) {
            throw CmsContentApiException.slugConflict(normalized);
        }
        return normalized;
    }

    private String requireAllowed(String slug) {
        if (slug.length() < 3) {
            throw CmsContentApiException.validation("Content slug must contain at least 3 URL-safe characters.");
        }
        if (isReserved(slug)) {
            throw CmsContentApiException.validation("Content slug is reserved: " + slug);
        }
        return slug;
    }

    private boolean isReserved(String slug) {
        return RESERVED.contains(slug);
    }

    private String withSuffix(String base, int suffix) {
        String ending = "-" + suffix;
        return trimToLength(base, ContentValidation.SLUG_MAX - ending.length()) + ending;
    }

    private String trimToLength(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength).replaceAll("-+$", "");
    }
}
