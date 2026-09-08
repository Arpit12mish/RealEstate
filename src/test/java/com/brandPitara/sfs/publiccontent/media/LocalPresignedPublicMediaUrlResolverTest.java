package com.brandPitara.sfs.publiccontent.media;

import com.brandPitara.sfs.cms.media.domain.CmsMediaStatus;
import com.brandPitara.sfs.cms.media.domain.CmsMediaType;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.media.service.MediaObjectStorageService;
import com.brandPitara.sfs.media.service.PresignedReadResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The "local" stand-in for CloudFrontPublicMediaUrlResolver - proves it
 * legitimately returns a short-lived presigned GET (acceptable only under
 * the local profile, never wired for prod - see PublicMediaUrlResolver's
 * two @Profile-gated implementations).
 */
class LocalPresignedPublicMediaUrlResolverTest {

    @Test
    void resolvesReadyAssetToThePresignedReadUrlFromStorage() {
        MediaObjectStorageService storage = mock(MediaObjectStorageService.class);
        when(storage.createPresignedRead("cms-bucket", "cms/images/2026/08/a.jpg"))
                .thenReturn(new PresignedReadResult(
                        "https://cms-bucket.s3.ap-south-1.amazonaws.com/cms/images/2026/08/a.jpg?X-Amz-Expires=300",
                        300));
        var resolver = new LocalPresignedPublicMediaUrlResolver(storage, new PublicMediaDeliveryMetrics(new SimpleMeterRegistry()));

        String resolved = resolver.resolve(asset(CmsMediaStatus.READY, "cms-bucket", "cms/images/2026/08/a.jpg"));

        assertThat(resolved).isEqualTo(
                "https://cms-bucket.s3.ap-south-1.amazonaws.com/cms/images/2026/08/a.jpg?X-Amz-Expires=300");
    }

    @Test
    void rejectsNonReadyAssetWithoutCallingStorage() {
        MediaObjectStorageService storage = mock(MediaObjectStorageService.class);
        var resolver = new LocalPresignedPublicMediaUrlResolver(storage, new PublicMediaDeliveryMetrics(new SimpleMeterRegistry()));

        assertThatThrownBy(() -> resolver.resolve(asset(CmsMediaStatus.PENDING_UPLOAD, "cms-bucket", "cms/images/a.jpg")))
                .isInstanceOf(PublicMediaDeliveryException.class);
    }

    @Test
    void wrapsStorageFailureAsPublicMediaDeliveryException() {
        MediaObjectStorageService storage = mock(MediaObjectStorageService.class);
        when(storage.createPresignedRead("cms-bucket", "cms/images/a.jpg"))
                .thenThrow(new RuntimeException("s3 unreachable"));
        var resolver = new LocalPresignedPublicMediaUrlResolver(storage, new PublicMediaDeliveryMetrics(new SimpleMeterRegistry()));

        assertThatThrownBy(() -> resolver.resolve(asset(CmsMediaStatus.READY, "cms-bucket", "cms/images/a.jpg")))
                .isInstanceOf(PublicMediaDeliveryException.class);
    }

    private CmsMediaAssetEntity asset(CmsMediaStatus status, String bucket, String key) {
        return CmsMediaAssetEntity.builder()
                .id(1L)
                .mediaType(CmsMediaType.IMAGE)
                .status(status)
                .storageBucket(bucket)
                .storageKey(key)
                .contentType("image/jpeg")
                .build();
    }
}
