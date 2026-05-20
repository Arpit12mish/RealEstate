package com.brandPitara.sfs.dashboard.scraping.candidate.service.impl;

import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.projectmeter.dto.DashboardProjectComplianceItemRequest;
import com.brandPitara.sfs.dashboard.projectmeter.dto.DashboardProjectCostBreakdownRequest;
import com.brandPitara.sfs.dashboard.projectmeter.dto.DashboardProjectLandUtilizationRequest;
import com.brandPitara.sfs.dashboard.projectmeter.service.DashboardProjectMeterWriteService;
import com.brandPitara.sfs.dashboard.project.service.DashboardProjectOwnershipService;
import com.brandPitara.sfs.dashboard.scraping.candidate.dto.ApplyToProjectRequest;
import com.brandPitara.sfs.dashboard.scraping.candidate.dto.ApplyToProjectResponse;
import com.brandPitara.sfs.dashboard.scraping.candidate.entity.*;
import com.brandPitara.sfs.dashboard.scraping.candidate.enums.ApplyMode;
import com.brandPitara.sfs.dashboard.scraping.candidate.enums.ScrapeCandidateStatus;
import com.brandPitara.sfs.dashboard.scraping.candidate.repository.ScrapeCandidateComplianceItemRepository;
import com.brandPitara.sfs.dashboard.scraping.candidate.repository.ScrapeCandidateCostBreakdownRepository;
import com.brandPitara.sfs.dashboard.scraping.candidate.repository.ScrapeCandidateLandUtilizationRepository;
import com.brandPitara.sfs.dashboard.scraping.candidate.repository.ScrapeCandidateProjectRepository;
import com.brandPitara.sfs.dashboard.scraping.candidate.repository.ScrapeCandidateRepository;
import com.brandPitara.sfs.dashboard.scraping.candidate.service.ScrapeCandidateApplyService;
import com.brandPitara.sfs.dashboard.scraping.candidate.service.ScrapeCandidateValidationService;
import com.brandPitara.sfs.project.dto.ProjectResponse;
import com.brandPitara.sfs.project.dto.ProjectUpsertRequest;
import com.brandPitara.sfs.project.enums.ProjectStatus;
import com.brandPitara.sfs.project.enums.PropertyType;
import com.brandPitara.sfs.project.service.ProjectService;
import com.brandPitara.sfs.projectmeter.entity.ProjectComplianceItemEntity;
import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceGroup;
import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceStatus;
import com.brandPitara.sfs.projectmeter.repository.ProjectComplianceItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapeCandidateApplyServiceImpl implements ScrapeCandidateApplyService {

    private static final int MAX_RERA_NUMBER_LENGTH = 50;

    private final ScrapeCandidateServiceImpl candidateService;
    private final ScrapeCandidateValidationService validationService;
    private final DashboardCurrentUserService currentUserService;

    private final ScrapeCandidateProjectRepository     projectRepo;
    private final ScrapeCandidateComplianceItemRepository complianceRepo;
    private final ScrapeCandidateCostBreakdownRepository  costBreakdownRepo;
    private final ScrapeCandidateLandUtilizationRepository landUtilRepo;
    private final ScrapeCandidateRepository candidateRepository;

    private final ProjectService                      projectService;
    private final DashboardProjectOwnershipService    ownershipService;
    private final DashboardProjectMeterWriteService   meterService;
    private final ProjectComplianceItemRepository     complianceItemRepository;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ApplyToProjectResponse applyToProject(Long candidateId, ApplyToProjectRequest request) {
        DashboardScrapeCandidateEntity candidate = candidateService.loadOrThrow(candidateId);

        DashboardScrapeCandidateProjectEntity candidateProject =
                projectRepo.findByCandidateId(candidateId).orElse(null);

        validationService.validateApplyable(candidate, candidateProject, request);

        List<String> warnings = new ArrayList<>();

        Long projectId;
        boolean created;

        if (request.getMode() == ApplyMode.CREATE_NEW_PROJECT) {
            Long builderId = resolveBuilderId(candidate, request);
            ProjectUpsertRequest upsert = buildUpsertFromCandidate(candidateProject, request, null, warnings);
            ProjectResponse created_ = projectService.create(builderId, upsert);
            projectId = created_.getId();
            created = true;
            ownershipService.assignCurrentUserAsCreator(projectId);
            log.info("ScrapeCandidateApply: created project={} for candidateId={}", projectId, candidateId);
        } else {
            projectId = request.getProjectId();
            ownershipService.assertCurrentUserCanEditProject(projectId);
            ProjectResponse existing = projectService.adminGet(projectId);
            ProjectUpsertRequest upsert = buildUpsertFromCandidate(candidateProject, request, existing, warnings);
            projectService.update(projectId, upsert);
            created = false;
            log.info("ScrapeCandidateApply: updated project={} for candidateId={}", projectId, candidateId);
        }

        int[] compCounts = applyComplianceItems(candidateId, projectId, request.isOverwrite(), warnings);
        List<String> meterSections = applyMeterSections(candidateId, projectId, request.isOverwrite(), warnings);

        int fieldsApplied  = countAppliedFields(candidateProject);
        int fieldsSkipped  = countSkippedFields(candidateProject, created, request);

        markApplied(candidate, projectId);

        log.info("ScrapeCandidateApply: candidateId={} -> projectId={} created={} compCreated={} compSkipped={}",
                candidateId, projectId, created, compCounts[0], compCounts[1]);

        return ApplyToProjectResponse.builder()
                .createdProject(created)
                .projectId(projectId)
                .candidateId(candidateId)
                .fieldsApplied(fieldsApplied)
                .fieldsSkipped(fieldsSkipped)
                .complianceItemsCreated(compCounts[0])
                .complianceItemsSkipped(compCounts[1])
                .meterSectionsCreated(meterSections)
                .warnings(warnings)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Project upsert building
    // ─────────────────────────────────────────────────────────────────────────

    private ProjectUpsertRequest buildUpsertFromCandidate(
            DashboardScrapeCandidateProjectEntity cp,
            ApplyToProjectRequest request,
            ProjectResponse existing,
            List<String> warnings) {

        boolean overwrite = request.isOverwrite();

        // For CREATE, existing is null — every candidate field is used if non-null.
        // For UPDATE, only apply candidate value if (overwrite OR existing field is null).

        String name = merge(cp != null ? cp.getName() : null,
                existing != null ? existing.getName() : null, overwrite);
        if (name == null && existing != null) name = existing.getName();

        String description = merge(cp != null ? cp.getDescription() : null,
                existing != null ? existing.getDescription() : null, overwrite);

        Long cityId = request.getCityId() != null ? request.getCityId()
                : (existing != null ? existing.getCityId() : null);

        String addressLine = merge(cp != null ? cp.getAddressLine() : null,
                existing != null ? existing.getAddressLine() : null, overwrite);

        Double latitude  = mergeDouble(cp != null && cp.getLatitude() != null ? cp.getLatitude().doubleValue() : null,
                existing != null ? existing.getLatitude() : null, overwrite);
        Double longitude = mergeDouble(cp != null && cp.getLongitude() != null ? cp.getLongitude().doubleValue() : null,
                existing != null ? existing.getLongitude() : null, overwrite);

        Long priceMin = mergeLong(cp != null ? cp.getPriceMin() : null,
                existing != null ? existing.getPriceMin() : null, overwrite);
        Long priceMax = mergeLong(cp != null ? cp.getPriceMax() : null,
                existing != null ? existing.getPriceMax() : null, overwrite);

        var possessionDate = cp != null && cp.getPossessionDate() != null
                && (overwrite || existing == null || existing.getPossessionDate() == null)
                ? cp.getPossessionDate()
                : (existing != null ? existing.getPossessionDate() : null);

        String rawRera = cp != null ? cp.getReraNumber() : null;
        if (rawRera != null && rawRera.length() > MAX_RERA_NUMBER_LENGTH) {
            warnings.add("RERA number truncated from " + rawRera.length() +
                    " to " + MAX_RERA_NUMBER_LENGTH + " chars to fit project field.");
            rawRera = rawRera.substring(0, MAX_RERA_NUMBER_LENGTH);
        }
        String reraNumber = merge(rawRera,
                existing != null ? existing.getReraNumber() : null, overwrite);

        ProjectStatus status = null;
        if (cp != null && cp.getProjectStatus() != null) {
            try {
                ProjectStatus parsed = ProjectStatus.valueOf(cp.getProjectStatus());
                status = (overwrite || existing == null || existing.getStatus() == null)
                        ? parsed : existing.getStatus();
            } catch (IllegalArgumentException e) {
                warnings.add("Unrecognized projectStatus in candidate: " + cp.getProjectStatus());
                status = existing != null ? existing.getStatus() : null;
            }
        } else if (existing != null) {
            status = existing.getStatus();
        }

        Set<PropertyType> propertyTypes = null;
        if (cp != null && cp.getPropertyTypes() != null && !cp.getPropertyTypes().isBlank()) {
            Set<PropertyType> parsed = parsePropertyTypes(cp.getPropertyTypes(), warnings);
            if (!parsed.isEmpty()) {
                propertyTypes = (overwrite || existing == null
                        || existing.getPropertyTypes() == null
                        || existing.getPropertyTypes().isEmpty())
                        ? parsed : existing.getPropertyTypes();
            }
        }
        if (propertyTypes == null && existing != null) {
            propertyTypes = existing.getPropertyTypes();
        }

        return ProjectUpsertRequest.builder()
                .name(name)
                .description(description)
                .cityId(cityId)
                .addressLine(addressLine)
                .latitude(latitude)
                .longitude(longitude)
                .priceMin(priceMin)
                .priceMax(priceMax)
                .possessionDate(possessionDate)
                .reraNumber(reraNumber)
                .status(status)
                .propertyTypes(propertyTypes)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Compliance items
    // ─────────────────────────────────────────────────────────────────────────

    private int[] applyComplianceItems(Long candidateId, Long projectId,
                                        boolean overwrite, List<String> warnings) {
        List<DashboardScrapeCandidateComplianceItemEntity> candidateItems =
                complianceRepo.findByCandidateIdOrderByDisplayOrderAscIdAsc(candidateId);

        if (candidateItems.isEmpty()) return new int[]{0, 0};

        List<ProjectComplianceItemEntity> existing =
                complianceItemRepository.findByProjectIdOrderByDisplayOrderAscIdAsc(projectId);
        Map<String, ProjectComplianceItemEntity> byKey = existing.stream()
                .filter(e -> e.getItemKey() != null)
                .collect(Collectors.toMap(ProjectComplianceItemEntity::getItemKey,
                        e -> e, (a, b) -> a));

        int created = 0;
        int skipped = 0;

        for (DashboardScrapeCandidateComplianceItemEntity ci : candidateItems) {
            if (ci.getItemKey() == null || ci.getItemLabel() == null
                    || ci.getItemGroup() == null || ci.getStatus() == null) {
                warnings.add("Skipped compliance item with missing required fields: " + ci.getItemKey());
                skipped++;
                continue;
            }

            ProjectComplianceGroup group;
            ProjectComplianceStatus status;
            try {
                group  = ProjectComplianceGroup.valueOf(ci.getItemGroup());
                status = ProjectComplianceStatus.valueOf(ci.getStatus());
            } catch (IllegalArgumentException e) {
                warnings.add("Skipped compliance item '" + ci.getItemKey() +
                        "': unrecognized group or status value.");
                skipped++;
                continue;
            }

            DashboardProjectComplianceItemRequest req = new DashboardProjectComplianceItemRequest();
            req.setItemGroup(group);
            req.setItemKey(truncate(ci.getItemKey(), 100));
            req.setItemLabel(truncate(ci.getItemLabel(), 180));
            req.setStatus(status);
            req.setValueText(truncate(ci.getValueText(), 300));
            req.setDocumentUrl(truncate(ci.getDocumentUrl(), 500));
            req.setRemarks(truncate(ci.getRemarks(), 500));
            req.setDisplayOrder(Math.min(Math.max(ci.getDisplayOrder(), 0), 999));
            req.setVerified(ci.isVerified());

            if (byKey.containsKey(ci.getItemKey())) {
                if (overwrite) {
                    meterService.updateComplianceItem(projectId,
                            byKey.get(ci.getItemKey()).getId(), req);
                    created++;
                } else {
                    warnings.add("Compliance item '" + ci.getItemKey() + "' already exists; skipped (overwrite=false).");
                    skipped++;
                }
            } else {
                meterService.createComplianceItem(projectId, req);
                created++;
            }
        }

        return new int[]{created, skipped};
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Meter sections: cost breakdown + land utilization
    // ─────────────────────────────────────────────────────────────────────────

    private List<String> applyMeterSections(Long candidateId, Long projectId,
                                             boolean overwrite, List<String> warnings) {
        List<String> created = new ArrayList<>();

        costBreakdownRepo.findByCandidateId(candidateId).ifPresent(cb -> {
            boolean applied = applyCostBreakdown(cb, projectId, overwrite, warnings);
            if (applied) created.add("COST_BREAKDOWN");
        });

        landUtilRepo.findByCandidateId(candidateId).ifPresent(lu -> {
            boolean applied = applyLandUtilization(lu, projectId, overwrite, warnings);
            if (applied) created.add("LAND_UTILIZATION");
        });

        return created;
    }

    private boolean applyCostBreakdown(DashboardScrapeCandidateCostBreakdownEntity cb,
                                        Long projectId, boolean overwrite, List<String> warnings) {
        try {
            var existing = meterService.getCostBreakdown(projectId);
            if (existing != null && !overwrite) {
                warnings.add("Cost breakdown already exists for project " + projectId + "; skipped (overwrite=false).");
                return false;
            }
        } catch (Exception ignored) {
            // no existing breakdown — proceed to create
        }

        DashboardProjectCostBreakdownRequest req = new DashboardProjectCostBreakdownRequest();
        req.setLandCost(cb.getLandCost());
        req.setConstructionCost(cb.getConstructionCost());
        req.setInfrastructureCost(cb.getInfrastructureCost());
        req.setOtherCost(cb.getOtherCost());
        req.setTotalCost(cb.getTotalCost());
        req.setSourceLabel(cb.getSourceLabel());
        req.setRemarks(cb.getRemarks());
        req.setVerified(cb.isVerified());
        meterService.upsertCostBreakdown(projectId, req);
        return true;
    }

    private boolean applyLandUtilization(DashboardScrapeCandidateLandUtilizationEntity lu,
                                          Long projectId, boolean overwrite, List<String> warnings) {
        try {
            var existing = meterService.getLandUtilization(projectId);
            if (existing != null && !overwrite) {
                warnings.add("Land utilization already exists for project " + projectId + "; skipped (overwrite=false).");
                return false;
            }
        } catch (Exception ignored) {
            // no existing record — proceed to create
        }

        DashboardProjectLandUtilizationRequest req = new DashboardProjectLandUtilizationRequest();
        req.setTotalLandAreaSqm(lu.getTotalLandAreaSqm() != null ? lu.getTotalLandAreaSqm().doubleValue() : null);
        req.setResidentialAreaSqm(lu.getResidentialAreaSqm() != null ? lu.getResidentialAreaSqm().doubleValue() : null);
        req.setCommercialAreaSqm(lu.getCommercialAreaSqm() != null ? lu.getCommercialAreaSqm().doubleValue() : null);
        req.setParksAreaSqm(lu.getParksAreaSqm() != null ? lu.getParksAreaSqm().doubleValue() : null);
        req.setOpenAreaSqm(lu.getOpenAreaSqm() != null ? lu.getOpenAreaSqm().doubleValue() : null);
        req.setParkingAreaSqm(lu.getParkingAreaSqm() != null ? lu.getParkingAreaSqm().doubleValue() : null);
        req.setUtilityAreaSqm(lu.getUtilityAreaSqm() != null ? lu.getUtilityAreaSqm().doubleValue() : null);
        meterService.upsertLandUtilization(projectId, req);
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mark candidate applied
    // ─────────────────────────────────────────────────────────────────────────

    private void markApplied(DashboardScrapeCandidateEntity candidate, Long projectId) {
        Long userId = null;
        try { userId = currentUserService.getCurrentUserOrThrow().getId(); } catch (Exception ignored) {}
        candidate.setStatus(ScrapeCandidateStatus.APPLIED);
        candidate.setAppliedProjectId(projectId);
        candidate.setAppliedAt(OffsetDateTime.now());
        candidate.setAppliedByDashboardUserId(userId);
        candidateRepository.save(candidate);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Long resolveBuilderId(DashboardScrapeCandidateEntity candidate, ApplyToProjectRequest request) {
        Long id = request.getBuilderId() != null ? request.getBuilderId() : candidate.getLinkedBuilderId();
        if (id == null) throw new IllegalArgumentException("builderId could not be resolved.");
        return id;
    }

    private String merge(String candidateVal, String existingVal, boolean overwrite) {
        if (candidateVal != null && !candidateVal.isBlank()) {
            return (overwrite || existingVal == null || existingVal.isBlank())
                    ? candidateVal : existingVal;
        }
        return existingVal;
    }

    private Double mergeDouble(Double candidateVal, Double existingVal, boolean overwrite) {
        if (candidateVal != null) {
            return (overwrite || existingVal == null) ? candidateVal : existingVal;
        }
        return existingVal;
    }

    private Long mergeLong(Long candidateVal, Long existingVal, boolean overwrite) {
        if (candidateVal != null) {
            return (overwrite || existingVal == null) ? candidateVal : existingVal;
        }
        return existingVal;
    }

    private Set<PropertyType> parsePropertyTypes(String stored, List<String> warnings) {
        Set<PropertyType> types = new LinkedHashSet<>();
        for (String part : stored.split(",")) {
            try {
                types.add(PropertyType.valueOf(part.trim()));
            } catch (IllegalArgumentException e) {
                warnings.add("Unrecognized propertyType in candidate: " + part.trim());
            }
        }
        return types;
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }

    private int countAppliedFields(DashboardScrapeCandidateProjectEntity cp) {
        if (cp == null) return 0;
        int count = 0;
        if (cp.getName()           != null) count++;
        if (cp.getDescription()    != null) count++;
        if (cp.getCityName()       != null) count++;
        if (cp.getAddressLine()    != null) count++;
        if (cp.getLatitude()       != null) count++;
        if (cp.getLongitude()      != null) count++;
        if (cp.getPriceMin()       != null) count++;
        if (cp.getPriceMax()       != null) count++;
        if (cp.getPossessionDate() != null) count++;
        if (cp.getReraNumber()     != null) count++;
        if (cp.getProjectStatus()  != null) count++;
        if (cp.getPropertyTypes()  != null) count++;
        return count;
    }

    private int countSkippedFields(DashboardScrapeCandidateProjectEntity cp,
                                    boolean created, ApplyToProjectRequest request) {
        if (created || request.isOverwrite() || cp == null) return 0;
        // For UPDATE with overwrite=false, skipped = null fields in candidate
        int total = 12;
        return total - countAppliedFields(cp);
    }
}
