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
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import com.brandPitara.sfs.project.mapper.ProjectMediaPicker;
import com.brandPitara.sfs.project.repository.ProjectMediaRepository;
import com.brandPitara.sfs.project.service.ProjectFavoriteService;
import com.brandPitara.sfs.projectmeter.entity.ProjectMeterSnapshotEntity;
import com.brandPitara.sfs.projectmeter.repository.ProjectMeterSnapshotRepository;
import com.brandPitara.sfs.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
    private final ProjectFavoriteService projectFavoriteService;
    private final CityRepository cityRepository;
    private final ProjectMeterSnapshotRepository projectMeterSnapshotRepository;

    public SearchSuggestResponse suggest(String query, Long cityId, Integer limit) {
        return suggest(query, cityId, null, limit);
    }

    @Override
    public SearchSuggestResponse suggest(String query, Long cityId, String citySlug, Integer limit) {
        String normalizedQuery = normalizeQuery(query);
        int safeLimit = normalizeLimit(limit);
        ResolvedCityFilter cityFilter = resolveCityFilter(cityId, citySlug);

        if (normalizedQuery.length() < 2 || cityFilter.invalid()) {
            return SearchSuggestResponse.builder()
                    .query(normalizedQuery)
                    .sections(List.of())
                    .build();
        }

        List<ProjectEntity> projects = projectSearchRepository.searchProjects(
                normalizedQuery, cityFilter.cityId(), PageRequest.of(0, safeLimit)
        );
        List<BuilderEntity> builders = builderSearchRepository.searchBuilders(
                normalizedQuery, cityFilter.cityId(), PageRequest.of(0, safeLimit)
        );
        List<CompanyEntity> companies = companySearchRepository.searchCompanies(
                normalizedQuery, PageRequest.of(0, safeLimit)
        );

        Map<Long, List<ProjectMediaEntity>> mediaMap = loadProjectMediaMap(projects);

        List<SearchSectionDto> sections = new ArrayList<>();

        if (!projects.isEmpty()) {
            List<SearchItemDto> projectItems = projects.stream()
                    .limit(4)
                    .map(project -> searchMapper.toProjectItem(
                            project,
                            resolveProjectImageUrl(project.getId(), mediaMap),
                            buildProjectTags(project)
                    ))
                    .toList();
            projectFavoriteService.enrichSearchProjectItems(projectItems);

            sections.add(SearchSectionDto.builder()
                    .key("projects")
                    .title("Projects")
                    .items(projectItems)
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
        return search(new SearchCriteria(query, cityId, null, page, size));
    }

    @Override
    public SearchResultResponse search(SearchCriteria criteria) {
        SearchCriteria normalizedCriteria = normalizeCriteria(criteria);
        String normalizedQuery = normalizedCriteria.query();
        int safePage = normalizedCriteria.page();
        int safeSize = normalizedCriteria.size();
        ResolvedCityFilter cityFilter = resolveCityFilter(normalizedCriteria.cityId(), normalizedCriteria.citySlug());

        if (cityFilter.invalid()) {
            return emptyResult(normalizedQuery, safePage, safeSize);
        }

        boolean hasCityFilter = cityFilter.cityId() != null;
        SearchCriteria resolvedCriteria = withResolvedCity(normalizedCriteria, cityFilter.cityId());
        boolean hasKeyword = !normalizedQuery.isBlank();
        boolean hasFilters = hasSearchFilters(resolvedCriteria);

        if (!hasKeyword && !hasFilters) {
            return emptyResult(normalizedQuery, safePage, safeSize);
        }

        Page<ProjectEntity> projectPage = projectSearchRepository.searchProjects(
                resolvedCriteria,
                PageRequest.of(safePage, safeSize)
        );
        List<ProjectEntity> projects = projectPage.getContent();

        if (hasFilters) {
            List<SearchItemDto> projectItems = mapProjectItems(projects);
            projectFavoriteService.enrichSearchProjectItems(projectItems);

            return SearchResultResponse.builder()
                    .query(normalizedQuery)
                    .page(safePage)
                    .size(safeSize)
                    .totalElements(projectPage.getTotalElements())
                    .totalPages(projectPage.getTotalPages())
                    .hasNextPage(projectPage.hasNext())
                    .items(projectItems)
                    .build();
        }

        List<BuilderEntity> builders = builderSearchRepository.searchBuilders(
                normalizedQuery, cityFilter.cityId(), PageRequest.of(safePage, safeSize)
        );
        List<CompanyEntity> companies = companySearchRepository.searchCompanies(
                normalizedQuery, PageRequest.of(safePage, safeSize)
        );

        List<SearchItemDto> items = new ArrayList<>(mapProjectItems(projects));

        builders.stream()
                .map(searchMapper::toBuilderItem)
                .forEach(items::add);

        companies.stream()
                .map(searchMapper::toCompanyItem)
                .forEach(items::add);

        projectFavoriteService.enrichSearchProjectItems(items);

        return SearchResultResponse.builder()
                .query(normalizedQuery)
                .page(safePage)
                .size(safeSize)
                .totalElements((long) items.size())
                .totalPages(1)
                .hasNextPage(false)
                .items(items)
                .build();
    }

    private SearchResultResponse emptyResult(String normalizedQuery, int safePage, int safeSize) {
        return SearchResultResponse.builder()
                .query(normalizedQuery)
                .page(safePage)
                .size(safeSize)
                .totalElements(0L)
                .totalPages(0)
                .hasNextPage(false)
                .items(List.of())
                .build();
    }

    private List<SearchItemDto> mapProjectItems(List<ProjectEntity> projects) {
        Map<Long, List<ProjectMediaEntity>> mediaMap = loadProjectMediaMap(projects);
        Map<Long, ProjectMeterSnapshotEntity> snapshotMap = loadProjectSnapshotMap(projects);

        return projects.stream()
                .map(project -> searchMapper.toProjectItem(
                        project,
                        resolveProjectImageUrl(project.getId(), mediaMap),
                        buildProjectTags(project),
                        snapshotMap.get(project.getId())
                ))
                .toList();
    }

    private SearchCriteria normalizeCriteria(SearchCriteria criteria) {
        SearchCriteria raw = criteria == null
                ? new SearchCriteria(null, null, null, null, null)
                : criteria;

        int page = raw.page() == null ? 0 : raw.page();
        int size = raw.size() == null ? 10 : raw.size();

        if (page < 0) {
            throw badRequest("page must be greater than or equal to 0");
        }
        if (size < 1) {
            throw badRequest("size must be greater than or equal to 1");
        }

        Long priceMin = raw.priceMin();
        Long priceMax = raw.priceMax();
        if (priceMin != null && priceMin < 0) {
            throw badRequest("priceMin must be greater than or equal to 0");
        }
        if (priceMax != null && priceMax < 0) {
            throw badRequest("priceMax must be greater than or equal to 0");
        }
        if (priceMin != null && priceMax != null && priceMin > priceMax) {
            throw badRequest("priceMin must be less than or equal to priceMax");
        }

        Integer progressMin = raw.constructionProgressMin();
        Integer progressMax = raw.constructionProgressMax();
        validateProgress(progressMin, "constructionProgressMin");
        validateProgress(progressMax, "constructionProgressMax");
        if (progressMin != null && progressMax != null && progressMin > progressMax) {
            throw badRequest("constructionProgressMin must be less than or equal to constructionProgressMax");
        }

        if (raw.possessionFrom() != null && raw.possessionTo() != null
                && raw.possessionFrom().isAfter(raw.possessionTo())) {
            throw badRequest("possessionFrom must be less than or equal to possessionTo");
        }

        String query = normalizeQuery(raw.query());
        if (query.length() < 2) {
            query = "";
        }

        return new SearchCriteria(
                query,
                raw.cityId(),
                normalizeBlankToNull(raw.citySlug()),
                priceMin,
                priceMax,
                progressMin,
                progressMax,
                raw.possessionFrom(),
                raw.possessionTo(),
                raw.statuses() == null ? List.of() : raw.statuses(),
                raw.propertyTypes() == null ? List.of() : raw.propertyTypes(),
                raw.sort() == null ? defaultSort(query) : raw.sort(),
                page,
                Math.min(size, 20)
        );
    }

    private SearchCriteria withResolvedCity(SearchCriteria criteria, Long cityId) {
        return new SearchCriteria(
                criteria.query(),
                cityId,
                null,
                criteria.priceMin(),
                criteria.priceMax(),
                criteria.constructionProgressMin(),
                criteria.constructionProgressMax(),
                criteria.possessionFrom(),
                criteria.possessionTo(),
                criteria.statuses(),
                criteria.propertyTypes(),
                criteria.sort(),
                criteria.page(),
                criteria.size()
        );
    }

    private boolean hasSearchFilters(SearchCriteria criteria) {
        return criteria.cityId() != null
                || criteria.priceMin() != null
                || criteria.priceMax() != null
                || criteria.constructionProgressMin() != null
                || criteria.constructionProgressMax() != null
                || criteria.possessionFrom() != null
                || criteria.possessionTo() != null
                || (criteria.statuses() != null && !criteria.statuses().isEmpty())
                || (criteria.propertyTypes() != null && !criteria.propertyTypes().isEmpty())
                || criteria.sort() != null && criteria.sort() != defaultSort(criteria.query());
    }

    private SearchSort defaultSort(String query) {
        return query == null || query.isBlank() ? SearchSort.PRIORITY : SearchSort.RELEVANCE;
    }

    private void validateProgress(Integer value, String fieldName) {
        if (value != null && (value < 0 || value > 100)) {
            throw badRequest(fieldName + " must be between 0 and 100");
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private String normalizeBlankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private ResolvedCityFilter resolveCityFilter(Long cityId, String citySlug) {
        if (cityId != null) {
            return new ResolvedCityFilter(cityId, false);
        }

        String normalizedSlug = citySlug == null ? "" : citySlug.trim();
        if (normalizedSlug.isBlank()) {
            return new ResolvedCityFilter(null, false);
        }

        return cityRepository.findBySlugIgnoreCaseAndActiveTrue(normalizedSlug)
                .map(CityEntity::getId)
                .map(id -> new ResolvedCityFilter(id, false))
                .orElseGet(() -> new ResolvedCityFilter(null, true));
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

    private Map<Long, ProjectMeterSnapshotEntity> loadProjectSnapshotMap(List<ProjectEntity> projects) {
        if (projects == null || projects.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> projectIds = projects.stream()
                .map(ProjectEntity::getId)
                .distinct()
                .toList();

        return projectMeterSnapshotRepository.findByProjectIdIn(projectIds).stream()
                .filter(snapshot -> snapshot.getProject() != null && snapshot.getProject().getId() != null)
                .collect(Collectors.toMap(
                        snapshot -> snapshot.getProject().getId(),
                        snapshot -> snapshot,
                        (first, ignored) -> first,
                        LinkedHashMap::new
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

    private record ResolvedCityFilter(Long cityId, boolean invalid) {
    }
}
