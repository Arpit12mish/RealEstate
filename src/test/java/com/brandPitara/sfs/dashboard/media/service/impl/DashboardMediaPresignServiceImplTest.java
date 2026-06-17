package com.brandPitara.sfs.dashboard.media.service.impl;

import com.brandPitara.sfs.dashboard.media.dto.DashboardPresignUploadRequest;
import com.brandPitara.sfs.dashboard.media.dto.DashboardPresignUploadResponse;
import com.brandPitara.sfs.dashboard.media.enums.DashboardMediaUploadType;
import com.brandPitara.sfs.dashboard.project.service.DashboardProjectOwnershipService;
import com.brandPitara.sfs.dashboard.validator.DashboardMediaUploadValidator;
import com.brandPitara.sfs.media.config.S3Properties;
import com.brandPitara.sfs.repository.CityRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;

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

    private DashboardMediaPresignServiceImpl service(S3Presigner s3Presigner, CityRepository cityRepository) {
        S3Properties properties = new S3Properties();
        properties.setBucket("sfs-test");
        properties.setRegion("ap-south-1");
        properties.setPublicBaseUrl("https://cdn.squarefootstory.com");
        properties.setPresignExpirySeconds(300);

        return new DashboardMediaPresignServiceImpl(
                s3Presigner,
                properties,
                mock(DashboardProjectOwnershipService.class),
                new DashboardMediaUploadValidator(),
                cityRepository
        );
    }
}
