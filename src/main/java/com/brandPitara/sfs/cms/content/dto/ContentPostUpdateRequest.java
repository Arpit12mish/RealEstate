package com.brandPitara.sfs.cms.content.dto;

import com.brandPitara.sfs.cms.content.domain.ContentType;
import com.brandPitara.sfs.cms.content.domain.ContentValidation;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record ContentPostUpdateRequest(
        @NotNull @PositiveOrZero Long version,
        @NotNull ContentType contentType,
        @NotBlank
        @Size(min = ContentValidation.TITLE_MIN, max = ContentValidation.TITLE_MAX)
        @Pattern(regexp = ContentValidation.NO_CONTROL_CHARACTERS,
                message = "title must not contain control characters")
        String title,
        @NotBlank @Size(max = ContentValidation.SLUG_MAX) String slug,
        @Size(max = ContentValidation.EXCERPT_MAX)
        @Pattern(regexp = ContentValidation.NO_CONTROL_CHARACTERS,
                message = "excerpt must not contain control characters")
        String excerpt,
        @Size(max = ContentValidation.SEO_TITLE_MAX)
        @Pattern(regexp = ContentValidation.NO_CONTROL_CHARACTERS,
                message = "seoTitle must not contain control characters")
        String seoTitle,
        @Size(max = ContentValidation.SEO_DESCRIPTION_MAX)
        @Pattern(regexp = ContentValidation.NO_CONTROL_CHARACTERS,
                message = "seoDescription must not contain control characters")
        String seoDescription,
        @Size(max = ContentValidation.CANONICAL_URL_MAX) String canonicalUrl,
        @NotNull Boolean robotsIndex,
        @NotNull Boolean robotsFollow,
        Long publicAuthorId,
        Long categoryId,
        @Size(max = 15) Set<Long> tagIds,
        Long coverMediaAssetId,
        @Size(max = 300) String coverAltText,
        @Min(ContentValidation.READING_TIME_MIN) @Max(ContentValidation.READING_TIME_MAX) Integer readingTimeMinutes
) {
    public ContentPostUpdateRequest(Long version,ContentType contentType,String title,String slug,String excerpt,
            String seoTitle,String seoDescription,String canonicalUrl,Boolean robotsIndex,Boolean robotsFollow) {
        this(version,contentType,title,slug,excerpt,seoTitle,seoDescription,canonicalUrl,robotsIndex,robotsFollow,
                null,null,Set.of(),null,null,null);
    }
    @JsonAnySetter
    public void rejectUnknownField(String field, Object ignoredValue) {
        throw new IllegalArgumentException("Unknown content-update field: " + field);
    }
}
