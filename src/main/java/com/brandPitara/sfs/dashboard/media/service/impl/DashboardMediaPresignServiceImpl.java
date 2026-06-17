package com.brandPitara.sfs.dashboard.media.service.impl;

import com.brandPitara.sfs.dashboard.media.dto.DashboardPresignUploadRequest;
import com.brandPitara.sfs.dashboard.media.dto.DashboardPresignUploadResponse;
import com.brandPitara.sfs.dashboard.media.enums.DashboardMediaUploadType;
import com.brandPitara.sfs.dashboard.media.service.DashboardMediaPresignService;
import com.brandPitara.sfs.dashboard.project.service.DashboardProjectOwnershipService;
import com.brandPitara.sfs.dashboard.validator.DashboardMediaUploadValidator;
import com.brandPitara.sfs.media.config.S3Properties;
import com.brandPitara.sfs.repository.CityRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardMediaPresignServiceImpl implements DashboardMediaPresignService {

    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;
    private final DashboardProjectOwnershipService dashboardProjectOwnershipService;
    private final DashboardMediaUploadValidator uploadValidator;
    private final CityRepository cityRepository;

    @Override
    public DashboardPresignUploadResponse createPresignedUpload(DashboardPresignUploadRequest request) {
        uploadValidator.validateContentType(request.uploadType(), request.contentType());
        uploadValidator.validateFileSize(request.uploadType(), request.fileSizeBytes());
        uploadValidator.validateContextIds(request.uploadType(), request.projectId(), request.builderId(), request.cityId());
        assertOwnershipForProjectUpload(request.uploadType(), request.projectId());
        assertCityExistsForCityUpload(request.uploadType(), request.cityId());

        String ext = extFromContentType(request.contentType());
        String key = buildKey(request.uploadType(), request.projectId(), request.builderId(), request.cityId(), ext);

        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .contentType(request.contentType())
                .cacheControl(CACHE_CONTROL)
                .build();

        PutObjectPresignRequest presignReq = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(s3Properties.getPresignExpirySeconds()))
                .putObjectRequest(putReq)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignReq);

        Map<String, String> requiredHeaders = Map.of(
                "Content-Type", request.contentType(),
                "Cache-Control", CACHE_CONTROL
        );

        return new DashboardPresignUploadResponse(
                presigned.url().toString(),
                buildPublicUrl(key),
                key,
                s3Properties.getPresignExpirySeconds(),
                requiredHeaders
        );
    }

    private void assertOwnershipForProjectUpload(DashboardMediaUploadType uploadType, Long projectId) {
        if (uploadType.isProjectScoped()) {
            dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
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
            case CONNECTIVITY_MAP -> "dashboard/projects/" + projectId + "/connectivity/" + filename;
            case BROCHURE_PDF     -> "dashboard/projects/" + projectId + "/brochures/" + filename;
            case BUILDER_LOGO     -> "dashboard/builders/" + builderId + "/logos/" + filename;
            case CITY_COVER_IMAGE -> "dashboard/cities/" + cityId + "/cover/" + filename;
        };
    }

    private String buildPublicUrl(String key) {
        String base = s3Properties.getPublicBaseUrl();
        if (base != null && !base.isBlank()) {
            return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + "/" + key;
        }
        return "https://" + s3Properties.getBucket() + ".s3." + s3Properties.getRegion() + ".amazonaws.com/" + key;
    }

    private String extFromContentType(String contentType) {
        return switch (contentType) {
            case "image/png"       -> "png";
            case "image/webp"      -> "webp";
            case "application/pdf" -> "pdf";
            default                -> "jpg";
        };
    }
}
