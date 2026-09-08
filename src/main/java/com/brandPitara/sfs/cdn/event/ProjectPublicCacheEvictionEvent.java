package com.brandPitara.sfs.cdn.event;

public record ProjectPublicCacheEvictionEvent(
        long projectId,
        ProjectCacheEvictionReason reason
) {
    public ProjectPublicCacheEvictionEvent {
        if (projectId <= 0) throw new IllegalArgumentException("projectId must be positive");
        if (reason == null) throw new IllegalArgumentException("reason is required");
    }
}
