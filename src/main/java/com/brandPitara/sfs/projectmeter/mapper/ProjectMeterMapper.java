package com.brandPitara.sfs.projectmeter.mapper;

import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import com.brandPitara.sfs.projectmeter.dto.ProjectConstructionStageResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterCardResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterSummaryResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectTimelineResponse;
import com.brandPitara.sfs.projectmeter.entity.ProjectConstructionStageEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectMeterSnapshotEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class ProjectMeterMapper {

    private ProjectMeterMapper() {
    }

    public static ProjectMeterSummaryResponse toSummaryResponse(ProjectMeterSnapshotEntity entity) {
        ProjectTimelineResponse timeline = toTimelineResponse(entity);
        return ProjectMeterSummaryResponse.builder()
            .projectId(entity.getProject().getId())
            .constructionProgressPercent(entity.getConstructionProgressPercent())
            .delayDays(entity.getDelayDays())
            .constructionStartDate(entity.getConstructionStartDate())
            .expectedCompletionDate(entity.getExpectedCompletionDate())
            .revisedCompletionDate(entity.getRevisedCompletionDate())
            .timeline(timeline)
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

        ProjectTimelineResponse timeline = snapshot != null ? toTimelineResponse(snapshot) : null;
        String timelineStatus = timeline != null && timeline.getTimelineStatus() != null
            ? timeline.getTimelineStatus()
            : "ON_TRACK";
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
            .projectStartDate(project.getStartDate())
            .startedOn(project.getStartDate())
            .timelineStatus(timelineStatus)
            .timelineLabel(timeline != null ? timeline.getTimelineLabel() : null)
            .timelineHint(timeline != null ? timeline.getTimelineHint() : null)
            .timeline(timeline)
            .delayDays(delayDays)
            .delayed("DELAYED".equals(timelineStatus))
            .lastUpdatedAt(resolveLastUpdatedAt(snapshot))
            .build();
    }

    public static ProjectTimelineResponse toTimelineResponse(ProjectMeterSnapshotEntity snapshot) {
        if (snapshot == null) return null;

        LocalDate original = snapshot.getOriginalCompletionDate() != null
            ? snapshot.getOriginalCompletionDate()
            : snapshot.getExpectedCompletionDate();
        LocalDate latest = snapshot.getLatestReraCompletionDate() != null
            ? snapshot.getLatestReraCompletionDate()
            : (snapshot.getRevisedCompletionDate() != null ? snapshot.getRevisedCompletionDate() : original);
        LocalDate actual = snapshot.getActualCompletionDate();
        int extensions = snapshot.getReraExtensionCount() != null ? Math.max(snapshot.getReraExtensionCount(), 0) : 0;

        Integer delayVsOriginal = snapshot.getDelayVsOriginalDays();
        Integer delayVsLatest = snapshot.getDelayVsLatestReraDays();

        String status = snapshot.getTimelineStatus();
        String label = snapshot.getTimelineLabel();
        String hint = snapshot.getTimelineHint();

        if (status == null || label == null) {
            TimelineComputed computed = computeTimeline(original, latest, actual, extensions);
            if (status == null) status = computed.status();
            if (label == null) label = computed.label();
            if (hint == null) hint = computed.hint();
            if (delayVsOriginal == null) delayVsOriginal = computed.delayVsOriginalDays();
            if (delayVsLatest == null) delayVsLatest = computed.delayVsLatestReraDays();
        }

        return ProjectTimelineResponse.builder()
            .originalCompletionDate(original)
            .latestReraCompletionDate(latest)
            .actualCompletionDate(actual)
            .reraExtensionCount(extensions)
            .delayVsOriginalDays(delayVsOriginal)
            .delayVsLatestReraDays(delayVsLatest)
            .timelineStatus(status)
            .timelineLabel(label)
            .timelineHint(hint)
            .build();
    }

    public static TimelineComputed computeTimeline(
        LocalDate original,
        LocalDate latest,
        LocalDate actual,
        Integer extensionCount
    ) {
        int extensions = extensionCount != null ? Math.max(extensionCount, 0) : 0;
        LocalDate today = LocalDate.now();
        LocalDate effectiveLatest = latest != null ? latest : original;

        Integer delayVsOriginal = daysLate(original, actual != null ? actual : today);
        Integer delayVsLatest = daysLate(effectiveLatest, actual != null ? actual : today);

        if (actual != null) {
            if (original == null && effectiveLatest == null) {
                return new TimelineComputed("COMPLETED_UNKNOWN", "Completed", "Timeline dates unavailable", null, null);
            }
            if (delayVsOriginal == null || delayVsOriginal <= 0) {
                return new TimelineComputed("COMPLETED_ON_TIME", "Completed on time", "Completed within the original RERA promise", delayVsOriginal, delayVsLatest);
            }
            if (delayVsLatest != null && delayVsLatest <= 0) {
                String label = extensions > 0
                    ? "Completed after " + extensions + " RERA " + plural(extensions, "extension", "extensions")
                    : "Completed within revised RERA timeline";
                String hint = "Delayed " + delayVsOriginal + " days against original promise, within latest RERA-approved date";
                return new TimelineComputed("COMPLETED_WITHIN_REVISED_RERA", label, hint, delayVsOriginal, delayVsLatest);
            }
            String label = "Completed late";
            String hint = delayVsLatest != null
                ? "Completed " + delayVsLatest + " days after latest RERA-approved date"
                : "Completed after original promise";
            return new TimelineComputed("COMPLETED_LATE", label, hint, delayVsOriginal, delayVsLatest);
        }

        if (effectiveLatest != null && today.isAfter(effectiveLatest)) {
            String label = delayVsLatest != null ? delayVsLatest + "d Delayed" : "Delayed";
            String hint = "Past latest RERA-approved completion date";
            return new TimelineComputed("DELAYED", label, hint, delayVsOriginal, delayVsLatest);
        }

        if (extensions > 0) {
            String label = "RERA timeline extended " + extensions + " " + plural(extensions, "time", "times");
            String hint = effectiveLatest != null ? "Latest RERA-approved date is " + effectiveLatest : "RERA timeline has been extended";
            return new TimelineComputed("EXTENDED_ON_TRACK", label, hint, delayVsOriginal, delayVsLatest);
        }

        return new TimelineComputed("ON_TRACK", "On Track", "Within current RERA timeline", delayVsOriginal, delayVsLatest);
    }

    private static Integer daysLate(LocalDate baseline, LocalDate actualOrToday) {
        if (baseline == null || actualOrToday == null || !actualOrToday.isAfter(baseline)) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(baseline, actualOrToday);
    }

    private static String plural(int count, String singular, String plural) {
        return count == 1 ? singular : plural;
    }

    public record TimelineComputed(
        String status,
        String label,
        String hint,
        Integer delayVsOriginalDays,
        Integer delayVsLatestReraDays
    ) {}

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
