package com.brandPitara.sfs.dbsearch.app;

import com.brandPitara.sfs.dbsearch.dto.SearchResultResponse;
import com.brandPitara.sfs.dbsearch.dto.SearchSuggestResponse;
import com.brandPitara.sfs.dbsearch.infra.BuilderSearchRepository;
import com.brandPitara.sfs.dbsearch.infra.CompanySearchRepository;
import com.brandPitara.sfs.dbsearch.infra.ProjectSearchRepository;
import com.brandPitara.sfs.dbsearch.mapper.SearchMapper;
import com.brandPitara.sfs.project.repository.ProjectMediaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

    @Mock private ProjectSearchRepository projectSearchRepository;
    @Mock private BuilderSearchRepository builderSearchRepository;
    @Mock private CompanySearchRepository companySearchRepository;
    @Mock private ProjectMediaRepository projectMediaRepository;
    @Mock private SearchMapper searchMapper;

    @InjectMocks private SearchServiceImpl service;

    @Captor private ArgumentCaptor<Pageable> pageableCaptor;
    @Captor private ArgumentCaptor<String> queryCaptor;

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

    // ── search: page size cap at 20 ───────────────────────────────────────────

    @Test
    void search_capsPageSizeAt20_whenCallerRequestsMore() {
        stubEmptyRepos();

        service.search("prestige", null, 0, 500);

        verify(projectSearchRepository).searchProjects(anyString(), any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isLessThanOrEqualTo(20);
    }

    // ── search: negative page defaults to 0 ──────────────────────────────────

    @Test
    void search_treatsNegativePageAsZero() {
        stubEmptyRepos();

        SearchResultResponse response = service.search("dlf", null, -5, 10);

        assertThat(response.getPage()).isZero();
    }

    // ── search: query is trimmed ──────────────────────────────────────────────

    @Test
    void search_trimsWhitespaceFromQuery() {
        stubEmptyRepos();

        service.search("  brigade  ", null, 0, 10);

        verify(projectSearchRepository).searchProjects(queryCaptor.capture(), any(), any());
        assertThat(queryCaptor.getValue()).isEqualTo("brigade");
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void stubEmptyRepos() {
        when(projectSearchRepository.searchProjects(anyString(), any(), any()))
                .thenReturn(List.of());
        when(builderSearchRepository.searchBuilders(anyString(), any(), any()))
                .thenReturn(List.of());
        when(companySearchRepository.searchCompanies(anyString(), any()))
                .thenReturn(List.of());
    }
}
