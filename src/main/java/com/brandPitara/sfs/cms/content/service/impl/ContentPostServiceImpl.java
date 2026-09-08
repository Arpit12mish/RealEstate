package com.brandPitara.sfs.cms.content.service.impl;

import com.brandPitara.sfs.cms.content.domain.ContentStatus;
import com.brandPitara.sfs.cms.content.domain.ContentType;
import com.brandPitara.sfs.cms.content.dto.ContentPostCreateRequest;
import com.brandPitara.sfs.cms.content.dto.ContentPostDetailResponse;
import com.brandPitara.sfs.cms.content.dto.ContentPostListResponse;
import com.brandPitara.sfs.cms.content.dto.ContentPostUpdateRequest;
import com.brandPitara.sfs.cms.content.entity.ContentPostEntity;
import com.brandPitara.sfs.cms.content.exception.CmsContentApiException;
import com.brandPitara.sfs.cms.content.repository.ContentPostRepository;
import com.brandPitara.sfs.cms.content.service.ContentMetadataNormalizer;
import com.brandPitara.sfs.cms.content.service.ContentPostService;
import com.brandPitara.sfs.cms.content.slug.ContentSlugService;
import com.brandPitara.sfs.cms.security.CmsContentAccessPolicy;
import com.brandPitara.sfs.cms.workflow.service.ContentWorkflowTransitionPolicy;
import com.brandPitara.sfs.cms.author.entity.CmsPublicAuthorEntity;
import com.brandPitara.sfs.cms.author.repository.CmsPublicAuthorRepository;
import com.brandPitara.sfs.cms.taxonomy.entity.*;
import com.brandPitara.sfs.cms.taxonomy.repository.*;
import com.brandPitara.sfs.cms.media.domain.*;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.cms.media.repository.CmsMediaAssetRepository;
import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Objects;
import java.util.*;

@Service
public class ContentPostServiceImpl implements ContentPostService {

    private final ContentPostRepository contentPostRepository;
    private final DashboardUserRepository dashboardUserRepository;
    private final DashboardCurrentUserService currentUserService;
    private final CmsContentAccessPolicy accessPolicy;
    private final ContentSlugService slugService;
    private final ContentMetadataNormalizer metadataNormalizer;
    private final ContentWorkflowTransitionPolicy transitionPolicy;
    private final CmsPublicAuthorRepository authorRepository;
    private final CmsContentCategoryRepository categoryRepository;
    private final CmsContentTagRepository tagRepository;
    private final CmsMediaAssetRepository mediaRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public ContentPostServiceImpl(ContentPostRepository contentPostRepository,
            DashboardUserRepository dashboardUserRepository, DashboardCurrentUserService currentUserService,
            CmsContentAccessPolicy accessPolicy, ContentSlugService slugService,
            ContentMetadataNormalizer metadataNormalizer, ContentWorkflowTransitionPolicy transitionPolicy,
            CmsPublicAuthorRepository authorRepository, CmsContentCategoryRepository categoryRepository,
            CmsContentTagRepository tagRepository, CmsMediaAssetRepository mediaRepository) {
        this.contentPostRepository = contentPostRepository;
        this.dashboardUserRepository = dashboardUserRepository;
        this.currentUserService = currentUserService;
        this.accessPolicy = accessPolicy;
        this.slugService = slugService;
        this.metadataNormalizer = metadataNormalizer;
        this.transitionPolicy = transitionPolicy;
        this.authorRepository = authorRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.mediaRepository = mediaRepository;
    }

    public ContentPostServiceImpl(ContentPostRepository contentPostRepository,
            DashboardUserRepository dashboardUserRepository, DashboardCurrentUserService currentUserService,
            CmsContentAccessPolicy accessPolicy, ContentSlugService slugService,
            ContentMetadataNormalizer metadataNormalizer, ContentWorkflowTransitionPolicy transitionPolicy) {
        this(contentPostRepository, dashboardUserRepository, currentUserService, accessPolicy, slugService,
                metadataNormalizer, transitionPolicy, null, null, null, null);
    }

