package com.brandPitara.sfs.builderimprovement.mapper;

import com.brandPitara.sfs.builderimprovement.dto.response.*;
import com.brandPitara.sfs.builderimprovement.entity.*;
import org.springframework.stereotype.Component;

@Component
public class BuilderImprovementMapper {

    public BuilderImprovementActionResponse toActionResponse(BuilderImprovementActionEntity entity) {
        if (entity == null) return null;

        return BuilderImprovementActionResponse.builder()
                .id(entity.getId())
                .actionType(entity.getActionType() != null ? entity.getActionType().name() : null)
                .title(entity.getTitle())
                .subtitle(entity.getSubtitle())

                // UI-specific naming
                .problem(entity.getContextText())
                .actionTaken(entity.getActionText())
                .result(entity.getResultText())

                .iconKey(entity.getIconKey())
                .impactLevel(entity.getImpactLevel() != null ? entity.getImpactLevel().name() : null)
                .evidenceStatus(entity.getEvidenceStatus() != null ? entity.getEvidenceStatus().name() : null)
                .displayOrder(entity.getDisplayOrder())
                .build();
    }

public BuilderImprovementIssueResponse toIssueResponse(BuilderImprovementIssueEntity entity) {
    if (entity == null) return null;

    String status = entity.getStatus() != null ? entity.getStatus().name() : null;

    String resolvedDateLabel = formatOffsetDate(entity.getResolvedAt());
    String targetDateLabel = formatLocalDate(entity.getTargetResolutionDate());

    String dateLabel = resolvedDateLabel;
    if (dateLabel == null) {
        dateLabel = targetDateLabel;
    }
    if (dateLabel == null) {
        dateLabel = formatOffsetDate(entity.getReportedAt());
    }

    return BuilderImprovementIssueResponse.builder()
            .id(entity.getId())
            .issueType(entity.getIssueType() != null ? entity.getIssueType().name() : null)

            .title(entity.getTitle())
            .description(entity.getDescription())
            .shortDescription(entity.getDescription())

            .status(status)
            .statusLabel(toReadableStatus(status))

            .reportedAt(entity.getReportedAt())
            .resolvedAt(entity.getResolvedAt())
            .targetResolutionDate(entity.getTargetResolutionDate())

            .dateLabel(dateLabel)
            .resolvedDateLabel(resolvedDateLabel)
            .targetDateLabel(targetDateLabel)

            .actionText(entity.getResolutionSummary())
            .delayReason(entity.getDelayReason())

            .evidenceStatus(entity.getEvidenceStatus() != null ? entity.getEvidenceStatus().name() : null)
            .displayOrder(entity.getDisplayOrder())
            .build();
}

private String formatOffsetDate(java.time.OffsetDateTime dateTime) {
    if (dateTime == null) {
        return null;
    }

    return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"));
}

private String formatLocalDate(java.time.LocalDate date) {
    if (date == null) {
        return null;
    }

    return date.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"));
}

private String toReadableStatus(String status) {
    if (status == null || status.isBlank()) {
        return null;
    }

    return status.replace("_", " ");
}

public BuilderAfterSalesUpgradeResponse toAfterSalesUpgradeResponse(BuilderAfterSalesUpgradeEntity entity) {
    if (entity == null) return null;

    return BuilderAfterSalesUpgradeResponse.builder()
            .id(entity.getId())

            // Card
            .cardProblem(entity.getTitle())
            .cardAction(entity.getShortSummary())
            .iconKey(entity.getIconKey())

            // Bottom sheet
            .detailTitle(entity.getTitle())

            .legacyIssueLabel("LEGACY ISSUE")
            .legacyIssueTitle(entity.getLegacyIssueTitle())
            .legacyIssueDescription(entity.getLegacyIssueDescription())

            .solutionLabel("THE SOLUTION")
            .solutionTitle(entity.getSolutionTitle())
            .solutionDescription(entity.getSolutionDescription())

            .metricValue(entity.getMetricValue())
            .metricLabel(entity.getMetricLabel())

            .resultTitle(entity.getResultTitle())
            .resultDescription(entity.getResultDescription())

            .implementedAt(entity.getImplementedAt())
            .implementedMonthYear(formatMonthYear(entity.getImplementedAt()))
            .implementationLabel(buildImplementationLabel(entity.getImplementedAt()))

            .impactLevel(entity.getImpactLevel() != null ? entity.getImpactLevel().name() : null)
            .evidenceStatus(entity.getEvidenceStatus() != null ? entity.getEvidenceStatus().name() : null)
            .displayOrder(entity.getDisplayOrder())
            .build();
}

private String formatMonthYear(java.time.LocalDate date) {
    if (date == null) {
        return null;
    }

    return date.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy"));
}

private String buildImplementationLabel(java.time.LocalDate date) {
    String monthYear = formatMonthYear(date);
    return monthYear == null ? null : monthYear + " Implementation";
}

public BuilderImprovementTimelineResponse toTimelineResponse(BuilderImprovementTimelineEntity entity) {
    if (entity == null) return null;

    String monthLabel = resolveTimelineMonthLabel(entity);
    String detailHeader = buildTimelineDetailHeader(monthLabel, entity.getTitle());

    return BuilderImprovementTimelineResponse.builder()
            .id(entity.getId())
            .timelineType(entity.getTimelineType() != null ? entity.getTimelineType().name() : null)
            .iconKey(entity.getIconKey())

            .eventDate(entity.getEventDate())
            .monthLabel(monthLabel)
            .detailHeader(detailHeader)

            .title(entity.getTitle())
            .shortDescription(entity.getShortDescription())

            .problemLabel("Problem Observed")
            .problemObserved(entity.getProblemObserved())

            .actionLabel("Action Taken")
            .actionTaken(entity.getActionTaken())

            .currentStatusLabel("CURRENT STATUS")
            .currentStatus(entity.getCurrentStatus())

            .evidenceStatus(entity.getEvidenceStatus() != null ? entity.getEvidenceStatus().name() : null)
            .displayOrder(entity.getDisplayOrder())
            .build();
}

private String resolveTimelineMonthLabel(BuilderImprovementTimelineEntity entity) {
    if (entity.getEventMonthLabel() != null && !entity.getEventMonthLabel().isBlank()) {
        return entity.getEventMonthLabel();
    }

    if (entity.getEventDate() == null) {
        return null;
    }

    return entity.getEventDate()
            .format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy"))
            .toUpperCase();
}

private String buildTimelineDetailHeader(String monthLabel, String title) {
    if (monthLabel == null || monthLabel.isBlank()) {
        return title;
    }

    if (title == null || title.isBlank()) {
        return monthLabel;
    }

    return monthLabel + " • " + title;
}

    public BuilderImprovementBuilderResponseDto toBuilderResponse(BuilderImprovementProfileEntity profile) {
        if (profile == null) return null;

        boolean hasAnyBuilderResponse =
                hasText(profile.getBuilderResponseTitle())
                        || hasText(profile.getBuilderResponsePersonName())
                        || hasText(profile.getBuilderResponsePersonDesignation())
                        || hasText(profile.getBuilderResponseText());

        if (!hasAnyBuilderResponse) {
            return null;
        }

        return BuilderImprovementBuilderResponseDto.builder()
                .title(profile.getBuilderResponseTitle())
                .personName(profile.getBuilderResponsePersonName())
                .personDesignation(profile.getBuilderResponsePersonDesignation())
                .responseText(profile.getBuilderResponseText())
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}