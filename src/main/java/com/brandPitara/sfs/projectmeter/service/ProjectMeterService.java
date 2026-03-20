package com.brandPitara.sfs.projectmeter.service;

import com.brandPitara.sfs.projectmeter.dto.ProjectConstructionProgressResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterCardResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterDetailResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectMeterService {
    ProjectMeterSummaryResponse publicGetMeterSummary(Long projectId);
    ProjectConstructionProgressResponse publicGetConstructionProgress(Long projectId);
    ProjectMeterDetailResponse publicGetMeterDetail(Long projectId);
    Page<ProjectMeterCardResponse> publicListMeterCards(Long cityId, Pageable pageable);
}