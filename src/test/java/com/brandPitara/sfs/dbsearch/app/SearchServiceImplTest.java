package com.brandPitara.sfs.dbsearch.app;

import com.brandPitara.sfs.dbsearch.dto.SearchEntityType;
import com.brandPitara.sfs.dbsearch.dto.SearchItemDto;
import com.brandPitara.sfs.dbsearch.dto.SearchResultResponse;
import com.brandPitara.sfs.dbsearch.dto.SearchSuggestResponse;
import com.brandPitara.sfs.dbsearch.infra.BuilderSearchRepository;
import com.brandPitara.sfs.dbsearch.infra.CompanySearchRepository;
import com.brandPitara.sfs.dbsearch.infra.ProjectSearchRepository;
import com.brandPitara.sfs.dbsearch.mapper.SearchMapper;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.enums.ProjectStatus;
import com.brandPitara.sfs.project.enums.PropertyType;
import com.brandPitara.sfs.project.repository.ProjectMediaRepository;
import com.brandPitara.sfs.project.service.ProjectFavoriteService;
import com.brandPitara.sfs.projectmeter.repository.ProjectMeterSnapshotRepository;
import com.brandPitara.sfs.repository.CityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

    @Mock private ProjectSearchRepository projectSearchRepository;
    @Mock private BuilderSearchRepository builderSearchRepository;
    @Mock private CompanySearchRepository companySearchRepository;
    @Mock private ProjectMediaRepository projectMediaRepository;
    @Mock private SearchMapper searchMapper;
    @Mock private ProjectFavoriteService projectFavoriteService;
    @Mock private CityRepository cityRepository;
    @Mock private ProjectMeterSnapshotRepository projectMeterSnapshotRepository;

    @InjectMocks private SearchServiceImpl service;

    @Captor private ArgumentCaptor<Pageable> pageableCaptor;
    @Captor private ArgumentCaptor<String> queryCaptor;
    @Captor private ArgumentCaptor<Long> cityIdCaptor;
    @Captor private ArgumentCaptor<SearchCriteria> criteriaCaptor;

    // ── suggest: empty/blank/short query → no DB call ─────────────────────────

    @Test
    void suggest_returnsEmptySections_forNullQuery() {
        SearchSuggestResponse response = service.suggest(null, null, null);

        assertThat(response.getSections()).isEmpty();
        verify(projectSearchRepository, never()).searchProjects(anyString(), any(), any());
    }

    @Test
    void suggest_returnsEmptySections_forBlankQuery() {
        SearchSuggestResponse response = service.suggest("   ", null, null);

        assertThat(response.getSections()).isEmpty();
        verify(projectSearchRepository, never()).searchProjects(anyString(), any(), any());
    }

    @Test
    void suggest_returnsEmptySections_forSingleCharQuery() {
        SearchSuggestResponse response = service.suggest("a", null, null);

        assertThat(response.getSections()).isEmpty();
        verify(projectSearchRepository, never()).searchProjects(anyString(), any(), any());
    }

    // ── suggest: query normalisation ──────────────────────────────────────────

    @Test
    void suggest_trimsWhitespaceBeforePassingToRepo() {
        stubEmptyRepos();

        service.suggest("  gurgaon  ", null, null);

        verify(projectSearchRepository).searchProjects(queryCaptor.capture(), any(), any());
        assertThat(queryCaptor.getValue()).isEqualTo("gurgaon");
    }

    // ── suggest: limit capped at 12 ───────────────────────────────────────────

    @Test
    void suggest_capsLimitAt12_whenCallerRequestsMore() {
        stubEmptyRepos();

        service.suggest("prestige", null, 999);

        verify(projectSearchRepository).searchProjects(anyString(), any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isLessThanOrEqualTo(12);
    }

    @Test
    void suggest_usesDefaultLimit8_whenLimitIsNull() {
        stubEmptyRepos();

        service.suggest("dlf", null, null);

        verify(projectSearchRepository).searchProjects(anyString(), any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(8);
    }

    // ── search: short query → empty result, no DB call ────────────────────────

    @Test
    void search_returnsEmptyResult_forSingleCharQuery() {
        SearchResultResponse response = service.search("x", null, 0, 20);

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
        verify(projectSearchRepository, never()).searchProjects(anyString(), any(), any());
    }

    @Test
    void search_returnsEmptyResult_forBlankQuery() {
        SearchResultResponse response = service.search("", null, 0, 20);

        assertThat(response.getItems()).isEmpty();
        verify(projectSearchRepository, never()).searchProjects(anyString(), any(), any());
    }

    @Test
    void search_returnsEmptyResult_forNullQuery() {
        SearchResultResponse response = service.search(null, null, 0, 20);

        assertThat(response.getItems()).isEmpty();
        verify(projectSearchRepository, never()).searchProjects(anyString(), any(), any());
    }

    @Test
    void search_withCityOnlyReturnsProjectsInCity() {
        ProjectEntity project = new ProjectEntity();
        project.setId(101L);

        SearchItemDto projectItem = SearchItemDto.builder()
                .id(101L)
                .entityType(SearchEntityType.PROJECT)
                .build();

        when(projectSearchRepository.searchProjects(any(SearchCriteria.class), any()))
                .thenReturn(new PageImpl<>(List.of(project), org.springframework.data.domain.PageRequest.of(0, 20), 1));
        when(projectMediaRepository.findActiveByProjectIds(List.of(101L)))
                .thenReturn(List.of());
        when(projectMeterSnapshotRepository.findByProjectIdIn(List.of(101L)))
                .thenReturn(List.of());
        when(searchMapper.toProjectItem(project, null, List.of(), null))
                .thenReturn(projectItem);

        SearchResultResponse response = service.search("", 1L, 0, 20);

        assertThat(response.getItems()).containsExactly(projectItem);
        assertThat(response.getTotalElements()).isEqualTo(1L);
        verify(builderSearchRepository, never()).searchBuilders(anyString(), any(), any());
        verify(companySearchRepository, never()).searchCompanies(anyString(), any());
        verify(projectFavoriteService).enrichSearchProjectItems(response.getItems());
    }

    @Test
    void search_withKeywordAndCityKeepsFiltersSeparate() {
        stubEmptyRepos();

        service.search("  m3m  ", 1L, 0, 10);

        verify(projectSearchRepository).searchProjects(criteriaCaptor.capture(), any());
        assertThat(criteriaCaptor.getValue().query()).isEqualTo("m3m");
        assertThat(criteriaCaptor.getValue().cityId()).isEqualTo(1L);
    }

    @Test
    void search_withPriceRangePassesPriceCriteria() {
        stubEmptyRepos();

        service.search(new SearchCriteria(
                null,
                1L,
                null,
                10_000_000L,
                30_000_000L,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                null,
                0,
                10
        ));

        verify(projectSearchRepository).searchProjects(criteriaCaptor.capture(), any());
        assertThat(criteriaCaptor.getValue().priceMin()).isEqualTo(10_000_000L);
        assertThat(criteriaCaptor.getValue().priceMax()).isEqualTo(30_000_000L);
    }

    @Test
    void search_withProgressRangePassesProgressCriteria() {
        stubEmptyRepos();

        service.search(new SearchCriteria(
                null,
                1L,
                null,
                null,
                null,
                40,
                80,
                null,
                null,
                List.of(),
                List.of(),
                null,
                0,
                10
        ));

        verify(projectSearchRepository).searchProjects(criteriaCaptor.capture(), any());
        assertThat(criteriaCaptor.getValue().constructionProgressMin()).isEqualTo(40);
        assertThat(criteriaCaptor.getValue().constructionProgressMax()).isEqualTo(80);
    }

    @Test
    void search_withPossessionStatusAndPropertyTypesPassesCriteria() {
        stubEmptyRepos();
        LocalDate from = LocalDate.of(2026, 7, 8);
        LocalDate to = LocalDate.of(2027, 7, 8);

        service.search(new SearchCriteria(
                "m3m",
                1L,
                null,
                null,
                null,
                null,
                null,
                from,
                to,
                List.of(ProjectStatus.UNDER_CONSTRUCTION),
                List.of(PropertyType.APARTMENT, PropertyType.VILLA),
                SearchSort.PRICE_LOW,
                0,
                10
        ));

        verify(projectSearchRepository).searchProjects(criteriaCaptor.capture(), any());
        SearchCriteria value = criteriaCaptor.getValue();
        assertThat(value.possessionFrom()).isEqualTo(from);
        assertThat(value.possessionTo()).isEqualTo(to);
        assertThat(value.statuses()).containsExactly(ProjectStatus.UNDER_CONSTRUCTION);
        assertThat(value.propertyTypes()).containsExactly(PropertyType.APARTMENT, PropertyType.VILLA);
        assertThat(value.sort()).isEqualTo(SearchSort.PRICE_LOW);
    }

    @Test
    void search_rejectsInvalidPriceRange() {
        assertThatThrownBy(() -> service.search(new SearchCriteria(
                null,
                1L,
                null,
                30_000_000L,
                10_000_000L,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                null,
                0,
                10
        ))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void search_rejectsInvalidProgressRange() {
        assertThatThrownBy(() -> service.search(new SearchCriteria(
                null,
                1L,
                null,
                null,
                null,
                101,
                null,
                null,
                null,
                List.of(),
                List.of(),
                null,
                0,
                10
        ))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void search_withCitySlugResolvesCityId() {
        CityEntity city = CityEntity.builder()
                .id(7L)
                .name("Gurugram")
                .slug("gurugram")
                .active(true)
                .build();

        when(cityRepository.findBySlugIgnoreCaseAndActiveTrue("gurugram"))
                .thenReturn(Optional.of(city));
        when(projectSearchRepository.searchProjects(any(SearchCriteria.class), any()))
                .thenReturn(new PageImpl<>(List.of()));

        SearchResultResponse response = service.search(
                new SearchCriteria("", null, " gurugram ", 0, 10)
        );

        assertThat(response.getItems()).isEmpty();
        verify(projectSearchRepository).searchProjects(criteriaCaptor.capture(), any());
        assertThat(criteriaCaptor.getValue().cityId()).isEqualTo(7L);
    }

    @Test
    void search_withInvalidCitySlugReturnsEmptyResultWithoutBroadSearch() {
        when(cityRepository.findBySlugIgnoreCaseAndActiveTrue("missing"))
                .thenReturn(Optional.empty());

        SearchResultResponse response = service.search(
                new SearchCriteria("", null, "missing", 0, 10)
        );

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
        verify(projectSearchRepository, never()).searchProjects(anyString(), any(), any());
    }

    // ── search: page size cap at 20 ───────────────────────────────────────────

    @Test
    void search_capsPageSizeAt20_whenCallerRequestsMore() {
        stubEmptyRepos();

        service.search("prestige", null, 0, 500);

        verify(projectSearchRepository).searchProjects(any(SearchCriteria.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isLessThanOrEqualTo(20);
    }

    // ── search: invalid page returns 400 ─────────────────────────────────────

    @Test
    void search_rejectsNegativePage() {
        assertThatThrownBy(() -> service.search("dlf", null, -5, 10))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    // ── search: query is trimmed ──────────────────────────────────────────────

    @Test
    void search_trimsWhitespaceFromQuery() {
        stubEmptyRepos();

        service.search("  brigade  ", null, 0, 10);

        verify(projectSearchRepository).searchProjects(criteriaCaptor.capture(), any());
        assertThat(criteriaCaptor.getValue().query()).isEqualTo("brigade");
    }

    @Test
    void search_enrichesProjectItemsThroughFavoriteService() {
        ProjectEntity project = new ProjectEntity();
        project.setId(101L);

        SearchItemDto projectItem = SearchItemDto.builder()
                .id(101L)
                .entityType(SearchEntityType.PROJECT)
                .build();

        when(projectSearchRepository.searchProjects(any(SearchCriteria.class), any()))
                .thenReturn(new PageImpl<>(List.of(project)));
        when(builderSearchRepository.searchBuilders(anyString(), any(), any()))
                .thenReturn(List.of());
        when(companySearchRepository.searchCompanies(anyString(), any()))
                .thenReturn(List.of());
        when(projectMediaRepository.findActiveByProjectIds(List.of(101L)))
                .thenReturn(List.of());
        when(projectMeterSnapshotRepository.findByProjectIdIn(List.of(101L)))
                .thenReturn(List.of());
        when(searchMapper.toProjectItem(project, null, List.of(), null))
                .thenReturn(projectItem);

        SearchResultResponse response = service.search("m3m", null, 0, 10);

        assertThat(response.getItems()).containsExactly(projectItem);
        verify(projectFavoriteService).enrichSearchProjectItems(response.getItems());
    }

    @Test
    void suggest_enrichesProjectSuggestionItemsThroughFavoriteService() {
        ProjectEntity project = new ProjectEntity();
        project.setId(202L);

        SearchItemDto projectItem = SearchItemDto.builder()
                .id(202L)
                .entityType(SearchEntityType.PROJECT)
                .build();

        when(projectSearchRepository.searchProjects(anyString(), any(), any()))
                .thenReturn(List.of(project));
        when(builderSearchRepository.searchBuilders(anyString(), any(), any()))
                .thenReturn(List.of());
        when(companySearchRepository.searchCompanies(anyString(), any()))
                .thenReturn(List.of());
        when(projectMediaRepository.findActiveByProjectIds(List.of(202L)))
                .thenReturn(List.of());
        when(searchMapper.toProjectItem(project, null, List.of()))
                .thenReturn(projectItem);

        SearchSuggestResponse response = service.suggest("m3m", null, null);

        List<SearchItemDto> items = response.getSections().get(0).getItems();
        assertThat(items).containsExactly(projectItem);
        verify(projectFavoriteService).enrichSearchProjectItems(items);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void stubEmptyRepos() {
        lenient().when(projectSearchRepository.searchProjects(any(SearchCriteria.class), any()))
                .thenReturn(new PageImpl<>(List.of()));
        lenient().when(projectSearchRepository.searchProjects(anyString(), any(), any()))
                .thenReturn(List.of());
        lenient().when(builderSearchRepository.searchBuilders(anyString(), any(), any()))
                .thenReturn(List.of());
        lenient().when(companySearchRepository.searchCompanies(anyString(), any()))
                .thenReturn(List.of());
    }
}
