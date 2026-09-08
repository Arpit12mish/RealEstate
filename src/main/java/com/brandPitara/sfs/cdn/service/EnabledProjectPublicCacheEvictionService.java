package com.brandPitara.sfs.cdn.service;

import com.brandPitara.sfs.cdn.event.ProjectCacheEvictionReason;
import com.brandPitara.sfs.cdn.gateway.CdnInvalidationGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sfs.cdn", name = "enabled", havingValue = "true")
public class EnabledProjectPublicCacheEvictionService implements ProjectPublicCacheEvictionService {

    private final CdnInvalidationGateway gateway;
    private final ProjectPublicCacheEvictionMetrics metrics;

    @Override
    public void evict(long projectId, ProjectCacheEvictionReason reason) {
        String path = ProjectPublicCachePaths.detail(projectId);
        try {
            gateway.invalidate(List.of(path));
            metrics.success(reason);
            log.info("event=cdn_invalidation projectId={} reason={} path={} provider=cloudfront result=success",
                    projectId, reason, path);
        } catch (RuntimeException failure) {
            metrics.failure(reason);
            log.error("event=cdn_invalidation projectId={} reason={} path={} provider=cloudfront result=failure",
                    projectId, reason, path, failure);
        }
    }
}
