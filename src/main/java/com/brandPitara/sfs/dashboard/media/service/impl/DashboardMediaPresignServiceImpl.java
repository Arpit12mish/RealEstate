package com.brandPitara.sfs.dashboard.media.service.impl;

import com.brandPitara.sfs.dashboard.media.dto.DashboardPresignUploadRequest;
import com.brandPitara.sfs.dashboard.media.dto.DashboardPresignUploadResponse;
import com.brandPitara.sfs.dashboard.media.enums.DashboardMediaUploadType;
import com.brandPitara.sfs.dashboard.media.service.DashboardMediaPresignService;
import com.brandPitara.sfs.dashboard.project.service.DashboardProjectOwnershipService;
import com.brandPitara.sfs.dashboard.validator.DashboardMediaUploadValidator;
import com.brandPitara.sfs.media.service.MediaStorageService;
import com.brandPitara.sfs.media.service.PresignedUploadRequest;
import com.brandPitara.sfs.media.service.PresignedUploadResult;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.repository.CityRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardMediaPresignServiceImpl implements DashboardMediaPresignService {

    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    private final MediaStorageService mediaStorageService;
    private final DashboardProjectOwnershipService dashboardProjectOwnershipService;
    private final DashboardMediaUploadValidator uploadValidator;
    private final CityRepository cityRepository;
    private final ProjectRepository projectRepository;

    @Override
    public DashboardPresignUploadResponse createPresignedUpload(DashboardPresignUploadRequest request) {
        uploadValidator.validateContentType(request.uploadType(), request.contentType());
        uploadValidator.validateFileSize(request.uploadType(), request.fileSizeBytes());
        uploadValidator.validateContextIds(request.uploadType(), request.projectId(), request.builderId(), request.cityId());
        assertProjectUploadAllowed(request.uploadType(), request.projectId());
        assertCityExistsForCityUpload(request.uploadType(), request.cityId());

        String ext = extFromContentType(request.contentType());
        String key = buildKey(request.uploadType(), request.projectId(), request.builderId(), request.cityId(), ext);

        Map<String, String> requiredHeaders = Map.of(
                "Content-Type", request.contentType(),
                "Cache-Control", CACHE_CONTROL
        );

        PresignedUploadResult result = mediaStorageService.createPresignedUpload(
                new PresignedUploadRequest(key, request.contentType(), requiredHeaders)
        );

        return new DashboardPresignUploadResponse(
                result.uploadUrl(),
                result.publicUrl(),
                result.storageKey(),
                result.expiresInSeconds(),
                result.requiredHeaders()
        );
    }

    private void assertProjectUploadAllowed(DashboardMediaUploadType uploadType, Long projectId) {
        if (!uploadType.isProjectScoped()) {
            return;
        }

        if (uploadType == DashboardMediaUploadType.MASTER_PLAN_IMAGE) {
            assertProjectExists(projectId);
            return;
        }

        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
    }

    private void assertProjectExists(Long projectId) {
        if (projectId == null || projectRepository.findByIdAndDeletedFalse(projectId).isEmpty()) {
            throw new EntityNotFoundException("Project not found: " + projectId);
        }
    }

    private void assertCityExistsForCityUpload(DashboardMediaUploadType uploadType, Long cityId) {
        if (uploadType.isCityScoped() && !cityRepository.existsById(cityId)) {
            throw new EntityNotFoundException("City not found: " + cityId);
        }
    }

    // --- key building ---

    private String buildKey(DashboardMediaUploadType uploadType, Long projectId, Long builderId, Long cityId, String ext) {
        String filename = UUID.randomUUID() + "." + ext;
        return switch (uploadType) {
            case PROJECT_IMAGE    -> "dashboard/projects/" + projectId + "/images/" + filename;
            case FLOOR_PLAN_IMAGE -> "dashboard/projects/" + projectId + "/floor-plans/" + filename;
            case MASTER_PLAN_IMAGE -> "dashboard/projects/" + projectId + "/master-plan/" + filename;
            case CONNECTIVITY_MAP -> "dashboard/projects/" + projectId + "/connectivity/" + filename;
            case BROCHURE_PDF     -> "dashboard/projects/" + projectId + "/brochures/" + filename;
            case BUILDER_LOGO     -> "dashboard/builders/" + builderId + "/logos/" + filename;
            case CITY_COVER_IMAGE -> "dashboard/cities/" + cityId + "/cover/" + filename;
            case INSTAGRAM_REEL_THUMBNAIL -> "dashboard/instagram-reels/thumbnails/" + filename;
            case INSTAGRAM_REEL_PREVIEW_VIDEO -> "dashboard/instagram-reels/previews/" + filename;
            case HOME_LOTTIE_JSON -> "home/lottie/" + filename;
            case APP_SCREEN_LOTTIE_JSON -> "app/screen-content/lottie/" + filename;
            case APP_SCREEN_VIDEO -> "app/screen-content/video/" + filename;
            case BUILDER_HIGHLIGHT_IMAGE -> "dashboard/builders/" + builderId + "/highlights/images/" + filename;
            case BUILDER_HIGHLIGHT_THUMBNAIL -> "dashboard/builders/" + builderId + "/highlights/thumbnails/" + filename;
            case BUILDER_ANALYSIS_VIDEO_THUMBNAIL -> "dashboard/builders/" + builderId + "/highlights/analysis-thumbnails/" + filename;
        };
    }

    private String extFromContentType(String contentType) {
        return switch (contentType) {
            case "image/png"       -> "png";
            case "image/webp"      -> "webp";
            case "video/mp4"       -> "mp4";
            case "application/pdf" -> "pdf";
            case "application/json" -> "json";
            default                -> "jpg";
        };
    }
}
