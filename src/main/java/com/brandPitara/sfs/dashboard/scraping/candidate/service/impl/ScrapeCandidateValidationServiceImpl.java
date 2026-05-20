package com.brandPitara.sfs.dashboard.scraping.candidate.service.impl;

import com.brandPitara.sfs.dashboard.scraping.candidate.dto.ApplyToProjectRequest;
import com.brandPitara.sfs.dashboard.scraping.candidate.dto.UpdateCandidateStatusRequest;
import com.brandPitara.sfs.dashboard.scraping.candidate.entity.DashboardScrapeCandidateEntity;
import com.brandPitara.sfs.dashboard.scraping.candidate.entity.DashboardScrapeCandidateProjectEntity;
import com.brandPitara.sfs.dashboard.scraping.candidate.enums.ApplyMode;
import com.brandPitara.sfs.dashboard.scraping.candidate.enums.ScrapeCandidateStatus;
import com.brandPitara.sfs.dashboard.scraping.candidate.service.ScrapeCandidateValidationService;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class ScrapeCandidateValidationServiceImpl implements ScrapeCandidateValidationService {

    // Allowed transitions: current → allowed targets
    private static final java.util.Map<ScrapeCandidateStatus, Set<ScrapeCandidateStatus>> TRANSITIONS =
            new java.util.HashMap<>(java.util.Map.of(
                    ScrapeCandidateStatus.READY_FOR_REVIEW,  Set.of(ScrapeCandidateStatus.NEEDS_REVIEW,
                                                                     ScrapeCandidateStatus.READY_TO_APPLY,
                                                                     ScrapeCandidateStatus.PARTIAL,
                                                                     ScrapeCandidateStatus.REJECTED),
                    ScrapeCandidateStatus.PARTIAL,           Set.of(ScrapeCandidateStatus.NEEDS_REVIEW,
                                                                     ScrapeCandidateStatus.READY_FOR_REVIEW,
                                                                     ScrapeCandidateStatus.REJECTED,
                                                                     ScrapeCandidateStatus.FAILED),
                    ScrapeCandidateStatus.SCRAPED,           Set.of(ScrapeCandidateStatus.NEEDS_REVIEW,
                                                                     ScrapeCandidateStatus.READY_TO_APPLY,
                                                                     ScrapeCandidateStatus.REJECTED),
                    ScrapeCandidateStatus.NEEDS_REVIEW,      Set.of(ScrapeCandidateStatus.SCRAPED,
                                                                     ScrapeCandidateStatus.READY_TO_APPLY,
                                                                     ScrapeCandidateStatus.READY_FOR_REVIEW,
                                                                     ScrapeCandidateStatus.REJECTED),
                    ScrapeCandidateStatus.READY_TO_APPLY,    Set.of(ScrapeCandidateStatus.SCRAPED,
                                                                     ScrapeCandidateStatus.NEEDS_REVIEW,
                                                                     ScrapeCandidateStatus.REJECTED),
                    ScrapeCandidateStatus.FAILED,            Set.of(ScrapeCandidateStatus.NEEDS_REVIEW),
                    ScrapeCandidateStatus.REJECTED,          Set.of(ScrapeCandidateStatus.NEEDS_REVIEW),
                    ScrapeCandidateStatus.CAPTCHA_REQUIRED,  Set.of(ScrapeCandidateStatus.FAILED,
                                                                     ScrapeCandidateStatus.NEEDS_REVIEW),
                    ScrapeCandidateStatus.PENDING,           Set.of(ScrapeCandidateStatus.CAPTCHA_REQUIRED,
                                                                     ScrapeCandidateStatus.FAILED),
                    ScrapeCandidateStatus.APPLIED,           Set.of()
            ));

    @Override
    public void validateStatusTransition(DashboardScrapeCandidateEntity candidate,
                                         UpdateCandidateStatusRequest request) {
        ScrapeCandidateStatus current = candidate.getStatus();
        ScrapeCandidateStatus target  = request.getStatus();

        if (current == target) return;

        Set<ScrapeCandidateStatus> allowed = TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new IllegalArgumentException(
                    "Cannot transition candidate " + candidate.getId() +
                    " from " + current + " to " + target + ".");
        }

        if (target == ScrapeCandidateStatus.REJECTED
                && (request.getRemarks() == null || request.getRemarks().isBlank())) {
            throw new IllegalArgumentException(
                    "remarks are required when rejecting a candidate.");
        }

        if (target == ScrapeCandidateStatus.READY_TO_APPLY) {
            if (!candidate.isFound()) {
                throw new IllegalArgumentException(
                        "Cannot mark candidate as READY_TO_APPLY: scrape result has found=false.");
            }
        }
    }

    @Override
    public void validateApplyable(DashboardScrapeCandidateEntity candidate,
                                   DashboardScrapeCandidateProjectEntity project,
                                   ApplyToProjectRequest request) {
        if (!candidate.getStatus().isApplyable()) {
            throw new IllegalArgumentException(
                    "Candidate " + candidate.getId() + " has status " + candidate.getStatus() +
                    " and cannot be applied. Required: SCRAPED, NEEDS_REVIEW, or READY_TO_APPLY.");
        }

        if (!candidate.isFound()) {
            throw new IllegalArgumentException(
                    "Candidate " + candidate.getId() + " has found=false and cannot be applied.");
        }

        if (project == null || isBlank(project.getName())) {
            throw new IllegalArgumentException(
                    "Candidate " + candidate.getId() + " has no project name and cannot create a project draft.");
        }

        if (request.getMode() == ApplyMode.CREATE_NEW_PROJECT) {
            Long builderId = request.getBuilderId() != null
                    ? request.getBuilderId()
                    : candidate.getLinkedBuilderId();
            if (builderId == null) {
                throw new IllegalArgumentException(
                        "builderId is required for CREATE_NEW_PROJECT. " +
                        "Either provide builderId in the request or link a builder to this candidate first.");
            }
        }

        if (request.getMode() == ApplyMode.UPDATE_EXISTING_PROJECT && request.getProjectId() == null) {
            throw new IllegalArgumentException(
                    "projectId is required for UPDATE_EXISTING_PROJECT.");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
