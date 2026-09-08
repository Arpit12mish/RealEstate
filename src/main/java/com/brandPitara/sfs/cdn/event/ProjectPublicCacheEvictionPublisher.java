package com.brandPitara.sfs.cdn.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
public class ProjectPublicCacheEvictionPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publish(long projectId, ProjectCacheEvictionReason reason) {
        eventPublisher.publishEvent(new ProjectPublicCacheEvictionEvent(projectId, reason));
    }

    public void publishAll(Collection<Long> projectIds, ProjectCacheEvictionReason reason) {
        if (projectIds == null) return;
        projectIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .forEach(id -> publish(id, reason));
    }
}
