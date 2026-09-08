package com.brandPitara.sfs.company.service.impl;

import com.brandPitara.sfs.company.dto.CompanyResponse;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Hardens GAP-034: the plain public Company Listing/Detail contract
 * (`CompanyPublicController` -> `CompanyPublicServiceImpl` -> `CompanyResponse`)
 * previously had zero test coverage at all — confirmed via exhaustive
 * search, Phase 7A-R/7A. Every asserted value here is derived from the
 * existing, unmodified implementation, not invented; no endpoint or DTO
 * redesign happens in this phase.
 *
 * Also covers publicGetBySlug() (Phase 7B-G), the canonical Company slug
 * lookup added to this same service — merged into this file during Phase
 * 7B-GI's integration (the two contributing branches each independently
 * created a file at this exact path; both sets of tests are preserved
 * here rather than one replacing the other).
 */
class CompanyPublicServiceImplTest {

    private CompanyRepository companyRepository;
    private CompanyPublicServiceImpl service;

    @BeforeEach
    void setUp() {
        companyRepository = mock(CompanyRepository.class);
        service = new CompanyPublicServiceImpl(companyRepository);
    }

    private static Pageable pageable() {
        return PageRequest.of(0, 20, Sort.by("priority").ascending().and(Sort.by("id").descending()));
    }

    private CompanyEntity entity(long id, String name, String companyType) {
        return CompanyEntity.builder()
            .id(id)
            .name(name)
            .slug(name.toLowerCase().replace(" ", "-"))
            .companyType(companyType)
            .active(true)
            .published(true)
            .deleted(false)
            .build();
    }

    private CompanyEntity eligibleCompanyBySlug() {
        return CompanyEntity.builder()
            .id(701L).name("Meridian Architects").slug("meridian-architects").companyType("ARCHITECT")
            .logoUrl("https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/companies/meridian/logo.png")
            .description("Award-winning architecture studio.")
            .phone("+919876543210").whatsapp("+919876543210")
            .active(true).published(true).everPublished(true).deleted(false)
            .build();
    }

    // ── Pagination ───────────────────────────────────────────────────────────

    @Test
    void publicListReturnsAllEligibleCompaniesWhenNoTypeFilterProvided() {
        Page<CompanyEntity> page = new PageImpl<>(List.of(entity(1, "Meridian Architects", "ARCHITECT"), entity(2, "Studio Interiors", "INTERIOR_DESIGNER")));
        when(companyRepository.findByActiveTrueAndPublishedTrueAndDeletedFalse(any())).thenReturn(page);

        Page<CompanyResponse> result = service.publicList(null, pageable());

        assertThat(result.getContent()).hasSize(2);
        verify(companyRepository).findByActiveTrueAndPublishedTrueAndDeletedFalse(any());
        verify(companyRepository, never()).findByCompanyTypeAndActiveTrueAndPublishedTrueAndDeletedFalse(any(), any());
    }

    @Test
    void publicListTreatsBlankCompanyTypeSameAsNull() {
        when(companyRepository.findByActiveTrueAndPublishedTrueAndDeletedFalse(any())).thenReturn(Page.empty());

        service.publicList("   ", pageable());

        verify(companyRepository).findByActiveTrueAndPublishedTrueAndDeletedFalse(any());
        verify(companyRepository, never()).findByCompanyTypeAndActiveTrueAndPublishedTrueAndDeletedFalse(any(), any());
    }

