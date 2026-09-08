package com.brandPitara.sfs.builder.service.impl;

import com.brandPitara.sfs.builder.dto.BuilderUpsertRequest;
import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.cdn.event.ProjectPublicCacheEvictionPublisher;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.project.exception.PublicationConflictException;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class BuilderPublicationLifecycleTest {

    private BuilderRepository builders;
    private ProjectRepository projects;
    private BuilderServiceImpl service;
    private BuilderEntity builder;

    @BeforeEach
    void setUp() {
        builders = mock(BuilderRepository.class);
        projects = mock(ProjectRepository.class);
        service = new BuilderServiceImpl(
                builders,
                mock(ContentVersionService.class),
                projects,
                mock(ProjectPublicCacheEvictionPublisher.class));
        builder = BuilderEntity.builder()
                .id(51L)
                .name("M3M Group")
                .published(true)
                .active(true)
                .deleted(false)
                .build();
        when(builders.findByIdAndDeletedFalseForUpdate(51L)).thenReturn(Optional.of(builder));
        when(builders.save(builder)).thenReturn(builder);
        when(projects.findIdsByBuilderIdAndDeletedFalse(51L)).thenReturn(List.of());
    }

    @Test
    void unpublishSucceedsWhenNoPublishedProjectsDependOnBuilder() {
        when(projects.countByBuilderIdAndPublishedTrueAndDeletedFalse(51L)).thenReturn(0L);

        service.setPublished(51L, false);

        assertThat(builder.getPublished()).isFalse();
        verify(builders).save(builder);
    }

    @Test
    void unpublishFailsWhenPublishedProjectsDependOnBuilder() {
        when(projects.countByBuilderIdAndPublishedTrueAndDeletedFalse(51L)).thenReturn(4L);

        assertConflict(() -> service.setPublished(51L, false));

        assertThat(builder.getPublished()).isTrue();
        verify(builders, never()).save(builder);
    }

    @Test
    void deactivateFailsWhenPublishedProjectsDependOnBuilder() {
        when(projects.countByBuilderIdAndPublishedTrueAndDeletedFalse(51L)).thenReturn(1L);

        assertConflict(() -> service.update(51L,
                BuilderUpsertRequest.builder().name("M3M Group").active(false).build()));

        assertThat(builder.getActive()).isTrue();
        verify(builders, never()).save(builder);
    }

    @Test
    void deleteFailsWhenPublishedProjectsDependOnBuilder() {
        when(projects.countByBuilderIdAndPublishedTrueAndDeletedFalse(51L)).thenReturn(2L);

        assertConflict(() -> service.softDelete(51L));

        assertThat(builder.getDeleted()).isFalse();
        verify(builders, never()).save(builder);
    }

    private void assertConflict(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOf(PublicationConflictException.class)
                .satisfies(ex -> assertThat(((PublicationConflictException) ex).getCode())
                        .isEqualTo("BUILDER_HAS_PUBLISHED_PROJECTS"));
    }
}
