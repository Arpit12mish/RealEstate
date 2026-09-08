package com.brandPitara.sfs.publiccontent.service;

import com.brandPitara.sfs.cms.content.domain.ContentType;
import com.brandPitara.sfs.cms.content.exception.CmsContentApiException;
import com.brandPitara.sfs.cms.content.document.CmsMediaReferenceService;
import com.brandPitara.sfs.cms.content.document.ContentDocument;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.publiccontent.dto.*;
import com.brandPitara.sfs.publiccontent.exception.PublicContentApiException;
import com.brandPitara.sfs.publiccontent.media.PublicMediaDeliveryException;
import com.brandPitara.sfs.publiccontent.media.PublicMediaUrlResolver;
import com.brandPitara.sfs.publiccontent.repository.PublicContentDetailView;
import com.brandPitara.sfs.publiccontent.repository.PublicContentListView;
import com.brandPitara.sfs.publiccontent.repository.PublicContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import com.brandPitara.sfs.cms.media.domain.CmsMediaType;

@Service
@RequiredArgsConstructor
public class PublicContentServiceImpl implements PublicContentService {
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAXIMUM_PAGE_SIZE = 50;
    public static final int MAX_SEARCH_QUERY_LENGTH = 100;

    private final PublicContentRepository repository;
    private final CmsMediaReferenceService mediaReferenceService;
    private final PublicContentEtag etagFactory;
    private final PublicMediaUrlResolver publicMediaUrlResolver;

    @Override
    @Transactional(readOnly = true)
    public PublicContentResult getBySlug(String slug, String ifNoneMatch) {
        PublicContentDetailView view = repository.findPublishedDetailBySlug(slug)
                .orElseThrow(PublicContentApiException::notFound);
        if (view.getPublishedAt() == null || !hasSupportedDocument(view)) {
            throw PublicContentApiException.temporarilyUnavailable();
        }
        String etag = etagFactory.create(view.getRevisionId(), view.getPublishedAt());
        if (etagFactory.matches(ifNoneMatch, etag)) {
            return new PublicContentResult(etag, true, null);
        }

        Map<Long, CmsMediaAssetEntity> assets;
        try {
            Map<Long, CmsMediaType> additional = new LinkedHashMap<>();
            if (validId(view.getCoverMediaAssetId())) additional.put(view.getCoverMediaAssetId(), CmsMediaType.IMAGE);
            if (validId(view.getPublicAuthorProfileMediaAssetId())) additional.put(view.getPublicAuthorProfileMediaAssetId(), CmsMediaType.IMAGE);
            assets = additional.isEmpty() ? mediaReferenceService.validateAndResolve(view.getContentDocument())
                    : mediaReferenceService.validateAndResolve(view.getContentDocument(), additional);
        } catch (CmsContentApiException exception) {
            throw PublicContentApiException.temporarilyUnavailable();
        }

        PublicContentSeoResponse seo = new PublicContentSeoResponse(
                view.getSeoTitle(), view.getSeoDescription(), view.getCanonicalUrl(),
                Boolean.TRUE.equals(view.getRobotsIndex()), Boolean.TRUE.equals(view.getRobotsFollow())
        );
        PublicContentDetailResponse response = new PublicContentDetailResponse(
                view.getPostId(), view.getSlug(), view.getContentType(), view.getTitle(), view.getExcerpt(),
                view.getReadingTimeMinutes(),
                author(view), category(view), tags(view), cover(view.getCoverMediaAssetId(), view.getCoverAltText(), assets),
                seo, view.getContentDocument(), view.getPublishedAt(), view.getRevisionCreatedAt(),
                toPublicMedia(assets)
        );
        return new PublicContentResult(etag, false, response);
    }

    private boolean hasSupportedDocument(PublicContentDetailView view) {
        ContentDocument document = view.getContentDocument();
        if (document == null || view.getContentDocumentSchemaVersion() == null) return false;
        int version = document.schemaVersion();
        return version == view.getContentDocumentSchemaVersion()
                && version >= ContentDocument.MINIMUM_READABLE_SCHEMA_VERSION
                && version <= ContentDocument.CURRENT_SCHEMA_VERSION;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicContentPageResponse list(
            ContentType contentType, String category, String author, String tag, String q, int page, int size
    ) {
        if (page < 0) throw new IllegalArgumentException("page must be zero or greater");
        if (size < 1 || size > MAXIMUM_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAXIMUM_PAGE_SIZE);
        }
        String categorySlug = normalizeFilter(category), authorSlug = normalizeFilter(author), tagSlug = normalizeFilter(tag);
        String searchTerm = normalizeSearchTerm(q);
        Page<PublicContentListView> result = categorySlug == null && authorSlug == null && tagSlug == null && searchTerm == null
                ? repository.findPublishedList(contentType, PageRequest.of(page, size))
                : repository.findPublishedListFiltered(
                        contentType, categorySlug, authorSlug, tagSlug, searchTerm, PageRequest.of(page, size));
        Map<Long, CmsMediaAssetEntity> covers = resolveListCovers(result.getContent());
        var items = result.getContent().stream()
                .map(view -> new PublicContentListItemResponse(
                        view.getPostId(), view.getSlug(), view.getContentType(), view.getTitle(),
                        view.getExcerpt(), view.getReadingTimeMinutes(),
                        new PublicAuthorSummaryResponse(view.getPublicAuthorId(), view.getPublicAuthorName(),
                                view.getPublicAuthorSlug(), view.getPublicAuthorDesignation(), null),
                        new PublicCategorySummaryResponse(view.getCategoryId(), view.getCategoryName(), view.getCategorySlug()),
                        cover(view.getCoverMediaAssetId(), view.getCoverAltText(), covers), view.getPublishedAt()
                ))
                .toList();
        return new PublicContentPageResponse(
                items, result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.isLast()
        );
    }

