package com.brandPitara.sfs.dashboard.media.service.impl;

import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.dashboard.media.dto.DashboardPresignUploadRequest;
import com.brandPitara.sfs.dashboard.media.dto.DashboardPresignUploadResponse;
import com.brandPitara.sfs.dashboard.media.enums.DashboardMediaUploadType;
import com.brandPitara.sfs.dashboard.project.service.DashboardProjectOwnershipService;
import com.brandPitara.sfs.dashboard.validator.DashboardMediaUploadValidator;
import com.brandPitara.sfs.media.service.MediaStorageService;
import com.brandPitara.sfs.media.service.PresignedUploadRequest;
import com.brandPitara.sfs.media.service.PresignedUploadResult;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.repository.CityRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Business logic (key building, validation, ownership checks) is unchanged
 * from before the MediaStorageService port existed - only the S3 mock is
 * replaced with a port mock. The fake MediaStorageService echoes back
 * whatever storageKey/additionalHeaders it was called with, exactly the way
 * S3MediaStorageServiceImpl really behaves, so these assertions still prove
 * the service builds the right key/headers per upload type.
 */
class DashboardMediaPresignServiceImplTest {

    @Test
    void cityCoverImagePresignBuildsCityStorageKeyAndPublicUrl() {
        MediaStorageService mediaStorageService = fakeMediaStorageService();
        CityRepository cityRepository = mock(CityRepository.class);
        DashboardMediaPresignServiceImpl service = service(mediaStorageService, cityRepository);

        when(cityRepository.existsById(7L)).thenReturn(true);

        DashboardPresignUploadResponse response = service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.CITY_COVER_IMAGE,
                "image/webp",
                524288L,
                null,
                null,
                7L,
                null
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
        verify(mediaStorageService).createPresignedUpload(any(PresignedUploadRequest.class));
    }

