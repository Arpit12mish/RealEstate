package com.brandPitara.sfs.publiccontent.media;

import com.brandPitara.sfs.cms.media.domain.CmsMediaStatus;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.media.service.MediaObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Local-dev stand-in for CloudFront. CMS_MEDIA_PUBLIC_BASE_URL is not wired
 * for the "local" profile, and CloudFrontPublicMediaUrlResolver rejects any
 * S3-hostname base URL unconditionally (by design - see that class), so a raw
 * bucket URL can never satisfy it. This resolver instead reuses the same
 * short-lived presigned-GET mechanism the dashboard media preview already
 * uses, letting the public content API be exercised locally against a
 * private or public bucket without CloudFront/OAC. Never active outside
 * "local" - every other profile keeps CloudFrontPublicMediaUrlResolver
 * unchanged.
 */
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalPresignedPublicMediaUrlResolver implements PublicMediaUrlResolver {

    private final MediaObjectStorageService storage;
    private final PublicMediaDeliveryMetrics metrics;

    @Override
    public String resolve(CmsMediaAssetEntity asset) {
        if (asset == null || asset.getStatus() != CmsMediaStatus.READY) {
            metrics.resolved(asset == null ? null : asset.getMediaType(), "failure");
            throw new PublicMediaDeliveryException("Only READY CMS media can be delivered publicly");
        }
        try {
            String url = storage.createPresignedRead(asset.getStorageBucket(), asset.getStorageKey()).url();
            metrics.resolved(asset.getMediaType(), "success");
            return url;
        } catch (RuntimeException failure) {
            metrics.resolved(asset.getMediaType(), "failure");
            throw new PublicMediaDeliveryException("Local presigned CMS media read failed");
        }
    }
}
