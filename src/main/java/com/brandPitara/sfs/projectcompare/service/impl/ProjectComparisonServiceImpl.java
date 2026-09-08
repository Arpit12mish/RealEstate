package com.brandPitara.sfs.projectcompare.service.impl;

import com.brandPitara.sfs.buildercredibility.dto.BuilderCredibilitySummaryResponse;
import com.brandPitara.sfs.buildercredibility.service.BuilderCredibilityService;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.entity.ProjectConnectivityPlaceEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanEntity;
import com.brandPitara.sfs.project.entity.ProjectMasterPlanEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import com.brandPitara.sfs.project.enums.ProjectMediaType;
import com.brandPitara.sfs.project.repository.ProjectConnectivityPlaceRepository;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanRepository;
import com.brandPitara.sfs.project.repository.ProjectMasterPlanRepository;
import com.brandPitara.sfs.project.repository.ProjectMediaRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.projectcompare.builder.ProjectComparisonOverviewInsightBuilder;
import com.brandPitara.sfs.projectcompare.builder.ProjectComparisonSectionBuilder;
import com.brandPitara.sfs.projectcompare.dto.request.ProjectComparisonRequest;
import com.brandPitara.sfs.projectcompare.dto.response.ComparisonProjectHeader;
import com.brandPitara.sfs.projectcompare.dto.response.ComparisonOverviewInsightResponse;
import com.brandPitara.sfs.projectcompare.dto.response.ComparisonSection;
import com.brandPitara.sfs.projectcompare.dto.response.ProjectComparisonResponse;
import com.brandPitara.sfs.projectcompare.enums.ComparisonSectionKey;
import com.brandPitara.sfs.projectcompare.service.ProjectComparisonService;
import com.brandPitara.sfs.projectmeter.entity.ProjectAmenityProgressEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectComplianceItemEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectConstructionStageEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectLocationScoreEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectMeterSnapshotEntity;
import com.brandPitara.sfs.projectmeter.repository.ProjectAmenityProgressRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectComplianceItemRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectConstructionStageRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectLocationScoreRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectMeterSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectComparisonServiceImpl implements ProjectComparisonService {

    private final ProjectRepository projectRepository;
    private final ProjectMeterSnapshotRepository snapshotRepository;
    private final ProjectLocationScoreRepository locationScoreRepository;
    private final ProjectAmenityProgressRepository amenityRepository;
    private final ProjectComplianceItemRepository complianceRepository;
    private final ProjectConstructionStageRepository stageRepository;
    private final ProjectFloorPlanRepository floorPlanRepository;
    private final ProjectConnectivityPlaceRepository connectivityPlaceRepository;
    private final ProjectMasterPlanRepository masterPlanRepository;
    private final ProjectMediaRepository mediaRepository;
    private final BuilderCredibilityService builderCredibilityService;
    private final ProjectComparisonOverviewInsightBuilder overviewInsightBuilder;
    private final ProjectComparisonSectionBuilder sectionBuilder;

    @Override
    @Transactional(readOnly = true)
    public ProjectComparisonResponse compare(ProjectComparisonRequest request) {
        List<Long> requestedIds = request.getProjectIds();

        // ── 1. Validate request ───────────────────────────────────────────────
        if (requestedIds == null || requestedIds.size() < 2) {
            throw new IllegalArgumentException("At least 2 project IDs are required");
        }
        if (requestedIds.size() > 4) {
            throw new IllegalArgumentException("At most 4 project IDs are allowed");
        }
        Set<Long> seen = new HashSet<>();
        for (Long id : requestedIds) {
            if (id == null) throw new IllegalArgumentException("Project ID must not be null");
            if (!seen.add(id)) {
                throw new IllegalArgumentException("Duplicate project ID: " + id);
            }
        }

        // ── 2. Resolve section keys ───────────────────────────────────────────
        List<ComparisonSectionKey> sectionKeys = resolveSectionKeys(request.getSectionKeys());

        // ── 3. Batch-fetch projects with visibility check ─────────────────────
        List<ProjectEntity> fetched = projectRepository
                .findByIdInAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
                        requestedIds, ReviewStatus.APPROVED);

        if (fetched.size() != requestedIds.size()) {
            Set<Long> foundIds = fetched.stream().map(ProjectEntity::getId).collect(Collectors.toSet());
            Long missing = requestedIds.stream().filter(id -> !foundIds.contains(id)).findFirst().orElse(null);
            throw new NotFoundException("Project not found or not publicly available: " + missing);
        }

        // ── 4. Preserve requested order ───────────────────────────────────────
        Map<Long, ProjectEntity> projectMap = fetched.stream()
                .collect(Collectors.toMap(ProjectEntity::getId, p -> p));
        List<ProjectEntity> projects = requestedIds.stream()
                .map(projectMap::get)
                .toList();

        List<Long> projectIds = requestedIds;

        // ── 5. Batch-fetch all supporting data ────────────────────────────────
        Map<Long, ProjectMeterSnapshotEntity> snapshots =
                snapshotRepository.findByProjectIdIn(projectIds).stream()
                        .collect(Collectors.toMap(s -> s.getProject().getId(), s -> s));

        Map<Long, ProjectLocationScoreEntity> locationScores =
                locationScoreRepository.findByProjectIdIn(projectIds).stream()
                        .collect(Collectors.toMap(ls -> ls.getProject().getId(), ls -> ls));

        Map<Long, List<ProjectAmenityProgressEntity>> amenitiesMap =
                amenityRepository.findByProjectIdInAndActiveTrueAndPublicVisibleTrueOrderByProjectIdAscCategoryDisplayOrderAscDisplayOrderAscIdAsc(projectIds)
                        .stream()
                        .collect(Collectors.groupingBy(a -> a.getProject().getId(), LinkedHashMap::new, Collectors.toList()));

        Map<Long, List<ProjectComplianceItemEntity>> complianceMap =
                complianceRepository.findByProjectIdInOrderByProjectIdAscItemGroupAscDisplayOrderAscIdAsc(projectIds)
                        .stream()
                        .collect(Collectors.groupingBy(c -> c.getProject().getId(), LinkedHashMap::new, Collectors.toList()));

        Map<Long, List<ProjectConstructionStageEntity>> stagesMap =
                stageRepository.findByProjectIdInOrderByProjectIdAscDisplayOrderAscIdAsc(projectIds)
                        .stream()
                        .collect(Collectors.groupingBy(s -> s.getProject().getId(), LinkedHashMap::new, Collectors.toList()));

        Map<Long, List<ProjectFloorPlanEntity>> floorPlansMap =
                floorPlanRepository.findByProjectIdInAndActiveTrueAndDeletedFalseOrderByProjectIdAscSortOrderAscIdAsc(projectIds)
                        .stream()
                        .collect(Collectors.groupingBy(fp -> fp.getProject().getId(), LinkedHashMap::new, Collectors.toList()));

        Map<Long, List<ProjectConnectivityPlaceEntity>> connectivityMap =
                connectivityPlaceRepository.findByProjectIdInAndActiveTrueAndDeletedFalseOrderByProjectIdAscCategoryAscSortOrderAscIdAsc(projectIds)
                        .stream()
                        .collect(Collectors.groupingBy(pl -> pl.getProject().getId(), LinkedHashMap::new, Collectors.toList()));

        Map<Long, ProjectMasterPlanEntity> masterPlans =
                masterPlanRepository.findByProjectIdInAndActiveTrueAndDeletedFalse(projectIds).stream()
                        .collect(Collectors.toMap(
                                plan -> plan.getProject().getId(),
                                plan -> plan,
                                (first, ignored) -> first,
                                LinkedHashMap::new
                        ));

        List<ProjectMediaEntity> media = mediaRepository.findActiveByProjectIds(projectIds);
        Map<Long, List<ProjectMediaEntity>> mediaMap = media.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getProject().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // Hero image: first IMAGE per project (media ordered by sortOrder asc, id asc for determinism)
        Map<Long, ProjectMediaEntity> heroImageMap = new LinkedHashMap<>();
        media.forEach(m -> {
            if (m.getMediaType() == ProjectMediaType.IMAGE) {
                heroImageMap.putIfAbsent(m.getProject().getId(), m);
            }
        });

        // Builder credibility — one batch query group for all unique builders.
        Set<Long> builderIds = projects.stream()
                .filter(p -> p.getBuilder() != null)
                .map(p -> p.getBuilder().getId())
                .collect(Collectors.toSet());
        Map<Long, BuilderCredibilitySummaryResponse> credibilityMap = new HashMap<>();
        try {
            Map<Long, BuilderCredibilitySummaryResponse> summaries =
                    builderCredibilityService.publicGetCredibilitySummaries(builderIds);
            if (summaries != null) {
                credibilityMap.putAll(summaries);
            }
        } catch (Exception ignored) {
            // credibility is enrichment — never fail the comparison for it
        }

        // ── 6. Build headers ──────────────────────────────────────────────────
        List<ComparisonProjectHeader> headers = sectionBuilder.buildHeaders(projects, heroImageMap);

        ComparisonOverviewInsightResponse overviewInsight = sectionKeys.contains(ComparisonSectionKey.OVERVIEW)
                ? overviewInsightBuilder.build(
                        projects,
                        snapshots,
                        locationScores,
                        floorPlansMap,
                        masterPlans,
                        credibilityMap
                )
                : null;

        // ── 7. Build requested sections ───────────────────────────────────────
        List<ComparisonSection> sections = new ArrayList<>();
        for (ComparisonSectionKey key : sectionKeys) {
            ComparisonSection section = switch (key) {
                case VISUAL_COMPARISON -> sectionBuilder.buildVisualComparison(projects, mediaMap);
                case OVERVIEW     -> sectionBuilder.buildOverview(projects, snapshots, overviewInsight);
                case PRICE        -> sectionBuilder.buildPrice(projects, snapshots);
                case UNITS        -> sectionBuilder.buildUnits(projects, floorPlansMap);
                case AMENITIES    -> sectionBuilder.buildAmenities(projects, amenitiesMap, snapshots);
                case LOCATION     -> sectionBuilder.buildLocation(projects, locationScores, connectivityMap);
                case CONSTRUCTION -> sectionBuilder.buildConstruction(projects, snapshots, stagesMap);
                case COMPLIANCE   -> sectionBuilder.buildCompliance(projects, complianceMap);
                case BUILDER      -> sectionBuilder.buildBuilder(projects, credibilityMap);
                case METER        -> sectionBuilder.buildMeter(projects, snapshots, locationScores, complianceMap);
            };
            sections.add(section);
        }

        sections.sort(Comparator.comparingInt(ComparisonSection::getDisplayOrder));

        return ProjectComparisonResponse.builder()
                .projects(headers)
                .sections(sections)
                .build();
    }

    private List<ComparisonSectionKey> resolveSectionKeys(List<ComparisonSectionKey> requested) {
        if (requested == null || requested.isEmpty()) {
            return Arrays.asList(ComparisonSectionKey.values());
        }
        return requested.stream().distinct().toList();
    }
}
