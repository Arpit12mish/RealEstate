package com.brandPitara.sfs.projectmeter.mapper;

import com.brandPitara.sfs.projectmeter.entity.ProjectAmenityProgressEntity;
import com.brandPitara.sfs.projectmeter.enums.ProjectAmenityCategory;
import com.brandPitara.sfs.projectmeter.enums.ProjectAmenityStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectAmenitiesAssemblerTest {

    private final ProjectAmenitiesAssembler assembler = new ProjectAmenitiesAssembler();

    @Test
    void preservesMeterGroupingOrderingAndWeightedCompletion() {
        ProjectAmenityProgressEntity sports = amenity(
                2L, "POOL", ProjectAmenityCategory.SPORTS, null, 20, 50, 2, 20);
        ProjectAmenityProgressEntity lifestyle = amenity(
                1L, "CLUB", ProjectAmenityCategory.LIFESTYLE, "Club Life", 80, 50, 1, 10);

        var result = assembler.assemble(List.of(lifestyle, sports), null);

        assertThat(result.getCompletionPercent()).isEqualTo(50);
        assertThat(result.getGroups()).extracting(group -> group.getCategory())
                .containsExactly(ProjectAmenityCategory.LIFESTYLE, ProjectAmenityCategory.SPORTS);
        assertThat(result.getGroups().get(0).getCategoryLabel()).isEqualTo("Club Life");
        assertThat(result.getGroups().get(1).getCategoryLabel())
                .isEqualTo(ProjectAmenityCategory.SPORTS.toLabel());
        assertThat(result.getItems()).extracting(item -> item.getAmenityCode())
                .containsExactly("CLUB", "POOL");
    }

    @Test
    void snapshotScoreTakesPrecedenceAndIsClampedExactlyAsMeterDid() {
        var result = assembler.assemble(List.of(
                amenity(1L, "CLUB", ProjectAmenityCategory.LIFESTYLE, null, 10, 100, 1, 1)
        ), 145);

        assertThat(result.getCompletionPercent()).isEqualTo(100);
    }

    @Test
    void excludesUnavailableAmenitiesFromFallbackCompletionButKeepsThemInOutput() {
        ProjectAmenityProgressEntity unavailable = amenity(
                1L, "SPA", ProjectAmenityCategory.WELLNESS, null, 100, 100, 1, 1);
        unavailable.setStatus(ProjectAmenityStatus.NOT_AVAILABLE);
        unavailable.setAvailable(false);

        var result = assembler.assemble(List.of(unavailable), null);

        assertThat(result.getCompletionPercent()).isZero();
        assertThat(result.getItems()).hasSize(1);
    }

    private ProjectAmenityProgressEntity amenity(
            Long id,
            String code,
            ProjectAmenityCategory category,
            String categoryLabel,
            int progress,
            int weight,
            int displayOrder,
            int categoryDisplayOrder
    ) {
        return ProjectAmenityProgressEntity.builder()
                .id(id)
                .amenityCode(code)
                .amenityLabel(code)
                .category(category)
                .categoryLabel(categoryLabel)
                .status(ProjectAmenityStatus.IN_PROGRESS)
                .progressPercent(progress)
                .weightPercent(weight)
                .displayOrder(displayOrder)
                .categoryDisplayOrder(categoryDisplayOrder)
                .available(true)
                .rare(false)
                .verified(false)
                .publicVisible(true)
                .active(true)
                .build();
    }
}
