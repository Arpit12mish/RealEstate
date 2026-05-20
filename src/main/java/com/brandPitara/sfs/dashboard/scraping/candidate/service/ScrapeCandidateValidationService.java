package com.brandPitara.sfs.dashboard.scraping.candidate.service;

import com.brandPitara.sfs.dashboard.scraping.candidate.dto.ApplyToProjectRequest;
import com.brandPitara.sfs.dashboard.scraping.candidate.dto.UpdateCandidateStatusRequest;
import com.brandPitara.sfs.dashboard.scraping.candidate.entity.DashboardScrapeCandidateEntity;
import com.brandPitara.sfs.dashboard.scraping.candidate.entity.DashboardScrapeCandidateProjectEntity;

public interface ScrapeCandidateValidationService {

    void validateStatusTransition(DashboardScrapeCandidateEntity candidate,
                                  UpdateCandidateStatusRequest request);

    void validateApplyable(DashboardScrapeCandidateEntity candidate,
                           DashboardScrapeCandidateProjectEntity project,
                           ApplyToProjectRequest request);
}