    @Test
    void cityCoverImagePresignRejectsMissingCity() {
        MediaStorageService mediaStorageService = fakeMediaStorageService();
        CityRepository cityRepository = mock(CityRepository.class);
        DashboardMediaPresignServiceImpl service = service(mediaStorageService, cityRepository);

        when(cityRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.CITY_COVER_IMAGE,
                "image/png",
                1000L,
                null,
                null,
                999L,
                null
        )))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("City not found: 999");

        verify(mediaStorageService, never()).createPresignedUpload(any(PresignedUploadRequest.class));
    }

    @Test
    void masterPlanImagePresignBuildsProjectStorageKey() {
        MediaStorageService mediaStorageService = fakeMediaStorageService();
        CityRepository cityRepository = mock(CityRepository.class);
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        DashboardProjectOwnershipService ownershipService = mock(DashboardProjectOwnershipService.class);
        DashboardMediaPresignServiceImpl service = service(mediaStorageService, cityRepository, ownershipService, projectRepository);

        when(projectRepository.findByIdAndDeletedFalse(42L)).thenReturn(Optional.of(ProjectEntity.builder().id(42L).build()));

        DashboardPresignUploadResponse response = service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.MASTER_PLAN_IMAGE,
                "image/png",
                524288L,
                42L,
                null,
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
        MediaStorageService mediaStorageService = fakeMediaStorageService();
        CityRepository cityRepository = mock(CityRepository.class);
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        DashboardProjectOwnershipService ownershipService = mock(DashboardProjectOwnershipService.class);
        DashboardMediaPresignServiceImpl service = service(mediaStorageService, cityRepository, ownershipService, projectRepository);

        when(projectRepository.findByIdAndDeletedFalse(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.MASTER_PLAN_IMAGE,
                "image/png",
                524288L,
                42L,
                null,
                null,
                null
        )))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Project not found: 42");

        verify(projectRepository).findByIdAndDeletedFalse(42L);
        verifyNoInteractions(ownershipService);
        verify(mediaStorageService, never()).createPresignedUpload(any(PresignedUploadRequest.class));
    }

    @Test
    void regularProjectImagePresignStillUsesOwnershipPolicy() {
        MediaStorageService mediaStorageService = fakeMediaStorageService();
        CityRepository cityRepository = mock(CityRepository.class);
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        DashboardProjectOwnershipService ownershipService = mock(DashboardProjectOwnershipService.class);
        DashboardMediaPresignServiceImpl service = service(mediaStorageService, cityRepository, ownershipService, projectRepository);

        DashboardPresignUploadResponse response = service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.PROJECT_IMAGE,
                "image/jpeg",
                524288L,
                42L,
                null,
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
    void instagramReelThumbnailPresignBuildsGenericStorageKey() {
        MediaStorageService mediaStorageService = fakeMediaStorageService();
        DashboardMediaPresignServiceImpl service = service(mediaStorageService, mock(CityRepository.class));

        DashboardPresignUploadResponse response = service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.INSTAGRAM_REEL_THUMBNAIL,
                "image/webp",
                524288L,
                null,
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
        verify(mediaStorageService).createPresignedUpload(any(PresignedUploadRequest.class));
    }

    @Test
    void instagramReelPreviewVideoPresignBuildsGenericStorageKey() {
        MediaStorageService mediaStorageService = fakeMediaStorageService();
        DashboardMediaPresignServiceImpl service = service(mediaStorageService, mock(CityRepository.class));

        DashboardPresignUploadResponse response = service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.INSTAGRAM_REEL_PREVIEW_VIDEO,
                "video/mp4",
                1024L * 1024L,
                null,
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
        verify(mediaStorageService).createPresignedUpload(any(PresignedUploadRequest.class));
    }

    @Test
    void builderHighlightThumbnailPresignBuildsBuilderScopedStorageKey() {
        MediaStorageService mediaStorageService = fakeMediaStorageService();
        DashboardMediaPresignServiceImpl service = service(mediaStorageService, mock(CityRepository.class));

        DashboardPresignUploadResponse response = service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.BUILDER_HIGHLIGHT_THUMBNAIL,
                "image/webp",
                524288L,
                null,
                7L,
                null,
                null
        ));

        assertThat(response.storageKey())
                .startsWith("dashboard/builders/7/highlights/thumbnails/")
                .endsWith(".webp");
        assertThat(response.publicUrl()).isEqualTo("https://cdn.squarefootstory.com/" + response.storageKey());
        verify(mediaStorageService).createPresignedUpload(any(PresignedUploadRequest.class));
    }

    @Test
    void builderAnalysisVideoThumbnailRequiresBuilderId() {
        DashboardMediaPresignServiceImpl service = service(fakeMediaStorageService(), mock(CityRepository.class));

        assertThatThrownBy(() -> service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.BUILDER_ANALYSIS_VIDEO_THUMBNAIL,
                "image/png",
                1024L,
                null,
                null,
                null,
                null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BUILDER_ANALYSIS_VIDEO_THUMBNAIL requires builderId");
    }

    // --- Brand uploads (Phase 2B.2) ---

    @Test
    void brandLogoPresignBuildsBrandScopedStorageKey() {
        MediaStorageService mediaStorageService = fakeMediaStorageService();
        BrandRepository brandRepository = mock(BrandRepository.class);
        DashboardMediaPresignServiceImpl service = service(mediaStorageService, mock(CityRepository.class), brandRepository);

        when(brandRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(BrandEntity.builder().id(5L).build()));

        DashboardPresignUploadResponse response = service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.BRAND_LOGO,
                "image/png",
                1024L * 1024L,
                null,
                null,
                null,
                5L
        ));

        assertThat(response.storageKey())
                .startsWith("dashboard/brands/5/logo/")
                .endsWith(".png");
        assertThat(response.publicUrl()).isEqualTo("https://cdn.squarefootstory.com/" + response.storageKey());
        verify(mediaStorageService).createPresignedUpload(any(PresignedUploadRequest.class));
    }

    @Test
    void brandHeroImagePresignBuildsBrandScopedStorageKey() {
        MediaStorageService mediaStorageService = fakeMediaStorageService();
        BrandRepository brandRepository = mock(BrandRepository.class);
        DashboardMediaPresignServiceImpl service = service(mediaStorageService, mock(CityRepository.class), brandRepository);

        when(brandRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(BrandEntity.builder().id(5L).build()));

        DashboardPresignUploadResponse response = service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.BRAND_HERO_IMAGE,
                "image/webp",
                3L * 1024 * 1024,
                null,
                null,
                null,
                5L
        ));

        assertThat(response.storageKey())
                .startsWith("dashboard/brands/5/hero/")
                .endsWith(".webp");
    }

    @Test
    void brandSkuImagePresignBuildsBrandScopedStorageKey() {
        MediaStorageService mediaStorageService = fakeMediaStorageService();
        BrandRepository brandRepository = mock(BrandRepository.class);
        DashboardMediaPresignServiceImpl service = service(mediaStorageService, mock(CityRepository.class), brandRepository);

        when(brandRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(BrandEntity.builder().id(5L).build()));

        DashboardPresignUploadResponse response = service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.BRAND_SKU_IMAGE,
                "image/jpeg",
                1024L * 1024L,
                null,
                null,
                null,
                5L
        ));

        assertThat(response.storageKey())
                .startsWith("dashboard/brands/5/skus/")
                .endsWith(".jpg");
    }

    @Test
    void brandCertificateFilePresignAllowsPdfAndBuildsBrandScopedStorageKey() {
        MediaStorageService mediaStorageService = fakeMediaStorageService();
        BrandRepository brandRepository = mock(BrandRepository.class);
        DashboardMediaPresignServiceImpl service = service(mediaStorageService, mock(CityRepository.class), brandRepository);

        when(brandRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(BrandEntity.builder().id(5L).build()));

        DashboardPresignUploadResponse response = service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.BRAND_CERTIFICATE_FILE,
                "application/pdf",
                4L * 1024 * 1024,
                null,
                null,
                null,
                5L
        ));

        assertThat(response.storageKey())
                .startsWith("dashboard/brands/5/certificates/")
                .endsWith(".pdf");
    }

    @Test
    void brandPromoMediaPresignAllowsVideoAndBuildsBrandScopedStorageKey() {
        MediaStorageService mediaStorageService = fakeMediaStorageService();
        BrandRepository brandRepository = mock(BrandRepository.class);
        DashboardMediaPresignServiceImpl service = service(mediaStorageService, mock(CityRepository.class), brandRepository);

        when(brandRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(BrandEntity.builder().id(5L).build()));

        DashboardPresignUploadResponse response = service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.BRAND_PROMO_MEDIA,
                "video/mp4",
                20L * 1024 * 1024,
                null,
                null,
                null,
                5L
        ));

        assertThat(response.storageKey())
                .startsWith("dashboard/brands/5/promo/")
                .endsWith(".mp4");
    }

    @Test
    void brandProductCategoryImagePresignBuildsBrandScopedStorageKey() {
        MediaStorageService mediaStorageService = fakeMediaStorageService();
        BrandRepository brandRepository = mock(BrandRepository.class);
        DashboardMediaPresignServiceImpl service = service(mediaStorageService, mock(CityRepository.class), brandRepository);

        when(brandRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(BrandEntity.builder().id(5L).build()));

        DashboardPresignUploadResponse response = service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.BRAND_PRODUCT_CATEGORY_IMAGE,
                "image/png",
                1024L * 1024L,
                null,
                null,
                null,
                5L
        ));

        assertThat(response.storageKey())
                .startsWith("dashboard/brands/5/product-categories/")
                .endsWith(".png");
    }

    @Test
    void brandLogoPresignRejectsMissingBrand() {
        MediaStorageService mediaStorageService = fakeMediaStorageService();
        BrandRepository brandRepository = mock(BrandRepository.class);
        DashboardMediaPresignServiceImpl service = service(mediaStorageService, mock(CityRepository.class), brandRepository);

        when(brandRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.BRAND_LOGO,
                "image/png",
                1024L,
                null,
                null,
                null,
                999L
        )))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Brand not found: 999");

        verify(mediaStorageService, never()).createPresignedUpload(any(PresignedUploadRequest.class));
    }

    @Test
    void brandLogoPresignRequiresBrandId() {
        DashboardMediaPresignServiceImpl service = service(fakeMediaStorageService(), mock(CityRepository.class));

        assertThatThrownBy(() -> service.createPresignedUpload(new DashboardPresignUploadRequest(
                DashboardMediaUploadType.BRAND_LOGO,
                "image/png",
                1024L,
                null,
                null,
                null,
                null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BRAND_LOGO requires brandId");
    }

    /**
     * Fake port that mirrors S3MediaStorageServiceImpl's real behavior closely
     * enough for these tests: echoes back the storageKey/headers it was asked
     * to presign, using a fixed CDN base and expiry.
     */
    private MediaStorageService fakeMediaStorageService() {
        MediaStorageService mediaStorageService = mock(MediaStorageService.class);
        when(mediaStorageService.createPresignedUpload(any(PresignedUploadRequest.class)))
                .thenAnswer(invocation -> {
                    PresignedUploadRequest request = invocation.getArgument(0);
                    return new PresignedUploadResult(
                            "https://upload.example.com/presigned",
                            "https://cdn.squarefootstory.com/" + request.storageKey(),
                            request.storageKey(),
                            300,
                            request.additionalHeaders()
                    );
                });
        return mediaStorageService;
    }

    private DashboardMediaPresignServiceImpl service(MediaStorageService mediaStorageService, CityRepository cityRepository) {
        return service(mediaStorageService, cityRepository, mock(ProjectRepository.class), mock(BrandRepository.class));
    }

    private DashboardMediaPresignServiceImpl service(
            MediaStorageService mediaStorageService,
            CityRepository cityRepository,
            BrandRepository brandRepository
    ) {
        return service(mediaStorageService, cityRepository, mock(ProjectRepository.class), brandRepository);
    }

    private DashboardMediaPresignServiceImpl service(
            MediaStorageService mediaStorageService,
            CityRepository cityRepository,
            ProjectRepository projectRepository,
            BrandRepository brandRepository
    ) {
        return service(mediaStorageService, cityRepository, mock(DashboardProjectOwnershipService.class), projectRepository, brandRepository);
    }

    private DashboardMediaPresignServiceImpl service(
            MediaStorageService mediaStorageService,
            CityRepository cityRepository,
            DashboardProjectOwnershipService ownershipService,
            ProjectRepository projectRepository
    ) {
        return service(mediaStorageService, cityRepository, ownershipService, projectRepository, mock(BrandRepository.class));
    }

    private DashboardMediaPresignServiceImpl service(
            MediaStorageService mediaStorageService,
            CityRepository cityRepository,
            DashboardProjectOwnershipService ownershipService,
            ProjectRepository projectRepository,
            BrandRepository brandRepository
    ) {
        return new DashboardMediaPresignServiceImpl(
                mediaStorageService,
                ownershipService,
                new DashboardMediaUploadValidator(),
                cityRepository,
                projectRepository,
                brandRepository
        );
    }
}
