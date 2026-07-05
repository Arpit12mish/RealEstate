package com.brandPitara.sfs.provider.service.impl;

import com.brandPitara.sfs.media.service.MediaStorageService;
import com.brandPitara.sfs.media.service.PresignedUploadRequest;
import com.brandPitara.sfs.media.service.PresignedUploadResult;
import com.brandPitara.sfs.provider.dto.PresignProviderMediaRequest;
import com.brandPitara.sfs.provider.dto.PresignProviderMediaResponse;
import com.brandPitara.sfs.provider.entity.ProviderProfileEntity;
import com.brandPitara.sfs.provider.enums.ProviderMediaType;
import com.brandPitara.sfs.provider.repository.ProviderProfileRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Proves ProviderMediaPresignServiceImpl calls the MediaStorageService port
 * (not S3Presigner directly) and preserves storage-key format and returned
 * upload/public URL behavior.
 */
class ProviderMediaPresignServiceImplTest {

    @Test
    void profilePhotoPresignBuildsProfileScopedKeyAndDelegatesToPort() {
        MediaStorageService mediaStorageService = mock(MediaStorageService.class);
        ProviderProfileRepository providerProfileRepository = mock(ProviderProfileRepository.class);
        ProviderMediaPresignServiceImpl service = new ProviderMediaPresignServiceImpl(mediaStorageService, providerProfileRepository);

        when(providerProfileRepository.findByUserId(10L))
                .thenReturn(Optional.of(ProviderProfileEntity.builder().id(5L).build()));
        when(mediaStorageService.createPresignedUpload(any(PresignedUploadRequest.class)))
                .thenAnswer(invocation -> {
                    PresignedUploadRequest req = invocation.getArgument(0);
                    return new PresignedUploadResult(
                            "https://upload.example.com/presigned",
                            "https://cdn.squarefootstory.com/" + req.storageKey(),
                            req.storageKey(),
                            300,
                            Map.of()
                    );
                });

        PresignProviderMediaResponse response = service.createPresignedUpload(10L,
                new PresignProviderMediaRequest(ProviderMediaType.PROFILE_PHOTO, "image/png"));

        assertThat(response.uploadUrl()).isEqualTo("https://upload.example.com/presigned");
        assertThat(response.storageKey()).startsWith("providers/5/profile/").endsWith(".png");
        assertThat(response.publicUrl()).isEqualTo("https://cdn.squarefootstory.com/" + response.storageKey());
        assertThat(response.expiresInSeconds()).isEqualTo(300);

        verify(mediaStorageService).createPresignedUpload(argThat(req ->
                req.contentType().equals("image/png") && req.storageKey().startsWith("providers/5/profile/")
        ));
    }

    @Test
    void galleryPresignBuildsGalleryScopedKey() {
        MediaStorageService mediaStorageService = mock(MediaStorageService.class);
        ProviderProfileRepository providerProfileRepository = mock(ProviderProfileRepository.class);
        ProviderMediaPresignServiceImpl service = new ProviderMediaPresignServiceImpl(mediaStorageService, providerProfileRepository);

        when(providerProfileRepository.findByUserId(10L))
                .thenReturn(Optional.of(ProviderProfileEntity.builder().id(5L).build()));
        when(mediaStorageService.createPresignedUpload(any(PresignedUploadRequest.class)))
                .thenAnswer(invocation -> {
                    PresignedUploadRequest req = invocation.getArgument(0);
                    return new PresignedUploadResult("url", "public/" + req.storageKey(), req.storageKey(), 300, Map.of());
                });

        PresignProviderMediaResponse response = service.createPresignedUpload(10L,
                new PresignProviderMediaRequest(ProviderMediaType.GALLERY, "image/webp"));

        assertThat(response.storageKey()).startsWith("providers/5/gallery/").endsWith(".webp");
    }

    @Test
    void missingProviderProfileThrowsAndNeverCallsThePort() {
        MediaStorageService mediaStorageService = mock(MediaStorageService.class);
        ProviderProfileRepository providerProfileRepository = mock(ProviderProfileRepository.class);
        ProviderMediaPresignServiceImpl service = new ProviderMediaPresignServiceImpl(mediaStorageService, providerProfileRepository);

        when(providerProfileRepository.findByUserId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPresignedUpload(10L,
                new PresignProviderMediaRequest(ProviderMediaType.PROFILE_PHOTO, "image/png")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Provider profile not found");

        verifyNoInteractions(mediaStorageService);
    }
}
