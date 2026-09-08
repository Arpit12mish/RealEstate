package com.brandPitara.sfs.project.mapper;

import com.brandPitara.sfs.project.dto.ProjectPublicResponse;
import com.brandPitara.sfs.project.dto.ProjectResponse;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.enums.PropertyType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectMapperPropertyTypesTest {

  @Test
  void dashboardResponseOwnsADetachedStandardJavaSet() {
    Set<PropertyType> entityPropertyTypes = new HashSet<>(
        Set.of(PropertyType.APARTMENT, PropertyType.VILLA));
    ProjectEntity project = ProjectEntity.builder()
        .propertyTypes(entityPropertyTypes)
        .build();

    ProjectResponse response = ProjectMapper.toResponse(project);

    assertThat(response.getPropertyTypes())
        .containsExactlyInAnyOrder(PropertyType.APARTMENT, PropertyType.VILLA);
    assertThat(response.getPropertyTypes()).isNotSameAs(project.getPropertyTypes());
    assertThat(response.getPropertyTypes().getClass().getName())
        .doesNotContain("Persistent");
  }

  @Test
  void publicResponseOwnsADetachedStandardJavaSet() {
    Set<PropertyType> entityPropertyTypes = new HashSet<>(
        Set.of(PropertyType.PLOT, PropertyType.RESIDENTIAL_PLOT));
    ProjectEntity project = ProjectEntity.builder()
        .propertyTypes(entityPropertyTypes)
        .build();

    ProjectPublicResponse response = ProjectMapper.toPublicResponse(project);

    assertThat(response.getPropertyTypes())
        .containsExactlyInAnyOrder(PropertyType.PLOT, PropertyType.RESIDENTIAL_PLOT);
    assertThat(response.getPropertyTypes()).isNotSameAs(project.getPropertyTypes());
    assertThat(response.getPropertyTypes().getClass().getName())
        .doesNotContain("Persistent");
  }

  @Test
  void nullAndEmptyEntityCollectionsMapToEmptyDetachedSets() {
    ProjectEntity nullTypes = new ProjectEntity();
    nullTypes.setPropertyTypes(null);

    ProjectEntity emptyTypes = ProjectEntity.builder().propertyTypes(new HashSet<>()).build();

    assertThat(ProjectMapper.toResponse(nullTypes).getPropertyTypes()).isEmpty();
    assertThat(ProjectMapper.toPublicResponse(emptyTypes).getPropertyTypes()).isEmpty();
    assertThat(ProjectMapper.toPublicResponse(emptyTypes).getPropertyTypes())
        .isNotSameAs(emptyTypes.getPropertyTypes());
  }
}
