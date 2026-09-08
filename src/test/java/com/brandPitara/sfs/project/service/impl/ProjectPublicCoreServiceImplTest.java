package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.project.dto.ProjectFloorPlanResponse;
import com.brandPitara.sfs.project.enums.UnitConfigurationType;
import com.brandPitara.sfs.project.service.model.ProjectDetailAnalyticsData;
import com.brandPitara.sfs.project.service.model.ProjectPublicContentData;
import com.brandPitara.sfs.project.service.model.ProjectPublicCoreData;
import com.brandPitara.sfs.project.service.model.ProjectPublicSectionFailure;
import com.brandPitara.sfs.project.service.reader.ProjectDetailAnalyticsReader;
import com.brandPitara.sfs.project.service.reader.ProjectPublicBaseReader;
import com.brandPitara.sfs.project.service.reader.ProjectPublicContentReader;
import com.brandPitara.sfs.projectmeter.dto.ProjectAmenitiesResponse;
import com.brandPitara.sfs.projectmeter.service.ProjectMeterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectPublicCoreServiceImplTest {

    @Mock private ProjectPublicBaseReader baseReader;
    @Mock private ProjectDetailAnalyticsReader analyticsReader;
    @Mock private ProjectPublicContentReader contentReader;

    private ProjectPublicCoreServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProjectPublicCoreServiceImpl(
                baseReader,
                analyticsReader,
                contentReader
        );
    }

    @Test
    void buildsDeterministicCoreWithAnalyticsAndGlobalFavoriteCount() {
        ProjectPublicCoreData base = base().toBuilder().averagePricePerSqft(null).build();
        ProjectAmenitiesResponse amenities = ProjectAmenitiesResponse.builder()
                .completionPercent(73)
                .groups(List.of())
                .items(List.of())
                .build();
        when(baseReader.readById(42L)).thenReturn(base);
        when(analyticsReader.read(42L)).thenReturn(new ProjectDetailAnalyticsData(12345L, 8.25, amenities));
        when(contentReader.readAfterVisibilityCheck(base)).thenReturn(new ProjectPublicContentData(
                List.of(
                        floorPlan(1L, "3 BHK Type A", UnitConfigurationType.BHK_3),
                        floorPlan(2L, "3 BHK Type B", UnitConfigurationType.BHK_3)
                ), null, null, Set.of()));

        ProjectPublicCoreData result = service.getById(42L);

        assertThat(result.getFavoriteCount()).isEqualTo(17L);
        assertThat(result.getPricing().getAverageAreaPrice()).isEqualTo(12345L);
        assertThat(result.getPricing().getAppreciationPercent()).isEqualTo(8.25);
        assertThat(result.getAmenities()).isSameAs(amenities);
        assertThat(result.getFloorPlanGroups()).singleElement().satisfies(group -> {
            assertThat(group.getGroupKey()).isEqualTo("BHK_3");
            assertThat(group.getGroupLabel()).isEqualTo("3 BHK");
            assertThat(group.getItems()).extracting(ProjectFloorPlanResponse::getTitle)
                    .containsExactly("3 BHK Type A", "3 BHK Type B");
        });
        assertThat(result.getFailedSections()).isEmpty();
    }

    @Test
    void preservesLegacyPartialResponseWhenOptionalReadersFail() {
        ProjectPublicCoreData base = base();
        when(baseReader.readById(42L)).thenReturn(base);
        when(analyticsReader.read(42L)).thenThrow(new IllegalStateException("analytics unavailable"));
        when(contentReader.readAfterVisibilityCheck(base)).thenReturn(new ProjectPublicContentData(
                List.of(), null, null, Set.of(
                        ProjectPublicSectionFailure.CONNECTIVITY,
                        ProjectPublicSectionFailure.MASTER_PLAN)));

        ProjectPublicCoreData result = service.getById(42L);

        assertThat(result.getAmenities()).isNull();
        assertThat(result.getConnectivity()).isNull();
        assertThat(result.getMasterPlan()).isNull();
        assertThat(result.getPricing().getAverageAreaPrice()).isEqualTo(9999L);
        assertThat(result.getFailedSections()).containsExactlyInAnyOrder(
                ProjectPublicSectionFailure.ANALYTICS,
                ProjectPublicSectionFailure.CONNECTIVITY,
                ProjectPublicSectionFailure.MASTER_PLAN
        );
    }

    @Test
    void slugAndIdUseTheSameAssemblyPipeline() {
        ProjectPublicCoreData base = base().toBuilder().favoriteCount(2L).build();
        when(baseReader.readBySlug("project-42")).thenReturn(base);
        when(analyticsReader.read(42L)).thenReturn(new ProjectDetailAnalyticsData(null, null,
                ProjectAmenitiesResponse.builder().groups(List.of()).items(List.of()).build()));
        when(contentReader.readAfterVisibilityCheck(base)).thenReturn(
                new ProjectPublicContentData(List.of(), null, null, Set.of()));

        ProjectPublicCoreData result = service.getBySlug("project-42");

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getFavoriteCount()).isEqualTo(2L);
    }

    @Test
    void coreHasNoDependencyOnTheFullMeterService() {
        assertThat(java.util.Arrays.stream(ProjectPublicCoreServiceImpl.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getType))
                .doesNotContain(ProjectMeterService.class);
    }

    @Test
    void orchestratorHasNoBroadTransactionWhileDatabaseReadersAreReadOnlyTransactional() throws Exception {
        assertThat(ProjectPublicCoreServiceImpl.class.getMethod("getById", Long.class)
                .getAnnotation(Transactional.class)).isNull();
        assertThat(ProjectServiceImpl.class.getMethod("publicGet", Long.class)
                .getAnnotation(Transactional.class)).isNull();
        assertThat(ProjectPublicBaseReader.class.getMethod("readById", Long.class)
                .getAnnotation(Transactional.class).readOnly()).isTrue();
        assertThat(ProjectDetailAnalyticsReader.class.getMethod("read", Long.class)
                .getAnnotation(Transactional.class).readOnly()).isTrue();
        assertThat(ProjectPublicContentReader.class.getMethod(
                        "readAfterVisibilityCheck", ProjectPublicCoreData.class)
                .getAnnotation(Transactional.class).readOnly()).isTrue();
    }

    private ProjectPublicCoreData base() {
        return ProjectPublicCoreData.builder()
                .id(42L)
                .slug("project-42")
                .priceMin(10_000_000L)
                .priceMax(20_000_000L)
                .averagePricePerSqft(9999L)
                .favoriteCount(17L)
                .propertyTypes(Set.of())
                .glimpses(List.of())
                .failedSections(Set.of())
                .build();
    }

    private ProjectFloorPlanResponse floorPlan(Long id, String title, UnitConfigurationType type) {
        return ProjectFloorPlanResponse.builder()
                .id(id)
                .projectId(42L)
                .title(title)
                .unitConfigurationType(type)
                .unitConfigurationTypeLabel(type.toLabel())
                .active(true)
                .build();
    }
}
