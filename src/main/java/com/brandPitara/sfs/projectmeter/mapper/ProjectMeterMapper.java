package com.brandPitara.sfs.projectmeter.mapper;

import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import com.brandPitara.sfs.projectmeter.dto.ProjectConstructionStageResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterCardResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterSummaryResponse;
import com.brandPitara.sfs.projectmeter.entity.ProjectConstructionStageEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectMeterSnapshotEntity;

import java.time.OffsetDateTime;
import java.util.List;

public final class ProjectMeterMapper {

    private ProjectMeterMapper() {
    }

    public static ProjectMeterSummaryResponse toSummaryResponse(ProjectMeterSnapshotEntity entity) {
        return ProjectMeterSummaryResponse.builder()
            .projectId(entity.getProject().getId())
            .constructionProgressPercent(entity.getConstructionProgressPercent())
            .delayDays(entity.getDelayDays())
            .constructionStartDate(entity.getConstructionStartDate())
            .expectedCompletionDate(entity.getExpectedCompletionDate())
            .revisedCompletionDate(entity.getRevisedCompletionDate())
            .verified(entity.getVerified())
            .computedAt(entity.getComputedAt())
            .lastVerifiedAt(entity.getLastVerifiedAt())
            .lastUpdatedAt(resolveLastUpdatedAt(entity))
            .build();
    }

    public static ProjectConstructionStageResponse toStageResponse(ProjectConstructionStageEntity entity) {
        return ProjectConstructionStageResponse.builder()
            .id(entity.getId())
            .stageCode(entity.getStageCode())
            .stageLabel(entity.getStageLabel())
            .displayOrder(entity.getDisplayOrder())
            .weightPercent(entity.getWeightPercent())
            .progressPercent(entity.getProgressPercent())
            .plannedStartDate(entity.getPlannedStartDate())
            .plannedEndDate(entity.getPlannedEndDate())
            .actualStartDate(entity.getActualStartDate())
            .actualEndDate(entity.getActualEndDate())
            .status(entity.getStatus())
            .remarks(entity.getRemarks())
            .evidenceCount(entity.getEvidenceCount())
            .verified(entity.getVerified())
            .build();
    }

    public static ProjectMeterCardResponse toCardResponse(
        ProjectEntity project,
        ProjectMeterSnapshotEntity snapshot,
        List<ProjectMediaEntity> media
    ) {
        var picked = com.brandPitara.sfs.project.mapper.ProjectMediaPicker.pick(media, false);

        String timelineStatus = "ON_TRACK";
        Integer delayDays = 0;

        if (snapshot != null && snapshot.getDelayDays() != null && snapshot.getDelayDays() > 0) {
            timelineStatus = "DELAYED";
            delayDays = snapshot.getDelayDays();
        } else if (snapshot != null && snapshot.getDelayDays() != null) {
            delayDays = snapshot.getDelayDays();
        }

        return ProjectMeterCardResponse.builder()
            .projectId(project.getId())
            .projectName(project.getName())
            .projectSlug(project.getSlug())
            .builderId(project.getBuilder() != null ? project.getBuilder().getId() : null)
            .builderName(project.getBuilder() != null ? project.getBuilder().getName() : null)
            .builderLogoUrl(project.getBuilder() != null ? project.getBuilder().getLogoUrl() : null)
            .coverImageUrl(picked.coverMediaUrl())
            .addressLine(project.getAddressLine())
            .cityName(project.getCity() != null ? project.getCity().getName() : null)
            .priceMin(project.getPriceMin())
            .priceMax(project.getPriceMax())
            .constructionProgressPercent(snapshot != null ? snapshot.getConstructionProgressPercent() : 0)
            .appreciationPercent(snapshot != null ? snapshot.getPriceAppreciationPercent() : null)
            .constructionStartDate(snapshot != null ? snapshot.getConstructionStartDate() : null)
            .timelineStatus(timelineStatus)
            .delayDays(delayDays)
            .lastUpdatedAt(resolveLastUpdatedAt(snapshot))
            .build();
    }

    public static OffsetDateTime resolveLastUpdatedAt(ProjectMeterSnapshotEntity entity) {
        if (entity == null) return null;

        if (entity.getLastVerifiedAt() != null) {
            return entity.getLastVerifiedAt();
        }

        if (entity.getComputedAt() != null) {
            return entity.getComputedAt();
        }

        return entity.getUpdatedAt();
    }
}