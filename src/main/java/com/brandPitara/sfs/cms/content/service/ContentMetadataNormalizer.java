package com.brandPitara.sfs.cms.content.service;

import com.brandPitara.sfs.cms.content.domain.ContentValidation;
import com.brandPitara.sfs.cms.content.exception.CmsContentApiException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

@Component
public class ContentMetadataNormalizer {

    public String title(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (normalized.length() < ContentValidation.TITLE_MIN
                || normalized.length() > ContentValidation.TITLE_MAX) {
            throw CmsContentApiException.validation("Title must be 3-220 characters after trimming.");
        }
        requireNoControlCharacters(normalized, "Title");
        return normalized;
    }

    public String excerpt(String value) {
        return optionalText(value, ContentValidation.EXCERPT_MAX, "Excerpt");
    }

    public String seoTitle(String value) {
        return optionalText(value, ContentValidation.SEO_TITLE_MAX, "SEO title");
    }

    public String seoDescription(String value) {
        return optionalText(value, ContentValidation.SEO_DESCRIPTION_MAX, "SEO description");
    }

    public String canonicalUrl(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > ContentValidation.CANONICAL_URL_MAX) {
            throw CmsContentApiException.validation("Canonical URL exceeds 2048 characters.");
        }
        try {
            URI uri = new URI(normalized);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !StringUtils.hasText(uri.getHost())
                    || uri.getUserInfo() != null) {
                throw CmsContentApiException.validation(
                        "Canonical URL must be an absolute HTTPS URL without embedded credentials."
                );
            }
            return uri.normalize().toASCIIString();
        } catch (URISyntaxException exception) {
            throw CmsContentApiException.validation("Canonical URL must be a valid HTTPS URL.");
        }
    }

    private String optionalText(String value, int maxLength, String label) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw CmsContentApiException.validation(label + " exceeds " + maxLength + " characters.");
        }
        requireNoControlCharacters(normalized, label);
        return normalized;
    }

    private void requireNoControlCharacters(String value, String label) {
        if (value.codePoints().anyMatch(codePoint -> Character.isISOControl(codePoint))) {
            throw CmsContentApiException.validation(label + " must not contain control characters.");
        }
    }
}
