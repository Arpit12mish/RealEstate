package com.brandPitara.sfs.provider.service.impl;

import com.brandPitara.sfs.media.config.S3Properties;
import com.brandPitara.sfs.provider.dto.PresignProviderMediaRequest;
import com.brandPitara.sfs.provider.dto.PresignProviderMediaResponse;
import com.brandPitara.sfs.provider.entity.ProviderProfileEntity;
import com.brandPitara.sfs.provider.enums.ProviderMediaType;
import com.brandPitara.sfs.provider.repository.ProviderProfileRepository;
import com.brandPitara.sfs.provider.service.ProviderMediaPresignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProviderMediaPresignServiceImpl implements ProviderMediaPresignService {

    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;
    private final ProviderProfileRepository providerProfileRepository;

    @Override
    public PresignProviderMediaResponse createPresignedUpload(Long currentUserId, PresignProviderMediaRequest request) {
        ProviderProfileEntity provider = providerProfileRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new IllegalStateException("Provider profile not found"));

        String ext = extFromContentType(request.contentType());
        String key = buildKey(provider.getId(), request.mediaType(), ext);

        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .contentType(request.contentType())
                .build();

        PutObjectPresignRequest presignReq = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(s3Properties.getPresignExpirySeconds()))
                .putObjectRequest(putReq)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignReq);

        return new PresignProviderMediaResponse(
                presigned.url().toString(),
                buildPublicUrl(key),
                key,
                s3Properties.getPresignExpirySeconds()
        );
    }

    private String buildKey(Long providerId, ProviderMediaType mediaType, String ext) {
        String file = UUID.randomUUID() + "." + ext;
        if (mediaType == ProviderMediaType.PROFILE_PHOTO) {
            return "providers/" + providerId + "/profile/" + file;
        }
        return "providers/" + providerId + "/gallery/" + file;
    }

    private String buildPublicUrl(String key) {
        String base = s3Properties.getPublicBaseUrl();
        if (base != null && !base.isBlank()) {
            return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + "/" + key;
        }
        // fallback S3 URL (only viewable if object public or behind CloudFront later)
        return "https://" + s3Properties.getBucket() + ".s3.ap-south-1.amazonaws.com/" + key;
    }

    private String extFromContentType(String ct) {
        return switch (ct) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }
}