    @Override
    @Transactional
    public ContentPostDetailResponse create(
            ContentPostCreateRequest request,
            Authentication authentication
    ) {
        accessPolicy.assertCanCreate(authentication);
        DashboardUserEntity currentUser = currentUserService.getCurrentUserOrThrow();
        DashboardUserEntity actor = dashboardUserRepository.getReferenceById(currentUser.getId());

        String normalizedTitle = metadataNormalizer.title(request.title());
        String slug = slugService.resolveForCreate(request.slug(), normalizedTitle);
        ContentPostEntity post = ContentPostEntity.builder()
                .contentType(request.contentType())
                .status(ContentStatus.DRAFT)
                .title(normalizedTitle)
                .slug(slug)
                .excerpt(metadataNormalizer.excerpt(request.excerpt()))
                .readingTimeMinutes(request.readingTimeMinutes())
                .publicAuthor(resolveAuthor(request.publicAuthorId()))
                .category(resolveCategory(request.categoryId()))
                .tags(resolveTags(request.tagIds()))
                .coverMediaAsset(resolveCover(request.coverMediaAssetId()))
                .coverAltText(coverAlt(request.coverAltText()))
                .contentOwner(actor)
                .createdBy(actor)
                .updatedBy(actor)
                .seoTitle(metadataNormalizer.seoTitle(request.seoTitle()))
                .seoDescription(metadataNormalizer.seoDescription(request.seoDescription()))
                .canonicalUrl(metadataNormalizer.canonicalUrl(request.canonicalUrl()))
                .robotsIndex(request.robotsIndex() == null || request.robotsIndex())
                .robotsFollow(request.robotsFollow() == null || request.robotsFollow())
                .build();

        try {
            ContentPostEntity saved = contentPostRepository.saveAndFlush(post);
            return ContentPostDetailResponse.from(saved);
        } catch (DataIntegrityViolationException exception) {
            if (isSlugUniqueViolation(exception)) {
                throw CmsContentApiException.slugConflict(slug);
            }
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ContentPostDetailResponse get(Long contentId, Authentication authentication) {
        ContentPostEntity post = findDetailed(contentId);
        accessPolicy.assertCanView(authentication, post.getContentOwner().getId());
        return ContentPostDetailResponse.from(post);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContentPostListResponse> list(
            ContentType contentType,
            ContentStatus status,
            Long ownerId,
            String search,
            Pageable pageable,
            Authentication authentication
    ) {
        DashboardUserEntity currentUser = currentUserService.getCurrentUserOrThrow();
        Long effectiveOwnerId = ownerId;
        if (!accessPolicy.canViewAll(authentication)) {
            accessPolicy.assertCanView(authentication, currentUser.getId());
            effectiveOwnerId = currentUser.getId();
        }

        String normalizedSearch = StringUtils.hasText(search)
                ? search.trim().toLowerCase(Locale.ROOT)
                : null;
        String searchPattern = normalizedSearch == null
                ? null
                : "%" + escapeLike(normalizedSearch) + "%";
        return contentPostRepository.findList(
                contentType, status, effectiveOwnerId, searchPattern, pageable
        ).map(ContentPostListResponse::from);
    }

    @Override
    @Transactional
    public ContentPostDetailResponse update(
            Long contentId,
            ContentPostUpdateRequest request,
            Authentication authentication
    ) {
        ContentPostEntity post = findDetailed(contentId);
        accessPolicy.assertCanEdit(authentication, post.getContentOwner().getId());
        if (!transitionPolicy.isEditable(post.getStatus())) {
            throw CmsContentApiException.notEditable();
        }
        if (!Objects.equals(post.getVersion(), request.version())) {
            throw CmsContentApiException.versionConflict();
        }

        String slug = slugService.resolveForUpdate(request.slug(), contentId);
        if (post.getPublishedAt() != null && !post.getSlug().equals(slug)) {
            throw CmsContentApiException.slugLocked();
        }
        DashboardUserEntity currentUser = currentUserService.getCurrentUserOrThrow();
        DashboardUserEntity actor = dashboardUserRepository.getReferenceById(currentUser.getId());

        post.setContentType(request.contentType());
        post.setTitle(metadataNormalizer.title(request.title()));
        post.setSlug(slug);
        post.setExcerpt(metadataNormalizer.excerpt(request.excerpt()));
        post.setReadingTimeMinutes(request.readingTimeMinutes());
        post.setPublicAuthor(resolveAuthor(request.publicAuthorId()));
        post.setCategory(resolveCategory(request.categoryId()));
        post.setTags(resolveTags(request.tagIds()));
        post.setCoverMediaAsset(resolveCover(request.coverMediaAssetId()));
        post.setCoverAltText(coverAlt(request.coverAltText()));
        post.setSeoTitle(metadataNormalizer.seoTitle(request.seoTitle()));
        post.setSeoDescription(metadataNormalizer.seoDescription(request.seoDescription()));
        post.setCanonicalUrl(metadataNormalizer.canonicalUrl(request.canonicalUrl()));
        post.setRobotsIndex(request.robotsIndex());
        post.setRobotsFollow(request.robotsFollow());
        post.setUpdatedBy(actor);

        try {
            ContentPostEntity saved = contentPostRepository.saveAndFlush(post);
            return ContentPostDetailResponse.from(saved);
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException exception) {
            throw CmsContentApiException.versionConflict();
        } catch (DataIntegrityViolationException exception) {
            if (isSlugUniqueViolation(exception)) {
                throw CmsContentApiException.slugConflict(slug);
            }
            throw exception;
        }
    }

    // Every other constraint on content_post (e.g. chk_content_post_document_schema_version)
    // also throws DataIntegrityViolationException on insert/update - only the slug unique
    // constraint should ever be reported to the caller as a slug conflict. Reporting any other
    // constraint violation as "slug already exists" hides the real cause (see V164 migration,
    // added after this exact mislabeling made a schema-version constraint failure look like a
    // slug collision).
    private boolean isSlugUniqueViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && message.contains("uk_content_post_slug")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private ContentPostEntity findDetailed(Long contentId) {
        return contentPostRepository.findDetailedById(contentId)
                .orElseThrow(() -> CmsContentApiException.notFound(contentId));
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private CmsPublicAuthorEntity resolveAuthor(Long id) {
        if (id == null) return null;
        CmsPublicAuthorEntity value = authorRepository.findById(id)
                .orElseThrow(() -> CmsContentApiException.validation("Public author not found: " + id));
        if (!Boolean.TRUE.equals(value.getActive())) throw CmsContentApiException.validation("Public author is inactive: " + id);
        return value;
    }

    private CmsContentCategoryEntity resolveCategory(Long id) {
        if (id == null) return null;
        CmsContentCategoryEntity value = categoryRepository.findById(id)
                .orElseThrow(() -> CmsContentApiException.validation("CMS category not found: " + id));
        if (!Boolean.TRUE.equals(value.getActive())) throw CmsContentApiException.validation("CMS category is inactive: " + id);
        return value;
    }

    private Set<CmsContentTagEntity> resolveTags(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return new LinkedHashSet<>();
        if (ids.size() > 15 || ids.stream().anyMatch(Objects::isNull)) {
            throw CmsContentApiException.validation("A post may contain at most 15 valid tags.");
        }
        List<CmsContentTagEntity> found = tagRepository.findAllById(ids);
        if (found.size() != ids.size()) throw CmsContentApiException.validation("One or more CMS tags were not found.");
        if (found.stream().anyMatch(tag -> !Boolean.TRUE.equals(tag.getActive()))) {
            throw CmsContentApiException.validation("Inactive CMS tags cannot be assigned.");
        }
        found.sort(Comparator.comparing(CmsContentTagEntity::getId));
        return new LinkedHashSet<>(found);
    }

    private CmsMediaAssetEntity resolveCover(Long id) {
        if (id == null) return null;
        CmsMediaAssetEntity value = mediaRepository.findById(id)
                .orElseThrow(() -> CmsContentApiException.mediaNotFound(id));
        if (value.getStatus() != CmsMediaStatus.READY) throw CmsContentApiException.mediaNotReady(id);
        if (value.getMediaType() != CmsMediaType.IMAGE) throw CmsContentApiException.mediaTypeMismatch(id, "IMAGE");
        return value;
    }

    private String coverAlt(String value) {
        if (!StringUtils.hasText(value)) return null;
        String normalized = value.trim();
        if (normalized.length() > 300 || normalized.codePoints().anyMatch(Character::isISOControl)) {
            throw CmsContentApiException.validation("Cover alt text must be plain text of at most 300 characters.");
        }
        return normalized;
    }
}
