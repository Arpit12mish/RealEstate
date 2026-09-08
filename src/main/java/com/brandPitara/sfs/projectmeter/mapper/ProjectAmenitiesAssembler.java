package com.brandPitara.sfs.projectmeter.mapper;

import com.brandPitara.sfs.projectmeter.dto.ProjectAmenityGroupResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectAmenityItemResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectAmenitiesResponse;
import com.brandPitara.sfs.projectmeter.entity.ProjectAmenityProgressEntity;
import com.brandPitara.sfs.projectmeter.enums.ProjectAmenityCategory;
import com.brandPitara.sfs.projectmeter.enums.ProjectAmenityStatus;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Pure, viewer-independent assembly of the public amenity representation. */
@Component
public class ProjectAmenitiesAssembler {

    public ProjectAmenitiesResponse assemble(
            List<ProjectAmenityProgressEntity> publicAmenities,
            Integer snapshotAmenityScore
    ) {
        List<ProjectAmenityProgressEntity> amenities = publicAmenities == null
                ? List.of()
                : List.copyOf(publicAmenities);

        return ProjectAmenitiesResponse.builder()
                .completionPercent(snapshotAmenityScore != null
                        ? safePercent(snapshotAmenityScore)
                        : calculateCompletionPercent(amenities))
                .groups(buildGroups(amenities))
                .items(amenities.stream().map(this::toItemResponse).toList())
                .build();
    }

    private int calculateCompletionPercent(List<ProjectAmenityProgressEntity> amenities) {
        if (amenities.isEmpty()) {
            return 0;
        }

        int weightedSum = 0;
        int totalWeight = 0;
        int countIncluded = 0;
        int simpleSum = 0;

        for (ProjectAmenityProgressEntity amenity : amenities) {
            if (!Boolean.TRUE.equals(amenity.getAvailable())
                    || amenity.getStatus() == ProjectAmenityStatus.NOT_AVAILABLE) {
                continue;
            }

            int progress = safePercent(amenity.getProgressPercent());
            int weight = amenity.getWeightPercent() == null
                    ? 0
                    : Math.max(amenity.getWeightPercent(), 0);

            weightedSum += progress * weight;
            totalWeight += weight;
            simpleSum += progress;
            countIncluded++;
        }

        if (totalWeight > 0) {
            return Math.round((float) weightedSum / totalWeight);
        }
        return countIncluded > 0 ? Math.round((float) simpleSum / countIncluded) : 0;
    }

    private List<ProjectAmenityGroupResponse> buildGroups(List<ProjectAmenityProgressEntity> amenities) {
        Map<ProjectAmenityCategory, List<ProjectAmenityProgressEntity>> grouped = amenities.stream()
                .collect(Collectors.groupingBy(
                        amenity -> amenity.getCategory() != null
                                ? amenity.getCategory()
                                : ProjectAmenityCategory.OTHER,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return grouped.entrySet().stream()
                .map(entry -> {
                    ProjectAmenityCategory category = entry.getKey();
                    List<ProjectAmenityProgressEntity> items = entry.getValue();
                    int displayOrder = items.stream()
                            .mapToInt(item -> item.getCategoryDisplayOrder() != null
                                    ? item.getCategoryDisplayOrder()
                                    : 0)
                            .min()
                            .orElse(0);
                    String label = items.stream()
                            .map(ProjectAmenityProgressEntity::getCategoryLabel)
                            .filter(candidate -> candidate != null && !candidate.isBlank())
                            .findFirst()
                            .orElse(category.toLabel());

                    return ProjectAmenityGroupResponse.builder()
                            .category(category)
                            .categoryLabel(label)
                            .displayOrder(displayOrder)
                            .items(items.stream().map(this::toItemResponse).toList())
                            .build();
                })
                .sorted(Comparator.comparingInt(ProjectAmenityGroupResponse::getDisplayOrder)
                        .thenComparing(group -> group.getCategory().name()))
                .toList();
    }

    private ProjectAmenityItemResponse toItemResponse(ProjectAmenityProgressEntity entity) {
        ProjectAmenityCategory category = entity.getCategory();
        String categoryLabel = entity.getCategoryLabel();
        if (categoryLabel == null || categoryLabel.isBlank()) {
            categoryLabel = category != null
                    ? category.toLabel()
                    : ProjectAmenityCategory.OTHER.toLabel();
        }

        return ProjectAmenityItemResponse.builder()
                .id(entity.getId())
                .amenityCode(entity.getAmenityCode())
                .amenityLabel(entity.getAmenityLabel())
                .category(category)
                .categoryLabel(categoryLabel)
                .iconKey(entity.getIconKey())
                .rare(entity.getRare())
                .available(entity.getAvailable())
                .status(entity.getStatus())
                .progressPercent(entity.getProgressPercent())
                .weightPercent(entity.getWeightPercent())
                .displayOrder(entity.getDisplayOrder())
                .categoryDisplayOrder(entity.getCategoryDisplayOrder())
                .remarks(entity.getRemarks())
                .verified(entity.getVerified())
                .publicVisible(entity.getPublicVisible())
                .active(entity.getActive())
                .build();
    }

    private int safePercent(Integer value) {
        if (value == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, value));
    }
}
