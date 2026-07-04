package com.brandPitara.sfs.builderhighlight.service.impl;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.buildercredibility.dto.BuilderCredibilitySummaryResponse;
import com.brandPitara.sfs.buildercredibility.service.BuilderCredibilityService;
import com.brandPitara.sfs.builderhighlight.dto.request.BuilderHighlightItemRequest;
import com.brandPitara.sfs.builderhighlight.dto.request.BuilderHighlightPointRequest;
import com.brandPitara.sfs.builderhighlight.dto.request.BuilderHighlightStatusUpdateRequest;
import com.brandPitara.sfs.builderhighlight.dto.response.BuilderHighlightBuilderResponse;
import com.brandPitara.sfs.builderhighlight.dto.response.BuilderHighlightItemResponse;
import com.brandPitara.sfs.builderhighlight.dto.response.BuilderHighlightPublicItemResponse;
import com.brandPitara.sfs.builderhighlight.dto.response.BuilderHighlightSectionResponse;
import com.brandPitara.sfs.builderhighlight.dto.response.BuilderHighlightsScreenResponse;
import com.brandPitara.sfs.builderhighlight.entity.BuilderHighlightItemEntity;
import com.brandPitara.sfs.builderhighlight.entity.BuilderHighlightPointEntity;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightMediaType;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightSourceType;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightStatus;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightType;
import com.brandPitara.sfs.builderhighlight.mapper.BuilderHighlightMapper;
import com.brandPitara.sfs.builderhighlight.repository.BuilderHighlightItemRepository;
import com.brandPitara.sfs.builderhighlight.service.BuilderHighlightService;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.repository.CityRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BuilderHighlightServiceImpl implements BuilderHighlightService {

    private static final int PUBLIC_SECTION_PREVIEW_SIZE = 5;
    private static final int PUBLIC_MAX_PAGE_SIZE = 20;
    private static final int DASHBOARD_MAX_PAGE_SIZE = 50;
    private static final String KEY_BUILDER_HIGHLIGHT = "BUILDER_HIGHLIGHT";
    private static final String KEY_HOME = "HOME";

    private final BuilderRepository builderRepository;
    private final ProjectRepository projectRepository;
    private final CityRepository cityRepository;
    private final BuilderHighlightItemRepository itemRepository;
    private final BuilderHighlightMapper mapper;
    private final BuilderCredibilityService builderCredibilityService;
    private final DashboardCurrentUserService dashboardCurrentUserService;
    private final ContentVersionService contentVersionService;

    @Override
    @Transactional(readOnly = true)
    public BuilderHighlightsScreenResponse publicGetScreen(Long builderId) {
        BuilderCredibilitySummaryResponse credibility = builderCredibilityService.publicGetCredibilitySummary(builderId);

        List<BuilderHighlightSectionResponse> sections = Arrays.stream(BuilderHighlightType.values())
            .map(type -> BuilderHighlightSectionResponse.builder()
                .type(type)
                .title(type.getSectionTitle())
                .subtitle(type.getSectionSubtitle())
                .items(publicListItems(builderId, type, publicPageable(0, PUBLIC_SECTION_PREVIEW_SIZE)).getContent())
                .build())
            .toList();

        return BuilderHighlightsScreenResponse.builder()
            .builder(BuilderHighlightBuilderResponse.builder()
                .id(credibility.getBuilderId())
                .name(credibility.getBuilderName())
                .logoUrl(credibility.getBuilderLogoUrl())
                .credibilityScore(credibility.getCredibilityScore())
                .credibilityLabel(credibility.getCredibilityLabel())
                .build())
            .sections(sections)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BuilderHighlightPublicItemResponse> publicListItems(Long builderId, BuilderHighlightType type, Pageable pageable) {
        getPublicBuilder(builderId);
        BuilderHighlightType safeType = requireType(type);
        int page = pageable != null && pageable.isPaged() ? pageable.getPageNumber() : 0;
        int size = pageable != null && pageable.isPaged() ? pageable.getPageSize() : PUBLIC_MAX_PAGE_SIZE;
        return itemRepository
            .findByBuilder_IdAndHighlightTypeAndStatusAndPublicVisibleTrueAndActiveTrueAndDeletedAtIsNull(
                builderId,
                safeType,
                BuilderHighlightStatus.PUBLISHED,
                publicPageable(page, size)
            )
            .map(mapper::toPublicResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BuilderHighlightPublicItemResponse publicGetItem(Long builderId, Long itemId) {
        getPublicBuilder(builderId);
        return itemRepository
            .findByIdAndBuilder_IdAndStatusAndPublicVisibleTrueAndActiveTrueAndDeletedAtIsNull(
                itemId,
                builderId,
                BuilderHighlightStatus.PUBLISHED
            )
            .map(mapper::toPublicResponse)
            .orElseThrow(() -> new NotFoundException("Builder highlight item not found: " + itemId));
    }

    @Override
    @Transactional
    public BuilderHighlightItemResponse create(Long builderId, BuilderHighlightItemRequest request) {
        DashboardUserEntity user = dashboardCurrentUserService.getCurrentUserOrThrow();
        BuilderEntity builder = getDashboardBuilder(builderId);
        validateRequest(builderId, request);

        BuilderHighlightStatus status = request.getStatus() != null ? request.getStatus() : BuilderHighlightStatus.DRAFT;
        assertStatusAllowedForRole(status, user.getRole());

        BuilderHighlightItemEntity entity = BuilderHighlightItemEntity.builder()
            .builder(builder)
            .project(resolveProject(builderId, request.getProjectId()))
            .city(resolveCity(request.getCityId()))
            .createdBy(user.getId())
            .updatedBy(user.getId())
            .build();

        applyRequest(entity, request, status);
        applyApprovalMetadataIfPublished(entity, user);
        replacePoints(entity, request.getPoints());

        BuilderHighlightItemEntity saved = itemRepository.save(entity);
        bumpContentVersions(saved);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BuilderHighlightItemResponse update(Long builderId, Long itemId, BuilderHighlightItemRequest request) {
        DashboardUserEntity user = dashboardCurrentUserService.getCurrentUserOrThrow();
        BuilderHighlightItemEntity entity = findDashboardItem(builderId, itemId);
        validateRequest(builderId, request);

        BuilderHighlightStatus status = request.getStatus() != null ? request.getStatus() : entity.getStatus();
        assertStatusAllowedForRole(status, user.getRole());

        entity.setProject(resolveProject(builderId, request.getProjectId()));
        entity.setCity(resolveCity(request.getCityId()));
        entity.setUpdatedBy(user.getId());

        applyRequest(entity, request, status);
        applyApprovalMetadataIfPublished(entity, user);
        replacePoints(entity, request.getPoints());

        BuilderHighlightItemEntity saved = itemRepository.save(entity);
        bumpContentVersions(saved);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BuilderHighlightItemResponse> dashboardList(
        Long builderId,
        BuilderHighlightType type,
        BuilderHighlightStatus status,
        Pageable pageable
    ) {
        getDashboardBuilder(builderId);
        Pageable safePageable = PageRequest.of(
            pageable != null && pageable.isPaged() ? Math.max(pageable.getPageNumber(), 0) : 0,
            pageable != null && pageable.isPaged()
                ? Math.min(Math.max(pageable.getPageSize(), 1), DASHBOARD_MAX_PAGE_SIZE)
                : 20,
            dashboardSort()
        );
        return itemRepository.dashboardList(builderId, type, status, safePageable)
            .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BuilderHighlightItemResponse dashboardGet(Long builderId, Long itemId) {
        return mapper.toResponse(findDashboardItem(builderId, itemId));
    }

    @Override
    @Transactional
    public void softDelete(Long builderId, Long itemId) {
        BuilderHighlightItemEntity entity = findDashboardItem(builderId, itemId);
        entity.setDeletedAt(OffsetDateTime.now());
        entity.setActive(false);
        entity.setPublicVisible(false);
        itemRepository.save(entity);
        bumpContentVersions(entity);
    }

    @Override
    @Transactional
    public BuilderHighlightItemResponse updateStatus(Long builderId, Long itemId, BuilderHighlightStatusUpdateRequest request) {
        DashboardUserEntity user = dashboardCurrentUserService.getCurrentUserOrThrow();
        BuilderHighlightItemEntity entity = findDashboardItem(builderId, itemId);
        BuilderHighlightStatus nextStatus = request.getStatus();

        assertTransitionAllowed(entity.getStatus(), nextStatus);
        assertStatusAllowedForRole(nextStatus, user.getRole());
        entity.setStatus(nextStatus);
        entity.setUpdatedBy(user.getId());

        if (request.getPublicVisible() != null) {
            entity.setPublicVisible(request.getPublicVisible());
        }
        if (nextStatus == BuilderHighlightStatus.PUBLISHED) {
            entity.setPublicVisible(true);
            entity.setActive(true);
            if (entity.getPublishedAt() == null) {
                entity.setPublishedAt(OffsetDateTime.now());
            }
            entity.setApprovedBy(user.getId());
            entity.setApprovedAt(OffsetDateTime.now());
        }
        if (nextStatus == BuilderHighlightStatus.ARCHIVED) {
            entity.setPublicVisible(false);
        }

        BuilderHighlightItemEntity saved = itemRepository.save(entity);
        bumpContentVersions(saved);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BuilderHighlightItemResponse setPublished(Long builderId, Long itemId, boolean value) {
        DashboardUserEntity user = dashboardCurrentUserService.getCurrentUserOrThrow();
        BuilderHighlightItemEntity entity = findDashboardItem(builderId, itemId);
        assertStatusAllowedForRole(BuilderHighlightStatus.PUBLISHED, user.getRole());

        if (value) {
            entity.setStatus(BuilderHighlightStatus.PUBLISHED);
            entity.setPublicVisible(true);
            entity.setActive(true);
            if (entity.getPublishedAt() == null) {
                entity.setPublishedAt(OffsetDateTime.now());
            }
            entity.setApprovedBy(user.getId());
            entity.setApprovedAt(OffsetDateTime.now());
        } else {
            entity.setStatus(BuilderHighlightStatus.ARCHIVED);
            entity.setPublicVisible(false);
        }
        entity.setUpdatedBy(user.getId());

        BuilderHighlightItemEntity saved = itemRepository.save(entity);
        bumpContentVersions(saved);
        return mapper.toResponse(saved);
    }

    private void applyRequest(
        BuilderHighlightItemEntity entity,
        BuilderHighlightItemRequest request,
        BuilderHighlightStatus status
    ) {
        entity.setHighlightType(request.getHighlightType());
        entity.setSourceType(request.getSourceType());
        entity.setMediaType(request.getMediaType());
        entity.setTitle(cleanRequired(request.getTitle(), "title is required"));
        entity.setSubtitle(clean(request.getSubtitle()));
        entity.setSummary(clean(request.getSummary()));
        entity.setBody(clean(request.getBody()));
        entity.setTagLabel(clean(request.getTagLabel()));
        entity.setTagType(clean(request.getTagType()));
        entity.setThumbnailUrl(clean(request.getThumbnailUrl()));
        entity.setImageUrl(clean(request.getImageUrl()));
        entity.setVideoUrl(clean(request.getVideoUrl()));
        entity.setYoutubeVideoId(clean(request.getYoutubeVideoId()));
        entity.setExternalUrl(clean(request.getExternalUrl()));
        entity.setWebviewEnabled(request.getWebviewEnabled() != null ? request.getWebviewEnabled() : false);
        entity.setPublisherName(clean(request.getPublisherName()));
        entity.setAuthorLabel(clean(request.getAuthorLabel()));
        entity.setReadTimeMinutes(request.getReadTimeMinutes() != null ? request.getReadTimeMinutes() : 0);
        entity.setPublishedAt(request.getPublishedAt());
        entity.setFeatured(request.getFeatured() != null ? request.getFeatured() : false);
        entity.setVerified(request.getVerified() != null ? request.getVerified() : false);
        entity.setPublicVisible(request.getPublicVisible() != null ? request.getPublicVisible() : false);
        entity.setActive(request.getActive() != null ? request.getActive() : true);
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        entity.setStatus(status);
    }

    private void replacePoints(BuilderHighlightItemEntity entity, List<BuilderHighlightPointRequest> requests) {
        entity.getPoints().clear();
        if (requests == null || requests.isEmpty()) {
            return;
        }
        for (BuilderHighlightPointRequest request : requests) {
            BuilderHighlightPointEntity point = BuilderHighlightPointEntity.builder()
                .highlightItem(entity)
                .pointType(request.getPointType())
                .title(clean(request.getTitle()))
                .text(clean(request.getText()))
                .iconKey(clean(request.getIconKey()))
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
            entity.getPoints().add(point);
        }
    }

    private void validateRequest(Long builderId, BuilderHighlightItemRequest request) {
        if (request.getHighlightType() == null) {
            throw new IllegalArgumentException("highlightType is required");
        }
        if (request.getSourceType() == null) {
            throw new IllegalArgumentException("sourceType is required");
        }
        if (request.getMediaType() == null) {
            throw new IllegalArgumentException("mediaType is required");
        }
        cleanRequired(request.getTitle(), "title is required");
        if (request.getReadTimeMinutes() != null && request.getReadTimeMinutes() < 0) {
            throw new IllegalArgumentException("readTimeMinutes must be >= 0");
        }
        if (request.getSortOrder() != null && request.getSortOrder() < 0) {
            throw new IllegalArgumentException("sortOrder must be >= 0");
        }
        if (request.getMediaType() == BuilderHighlightMediaType.WEBVIEW && !StringUtils.hasText(request.getExternalUrl())) {
            throw new IllegalArgumentException("externalUrl is required when mediaType is WEBVIEW");
        }
        if (request.getMediaType() == BuilderHighlightMediaType.YOUTUBE
            && !StringUtils.hasText(request.getYoutubeVideoId())
            && !StringUtils.hasText(request.getVideoUrl())) {
            throw new IllegalArgumentException("youtubeVideoId or videoUrl is required when mediaType is YOUTUBE");
        }
        if (request.getHighlightType() == BuilderHighlightType.NEWS_ARTICLE
            && request.getSourceType() == BuilderHighlightSourceType.EXTERNAL_NEWS
            && !StringUtils.hasText(request.getPublisherName())) {
            throw new IllegalArgumentException("publisherName is required for external news articles");
        }
        if (request.getProjectId() != null) {
            resolveProject(builderId, request.getProjectId());
        }
        if (request.getCityId() != null) {
            resolveCity(request.getCityId());
        }
    }

    private BuilderEntity getDashboardBuilder(Long builderId) {
        return builderRepository.findByIdAndDeletedFalse(builderId)
            .orElseThrow(() -> new EntityNotFoundException("Builder not found: " + builderId));
    }

    private BuilderEntity getPublicBuilder(Long builderId) {
        BuilderEntity builder = getDashboardBuilder(builderId);
        if (!Boolean.TRUE.equals(builder.getPublished()) || !Boolean.TRUE.equals(builder.getActive())) {
            throw new NotFoundException("Builder not found: " + builderId);
        }
        return builder;
    }

    private ProjectEntity resolveProject(Long builderId, Long projectId) {
        if (projectId == null) {
            return null;
        }
        ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
            .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));
        if (project.getBuilder() == null || !builderId.equals(project.getBuilder().getId())) {
            throw new IllegalArgumentException("projectId does not belong to builderId");
        }
        return project;
    }

    private CityEntity resolveCity(Long cityId) {
        if (cityId == null) {
            return null;
        }
        return cityRepository.findById(cityId)
            .orElseThrow(() -> new EntityNotFoundException("City not found: " + cityId));
    }

    private BuilderHighlightItemEntity findDashboardItem(Long builderId, Long itemId) {
        getDashboardBuilder(builderId);
        return itemRepository.findByIdAndBuilder_IdAndDeletedAtIsNull(itemId, builderId)
            .orElseThrow(() -> new EntityNotFoundException("Builder highlight item not found: " + itemId));
    }

    private BuilderHighlightType requireType(BuilderHighlightType type) {
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        return type;
    }

    private void assertTransitionAllowed(BuilderHighlightStatus current, BuilderHighlightStatus next) {
        if (current == next) {
            return;
        }
        boolean allowed = switch (current) {
            case DRAFT -> next == BuilderHighlightStatus.PENDING_REVIEW
                || next == BuilderHighlightStatus.PUBLISHED
                || next == BuilderHighlightStatus.ARCHIVED;
            case PENDING_REVIEW -> next == BuilderHighlightStatus.PUBLISHED
                || next == BuilderHighlightStatus.ARCHIVED
                || next == BuilderHighlightStatus.DRAFT;
            case PUBLISHED -> next == BuilderHighlightStatus.ARCHIVED;
            case ARCHIVED -> next == BuilderHighlightStatus.DRAFT;
        };
        if (!allowed) {
            throw new IllegalStateException("Invalid builder highlight status transition: " + current + " -> " + next);
        }
    }

    private void assertStatusAllowedForRole(BuilderHighlightStatus status, DashboardRole role) {
        if (status == BuilderHighlightStatus.PUBLISHED && role == DashboardRole.DATA_ENTRY) {
            throw new IllegalStateException("DATA_ENTRY cannot publish builder highlights");
        }
    }

    private void applyApprovalMetadataIfPublished(BuilderHighlightItemEntity entity, DashboardUserEntity user) {
        if (entity.getStatus() == BuilderHighlightStatus.PUBLISHED) {
            entity.setPublicVisible(true);
            if (entity.getPublishedAt() == null) {
                entity.setPublishedAt(OffsetDateTime.now());
            }
            entity.setApprovedBy(user.getId());
            entity.setApprovedAt(OffsetDateTime.now());
        }
    }

    private Pageable publicPageable(int page, int size) {
        return PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), PUBLIC_MAX_PAGE_SIZE),
            publicSort()
        );
    }

    private Sort publicSort() {
        return Sort.by(Sort.Order.desc("featured"))
            .and(Sort.by(Sort.Order.asc("sortOrder")))
            .and(Sort.by(Sort.Order.desc("publishedAt")))
            .and(Sort.by(Sort.Order.desc("id")));
    }

    private Sort dashboardSort() {
        return Sort.by(Sort.Order.asc("sortOrder"))
            .and(Sort.by(Sort.Order.desc("updatedAt")))
            .and(Sort.by(Sort.Order.desc("id")));
    }

    private void bumpContentVersions(BuilderHighlightItemEntity entity) {
        contentVersionService.bump(KEY_BUILDER_HIGHLIGHT);
        if (entity != null && entity.isPubliclyVisible()) {
            contentVersionService.bump(KEY_HOME);
        }
    }

    private String cleanRequired(String value, String message) {
        String cleaned = clean(value);
        if (!StringUtils.hasText(cleaned)) {
            throw new IllegalArgumentException(message);
        }
        return cleaned;
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
