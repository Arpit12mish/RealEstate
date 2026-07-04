package com.brandPitara.sfs.projectcompare.service;

import com.brandPitara.sfs.projectcompare.dto.request.ProjectComparisonRequest;
import com.brandPitara.sfs.projectcompare.dto.response.ProjectComparisonResponse;

public interface ProjectComparisonService {
    ProjectComparisonResponse compare(ProjectComparisonRequest request);
}
