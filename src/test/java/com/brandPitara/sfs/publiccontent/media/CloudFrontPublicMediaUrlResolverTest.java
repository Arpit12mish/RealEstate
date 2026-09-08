package com.brandPitara.sfs.publiccontent.media;

import com.brandPitara.sfs.cms.media.domain.CmsMediaStatus;
import com.brandPitara.sfs.cms.media.domain.CmsMediaType;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CloudFrontPublicMediaUrlResolverTest {

    @Test
    void resolvesReadyImageAndVideoWithoutDoubleSlashAndEncodesPathSegments() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        var resolver = resolver("https://media.example.com/base///", false, registry);

        assertThat(resolver.resolve(asset(CmsMediaType.IMAGE, CmsMediaStatus.READY,
                "cms/images/2026/08/image name.jpg", "image/jpeg")))
                .isEqualTo("https://media.example.com/base/cms/images/2026/08/image%20name.jpg");
        assertThat(resolver.resolve(asset(CmsMediaType.VIDEO, CmsMediaStatus.READY,
                "cms/videos/2026/08/video.mp4", "video/mp4")))
                .isEqualTo("https://media.example.com/base/cms/videos/2026/08/video.mp4");
        assertThat(registry.get("cms.public.media.resolve").tag("result", "success").counters())
                .hasSize(2);
    }

    @Test
    void rejectsNonReadyWrongPrefixTraversalAbsoluteAndUnsupportedType() {
        var resolver = resolver("https://media.example.com", false);

        assertRejected(resolver, asset(CmsMediaType.IMAGE, CmsMediaStatus.PENDING_UPLOAD,
                "cms/images/a.jpg", "image/jpeg"));
        assertRejected(resolver, asset(CmsMediaType.IMAGE, CmsMediaStatus.READY,
                "projects/a.jpg", "image/jpeg"));
        assertRejected(resolver, asset(CmsMediaType.IMAGE, CmsMediaStatus.READY,
                "cms/images/../private.jpg", "image/jpeg"));
        assertRejected(resolver, asset(CmsMediaType.IMAGE, CmsMediaStatus.READY,
                "https://bucket.s3.amazonaws.com/cms/images/a.jpg", "image/jpeg"));
        assertRejected(resolver, asset(CmsMediaType.IMAGE, CmsMediaStatus.READY,
                "cms/images/a.svg", "image/svg+xml"));
    }

    @Test
    void missingConfigurationIsAllowedLocallyButMediaResolutionFailsClosed() {
        var resolver = resolver(null, false);

        assertRejected(resolver, asset(CmsMediaType.IMAGE, CmsMediaStatus.READY,
                "cms/images/a.jpg", "image/jpeg"));
    }

    @Test
    void productionRequiresCredentialFreeHttpsConfiguration() {
        assertThatThrownBy(() -> resolver(null, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("required in production");
        assertThatThrownBy(() -> resolver("http://media.example.com", true))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> resolver("https://user:pass@media.example.com", true))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> resolver("https://media.example.com?token=value", true))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> resolver("https://private-bucket.s3.ap-south-1.amazonaws.com", true))
                .isInstanceOf(IllegalStateException.class);

        assertThat(resolver("https://media.example.com", true)).isNotNull();
    }

    private CloudFrontPublicMediaUrlResolver resolver(String baseUrl, boolean production) {
        return resolver(baseUrl, production, new SimpleMeterRegistry());
    }

    private CloudFrontPublicMediaUrlResolver resolver(
            String baseUrl, boolean production, SimpleMeterRegistry registry
    ) {
        PublicMediaDeliveryProperties properties = new PublicMediaDeliveryProperties();
        properties.setBaseUrl(baseUrl);
        MockEnvironment environment = new MockEnvironment();
        if (production) environment.setActiveProfiles("prod");
        CloudFrontPublicMediaUrlResolver resolver =
                new CloudFrontPublicMediaUrlResolver(properties, environment,
                        new PublicMediaDeliveryMetrics(registry));
        resolver.validateConfiguration();
        return resolver;
    }

    private CmsMediaAssetEntity asset(
            CmsMediaType type, CmsMediaStatus status, String key, String contentType
    ) {
        return CmsMediaAssetEntity.builder()
                .id(1L)
                .mediaType(type)
                .status(status)
                .storageKey(key)
                .contentType(contentType)
                .build();
    }

    private void assertRejected(
            CloudFrontPublicMediaUrlResolver resolver, CmsMediaAssetEntity asset
    ) {
        assertThatThrownBy(() -> resolver.resolve(asset))
                .isInstanceOf(PublicMediaDeliveryException.class);
    }
}
