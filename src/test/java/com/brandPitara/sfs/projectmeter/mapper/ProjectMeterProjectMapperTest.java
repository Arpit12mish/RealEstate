package com.brandPitara.sfs.projectmeter.mapper;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.enums.ProjectStatus;
import com.brandPitara.sfs.project.enums.PropertyType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectMeterProjectMapperTest {

    @Test
    void mapsOnlyMeterHeaderFieldsFromExplicitInputs() {
        ProjectEntity project = ProjectEntity.builder()
            .id(42L)
            .name("Project 42")
            .slug("project-42")
            .description("Header description")
            .builder(BuilderEntity.builder().id(7L).name("Builder 7").logoUrl("builder.png").build())
            .city(CityEntity.builder().id(3L).name("Gurugram").build())
            .addressLine("Golf Course Road")
            .latitude(28.45)
            .longitude(77.08)
            .priceMin(10_000_000L)
            .priceMax(20_000_000L)
            .monthlyEmiMin(80_000L)
            .monthlyEmiMax(160_000L)
            .averagePricePerSqft(15_000L)
            .startDate(LocalDate.of(2023, 1, 1))
            .possessionDate(LocalDate.of(2027, 6, 30))
            .reraNumber("RERA-42")
            .status(ProjectStatus.UNDER_CONSTRUCTION)
            .propertyTypes(Set.of(PropertyType.APARTMENT, PropertyType.PENTHOUSE))
            .build();

        var response = ProjectMeterProjectMapper.toResponse(
            project,
            "https://cdn/project-42.pdf",
            true,
            9L
        );

        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getName()).isEqualTo("Project 42");
        assertThat(response.getSlug()).isEqualTo("project-42");
        assertThat(response.getBuilderId()).isEqualTo(7L);
        assertThat(response.getBuilderName()).isEqualTo("Builder 7");
        assertThat(response.getBuilderLogoUrl()).isEqualTo("builder.png");
        assertThat(response.getCityId()).isEqualTo(3L);
        assertThat(response.getCityName()).isEqualTo("Gurugram");
        assertThat(response.getPriceMin()).isEqualTo(10_000_000L);
        assertThat(response.getAveragePricePerSqft()).isEqualTo(15_000L);
        assertThat(response.getPropertyTypes()).containsExactlyInAnyOrder(
            PropertyType.APARTMENT,
            PropertyType.PENTHOUSE
        );
        assertThat(response.getBrochureUrl()).isEqualTo("https://cdn/project-42.pdf");
        assertThat(response.getIsFavorite()).isTrue();
        assertThat(response.getFavoriteCount()).isEqualTo(9L);
    }
}
