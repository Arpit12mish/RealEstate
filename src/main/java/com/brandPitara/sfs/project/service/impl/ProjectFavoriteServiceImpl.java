package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.entity.UserFavoriteEntity;
import com.brandPitara.sfs.enums.FavoriteTargetType;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.dto.ProjectCardDto;
import com.brandPitara.sfs.project.dto.ProjectPublicResponse;
import com.brandPitara.sfs.project.dto.ProjectResponse;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import com.brandPitara.sfs.project.mapper.ProjectMapper;
import com.brandPitara.sfs.project.repository.ProjectMediaRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.project.service.ProjectFavoriteService;
import com.brandPitara.sfs.repository.UserFavoriteRepository;
import com.brandPitara.sfs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectFavoriteServiceImpl implements ProjectFavoriteService {

    private final UserFavoriteRepository userFavoriteRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMediaRepository projectMediaRepository;

    @Override
    public void toggleProjectFavorite(Long projectId) {
        User user = getCurrentUserOrThrow();

        ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

        if (!Boolean.TRUE.equals(project.getPublished())
                || !Boolean.TRUE.equals(project.getActive())
                || Boolean.TRUE.equals(project.getDeleted())
                || project.getReviewStatus() != ReviewStatus.APPROVED) {
            throw new NotFoundException("Project not found: " + projectId);
        }

        var existing = userFavoriteRepository.findByUser_IdAndTargetTypeAndTargetId(
                user.getId(),
                FavoriteTargetType.PROJECT,
                projectId
        );

        if (existing.isPresent()) {
            userFavoriteRepository.delete(existing.get());
            return;
        }

        try {
            userFavoriteRepository.save(
                    UserFavoriteEntity.builder()
                            .user(user)
                            .targetType(FavoriteTargetType.PROJECT)
                            .targetId(projectId)
                            .build()
            );
        } catch (org.springframework.dao.DataIntegrityViolationException ignored) {
            // race-safe
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isProjectFavorite(Long projectId) {
        User user = getCurrentUserOrThrow();
        return userFavoriteRepository.existsByUser_IdAndTargetTypeAndTargetId(
                user.getId(),
                FavoriteTargetType.PROJECT,
                projectId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long getProjectFavoriteCount(Long projectId) {
        return userFavoriteRepository.countByTargetTypeAndTargetId(FavoriteTargetType.PROJECT, projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectResponse> listMyFavoriteProjects(Pageable pageable) {
        User user = getCurrentUserOrThrow();

        List<UserFavoriteEntity> allFavorites =
                userFavoriteRepository.findByUser_IdAndTargetTypeOrderByCreatedAtDesc(
                        user.getId(),
                        FavoriteTargetType.PROJECT
                );

        List<Long> projectIds = allFavorites.stream()
                .map(UserFavoriteEntity::getTargetId)
                .toList();

        if (projectIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<ProjectEntity> projects = projectRepository
            .findByIdInAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
                    projectIds,
                    ReviewStatus.APPROVED
            );

        Map<Long, ProjectEntity> projectMap = projects.stream()
                .collect(Collectors.toMap(ProjectEntity::getId, p -> p));

        List<ProjectEntity> orderedProjects = projectIds.stream()
                .map(projectMap::get)
                .filter(Objects::nonNull)
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), orderedProjects.size());

        List<ProjectEntity> pageContent =
                start >= orderedProjects.size() ? List.of() : orderedProjects.subList(start, end);

        List<Long> pageProjectIds = pageContent.stream().map(ProjectEntity::getId).toList();

        Map<Long, List<ProjectMediaEntity>> mediaMap;
        if (!pageProjectIds.isEmpty()) {
            var mediaList = projectMediaRepository.findActiveByProjectIds(pageProjectIds);
            mediaMap = mediaList.stream()
                    .collect(Collectors.groupingBy(m -> m.getProject().getId()));
        } else {
            mediaMap = Collections.emptyMap();
        }

        List<ProjectResponse> responses = pageContent.stream()
                .map(p -> ProjectMapper.toResponse(p, mediaMap.getOrDefault(p.getId(), List.of())))
                .toList();

        enrichProjects(responses);

        return new PageImpl<>(responses, pageable, orderedProjects.size());
    }

    @Override
    @Transactional(readOnly = true)
    public void enrichProjects(List<ProjectResponse> projects) {
        if (projects == null || projects.isEmpty()) return;

        List<Long> projectIds = projects.stream()
                .map(ProjectResponse::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (projectIds.isEmpty()) return;

        Map<Long, Long> countMap = buildFavoriteCountMap(projectIds);
        Set<Long> favoritedIds = buildFavoritedIdSet(projectIds);

        for (ProjectResponse project : projects) {
            Long id = project.getId();
            project.setFavoriteCount(countMap.getOrDefault(id, 0L));
            project.setIsFavorite(favoritedIds.contains(id));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void enrichProject(ProjectResponse project) {
        if (project == null || project.getId() == null) return;
        enrichProjects(List.of(project));
    }

    @Override
    @Transactional(readOnly = true)
    public void enrichPublicProjects(List<ProjectPublicResponse> projects) {
        if (projects == null || projects.isEmpty()) return;

        List<Long> projectIds = projects.stream()
                .map(ProjectPublicResponse::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (projectIds.isEmpty()) return;

        Map<Long, Long> countMap = buildFavoriteCountMap(projectIds);
        Set<Long> favoritedIds = buildFavoritedIdSet(projectIds);

        for (ProjectPublicResponse project : projects) {
            Long id = project.getId();
            project.setFavoriteCount(countMap.getOrDefault(id, 0L));
            project.setIsFavorite(favoritedIds.contains(id));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void enrichPublicProject(ProjectPublicResponse project) {
        if (project == null || project.getId() == null) return;
        enrichPublicProjects(List.of(project));
    }

    @Override
    @Transactional(readOnly = true)
    public void enrichProjectCards(List<ProjectCardDto> cards) {
        if (cards == null || cards.isEmpty()) return;

        List<Long> projectIds = cards.stream()
                .map(ProjectCardDto::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (projectIds.isEmpty()) return;

        Map<Long, Long> countMap = buildFavoriteCountMap(projectIds);
        Set<Long> favoritedIds = buildFavoritedIdSet(projectIds);

        for (ProjectCardDto card : cards) {
            Long id = card.getId();
            card.setFavoriteCount(countMap.getOrDefault(id, 0L));
            card.setIsFavorite(favoritedIds.contains(id));
        }
    }

    private Map<Long, Long> buildFavoriteCountMap(List<Long> projectIds) {
        Map<Long, Long> countMap = new HashMap<>();

        var countRows = userFavoriteRepository.countByTargetTypeAndTargetIds(
                FavoriteTargetType.PROJECT,
                projectIds
        );

        for (Object[] row : countRows) {
            Long targetId = (Long) row[0];
            Long count = (Long) row[1];
            countMap.put(targetId, count);
        }

        return countMap;
    }

    private Set<Long> buildFavoritedIdSet(List<Long> projectIds) {
        Set<Long> favoritedIds = new HashSet<>();

        Optional<Long> userIdOpt = getCurrentUserIdOptional();
        if (userIdOpt.isPresent()) {
            favoritedIds.addAll(
                    userFavoriteRepository.findFavoritedTargetIds(
                            userIdOpt.get(),
                            FavoriteTargetType.PROJECT,
                            projectIds
                    )
            );
        }

        return favoritedIds;
    }

    private User getCurrentUserOrThrow() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new NotFoundException("User not found for phone: " + phone));
    }

    private Optional<Long> getCurrentUserIdOptional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) return Optional.empty();

        String name = auth.getName();
        if (name == null || name.isBlank() || "anonymousUser".equalsIgnoreCase(name)) {
            return Optional.empty();
        }

        return userRepository.findByPhoneNumber(name).map(User::getId);
    }
}