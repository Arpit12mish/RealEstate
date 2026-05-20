package com.brandPitara.sfs.dashboard.scraping.candidate.service;

import com.brandPitara.sfs.dashboard.scraping.candidate.dto.ApplyToProjectRequest;
import com.brandPitara.sfs.dashboard.scraping.candidate.dto.ApplyToProjectResponse;

public interface ScrapeCandidateApplyService {

    ApplyToProjectResponse applyToProject(Long candidateId, ApplyToProjectRequest request);
}
