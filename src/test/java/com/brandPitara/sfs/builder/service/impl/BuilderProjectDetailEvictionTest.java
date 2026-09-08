package com.brandPitara.sfs.builder.service.impl;

import com.brandPitara.sfs.builder.dto.BuilderUpsertRequest;
import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.cdn.event.ProjectCacheEvictionReason;
import com.brandPitara.sfs.cdn.event.ProjectPublicCacheEvictionPublisher;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.media.validator.TrustedMediaUrlValidator;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

class BuilderProjectDetailEvictionTest {

    @Test
    void builderNameOrLogoUpdateEvictsAffectedProjectDocuments() {
        BuilderRepository builders = mock(BuilderRepository.class);
        ProjectRepository projects = mock(ProjectRepository.class);
        ProjectPublicCacheEvictionPublisher publisher = mock(ProjectPublicCacheEvictionPublisher.class);
        BuilderEntity builder = BuilderEntity.builder()
                .id(7L).name("Old").active(true).published(true).deleted(false).build();
        when(builders.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(builder));
        when(builders.save(builder)).thenReturn(builder);
        when(projects.findIdsByBuilderIdAndDeletedFalse(7L)).thenReturn(List.of(27L, 28L));
        BuilderServiceImpl service = new BuilderServiceImpl(
                builders, mock(ContentVersionService.class), mock(TrustedMediaUrlValidator.class), projects, publisher);

        service.update(7L, BuilderUpsertRequest.builder().name("New").logoUrl("logo.webp").build());

        verify(publisher).publishAll(List.of(27L, 28L), ProjectCacheEvictionReason.BUILDER_CHANGED);
    }
}
