package com.brandPitara.sfs.dbsearch.app;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.dbsearch.dto.SearchItemDto;
import com.brandPitara.sfs.dbsearch.dto.SearchResultResponse;
import com.brandPitara.sfs.dbsearch.dto.SearchSectionDto;
import com.brandPitara.sfs.dbsearch.dto.SearchSuggestResponse;
import com.brandPitara.sfs.dbsearch.infra.BuilderSearchRepository;
import com.brandPitara.sfs.dbsearch.infra.CompanySearchRepository;
import com.brandPitara.sfs.dbsearch.infra.ProjectSearchRepository;
import com.brandPitara.sfs.dbsearch.mapper.SearchMapper;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import com.brandPitara.sfs.project.mapper.ProjectMediaPicker;
import com.brandPitara.sfs.project.repository.ProjectMediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ProjectSearchRepository projectSearchRepository;
    private final BuilderSearchRepository builderSearchRepository;
    private final CompanySearchRepository companySearchRepository;
    private final ProjectMediaRepository projectMediaRepository;
    private final SearchMapper searchMapper;

    @Override
    public SearchSuggestResponse suggest(String query, Long cityId, Integer limit) {
        String normalizedQuery = normalizeQuery(query);
        int safeLimit = normalizeLimit(limit);

        if (normalizedQuery.length() < 2) {
            return SearchSuggestResponse.builder()
                    .query(normalizedQuery)
                    .sections(List.of())
                    .build();
        }

        List<ProjectEntity> projects = projectSearchRepository.searchProjects(
                normalizedQuery, cityId, PageRequest.of(0, safeLimit)
        );
        List<BuilderEntity> builders = builderSearchRepository.searchBuilders(
                normalizedQuery, cityId, PageRequest.of(0, safeLimit)
        );
        List<CompanyEntity> companies = companySearchRepository.searchCompanies(
                normalizedQuery, PageRequest.of(0, safeLimit)
        );

        Map<Long, List<ProjectMediaEntity>> mediaMap = loadProjectMediaMap(projects);

        List<SearchSectionDto> sections = new ArrayList<>();

        if (!projects.isEmpty()) {
            sections.add(SearchSectionDto.builder()
                    .key("projects")
                    .title("Projects")
                    .items(projects.stream()
                            .limit(4)
                            .map(project -> searchMapper.toProjectItem(
                                    project,
                                    resolveProjectImageUrl(project.getId(), mediaMap),
                                    buildProjectTags(project)
                            ))
                            .toList())
                    .build());
        }

        if (!builders.isEmpty()) {
            sections.add(SearchSectionDto.builder()
                    .key("builders")
                    .title("Builders")
                    .items(builders.stream()
                            .limit(3)
                            .map(searchMapper::toBuilderItem)
                            .toList())
                    .build());
        }

        if (!companies.isEmpty()) {
            sections.add(SearchSectionDto.builder()
                    .key("companies")
                    .title("Architects & Designers")
                    .items(companies.stream()
                            .limit(3)
                            .map(searchMapper::toCompanyItem)
                            .toList())
                    .build());
        }

        return SearchSuggestResponse.builder()
                .query(normalizedQuery)
                .sections(sections)
                .build();
    }

    @Override
    public SearchResultResponse search(String query, Long cityId, Integer page, Integer size) {
        String normalizedQuery = normalizeQuery(query);
        int safePage = page != null && page >= 0 ? page : 0;
        int safeSize = size != null && size > 0 ? Math.min(size, 20) : 10;

        if (normalizedQuery.length() < 2) {
            return SearchResultResponse.builder()
                    .query(normalizedQuery)
                    .page(safePage)
                    .size(safeSize)
                    .totalElements(0L)
                    .items(List.of())
                    .build();
        }

        List<ProjectEntity> projects = projectSearchRepository.searchProjects(
                normalizedQuery, cityId, PageRequest.of(safePage, safeSize)
        );
        List<BuilderEntity> builders = builderSearchRepository.searchBuilders(
                normalizedQuery, cityId, PageRequest.of(safePage, safeSize)
        );
        List<CompanyEntity> companies = companySearchRepository.searchCompanies(
                normalizedQuery, PageRequest.of(safePage, safeSize)
        );

        Map<Long, List<ProjectMediaEntity>> mediaMap = loadProjectMediaMap(projects);

        List<SearchItemDto> items = new ArrayList<>();

        projects.stream()
                .map(project -> searchMapper.toProjectItem(
                        project,
                        resolveProjectImageUrl(project.getId(), mediaMap),
                        buildProjectTags(project)
                ))
                .forEach(items::add);

        builders.stream()
                .map(searchMapper::toBuilderItem)
                .forEach(items::add);

        companies.stream()
                .map(searchMapper::toCompanyItem)
                .forEach(items::add);

        return SearchResultResponse.builder()
                .query(normalizedQuery)
                .page(safePage)
                .size(safeSize)
                .totalElements((long) items.size())
                .items(items)
                .build();
    }

    private Map<Long, List<ProjectMediaEntity>> loadProjectMediaMap(List<ProjectEntity> projects) {
        if (projects == null || projects.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> projectIds = projects.stream()
                .map(ProjectEntity::getId)
                .distinct()
                .toList();

        List<ProjectMediaEntity> mediaList = projectMediaRepository.findActiveByProjectIds(projectIds);

        return mediaList.stream()
                .collect(Collectors.groupingBy(
                        media -> media.getProject().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private String resolveProjectImageUrl(Long projectId, Map<Long, List<ProjectMediaEntity>> mediaMap) {
        List<ProjectMediaEntity> media = mediaMap.get(projectId);
        if (media == null || media.isEmpty()) {
            return null;
        }

        var picked = ProjectMediaPicker.pick(media, false);
        return picked.coverMediaUrl();
    }

    private List<String> buildProjectTags(ProjectEntity project) {
        List<String> tags = new ArrayList<>();

        if (project.getCreatedAt() != null &&
                project.getCreatedAt().toLocalDate().isAfter(LocalDate.now().minusDays(45))) {
            tags.add("New Launch");
        }

        if (Boolean.TRUE.equals(project.getPublished()) && (project.getPriority() != null && project.getPriority() <= 2)) {
            tags.add("Most Liked");
        }

        // "Nearest to you" cannot be computed correctly yet without user lat/lon in search request.
        // Add it only after location-aware search is implemented.

        return tags;
    }

    private String normalizeQuery(String query) {
        return query == null ? "" : query.trim();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) return 8;
        return Math.min(limit, 12);
    }
}