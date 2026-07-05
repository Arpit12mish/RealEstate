package com.brandPitara.sfs.media.service.impl;

import com.brandPitara.sfs.media.config.S3Properties;
import com.brandPitara.sfs.media.service.MediaStorageService;
import com.brandPitara.sfs.media.service.PresignedUploadRequest;
import com.brandPitara.sfs.media.service.PresignedUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

/**
 * The one place {@code S3Presigner} is used. Consolidates presign logic that
 * used to be duplicated (with a slight divergence - see below) across
 * ProviderMediaPresignServiceImpl, ProfileServiceImpl, and
 * DashboardMediaPresignServiceImpl.
 */
@Service
@RequiredArgsConstructor
public class S3MediaStorageServiceImpl implements MediaStorageService {

    private static final String CACHE_CONTROL_HEADER = "Cache-Control";

    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    @Override
    public PresignedUploadResult createPresignedUpload(PresignedUploadRequest request) {
        PutObjectRequest.Builder putReqBuilder = PutObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(request.storageKey())
                .contentType(request.contentType());

        String cacheControl = request.additionalHeaders().get(CACHE_CONTROL_HEADER);
        if (cacheControl != null && !cacheControl.isBlank()) {
            putReqBuilder.cacheControl(cacheControl);
        }

        PutObjectPresignRequest presignReq = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(s3Properties.getPresignExpirySeconds()))
                .putObjectRequest(putReqBuilder.build())
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignReq);

        return new PresignedUploadResult(
                presigned.url().toString(),
                buildPublicUrl(request.storageKey()),
                request.storageKey(),
                s3Properties.getPresignExpirySeconds(),
                request.additionalHeaders()
        );
    }

    /**
     * Note: ProviderMediaPresignServiceImpl's pre-refactor version of this
     * fallback hardcoded "ap-south-1" instead of reading s3Properties.getRegion()
     * like the other two callers did. Since the configured region is already
     * "ap-south-1" in every environment, centralizing here does not change any
     * observed behavior today - it only removes the latent drift risk if the
     * region ever changes.
     */
    private String buildPublicUrl(String storageKey) {
        String base = s3Properties.getPublicBaseUrl();
        if (base != null && !base.isBlank()) {
            String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
            return normalized + "/" + storageKey;
        }
        return "https://" + s3Properties.getBucket() + ".s3." + s3Properties.getRegion() + ".amazonaws.com/" + storageKey;
    }
}