    private PublicAuthorSummaryResponse author(PublicContentDetailView view) {
        return new PublicAuthorSummaryResponse(view.getPublicAuthorId(), view.getPublicAuthorName(), view.getPublicAuthorSlug(),
                view.getPublicAuthorDesignation(), view.getPublicAuthorProfileMediaAssetId());
    }

    private PublicCategorySummaryResponse category(PublicContentDetailView view) {
        return new PublicCategorySummaryResponse(view.getCategoryId(), view.getCategoryName(), view.getCategorySlug());
    }

    private java.util.List<PublicTagSummaryResponse> tags(PublicContentDetailView view) {
        return view.getTagSnapshots() == null ? java.util.List.of() : view.getTagSnapshots().stream()
                .map(tag -> new PublicTagSummaryResponse(tag.id(), tag.name(), tag.slug())).toList();
    }

    private PublicContentCoverResponse cover(Long id, String alt, Map<Long, CmsMediaAssetEntity> assets) {
        if (!validId(id)) return null;
        CmsMediaAssetEntity asset = assets.get(id);
        if (asset == null) throw PublicContentApiException.temporarilyUnavailable();
        try {
            return new PublicContentCoverResponse(id, alt, publicMediaUrlResolver.resolve(asset),
                    asset.getContentType(), asset.getWidth(), asset.getHeight());
        } catch (PublicMediaDeliveryException e) {
            throw PublicContentApiException.temporarilyUnavailable();
        }
    }

    private Map<Long, CmsMediaAssetEntity> resolveListCovers(java.util.List<PublicContentListView> views) {
        Map<Long, CmsMediaType> expected = new LinkedHashMap<>();
        views.stream().map(PublicContentListView::getCoverMediaAssetId).filter(this::validId)
                .forEach(id -> expected.put(id, CmsMediaType.IMAGE));
        try {
            return mediaReferenceService.validateAndResolve(null, expected);
        } catch (CmsContentApiException e) {
            throw PublicContentApiException.temporarilyUnavailable();
        }
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        if (!normalized.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) throw new IllegalArgumentException("Invalid public content filter slug.");
        return normalized;
    }

    /**
     * Empty/blank q behaves like no search filter (returns null, same as an
     * absent param). A present q is trimmed, lowercased, and LIKE-wildcard
     * characters in the user's own text are escaped so "50%_off" is matched
     * literally rather than as a wildcard pattern - the returned value is
     * wrapped in %...% ready to bind against a "like :searchTerm escape '\'"
     * clause.
     */
    private String normalizeSearchTerm(String q) {
        if (q == null) return null;
        String trimmed = q.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.length() > MAX_SEARCH_QUERY_LENGTH) {
            throw new IllegalArgumentException("Search query must be " + MAX_SEARCH_QUERY_LENGTH + " characters or fewer.");
        }
        String escaped = trimmed.toLowerCase(java.util.Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private boolean validId(Long id) { return id != null && id > 0; }

    private Map<Long, PublicContentMediaResponse> toPublicMedia(
            Map<Long, CmsMediaAssetEntity> assets
    ) {
        if (assets.isEmpty()) return Map.of();
        Map<Long, PublicContentMediaResponse> response = new LinkedHashMap<>();
        try {
            assets.values().stream()
                    .sorted(Comparator.comparing(CmsMediaAssetEntity::getId))
                    .forEach(asset -> response.put(asset.getId(), new PublicContentMediaResponse(
                            asset.getId(), asset.getMediaType(), asset.getContentType(), asset.getSizeBytes(),
                            asset.getWidth(), asset.getHeight(), asset.getDurationMillis(),
                            publicMediaUrlResolver.resolve(asset)
                    )));
        } catch (PublicMediaDeliveryException invalidDelivery) {
            throw PublicContentApiException.temporarilyUnavailable();
        }
        return Map.copyOf(response);
    }
}
