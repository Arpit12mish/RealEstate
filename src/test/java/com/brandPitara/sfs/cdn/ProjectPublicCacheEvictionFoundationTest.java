package com.brandPitara.sfs.cdn;

import com.brandPitara.sfs.cdn.event.ProjectCacheEvictionReason;
import com.brandPitara.sfs.cdn.gateway.CdnInvalidationGateway;
import com.brandPitara.sfs.cdn.service.DisabledProjectPublicCacheEvictionService;
import com.brandPitara.sfs.cdn.service.EnabledProjectPublicCacheEvictionService;
import com.brandPitara.sfs.cdn.service.ProjectPublicCacheEvictionMetrics;
import com.brandPitara.sfs.cdn.service.ProjectPublicCachePaths;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class ProjectPublicCacheEvictionFoundationTest {

    @Test
    void disabledServiceSkipsWithoutOwningAnyGateway() {
        ProjectPublicCacheEvictionMetrics metrics = mock(ProjectPublicCacheEvictionMetrics.class);
        DisabledProjectPublicCacheEvictionService service =
                new DisabledProjectPublicCacheEvictionService(metrics);

        service.evict(27L, ProjectCacheEvictionReason.PROJECT_UPDATED);

        verify(metrics).skipped(ProjectCacheEvictionReason.PROJECT_UPDATED);
    }

    @Test
    void enabledServiceUsesExactProjectPath() {
        CdnInvalidationGateway gateway = mock(CdnInvalidationGateway.class);
        ProjectPublicCacheEvictionMetrics metrics = mock(ProjectPublicCacheEvictionMetrics.class);
        EnabledProjectPublicCacheEvictionService service =
                new EnabledProjectPublicCacheEvictionService(gateway, metrics);

        service.evict(27L, ProjectCacheEvictionReason.MEDIA_CHANGED);

        verify(gateway).invalidate(java.util.List.of("/api/v2/public/projects/27"));
        verify(metrics).success(ProjectCacheEvictionReason.MEDIA_CHANGED);
    }

    @Test
    void gatewayFailureNeverEscapesEvictionService() {
        CdnInvalidationGateway gateway = mock(CdnInvalidationGateway.class);
        ProjectPublicCacheEvictionMetrics metrics = mock(ProjectPublicCacheEvictionMetrics.class);
        doThrow(new IllegalStateException("AWS unavailable")).when(gateway).invalidate(any());
        EnabledProjectPublicCacheEvictionService service =
                new EnabledProjectPublicCacheEvictionService(gateway, metrics);

        assertThatCode(() -> service.evict(27L, ProjectCacheEvictionReason.PROJECT_DELETED))
                .doesNotThrowAnyException();
        verify(metrics).failure(ProjectCacheEvictionReason.PROJECT_DELETED);
    }

    @Test
    void pathBuilderRejectsWildcardOrInvalidIdsByConstruction() {
        assertThat(ProjectPublicCachePaths.detail(27L)).isEqualTo("/api/v2/public/projects/27");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> ProjectPublicCachePaths.detail(0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void favoriteToggleIntentionallyHasNoEvictionPublisherDependency() {
        assertThat(java.util.Arrays.stream(
                        com.brandPitara.sfs.project.service.impl.ProjectFavoriteServiceImpl.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getType))
                .doesNotContain(com.brandPitara.sfs.cdn.event.ProjectPublicCacheEvictionPublisher.class);
    }
}
