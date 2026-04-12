package com.brandPitara.sfs.project.service;

import com.brandPitara.sfs.project.dto.ProjectCardDto;
import com.brandPitara.sfs.project.dto.ProjectResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProjectFavoriteService {

    void toggleProjectFavorite(Long projectId);

    boolean isProjectFavorite(Long projectId);

    long getProjectFavoriteCount(Long projectId);

    Page<ProjectResponse> listMyFavoriteProjects(Pageable pageable);

    void enrichProjects(List<ProjectResponse> projects);

    void enrichProject(ProjectResponse project);

    void enrichProjectCards(List<ProjectCardDto> cards);
}