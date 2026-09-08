package com.brandPitara.sfs.cdn.event;

import com.brandPitara.sfs.cdn.service.ProjectPublicCacheEvictionMetrics;
import com.brandPitara.sfs.cdn.service.ProjectPublicCacheEvictionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class ProjectPublicCacheEvictionListener {

    private final ProjectPublicCacheEvictionService evictionService;
    private final ProjectPublicCacheEvictionMetrics metrics;
    private final TaskExecutor executor;

    public ProjectPublicCacheEvictionListener(
            ProjectPublicCacheEvictionService evictionService,
            ProjectPublicCacheEvictionMetrics metrics,
            @Qualifier("projectPublicCacheEvictionExecutor") TaskExecutor executor
    ) {
        this.evictionService = evictionService;
        this.metrics = metrics;
        this.executor = executor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
    public void afterCommit(ProjectPublicCacheEvictionEvent event) {
        metrics.requested(event.reason());
        try {
            executor.execute(() -> evictionService.evict(event.projectId(), event.reason()));
        } catch (RuntimeException rejected) {
            metrics.failure(event.reason());
            log.error("event=cdn_invalidation projectId={} reason={} provider=cloudfront result=rejected",
                    event.projectId(), event.reason(), rejected);
        }
    }
}
