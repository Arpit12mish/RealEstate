package com.brandPitara.sfs.cdn.service;

import com.brandPitara.sfs.cdn.event.ProjectCacheEvictionReason;

public interface ProjectPublicCacheEvictionService {
    void evict(long projectId, ProjectCacheEvictionReason reason);
}
