package com.brandPitara.sfs.dashboard.project.service.impl;

import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.dashboard.project.dto.DashboardMobilePreviewMeta;
import com.brandPitara.sfs.dashboard.project.dto.DashboardMobilePreviewResponse;
import com.brandPitara.sfs.dashboard.project.service.DashboardMobilePreviewService;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.dto.ProjectResponse;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import com.brandPitara.sfs.project.repository.ProjectMediaRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.project.service.ProjectDetailComposer;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterCardResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterDetailResponse;
import com.brandPitara.sfs.projectmeter.entity.ProjectMeterSnapshotEntity;
import com.brandPitara.sfs.projectmeter.mapper.ProjectMeterMapper;
import com.brandPitara.sfs.projectmeter.repository.ProjectMeterSnapshotRepository;
import com.brandPitara.sfs.projectmeter.service.ProjectMeterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardMobilePreviewServiceImpl implements DashboardMobilePreviewService {

    private final ProjectRepository projectRepository;
    private final ProjectMediaRepository projectMediaRepository;
    private final ProjectMeterSnapshotRepository projectMeterSnapshotRepository;
    private final ProjectMeterService projectMeterService;
    private final ProjectDetailComposer projectDetailComposer;

    @Override
    @Transactional(readOnly = true)
    public DashboardMobilePreviewResponse getPreview(Long projectId) {
        ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

        List<ProjectMediaEntity> media = projectMediaRepository
            .findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdDesc(projectId);

        ProjectMeterSnapshotEntity snapshot = projectMeterSnapshotRepository
            .findByProjectId(projectId).orElse(null);

        ProjectMeterDetailResponse meterDetail = safeGetMeterDetail(projectId);

        ProjectMeterCardResponse card = ProjectMeterMapper.toCardResponse(project, snapshot, media);

        ProjectResponse detail = projectDetailComposer.composeForPreview(project, media, meterDetail);

        List<String> warnings = buildWarnings(project, card, detail, meterDetail);
        List<String> missingSections = buildMissingSections(project, card, detail, meterDetail);

        DashboardMobilePreviewMeta meta = DashboardMobilePreviewMeta.builder()
            .projectId(project.getId())
            .projectName(project.getName())
            .reviewStatus(project.getReviewStatus())
            .published(Boolean.TRUE.equals(project.getPublished()))
            .active(Boolean.TRUE.equals(project.getActive()))
            .previewMode("DASHBOARD")
            .generatedAt(OffsetDateTime.now())
            .warnings(warnings)
            .missingSections(missingSections)
            .build();

        return DashboardMobilePreviewResponse.builder()
            .meta(meta)
            .card(card)
            .detail(detail)
            .meter(meterDetail)
            .build();
    }

    private ProjectMeterDetailResponse safeGetMeterDetail(Long projectId) {
        try {
            return projectMeterService.dashboardGetMeterDetail(projectId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<String> buildWarnings(ProjectEntity project, ProjectMeterCardResponse card,
                                       ProjectResponse detail, ProjectMeterDetailResponse meter) {
        List<String> warnings = new ArrayList<>();

        if (!Boolean.TRUE.equals(project.getPublished())) {
            warnings.add("Project is not published");
        }
        if (project.getReviewStatus() != ReviewStatus.APPROVED) {
            warnings.add("Project review status is " + project.getReviewStatus()
                + " — must be APPROVED before public visibility");
        }
        if (!Boolean.TRUE.equals(project.getActive())) {
            warnings.add("Project is inactive");
        }
        if (card.getCoverImageUrl() == null) {
            warnings.add("Project has no cover image");
        }
        if (detail.getBrochureUrl() == null) {
            warnings.add("Project has no brochure PDF");
        }
        if (!StringUtils.hasText(project.getReraNumber())) {
            warnings.add("Project has no RERA registration number");
        }
        if (detail.getFloorPlanGroups() == null || detail.getFloorPlanGroups().isEmpty()) {
            warnings.add("Project has no floor plans");
        }
        if (isAmenitiesEmpty(detail)) {
            warnings.add("Project has no amenities");
        }
        if (detail.getMasterPlan() == null) {
            warnings.add("Project has no master plan");
        }

        if (meter == null) {
            warnings.add("Project meter data could not be loaded");
        } else {
            if (isStagesEmpty(meter)) {
                warnings.add("Project has no construction stages");
            }
            if (isComplianceEmpty(meter.getLandLicense())) {
                warnings.add("Project has no Land & License compliance items");
            }
            if (isApprovalsEmpty(meter.getApprovals())) {
                warnings.add("Project has no Approval / NOC compliance items");
            }
            if (meter.getPropertyRates() == null || meter.getPropertyRates().isEmpty()) {
                warnings.add("Project has no price history");
            }
            if (meter.getPaymentPlan() == null || meter.getPaymentPlan().isEmpty()) {
                warnings.add("Project has no construction payment plan");
            }
            if (meter.getEstimatedCost() == null || meter.getEstimatedCost().getTotalCost() == null) {
                warnings.add("Project has no estimated cost data");
            }
            if (meter.getLandUtilization() == null || meter.getLandUtilization().getTotalLandAreaSqm() == null) {
                warnings.add("Project has no land utilization data");
            }
            if (meter.getLocationRadar() == null || meter.getLocationRadar().getFinalScore() == null) {
                warnings.add("Project has no location appreciation radar data");
            }
            if (meter.getBuilderCredibility() == null) {
                warnings.add("Builder credibility data is not available");
            }
        }

        return warnings;
    }

    private List<String> buildMissingSections(ProjectEntity project, ProjectMeterCardResponse card,
                                              ProjectResponse detail, ProjectMeterDetailResponse meter) {
        List<String> missing = new ArrayList<>();

        if (card.getCoverImageUrl() == null) missing.add("COVER_IMAGE");
        if (detail.getBrochureUrl() == null) missing.add("BROCHURE");
        if (!StringUtils.hasText(project.getReraNumber())) missing.add("RERA_REGISTRATION");
        if (detail.getFloorPlanGroups() == null || detail.getFloorPlanGroups().isEmpty()) missing.add("FLOOR_PLANS");
        if (isAmenitiesEmpty(detail)) missing.add("AMENITIES");
        if (detail.getMasterPlan() == null) missing.add("MASTER_PLAN");

        if (meter == null) {
            missing.add("CONSTRUCTION_PROGRESS");
            missing.add("COMPLIANCE_ITEMS");
            missing.add("PRICE_HISTORY");
            missing.add("PAYMENT_PLAN");
            missing.add("ESTIMATED_COST");
            missing.add("LAND_UTILIZATION");
            missing.add("LOCATION_APPRECIATION_RADAR");
            missing.add("BUILDER_CREDIBILITY");
        } else {
            if (isStagesEmpty(meter)) missing.add("CONSTRUCTION_PROGRESS");
            if (isComplianceEmpty(meter.getLandLicense()) && isApprovalsEmpty(meter.getApprovals())) {
                missing.add("COMPLIANCE_ITEMS");
            }
            if (meter.getPropertyRates() == null || meter.getPropertyRates().isEmpty()) missing.add("PRICE_HISTORY");
            if (meter.getPaymentPlan() == null || meter.getPaymentPlan().isEmpty()) missing.add("PAYMENT_PLAN");
            if (meter.getEstimatedCost() == null || meter.getEstimatedCost().getTotalCost() == null) {
                missing.add("ESTIMATED_COST");
            }
            if (meter.getLandUtilization() == null || meter.getLandUtilization().getTotalLandAreaSqm() == null) {
                missing.add("LAND_UTILIZATION");
            }
            if (meter.getLocationRadar() == null || meter.getLocationRadar().getFinalScore() == null) {
                missing.add("LOCATION_APPRECIATION_RADAR");
            }
            if (meter.getBuilderCredibility() == null) missing.add("BUILDER_CREDIBILITY");
        }

        return missing;
    }

    private boolean isAmenitiesEmpty(ProjectResponse detail) {
        return detail.getAmenities() == null
            || detail.getAmenities().getGroups() == null
            || detail.getAmenities().getGroups().isEmpty();
    }

    private boolean isStagesEmpty(ProjectMeterDetailResponse meter) {
        return meter.getConstruction() == null
            || meter.getConstruction().getStages() == null
            || meter.getConstruction().getStages().isEmpty();
    }

    private boolean isComplianceEmpty(com.brandPitara.sfs.projectmeter.dto.ProjectLandLicenseResponse landLicense) {
        return landLicense == null || landLicense.getItems() == null || landLicense.getItems().isEmpty();
    }

    private boolean isApprovalsEmpty(com.brandPitara.sfs.projectmeter.dto.ProjectApprovalsResponse approvals) {
        return approvals == null || approvals.getItems() == null || approvals.getItems().isEmpty();
    }
}
