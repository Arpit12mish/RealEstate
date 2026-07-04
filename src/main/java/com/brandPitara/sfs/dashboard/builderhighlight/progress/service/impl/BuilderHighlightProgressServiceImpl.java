package com.brandPitara.sfs.dashboard.builderhighlight.progress.service.impl;

import com.brandPitara.sfs.builder.dto.BuilderLiteRow;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightStatus;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightType;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.dto.BuilderHighlightBuilderProgressResponse;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.dto.BuilderHighlightOverviewResponse;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.dto.BuilderHighlightProgressRow;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.dto.BuilderHighlightRecentActivityResponse;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.dto.BuilderHighlightSectionCoverageResponse;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.dto.BuilderHighlightSectionProgressResponse;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.enums.BuilderHighlightOverallProgressStatus;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.enums.BuilderHighlightSectionStatus;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.repository.BuilderHighlightProgressRepository;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.service.BuilderHighlightProgressService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuilderHighlightProgressServiceImpl implements BuilderHighlightProgressService {

    private static final List<BuilderHighlightType> REQUIRED_SECTIONS = List.of(
        BuilderHighlightType.BUILDER_UPDATE,
        BuilderHighlightType.SOCIAL_IMPACT,
        BuilderHighlightType.NEWS_ARTICLE,
        BuilderHighlightType.SFS_ANALYSIS
    );

    private final BuilderRepository builderRepository;
    private final BuilderHighlightProgressRepository progressRepository;

    @Override
    public BuilderHighlightOverviewResponse getOverview() {
        List<BuilderHighlightProgressRow> rows = progressRepository.findAllProgressRows();
        long totalBuilders = builderRepository.countByDeletedFalse();

        Map<Long, List<BuilderHighlightProgressRow>> byBuilder = rows.stream()
            .collect(Collectors.groupingBy(BuilderHighlightProgressRow::builderId));

        long buildersWithAnyHighlights = byBuilder.size();
        long buildersWithAllRequiredSections = byBuilder.values().stream()
            .filter(builderRows -> countCompletedSections(builderRows) == REQUIRED_SECTIONS.size())
            .count();
        long buildersMissingHighlights = Math.max(0, totalBuilders - buildersWithAnyHighlights);

        long publishedItems = rows.stream().filter(r -> r.status() == BuilderHighlightStatus.PUBLISHED).count();
        long draftItems = rows.stream().filter(r -> r.status() == BuilderHighlightStatus.DRAFT).count();
        long pendingReviewItems = rows.stream().filter(r -> r.status() == BuilderHighlightStatus.PENDING_REVIEW).count();
        long archivedItems = rows.stream().filter(r -> r.status() == BuilderHighlightStatus.ARCHIVED).count();

        List<BuilderHighlightSectionCoverageResponse> sectionCoverage = REQUIRED_SECTIONS.stream()
            .map(type -> buildSectionCoverage(type, rows, totalBuilders))
            .collect(Collectors.toList());

        List<BuilderHighlightRecentActivityResponse> recentActivity = rows.stream()
            .sorted(Comparator.comparing(
                BuilderHighlightProgressRow::updatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
            ))
            .limit(10)
            .map(r -> BuilderHighlightRecentActivityResponse.builder()
                .builderId(r.builderId())
                .builderName(r.builderName())
                .highlightType(r.highlightType())
                .title(r.title())
                .status(r.status())
                .updatedAt(r.updatedAt())
                .build())
            .collect(Collectors.toList());

        return BuilderHighlightOverviewResponse.builder()
            .totalBuilders(totalBuilders)
            .buildersWithAnyHighlights(buildersWithAnyHighlights)
            .buildersWithAllRequiredSections(buildersWithAllRequiredSections)
            .buildersMissingHighlights(buildersMissingHighlights)
            .totalHighlightItems(rows.size())
            .publishedItems(publishedItems)
            .draftItems(draftItems)
            .pendingReviewItems(pendingReviewItems)
            .archivedItems(archivedItems)
            .sectionCoverage(sectionCoverage)
            .recentActivity(recentActivity)
            .build();
    }

    @Override
    public BuilderHighlightBuilderProgressResponse getBuilderProgress(Long builderId) {
        var builder = builderRepository.findByIdAndDeletedFalse(builderId)
            .orElseThrow(() -> new EntityNotFoundException("Builder not found: " + builderId));
        List<BuilderHighlightProgressRow> rows = progressRepository.findProgressRowsByBuilderIds(List.of(builderId));
        return buildBuilderProgress(builderId, builder.getName(), rows);
    }

    @Override
    public Page<BuilderHighlightBuilderProgressResponse> listBuilderProgress(
        String builderIdsCsv,
        String q,
        Long cityId,
        BuilderHighlightOverallProgressStatus highlightStatus,
        BuilderHighlightType missingSection,
        Pageable pageable
    ) {
        boolean batchMode = builderIdsCsv != null && !builderIdsCsv.isBlank();

        List<BuilderLiteRow> candidateBuilders;
        if (batchMode) {
            List<Long> ids = Arrays.stream(builderIdsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toList();
            Map<Long, String> namesById = builderRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(b -> b.getId(), b -> b.getName()));
            candidateBuilders = ids.stream()
                .filter(namesById::containsKey)
                .map(id -> new BuilderLiteRow(id, namesById.get(id)))
                .toList();
        } else {
            candidateBuilders = builderRepository.searchForDashboardProgress(cityId, blankToNull(q));
        }

        List<Long> candidateIds = candidateBuilders.stream().map(BuilderLiteRow::id).toList();
        List<BuilderHighlightProgressRow> rows = candidateIds.isEmpty()
            ? List.of()
            : progressRepository.findProgressRowsByBuilderIds(candidateIds);
        Map<Long, List<BuilderHighlightProgressRow>> byBuilder = rows.stream()
            .collect(Collectors.groupingBy(BuilderHighlightProgressRow::builderId));

        List<BuilderHighlightBuilderProgressResponse> filtered = candidateBuilders.stream()
            .map(b -> buildBuilderProgress(b.id(), b.name(), byBuilder.getOrDefault(b.id(), List.of())))
            .filter(p -> highlightStatus == null || p.getOverallStatus() == highlightStatus)
            .filter(p -> missingSection == null || hasMissingSection(p, missingSection))
            .collect(Collectors.toList());

        if (batchMode) {
            return new PageImpl<>(filtered, Pageable.unpaged(), filtered.size());
        }

        int pageSize = Math.max(1, pageable.getPageSize());
        int pageNumber = Math.max(0, pageable.getPageNumber());
        int fromIndex = Math.min(pageNumber * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        return new PageImpl<>(filtered.subList(fromIndex, toIndex), pageable, filtered.size());
    }

    private boolean hasMissingSection(BuilderHighlightBuilderProgressResponse progress, BuilderHighlightType type) {
        return progress.getSections().stream()
            .anyMatch(s -> s.getHighlightType() == type && s.getStatus() == BuilderHighlightSectionStatus.MISSING);
    }

    private BuilderHighlightBuilderProgressResponse buildBuilderProgress(
        Long builderId,
        String builderName,
        List<BuilderHighlightProgressRow> rows
    ) {
        Map<BuilderHighlightType, List<BuilderHighlightProgressRow>> byType = rows.stream()
            .collect(Collectors.groupingBy(BuilderHighlightProgressRow::highlightType));

        List<BuilderHighlightSectionProgressResponse> sections = REQUIRED_SECTIONS.stream()
            .map(type -> buildSectionProgress(type, byType.getOrDefault(type, List.of())))
            .collect(Collectors.toList());

        int completedSectionCount = (int) sections.stream()
            .filter(s -> s.getStatus() == BuilderHighlightSectionStatus.COMPLETE)
            .count();
        int missingSectionCount = (int) sections.stream()
            .filter(s -> s.getStatus() == BuilderHighlightSectionStatus.MISSING)
            .count();
        boolean anyPendingReview = sections.stream()
            .anyMatch(s -> s.getStatus() == BuilderHighlightSectionStatus.PENDING_REVIEW);
        boolean hasAnyHighlights = !rows.isEmpty();
        boolean hasPublishedPublicHighlights = completedSectionCount > 0;

        BuilderHighlightOverallProgressStatus overallStatus;
        if (completedSectionCount == REQUIRED_SECTIONS.size()) {
            overallStatus = BuilderHighlightOverallProgressStatus.READY;
        } else if (anyPendingReview) {
            overallStatus = BuilderHighlightOverallProgressStatus.NEEDS_REVIEW;
        } else if (hasAnyHighlights) {
            overallStatus = BuilderHighlightOverallProgressStatus.IN_PROGRESS;
        } else {
            overallStatus = BuilderHighlightOverallProgressStatus.NOT_STARTED;
        }

        int completionPercent = (int) Math.round((completedSectionCount * 100.0) / REQUIRED_SECTIONS.size());

        return BuilderHighlightBuilderProgressResponse.builder()
            .builderId(builderId)
            .builderName(builderName)
            .hasAnyHighlights(hasAnyHighlights)
            .hasPublishedPublicHighlights(hasPublishedPublicHighlights)
            .highlightsAvailableForMobile(hasPublishedPublicHighlights)
            .requiredSectionCount(REQUIRED_SECTIONS.size())
            .completedSectionCount(completedSectionCount)
            .missingSectionCount(missingSectionCount)
            .completionPercent(completionPercent)
            .overallStatus(overallStatus)
            .sections(sections)
            .build();
    }

    private BuilderHighlightSectionProgressResponse buildSectionProgress(
        BuilderHighlightType type,
        List<BuilderHighlightProgressRow> rows
    ) {
        int totalItemCount = rows.size();
        int draftCount = (int) rows.stream().filter(r -> r.status() == BuilderHighlightStatus.DRAFT).count();
        int pendingReviewCount = (int) rows.stream().filter(r -> r.status() == BuilderHighlightStatus.PENDING_REVIEW).count();
        int publishedCount = (int) rows.stream().filter(r -> r.status() == BuilderHighlightStatus.PUBLISHED).count();
        int publishedPublicItemCount = (int) rows.stream().filter(BuilderHighlightProgressRow::isPubliclyVisible).count();
        OffsetDateTime latestUpdatedAt = rows.stream()
            .map(BuilderHighlightProgressRow::updatedAt)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(null);

        BuilderHighlightSectionStatus status;
        if (publishedPublicItemCount > 0) {
            status = BuilderHighlightSectionStatus.COMPLETE;
        } else if (pendingReviewCount > 0) {
            status = BuilderHighlightSectionStatus.PENDING_REVIEW;
        } else if (totalItemCount > 0) {
            status = BuilderHighlightSectionStatus.DRAFT;
        } else {
            status = BuilderHighlightSectionStatus.MISSING;
        }

        return BuilderHighlightSectionProgressResponse.builder()
            .highlightType(type)
            .label(type.getSectionTitle())
            .required(true)
            .present(totalItemCount > 0)
            .totalItemCount(totalItemCount)
            .draftCount(draftCount)
            .pendingReviewCount(pendingReviewCount)
            .publishedCount(publishedCount)
            .publishedPublicItemCount(publishedPublicItemCount)
            .latestUpdatedAt(latestUpdatedAt)
            .status(status)
            .build();
    }

    private BuilderHighlightSectionCoverageResponse buildSectionCoverage(
        BuilderHighlightType type,
        List<BuilderHighlightProgressRow> allRows,
        long totalBuilders
    ) {
        List<BuilderHighlightProgressRow> typeRows = allRows.stream()
            .filter(r -> r.highlightType() == type)
            .toList();
        long buildersCovered = typeRows.stream().map(BuilderHighlightProgressRow::builderId).distinct().count();
        long buildersMissing = Math.max(0, totalBuilders - buildersCovered);
        int publishedItems = (int) typeRows.stream().filter(r -> r.status() == BuilderHighlightStatus.PUBLISHED).count();
        int draftItems = (int) typeRows.stream().filter(r -> r.status() == BuilderHighlightStatus.DRAFT).count();
        int pendingReviewItems = (int) typeRows.stream().filter(r -> r.status() == BuilderHighlightStatus.PENDING_REVIEW).count();

        return BuilderHighlightSectionCoverageResponse.builder()
            .highlightType(type)
            .label(type.getSectionTitle())
            .buildersCovered((int) buildersCovered)
            .buildersMissing((int) buildersMissing)
            .totalItems(typeRows.size())
            .publishedItems(publishedItems)
            .draftItems(draftItems)
            .pendingReviewItems(pendingReviewItems)
            .build();
    }

    private int countCompletedSections(List<BuilderHighlightProgressRow> builderRows) {
        Map<BuilderHighlightType, List<BuilderHighlightProgressRow>> byType = builderRows.stream()
            .collect(Collectors.groupingBy(BuilderHighlightProgressRow::highlightType));
        int completed = 0;
        for (BuilderHighlightType type : REQUIRED_SECTIONS) {
            boolean complete = byType.getOrDefault(type, List.of()).stream()
                .anyMatch(BuilderHighlightProgressRow::isPubliclyVisible);
            if (complete) completed++;
        }
        return completed;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
