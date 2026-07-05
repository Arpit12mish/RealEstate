package com.brandPitara.sfs.provider.service.impl;

import com.brandPitara.sfs.media.service.MediaStorageService;
import com.brandPitara.sfs.media.service.PresignedUploadRequest;
import com.brandPitara.sfs.media.service.PresignedUploadResult;
import com.brandPitara.sfs.provider.dto.PresignProviderMediaRequest;
import com.brandPitara.sfs.provider.dto.PresignProviderMediaResponse;
import com.brandPitara.sfs.provider.entity.ProviderProfileEntity;
import com.brandPitara.sfs.provider.enums.ProviderMediaType;
import com.brandPitara.sfs.provider.repository.ProviderProfileRepository;
import com.brandPitara.sfs.provider.service.ProviderMediaPresignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProviderMediaPresignServiceImpl implements ProviderMediaPresignService {

    private final MediaStorageService mediaStorageService;
    private final ProviderProfileRepository providerProfileRepository;

    @Override
    public PresignProviderMediaResponse createPresignedUpload(Long currentUserId, PresignProviderMediaRequest request) {
        ProviderProfileEntity provider = providerProfileRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new IllegalStateException("Provider profile not found"));

        String ext = extFromContentType(request.contentType());
        String key = buildKey(provider.getId(), request.mediaType(), ext);

        PresignedUploadResult result = mediaStorageService.createPresignedUpload(
                new PresignedUploadRequest(key, request.contentType())
        );

        return new PresignProviderMediaResponse(
                result.uploadUrl(),
                result.publicUrl(),
                result.storageKey(),
                result.expiresInSeconds()
        );
    }

    private String buildKey(Long providerId, ProviderMediaType mediaType, String ext) {
        String file = UUID.randomUUID() + "." + ext;
        if (mediaType == ProviderMediaType.PROFILE_PHOTO) {
            return "providers/" + providerId + "/profile/" + file;
        }
        return "providers/" + providerId + "/gallery/" + file;
    }

    private String extFromContentType(String ct) {
        return switch (ct) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }
}
