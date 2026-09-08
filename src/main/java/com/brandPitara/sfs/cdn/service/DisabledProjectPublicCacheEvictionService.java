package com.brandPitara.sfs.cdn.service;

import com.brandPitara.sfs.cdn.event.ProjectCacheEvictionReason;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sfs.cdn", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledProjectPublicCacheEvictionService implements ProjectPublicCacheEvictionService {

    private final ProjectPublicCacheEvictionMetrics metrics;

    @Override
    public void evict(long projectId, ProjectCacheEvictionReason reason) {
        metrics.skipped(reason);
        log.debug("event=cdn_invalidation projectId={} reason={} path={} provider=cloudfront result=skipped_disabled",
                projectId, reason, ProjectPublicCachePaths.detail(projectId));
    }
}
