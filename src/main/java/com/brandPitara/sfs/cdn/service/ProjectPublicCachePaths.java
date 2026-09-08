package com.brandPitara.sfs.cdn.service;

public final class ProjectPublicCachePaths {

    private ProjectPublicCachePaths() {
    }

    public static String detail(long projectId) {
        if (projectId <= 0) throw new IllegalArgumentException("projectId must be positive");
        return "/api/v2/public/projects/" + projectId;
    }
}
