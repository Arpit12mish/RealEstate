package com.brandPitara.sfs.projectmeter.service.reader;

import com.brandPitara.sfs.project.dto.ProjectConnectivityResponse;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanGroupResponse;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanResponse;
import com.brandPitara.sfs.project.dto.ProjectMasterPlanResponse;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import com.brandPitara.sfs.project.enums.UnitConfigurationType;
import com.brandPitara.sfs.project.mapper.ProjectConnectivityMapper;
import com.brandPitara.sfs.project.mapper.ProjectFloorPlanMapper;
import com.brandPitara.sfs.project.mapper.ProjectMasterPlanMapper;
import com.brandPitara.sfs.project.mapper.ProjectMediaPicker;
import com.brandPitara.sfs.project.repository.ProjectConnectivityPlaceRepository;
import com.brandPitara.sfs.project.repository.ProjectConnectivityRepository;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanRepository;
import com.brandPitara.sfs.project.repository.ProjectMasterPlanRepository;
import com.brandPitara.sfs.project.repository.ProjectMediaRepository;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterMediaResponse;
import com.brandPitara.sfs.projectmeter.mapper.ProjectMeterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProjectMeterSupplementalReader {

    private static final Pattern BHK_PATTERN = Pattern.compile("\\b(5\\+|[1-5](?:\\.5)?)\\s*BHK\\b");

    private final ProjectMediaRepository projectMediaRepository;
    private final ProjectMasterPlanRepository projectMasterPlanRepository;
    private final ProjectFloorPlanRepository projectFloorPlanRepository;
    private final ProjectConnectivityRepository projectConnectivityRepository;
    private final ProjectConnectivityPlaceRepository projectConnectivityPlaceRepository;

    @Transactional(readOnly = true)
    public SupplementalSections read(ProjectEntity project) {
        Long projectId = project.getId();
        List<ProjectMediaEntity> media = projectMediaRepository
            .findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdDesc(projectId);

        ProjectMeterMediaResponse meterMedia = ProjectMeterMapper.toMediaResponse(media);
        String brochureUrl = ProjectMediaPicker.pick(media, false).brochureUrl();

        ProjectMasterPlanResponse masterPlan = projectMasterPlanRepository
            .findByProjectIdAndActiveTrueAndDeletedFalse(projectId)
            .map(ProjectMasterPlanMapper::toPublicResponse)
            .orElse(null);

        List<ProjectFloorPlanResponse> floorPlans = projectFloorPlanRepository
            .findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(projectId)
            .stream()
            .map(ProjectFloorPlanMapper::toResponse)
            .toList();

        var connectivity = projectConnectivityRepository
            .findByProjectIdAndActiveTrueAndDeletedFalse(projectId)
            .orElse(null);
        var places = projectConnectivityPlaceRepository
            .findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(projectId);
        ProjectConnectivityResponse connectivityResponse = ProjectConnectivityMapper.toResponse(
            connectivity,
            project,
            places
        );

        return new SupplementalSections(
            meterMedia,
            brochureUrl,
            masterPlan,
            groupFloorPlans(floorPlans),
            connectivityResponse
        );
    }

    private List<ProjectFloorPlanGroupResponse> groupFloorPlans(List<ProjectFloorPlanResponse> floorPlans) {
        if (floorPlans == null || floorPlans.isEmpty()) {
            return List.of();
        }

        Map<String, List<ProjectFloorPlanResponse>> grouped = floorPlans.stream()
            .collect(Collectors.groupingBy(
                floorPlan -> floorPlan.getUnitConfigurationType() != null
                    ? floorPlan.getUnitConfigurationType().name()
                    : normalizeGroupKey(floorPlan.getUnitLabel(), floorPlan.getTitle(), floorPlan.getFloorCode()),
                LinkedHashMap::new,
                Collectors.toList()
            ));

        return grouped.entrySet().stream()
            .map(entry -> ProjectFloorPlanGroupResponse.builder()
                .groupKey(entry.getKey())
                .groupLabel(entry.getValue().stream()
                    .filter(item -> item.getUnitConfigurationType() != null)
                    .map(ProjectFloorPlanResponse::getUnitConfigurationTypeLabel)
                    .filter(label -> label != null)
                    .findFirst()
                    .orElse(toReadableGroupLabel(entry.getKey())))
                .items(entry.getValue())
                .build())
            .toList();
    }

    private String normalizeGroupKey(String unitLabel, String title, String floorCode) {
        String raw = firstNonBlank(unitLabel, title, floorCode);
        if (raw == null) return "OTHER";

        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        String normalizedForBhk = normalized.replace('_', '.');
        if (normalized.contains("STUDIO")) return "STUDIO";
        if (normalized.contains("OFFICE")) return "OFFICE";
        if (normalized.contains("RETAIL")) return "RETAIL";
        if (normalized.contains("PLOT")) return "PLOT";

        Matcher matcher = BHK_PATTERN.matcher(normalizedForBhk);
        if (matcher.find()) return toUnitConfigurationKey(matcher.group(1));
        if (normalized.contains("PENTHOUSE")) return "PENTHOUSE";
        if (normalized.contains("VILLA")) return "VILLA";
        return normalized.replaceAll("\\s+", "_");
    }

    private String toUnitConfigurationKey(String bhkValue) {
        return switch (bhkValue) {
            case "1" -> "BHK_1";
            case "1.5" -> "BHK_1_5";
            case "2" -> "BHK_2";
            case "2.5" -> "BHK_2_5";
            case "3" -> "BHK_3";
            case "3.5" -> "BHK_3_5";
            case "4" -> "BHK_4";
            case "4.5" -> "BHK_4_5";
            case "5" -> "BHK_5";
            case "5+" -> "BHK_5_PLUS";
            default -> "OTHER";
        };
    }

    private String toReadableGroupLabel(String key) {
        if (key == null || key.isBlank()) return "Other";
        try {
            return UnitConfigurationType.valueOf(key).toLabel();
        } catch (IllegalArgumentException ignored) {
            return key.replace("_", " ");
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    public record SupplementalSections(
        ProjectMeterMediaResponse media,
        String brochureUrl,
        ProjectMasterPlanResponse masterPlan,
        List<ProjectFloorPlanGroupResponse> floorPlanGroups,
        ProjectConnectivityResponse connectivity
    ) {
    }
}
