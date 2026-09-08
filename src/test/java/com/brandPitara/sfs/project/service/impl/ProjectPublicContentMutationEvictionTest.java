package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.cdn.event.ProjectCacheEvictionReason;
import com.brandPitara.sfs.cdn.event.ProjectPublicCacheEvictionPublisher;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanUpsertRequest;
import com.brandPitara.sfs.project.dto.ProjectMediaUpsertRequest;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.enums.ProjectMediaType;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanRepository;
import com.brandPitara.sfs.project.repository.ProjectMediaRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProjectPublicContentMutationEvictionTest {

    @Test
    void mediaWritePublishesMediaEviction() {
        ProjectRepository projects = mock(ProjectRepository.class);
        ProjectMediaRepository media = mock(ProjectMediaRepository.class);
        ProjectPublicCacheEvictionPublisher publisher = mock(ProjectPublicCacheEvictionPublisher.class);
        ProjectEntity project = project();
        when(projects.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(project));
        when(media.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ProjectMediaServiceImpl service = new ProjectMediaServiceImpl(
                projects, media, mock(ContentVersionService.class),
                mock(ProjectPublicVisibilityPolicy.class), publisher);

        service.addMedia(27L, ProjectMediaUpsertRequest.builder()
                .mediaType(ProjectMediaType.IMAGE)
                .url("https://cdn.example/project.webp")
                .build());

        verify(publisher).publish(27L, ProjectCacheEvictionReason.MEDIA_CHANGED);
    }

    @Test
    void floorPlanWritePublishesFloorPlanEviction() {
        ProjectRepository projects = mock(ProjectRepository.class);
        ProjectFloorPlanRepository floorPlans = mock(ProjectFloorPlanRepository.class);
        ProjectPublicCacheEvictionPublisher publisher = mock(ProjectPublicCacheEvictionPublisher.class);
        ProjectEntity project = project();
        when(projects.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(project));
        when(floorPlans.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ProjectFloorPlanServiceImpl service = new ProjectFloorPlanServiceImpl(
                projects, floorPlans, mock(ContentVersionService.class),
                mock(ProjectPublicVisibilityPolicy.class), publisher);

        service.create(27L, ProjectFloorPlanUpsertRequest.builder()
                .title("3 BHK")
                .imageUrl("https://cdn.example/floor.webp")
                .build());

        verify(publisher).publish(27L, ProjectCacheEvictionReason.FLOOR_PLAN_CHANGED);
    }

    private ProjectEntity project() {
        return ProjectEntity.builder()
                .id(27L)
                .published(true)
                .active(true)
                .deleted(false)
                .build();
    }
}