    @Test
    void publicListPreservesPageMetadataFromTheRepositoryPage() {
        // PageImpl recomputes total down to offset+content.size() whenever
        // offset+pageSize would otherwise exceed the declared total, so the
        // fixture must stay internally consistent: offset=40 (page 2, size
        // 20) + pageSize 20 = 60, which must not exceed the declared total.
        Page<CompanyEntity> page = new PageImpl<>(List.of(entity(1, "A", "ARCHITECT")), PageRequest.of(2, 20), 62);
        when(companyRepository.findByActiveTrueAndPublishedTrueAndDeletedFalse(any())).thenReturn(page);

        Page<CompanyResponse> result = service.publicList(null, pageable());

        assertThat(result.getNumber()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(62);
        assertThat(result.getTotalPages()).isEqualTo(4);
    }

    @Test
    void publicListReturnsAnEmptyPageWithoutError() {
        when(companyRepository.findByActiveTrueAndPublishedTrueAndDeletedFalse(any())).thenReturn(Page.empty());

        Page<CompanyResponse> result = service.publicList(null, pageable());

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void publicListForAPageBeyondAvailableDataReturnsAnEmptyPageNotAnError() {
        Page<CompanyEntity> page = new PageImpl<>(List.of(), PageRequest.of(99, 20), 3);
        when(companyRepository.findByActiveTrueAndPublishedTrueAndDeletedFalse(any())).thenReturn(page);

        Page<CompanyResponse> result = service.publicList(null, pageable());

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void publicListPassesTheExactPageableThroughToTheRepositoryUnchanged() {
        when(companyRepository.findByActiveTrueAndPublishedTrueAndDeletedFalse(any())).thenReturn(Page.empty());
        Pageable requested = PageRequest.of(4, 20, Sort.by("priority").ascending().and(Sort.by("id").descending()));

        service.publicList(null, requested);

        verify(companyRepository).findByActiveTrueAndPublishedTrueAndDeletedFalse(eq(requested));
    }

    // ── Company type filtering ──────────────────────────────────────────────

    @Test
    void publicListFiltersByCompanyTypeWhenProvided() {
        Page<CompanyEntity> page = new PageImpl<>(List.of(entity(1, "Meridian Architects", "ARCHITECT")));
        when(companyRepository.findByCompanyTypeAndActiveTrueAndPublishedTrueAndDeletedFalse(eq("ARCHITECT"), any())).thenReturn(page);

        Page<CompanyResponse> result = service.publicList("ARCHITECT", pageable());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCompanyType()).isEqualTo("ARCHITECT");
        verify(companyRepository, never()).findByActiveTrueAndPublishedTrueAndDeletedFalse(any());
    }

    @Test
    void publicListSupportsEveryKnownCompanyTypeValue() {
        for (String type : new String[] {"ARCHITECT", "DESIGNER", "INTERIOR_DESIGNER", "ARCHITECT&DESIGNERS", "DESIGNERS"}) {
            reset(companyRepository);
            when(companyRepository.findByCompanyTypeAndActiveTrueAndPublishedTrueAndDeletedFalse(eq(type), any()))
                .thenReturn(new PageImpl<>(List.of(entity(1, "X", type))));

            Page<CompanyResponse> result = service.publicList(type, pageable());

            assertThat(result.getContent()).hasSize(1);
            verify(companyRepository).findByCompanyTypeAndActiveTrueAndPublishedTrueAndDeletedFalse(eq(type), any());
        }
    }

    @Test
    void publicListWithAnUnrecognizedCompanyTypeStringReturnsAnEmptyPageNotAnError() {
        // companyType has no backend enum/CHECK constraint (confirmed via
        // source, CompanyEntity.companyType is a plain VARCHAR(40)) - a
        // string matching no real row is simply an equality filter that
        // matches nothing, not an invalid-input error.
        when(companyRepository.findByCompanyTypeAndActiveTrueAndPublishedTrueAndDeletedFalse(eq("NOT_A_REAL_TYPE"), any()))
            .thenReturn(Page.empty());

        Page<CompanyResponse> result = service.publicList("NOT_A_REAL_TYPE", pageable());

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void companyTypeFilteringStillReturnsAPageObjectWithFullMetadata() {
        // See publicListPreservesPageMetadataFromTheRepositoryPage above for
        // why offset+pageSize must not exceed the declared total (page size
        // 1 here keeps offset+pageSize=1, safely under the declared 7).
        Page<CompanyEntity> page = new PageImpl<>(List.of(entity(1, "A", "DESIGNER")), PageRequest.of(0, 1), 7);
        when(companyRepository.findByCompanyTypeAndActiveTrueAndPublishedTrueAndDeletedFalse(eq("DESIGNER"), any())).thenReturn(page);

        Page<CompanyResponse> result = service.publicList("DESIGNER", pageable());

        assertThat(result.getTotalElements()).isEqualTo(7);
    }

    // ── Visibility ───────────────────────────────────────────────────────────

    @Test
    void publicListOnlyEverQueriesTheActivePublishedNonDeletedRepositoryMethod() {
        // Documents the trust boundary: the service never re-filters in
        // Java, it trusts findByActiveTrueAndPublishedTrueAndDeletedFalse's
        // own query-level predicate entirely - unpublished/inactive/deleted
        // exclusion is proven by the repository method name itself, not
        // re-implemented here.
        when(companyRepository.findByActiveTrueAndPublishedTrueAndDeletedFalse(any())).thenReturn(Page.empty());

        service.publicList(null, pageable());

        verify(companyRepository, times(1)).findByActiveTrueAndPublishedTrueAndDeletedFalse(any());
        verifyNoMoreInteractions(companyRepository);
    }

    @Test
    void publicGetOnlyEverQueriesTheActivePublishedNonDeletedRepositoryMethod() {
        when(companyRepository.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(1L))
            .thenReturn(Optional.of(entity(1, "A", "ARCHITECT")));

        service.publicGet(1L);

        verify(companyRepository, times(1)).findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(1L);
        verifyNoMoreInteractions(companyRepository);
    }

    // ── Detail: 404 cases ────────────────────────────────────────────────────

    @Test
    void publicGetThrows404ForAnUnknownCompanyId() {
        when(companyRepository.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publicGet(999L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void publicGetThrows404ForAnInactiveCompany_becauseTheQueryPredicateExcludesIt() {
        // findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse excludes an
        // inactive row at the query level - simulated here by the mock
        // simply returning empty, matching what the real predicate would do.
        when(companyRepository.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publicGet(2L)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void publicGetThrows404ForAnUnpublishedCompany() {
        when(companyRepository.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publicGet(3L)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void publicGetThrows404ForADeletedCompany() {
        when(companyRepository.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publicGet(4L)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void publicGetErrorMessageIsSafeAndGeneric() {
        when(companyRepository.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publicGet(5L))
            .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(ResponseStatusException.class))
            .satisfies(ex -> assertThat(ex.getReason()).isEqualTo("Company not found"));
    }

    // ── Detail: success + DTO safety ────────────────────────────────────────

    @Test
    void publicGetReturnsTheMappedResponseForAnEligibleCompany() {
        CompanyEntity full = CompanyEntity.builder()
            .id(701L).name("Meridian Architects").slug("meridian-architects").companyType("ARCHITECT")
            .logoUrl("https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/companies/meridian/logo.png")
            .coverImageUrl("https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/companies/meridian/cover.png")
            .description("Award-winning architecture studio.")
            .phone("+919876543210").whatsapp("+919876543210")
            .email("internal-login@meridian.example").specializationText("Residential").addressLine("MG Road")
            .active(true).published(true).deleted(false).priority(1)
            .build();
        when(companyRepository.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(701L)).thenReturn(Optional.of(full));

        CompanyResponse response = service.publicGet(701L);

        assertThat(response.getId()).isEqualTo(701L);
        assertThat(response.getName()).isEqualTo("Meridian Architects");
        assertThat(response.getSlug()).isEqualTo("meridian-architects");
        assertThat(response.getCompanyType()).isEqualTo("ARCHITECT");
        assertThat(response.getLogoUrl()).isEqualTo(full.getLogoUrl());
        assertThat(response.getCoverImageUrl()).isEqualTo(full.getCoverImageUrl());
        assertThat(response.getDescription()).isEqualTo("Award-winning architecture studio.");
        assertThat(response.getPhone()).isEqualTo("+919876543210");
        assertThat(response.getWhatsapp()).isEqualTo("+919876543210");
    }

    @Test
    void publicGetNeverLeaksInternalOrAdminOnlyEntityFieldsIntoTheResponse() {
        CompanyEntity full = CompanyEntity.builder()
            .id(1L).name("A").slug("a").companyType("ARCHITECT")
            .email("secret@internal.example").specializationText("internal specialization text")
            .addressLine("internal address").servicesOffered("internal services list")
            .infoLine1("internal info 1").infoLine2("internal info 2")
            .priority(5).active(true).published(true).deleted(false)
            .build();
        when(companyRepository.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(1L)).thenReturn(Optional.of(full));

        CompanyResponse response = service.publicGet(1L);

        // CompanyResponse has exactly 9 fields (id/name/slug/companyType/
        // logoUrl/coverImageUrl/description/phone/whatsapp) - none of
        // email/specializationText/addressLine/servicesOffered/infoLine1/
        // infoLine2/priority/active/published/deleted exist on it at all,
        // so there is nothing further to assert null on; this test instead
        // locks in the exact field allowlist via reflection.
        java.util.Set<String> fieldNames = java.util.Arrays.stream(response.getClass().getDeclaredFields())
            .map(java.lang.reflect.Field::getName)
            .collect(java.util.stream.Collectors.toSet());
        assertThat(fieldNames).containsExactlyInAnyOrder(
            "id", "name", "slug", "companyType", "logoUrl", "coverImageUrl", "description", "phone", "whatsapp"
        );
    }

    @Test
    void publicGetHandlesNullOptionalFieldsSafely() {
        CompanyEntity minimal = CompanyEntity.builder()
            .id(1L).name("Bare Minimum Co").slug("bare-minimum-co").companyType("ARCHITECT")
            .logoUrl(null).coverImageUrl(null).description(null).phone(null).whatsapp(null)
            .active(true).published(true).deleted(false)
            .build();
        when(companyRepository.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(1L)).thenReturn(Optional.of(minimal));

        CompanyResponse response = service.publicGet(1L);

        assertThat(response.getLogoUrl()).isNull();
        assertThat(response.getCoverImageUrl()).isNull();
        assertThat(response.getDescription()).isNull();
        assertThat(response.getPhone()).isNull();
        assertThat(response.getWhatsapp()).isNull();
    }

    // ── Contact/media mapping ────────────────────────────────────────────────

    @Test
    void phoneMapsOnlyFromTheEntityPhoneField() {
        CompanyEntity e = entity(1, "A", "ARCHITECT");
        e.setPhone("080-4123-4567");
        e.setWhatsapp(null);
        when(companyRepository.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(1L)).thenReturn(Optional.of(e));

        CompanyResponse response = service.publicGet(1L);

        assertThat(response.getPhone()).isEqualTo("080-4123-4567");
        assertThat(response.getWhatsapp()).isNull();
    }

    @Test
    void whatsappMapsOnlyFromTheEntityWhatsappField() {
        CompanyEntity e = entity(1, "A", "ARCHITECT");
        e.setPhone(null);
        e.setWhatsapp("+919812345678");
        when(companyRepository.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(1L)).thenReturn(Optional.of(e));

        CompanyResponse response = service.publicGet(1L);

        assertThat(response.getPhone()).isNull();
        assertThat(response.getWhatsapp()).isEqualTo("+919812345678");
    }

    @Test
    void logoUrlMapsVerbatimWhenPresentAndNullWhenAbsent() {
        CompanyEntity withLogo = entity(1, "A", "ARCHITECT");
        withLogo.setLogoUrl("https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/companies/a/logo.png");
        when(companyRepository.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(1L)).thenReturn(Optional.of(withLogo));
        assertThat(service.publicGet(1L).getLogoUrl()).isEqualTo("https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/companies/a/logo.png");

        CompanyEntity withoutLogo = entity(2, "B", "ARCHITECT");
        when(companyRepository.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(2L)).thenReturn(Optional.of(withoutLogo));
        assertThat(service.publicGet(2L).getLogoUrl()).isNull();
    }

    // ── Slug lookup (Phase 7B-G): eligible lookup ───────────────────────────

    @Test
    void publicGetBySlug_returnsTheMappedResponseForAnEligibleCompany() {
        when(companyRepository.findBySlugAndActiveTrueAndPublishedTrueAndDeletedFalse("meridian-architects"))
            .thenReturn(Optional.of(eligibleCompanyBySlug()));

        CompanyResponse response = service.publicGetBySlug("meridian-architects");

        assertThat(response.getId()).isEqualTo(701L);
        assertThat(response.getSlug()).isEqualTo("meridian-architects");
        assertThat(response.getName()).isEqualTo("Meridian Architects");
    }

    @Test
    void publicGetBySlug_onlyEverQueriesTheActivePublishedNonDeletedRepositoryMethod() {
        when(companyRepository.findBySlugAndActiveTrueAndPublishedTrueAndDeletedFalse("meridian-architects"))
            .thenReturn(Optional.of(eligibleCompanyBySlug()));

        service.publicGetBySlug("meridian-architects");

        verify(companyRepository, times(1)).findBySlugAndActiveTrueAndPublishedTrueAndDeletedFalse("meridian-architects");
        verifyNoMoreInteractions(companyRepository);
    }

    @Test
    void publicGetBySlug_returnsTheSameNineFieldDtoShapeAsTheNumericLookup() {
        when(companyRepository.findBySlugAndActiveTrueAndPublishedTrueAndDeletedFalse("meridian-architects"))
            .thenReturn(Optional.of(eligibleCompanyBySlug()));

        CompanyResponse response = service.publicGetBySlug("meridian-architects");

        java.util.Set<String> fieldNames = java.util.Arrays.stream(response.getClass().getDeclaredFields())
            .map(java.lang.reflect.Field::getName)
            .collect(java.util.stream.Collectors.toSet());
        assertThat(fieldNames).containsExactlyInAnyOrder(
            "id", "name", "slug", "companyType", "logoUrl", "coverImageUrl", "description", "phone", "whatsapp"
        );
    }

    // ── Slug lookup: 404 cases ───────────────────────────────────────────────

    @Test
    void publicGetBySlug_throws404ForAnUnknownSlug() {
        when(companyRepository.findBySlugAndActiveTrueAndPublishedTrueAndDeletedFalse("unknown-slug"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publicGetBySlug("unknown-slug"))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void publicGetBySlug_throws404ForAWrongCaseSlug_repositoryLookupIsCaseSensitiveExactMatch() {
        // findBySlugAndActiveTrueAndPublishedTrueAndDeletedFalse is a plain derived-query exact
        // match (no lower()/ilike) - a differently-cased variant of a real slug simply does not
        // match any row, exactly like an unknown slug. Simulated here the same way a real
        // case-sensitive column comparison would behave: no matching row for "Meridian-Architects".
        when(companyRepository.findBySlugAndActiveTrueAndPublishedTrueAndDeletedFalse("Meridian-Architects"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publicGetBySlug("Meridian-Architects"))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void publicGetBySlug_throws404ForAnInactiveCompany() {
        when(companyRepository.findBySlugAndActiveTrueAndPublishedTrueAndDeletedFalse("inactive-co"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publicGetBySlug("inactive-co")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void publicGetBySlug_throws404ForAnUnpublishedCompany() {
        when(companyRepository.findBySlugAndActiveTrueAndPublishedTrueAndDeletedFalse("unpublished-co"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publicGetBySlug("unpublished-co")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void publicGetBySlug_throws404ForADeletedCompany() {
        when(companyRepository.findBySlugAndActiveTrueAndPublishedTrueAndDeletedFalse("deleted-co"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publicGetBySlug("deleted-co")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void publicGetBySlug_errorMessageIsSafeAndGeneric() {
        when(companyRepository.findBySlugAndActiveTrueAndPublishedTrueAndDeletedFalse("unknown-slug"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publicGetBySlug("unknown-slug"))
            .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(ResponseStatusException.class))
            .satisfies(ex -> assertThat(ex.getReason()).isEqualTo("Company not found"));
    }

    // ── Slug lookup: safe field mapping / no cross-entity queries ───────────

    @Test
    void publicGetBySlug_handlesNullOptionalFieldsSafely() {
        CompanyEntity minimal = CompanyEntity.builder()
            .id(1L).name("Bare Minimum Co").slug("bare-minimum-co").companyType("ARCHITECT")
            .logoUrl(null).coverImageUrl(null).description(null).phone(null).whatsapp(null)
            .active(true).published(true).everPublished(true).deleted(false)
            .build();
        when(companyRepository.findBySlugAndActiveTrueAndPublishedTrueAndDeletedFalse("bare-minimum-co"))
            .thenReturn(Optional.of(minimal));

        CompanyResponse response = service.publicGetBySlug("bare-minimum-co");

        assertThat(response.getLogoUrl()).isNull();
        assertThat(response.getCoverImageUrl()).isNull();
        assertThat(response.getDescription()).isNull();
        assertThat(response.getPhone()).isNull();
        assertThat(response.getWhatsapp()).isNull();
    }
}
