package com.brandPitara.sfs.dashboard.media.service.impl;

import com.brandPitara.sfs.dashboard.media.dto.DashboardPresignUploadRequest;
import com.brandPitara.sfs.dashboard.media.dto.DashboardPresignUploadResponse;
import com.brandPitara.sfs.dashboard.media.enums.DashboardMediaUploadType;
import com.brandPitara.sfs.dashboard.project.service.DashboardProjectOwnershipService;
import com.brandPitara.sfs.dashboard.validator.DashboardMediaUploadValidator;
import com.brandPitara.sfs.media.config.S3Properties;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.repository.CityRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DashboardMediaPresignServiceImplTest {

    @Test
    void cityCoverImagePresignBuildsCityStorageKeyAndPublicUrl() throws Exception {
        S3Presigner s3Presigner = mock(S3Presigner.class);
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        CityRepository cityRepository = mock(CityRepository.class);
        DashboardMediaPresignServiceImpl service = service(s3Presigner, cityRepository);

        when(cityRepository.existsById(7L)).thenReturn(true);
        when(presignedRequest.url()).thenReturn(new URL("https://upload.example.com/presigned"));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);

        DashboardPresignUploadResponse response = service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.CITY_COVER_IMAGE,
                "image/webp",
                524288L,
                null,
                null,
                7L
        ));

        assertThat(response.uploadUrl()).isEqualTo("https://upload.example.com/presigned");
        assertThat(response.storageKey())
                .startsWith("dashboard/cities/7/cover/")
                .endsWith(".webp");
        assertThat(response.publicUrl()).isEqualTo("https://cdn.squarefootstory.com/" + response.storageKey());
        assertThat(response.expiresInSeconds()).isEqualTo(300);
        assertThat(response.requiredHeaders())
                .containsEntry("Content-Type", "image/webp")
                .containsEntry("Cache-Control", "public, max-age=31536000, immutable");
        verify(s3Presigner).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    void cityCoverImagePresignRejectsMissingCity() {
        S3Presigner s3Presigner = mock(S3Presigner.class);
        CityRepository cityRepository = mock(CityRepository.class);
        DashboardMediaPresignServiceImpl service = service(s3Presigner, cityRepository);

        when(cityRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.CITY_COVER_IMAGE,
                "image/png",
                1000L,
                null,
                null,
                999L
        )))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("City not found: 999");

        verify(s3Presigner, never()).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    void masterPlanImagePresignBuildsProjectStorageKey() throws Exception {
        S3Presigner s3Presigner = mock(S3Presigner.class);
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        CityRepository cityRepository = mock(CityRepository.class);
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        DashboardProjectOwnershipService ownershipService = mock(DashboardProjectOwnershipService.class);
        DashboardMediaPresignServiceImpl service = service(s3Presigner, cityRepository, ownershipService, projectRepository);

        when(projectRepository.findByIdAndDeletedFalse(42L)).thenReturn(Optional.of(ProjectEntity.builder().id(42L).build()));
        when(presignedRequest.url()).thenReturn(new URL("https://upload.example.com/presigned"));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);

        DashboardPresignUploadResponse response = service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.MASTER_PLAN_IMAGE,
                "image/png",
                524288L,
                42L,
                null,
                null
        ));

        assertThat(response.storageKey())
                .startsWith("dashboard/projects/42/master-plan/")
                .endsWith(".png");
        verify(projectRepository).findByIdAndDeletedFalse(42L);
        verifyNoInteractions(ownershipService);
    }

    @Test
    void masterPlanImagePresignRejectsMissingProject() {
        S3Presigner s3Presigner = mock(S3Presigner.class);
        CityRepository cityRepository = mock(CityRepository.class);
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        DashboardProjectOwnershipService ownershipService = mock(DashboardProjectOwnershipService.class);
        DashboardMediaPresignServiceImpl service = service(s3Presigner, cityRepository, ownershipService, projectRepository);

        when(projectRepository.findByIdAndDeletedFalse(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.MASTER_PLAN_IMAGE,
                "image/png",
                524288L,
                42L,
                null,
                null
        )))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Project not found: 42");

        verify(projectRepository).findByIdAndDeletedFalse(42L);
        verifyNoInteractions(ownershipService);
        verify(s3Presigner, never()).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    void regularProjectImagePresignStillUsesOwnershipPolicy() throws Exception {
        S3Presigner s3Presigner = mock(S3Presigner.class);
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        CityRepository cityRepository = mock(CityRepository.class);
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        DashboardProjectOwnershipService ownershipService = mock(DashboardProjectOwnershipService.class);
        DashboardMediaPresignServiceImpl service = service(s3Presigner, cityRepository, ownershipService, projectRepository);

        when(presignedRequest.url()).thenReturn(new URL("https://upload.example.com/presigned"));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);

        DashboardPresignUploadResponse response = service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.PROJECT_IMAGE,
                "image/jpeg",
                524288L,
                42L,
                null,
                null
        ));

        assertThat(response.storageKey())
                .startsWith("dashboard/projects/42/images/")
                .endsWith(".jpg");
        verify(ownershipService).assertCurrentUserCanEditProject(42L);
        verifyNoInteractions(projectRepository);
    }

    @Test
    void instagramReelThumbnailPresignBuildsGenericStorageKey() throws Exception {
        S3Presigner s3Presigner = mock(S3Presigner.class);
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        DashboardMediaPresignServiceImpl service = service(s3Presigner, mock(CityRepository.class));

        when(presignedRequest.url()).thenReturn(new URL("https://upload.example.com/presigned"));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);

        DashboardPresignUploadResponse response = service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.INSTAGRAM_REEL_THUMBNAIL,
                "image/webp",
                524288L,
                null,
                null,
                null
        ));

        assertThat(response.storageKey())
                .startsWith("dashboard/instagram-reels/thumbnails/")
                .endsWith(".webp");
        assertThat(response.requiredHeaders())
                .containsEntry("Content-Type", "image/webp")
                .containsEntry("Cache-Control", "public, max-age=31536000, immutable");
        verify(s3Presigner).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    void instagramReelPreviewVideoPresignBuildsGenericStorageKey() throws Exception {
        S3Presigner s3Presigner = mock(S3Presigner.class);
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        DashboardMediaPresignServiceImpl service = service(s3Presigner, mock(CityRepository.class));

        when(presignedRequest.url()).thenReturn(new URL("https://upload.example.com/presigned"));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);

        DashboardPresignUploadResponse response = service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.INSTAGRAM_REEL_PREVIEW_VIDEO,
                "video/mp4",
                1024L * 1024L,
                null,
                null,
                null
        ));

        assertThat(response.storageKey())
                .startsWith("dashboard/instagram-reels/previews/")
                .endsWith(".mp4");
        assertThat(response.requiredHeaders())
                .containsEntry("Content-Type", "video/mp4")
                .containsEntry("Cache-Control", "public, max-age=31536000, immutable");
        verify(s3Presigner).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    void builderHighlightThumbnailPresignBuildsBuilderScopedStorageKey() throws Exception {
        S3Presigner s3Presigner = mock(S3Presigner.class);
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        DashboardMediaPresignServiceImpl service = service(s3Presigner, mock(CityRepository.class));

        when(presignedRequest.url()).thenReturn(new URL("https://upload.example.com/presigned"));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);

        DashboardPresignUploadResponse response = service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.BUILDER_HIGHLIGHT_THUMBNAIL,
                "image/webp",
                524288L,
                null,
                7L,
                null
        ));

        assertThat(response.storageKey())
                .startsWith("dashboard/builders/7/highlights/thumbnails/")
                .endsWith(".webp");
        assertThat(response.publicUrl()).isEqualTo("https://cdn.squarefootstory.com/" + response.storageKey());
        verify(s3Presigner).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    void builderAnalysisVideoThumbnailRequiresBuilderId() {
        DashboardMediaPresignServiceImpl service = service(mock(S3Presigner.class), mock(CityRepository.class));

        assertThatThrownBy(() -> service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.BUILDER_ANALYSIS_VIDEO_THUMBNAIL,
                "image/png",
                1024L,
                null,
                null,
                null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BUILDER_ANALYSIS_VIDEO_THUMBNAIL requires builderId");
    }

    private DashboardMediaPresignServiceImpl service(S3Presigner s3Presigner, CityRepository cityRepository) {
        return service(s3Presigner, cityRepository, mock(DashboardProjectOwnershipService.class));
    }

    private DashboardMediaPresignServiceImpl service(
            S3Presigner s3Presigner,
            CityRepository cityRepository,
            DashboardProjectOwnershipService ownershipService
    ) {
        return service(s3Presigner, cityRepository, ownershipService, mock(ProjectRepository.class));
    }

    private DashboardMediaPresignServiceImpl service(
            S3Presigner s3Presigner,
            CityRepository cityRepository,
            DashboardProjectOwnershipService ownershipService,
            ProjectRepository projectRepository
    ) {
        S3Properties properties = new S3Properties();
        properties.setBucket("sfs-test");
        properties.setRegion("ap-south-1");
        properties.setPublicBaseUrl("https://cdn.squarefootstory.com");
        properties.setPresignExpirySeconds(300);

        return new DashboardMediaPresignServiceImpl(
                s3Presigner,
                properties,
                ownershipService,
                new DashboardMediaUploadValidator(),
                cityRepository,
                projectRepository
        );
    }
}
