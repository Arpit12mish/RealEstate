package com.brandPitara.sfs.builderimprovement.service.impl;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.builderimprovement.composer.BuilderImprovementPageComposer;
import com.brandPitara.sfs.builderimprovement.dto.response.BuilderImprovementResponse;
import com.brandPitara.sfs.builderimprovement.dto.response.BuilderImprovementSummaryResponse;
import com.brandPitara.sfs.builderimprovement.entity.BuilderImprovementProfileEntity;
import com.brandPitara.sfs.builderimprovement.repository.*;
import com.brandPitara.sfs.builderimprovement.service.BuilderImprovementQueryService;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BuilderImprovementQueryServiceImpl implements BuilderImprovementQueryService {

    private final BuilderRepository builderRepository;
    private final ProjectRepository projectRepository;
    private final ProjectPublicVisibilityPolicy projectPublicVisibilityPolicy;

    private final BuilderImprovementProfileRepository profileRepository;
    private final BuilderImprovementActionRepository actionRepository;
    private final BuilderImprovementIssueRepository issueRepository;
    private final BuilderAfterSalesUpgradeRepository afterSalesUpgradeRepository;
    private final BuilderImprovementTimelineRepository timelineRepository;

    private final BuilderImprovementPageComposer pageComposer;

    @Override
    @Transactional(readOnly = true)
    public BuilderImprovementSummaryResponse getBuilderImprovementSummary(Long builderId) {
        BuilderEntity builder = getPublicBuilder(builderId);

        return profileRepository
                .findByBuilder_IdAndProjectIsNullAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
                        builderId,
                        ReviewStatus.APPROVED
                )
                .map(profile -> BuilderImprovementSummaryResponse.builder()
                        .builderId(builder.getId())
                        .builderName(builder.getName())
                        .builderLogoUrl(builder.getLogoUrl())
                        .enabled(true)
                        .profileId(profile.getId())
                        .badgeText(profile.getBadgeText())
                        .ctaText(profile.getBadgeText())
                        .headline(resolveSummaryHeadline(profile))
                        .overallStatus(profile.getOverallStatus() != null ? profile.getOverallStatus().name() : null)
                        .evidenceStatus(profile.getEvidenceStatus() != null ? profile.getEvidenceStatus().name() : null)
                        .lastReviewedAt(profile.getLastReviewedAt())
                        .build())
                .orElseGet(() -> BuilderImprovementSummaryResponse.builder()
                        .builderId(builder.getId())
                        .builderName(builder.getName())
                        .builderLogoUrl(builder.getLogoUrl())
                        .enabled(false)
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public BuilderImprovementResponse getBuilderLevelJourney(Long builderId) {
        getPublicBuilder(builderId);

        BuilderImprovementProfileEntity profile = profileRepository
                .findByBuilder_IdAndProjectIsNullAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
                        builderId,
                        ReviewStatus.APPROVED
                )
                .orElseThrow(() -> new NotFoundException(
                        "Builder improvement journey not found for builder: " + builderId
                ));

        return composePublicResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public BuilderImprovementResponse getProjectLevelJourney(Long builderId, Long projectId) {
        getPublicBuilder(builderId);

        ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

        projectPublicVisibilityPolicy.assertPubliclyVisible(project, projectId);

        if (project.getBuilder() == null || !project.getBuilder().getId().equals(builderId)) {
            throw new NotFoundException("Project not found for builder: " + projectId);
        }

        BuilderImprovementProfileEntity profile = profileRepository
                .findByBuilder_IdAndProject_IdAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
                        builderId,
                        projectId,
                        ReviewStatus.APPROVED
                )
                .orElseThrow(() -> new NotFoundException(
                        "Project improvement journey not found for project: " + projectId
                ));

        return composePublicResponse(profile);
    }

    private BuilderImprovementResponse composePublicResponse(BuilderImprovementProfileEntity profile) {
        Long profileId = profile.getId();

        return pageComposer.compose(
                profile,
                actionRepository.findByProfile_IdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(profileId),
                issueRepository.findByProfile_IdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(profileId),
                afterSalesUpgradeRepository.findByProfile_IdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(profileId),
                timelineRepository.findByProfile_IdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(profileId)
        );
    }

    private BuilderEntity getPublicBuilder(Long builderId) {
        BuilderEntity builder = builderRepository.findByIdAndDeletedFalse(builderId)
                .orElseThrow(() -> new NotFoundException("Builder not found: " + builderId));

        if (!Boolean.TRUE.equals(builder.getActive()) || !Boolean.TRUE.equals(builder.getPublished())) {
            throw new NotFoundException("Builder not found: " + builderId);
        }

        return builder;
    }

    private String resolveSummaryHeadline(BuilderImprovementProfileEntity profile) {
        if (hasText(profile.getSummary())) {
            return profile.getSummary();
        }

        if (hasText(profile.getHeroTitle())) {
            return profile.getHeroTitle();
        }

        return "Builder improvement journey available";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}