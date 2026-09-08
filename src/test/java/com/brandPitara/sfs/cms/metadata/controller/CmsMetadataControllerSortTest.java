package com.brandPitara.sfs.cms.metadata.controller;

import com.brandPitara.sfs.cms.metadata.service.CmsMetadataService;
import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the exact defect that made GET /api/dashboard/cms/authors 500 in production: the controller's
 * shared page() helper hardcoded a "name" sort property reused by authors/categories/tags, but
 * CmsPublicAuthorEntity has no "name" field (only "displayName") — Hibernate rejected the Sort Spring
 * Data appended onto the repository's @Query at runtime. Categories/tags never failed because their
 * entities genuinely have "name". This test fails immediately (assertion, not an HQL exception) if
 * anyone reverts any of the three endpoints to a mismatched sort property.
 */
@ExtendWith(MockitoExtension.class)
class CmsMetadataControllerSortTest {
    @Mock CmsMetadataService service;
    @Mock DashboardActionAuditService audit;
    CmsMetadataController controller;

    @BeforeEach
    void setUp() {
        controller = new CmsMetadataController(service, audit);
    }

    @Test
    void authorsListSortsByDisplayNameNotName() {
        when(service.authors(isNull(), isNull(), any())).thenReturn(Page.empty());

        controller.authors(null, null, 0, 20);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).authors(isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("displayName")).isNotNull();
        assertThat(captor.getValue().getSort().getOrderFor("name")).isNull();
    }

    @Test
    void categoriesListSortsByName() {
        when(service.categories(isNull(), isNull(), any())).thenReturn(Page.empty());

        controller.categories(null, null, 0, 20);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).categories(isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("name")).isNotNull();
    }

    @Test
    void tagsListSortsByName() {
        when(service.tags(isNull(), isNull(), any())).thenReturn(Page.empty());

        controller.tags(null, null, 0, 20);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).tags(isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("name")).isNotNull();
    }

    @Test
    void pageSizeIsClampedTo100() {
        when(service.authors(isNull(), isNull(), any())).thenReturn(Page.empty());

        controller.authors(null, null, 0, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).authors(isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }
}
