package com.brandPitara.sfs.project.service.reader;

import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import com.brandPitara.sfs.project.mapper.ProjectPublicCoreMapper;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.repository.ProjectMediaRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.project.service.model.ProjectPublicCoreData;
import com.brandPitara.sfs.enums.FavoriteTargetType;
import com.brandPitara.sfs.repository.UserFavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectPublicBaseReader {

    private final ProjectRepository projectRepository;
    private final ProjectMediaRepository projectMediaRepository;
    private final ProjectPublicVisibilityPolicy visibilityPolicy;
    private final UserFavoriteRepository userFavoriteRepository;

    @Transactional(readOnly = true)
    public ProjectPublicCoreData readById(Long projectId) {
        ProjectEntity project = projectRepository.findDetailByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
        visibilityPolicy.assertPubliclyVisible(project, projectId);
        return mapDetached(project);
    }

    @Transactional(readOnly = true)
    public ProjectPublicCoreData readBySlug(String projectSlug) {
        ProjectEntity project = projectRepository.findBySlugAndDeletedFalse(projectSlug)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectSlug));
        visibilityPolicy.assertPubliclyVisible(project, projectSlug);
        return mapDetached(project);
    }

    private ProjectPublicCoreData mapDetached(ProjectEntity project) {
        List<ProjectMediaEntity> media = projectMediaRepository
                .findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdDesc(project.getId());
        long favoriteCount = userFavoriteRepository.countByTargetTypeAndTargetId(
                FavoriteTargetType.PROJECT,
                project.getId()
        );
        return ProjectPublicCoreMapper.fromEntity(project, media).toBuilder()
                .favoriteCount(favoriteCount)
                .build();
    }
}
