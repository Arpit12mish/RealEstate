package com.brandPitara.sfs.project.service;

import com.brandPitara.sfs.builder.dto.BuilderProjectCardDto;
import com.brandPitara.sfs.dbsearch.dto.SearchItemDto;
import com.brandPitara.sfs.home.dto.GenericCardDto;
import com.brandPitara.sfs.project.dto.ProjectCardDto;
import com.brandPitara.sfs.project.dto.ProjectNearbyListingCardDto;
import com.brandPitara.sfs.project.dto.ProjectPublicResponse;
import com.brandPitara.sfs.project.dto.ProjectResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterCardResponse;
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

    void enrichPublicProjects(List<ProjectPublicResponse> projects);

    void enrichPublicProject(ProjectPublicResponse project);

    void enrichProjectCards(List<ProjectCardDto> cards);

    void enrichNearbyListingCards(List<ProjectNearbyListingCardDto> cards);

    void enrichProjectMeterCards(List<ProjectMeterCardResponse> cards);

    void enrichBuilderProjectCards(List<BuilderProjectCardDto> cards);

    void enrichSearchProjectItems(List<SearchItemDto> items);

    void enrichGenericProjectCards(List<GenericCardDto> cards);
}
