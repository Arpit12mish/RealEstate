package com.brandPitara.sfs.company.controller.publicapi;

import com.brandPitara.sfs.company.dto.CompanyResponse;
import com.brandPitara.sfs.company.service.CompanyPublicService;
import com.brandPitara.sfs.exception.GlobalExceptionHandler;
import com.brandPitara.sfs.observability.LogSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller/error-envelope layer for GAP-034. Uses the same standaloneSetup
 * + GlobalExceptionHandler pattern established in Phase 6C-G for Builder
 * Credibility (Spring Boot 4.0.0's resolved dependency tree ships no
 * @WebMvcTest/@MockBean web slice here - confirmed absent from
 * spring-boot-test-autoconfigure-4.0.0.jar - so plain spring-test MockMvc
 * against a manually-advised standalone context is used instead).
 */
class CompanyPublicControllerTest {

    private CompanyPublicService companyPublicService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        companyPublicService = mock(CompanyPublicService.class);
        mockMvc = MockMvcBuilders
            .standaloneSetup(new CompanyPublicController(companyPublicService))
            .setControllerAdvice(new GlobalExceptionHandler(new LogSanitizer()))
            .build();
    }

    private CompanyResponse sample() {
        return CompanyResponse.builder()
            .id(701L).name("Meridian Architects").slug("meridian-architects").companyType("ARCHITECT")
            .logoUrl("https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/companies/meridian/logo.png")
            .description("Award-winning architecture studio.")
            .phone("+919876543210").whatsapp("+919876543210")
            .build();
    }

    /**
     * PageImpl's single/two-arg convenience constructors default to
     * Pageable.unpaged(), whose getOffset() throws
     * UnsupportedOperationException - Jackson trips over that while
     * serializing the response body. The real controller never hits this
     * (its Pageable always comes from a real PageRequest), so every mock
     * response here must carry an explicit Pageable too.
     */
    private <T> PageImpl<T> page(List<T> content) {
        return new PageImpl<>(content, PageRequest.of(0, 10), content.size());
    }

    // ── Unauthenticated access ──────────────────────────────────────────────

    @Test
    void listIsReachableWithoutAnyAuthenticationHeaderOrPrincipal() throws Exception {
        when(companyPublicService.publicList(isNull(), any())).thenReturn(page(List.of(sample())));

        mockMvc.perform(get("/api/public/companies"))
            .andExpect(status().isOk());
    }

    @Test
    void detailIsReachableWithoutAnyAuthenticationHeaderOrPrincipal() throws Exception {
        when(companyPublicService.publicGet(701L)).thenReturn(sample());

        mockMvc.perform(get("/api/public/companies/701"))
            .andExpect(status().isOk());
    }

    // ── Default paging / size capping (controller-owned logic) ─────────────

    @Test
    void listWithNoParamsRequestsPageZeroSizeTenFromTheService() throws Exception {
        when(companyPublicService.publicList(isNull(), any())).thenReturn(page(List.of()));

        mockMvc.perform(get("/api/public/companies")).andExpect(status().isOk());

        verify(companyPublicService).publicList(isNull(), argThat(p ->
            p.getPageNumber() == 0 && p.getPageSize() == 10));
    }

    @Test
    void listCapsAnOversizedSizeParamAtTwenty() throws Exception {
        when(companyPublicService.publicList(isNull(), any())).thenReturn(page(List.of()));

        mockMvc.perform(get("/api/public/companies").param("size", "500")).andExpect(status().isOk());

        verify(companyPublicService).publicList(isNull(), argThat(p -> p.getPageSize() == 20));
    }

    @Test
    void listHonorsAnExplicitInBoundsSizeParam() throws Exception {
        when(companyPublicService.publicList(isNull(), any())).thenReturn(page(List.of()));

        mockMvc.perform(get("/api/public/companies").param("size", "5")).andExpect(status().isOk());

        verify(companyPublicService).publicList(isNull(), argThat(p -> p.getPageSize() == 5));
    }

    @Test
    void listPassesTheCompanyTypeParamThroughVerbatim() throws Exception {
        when(companyPublicService.publicList(eq("ARCHITECT"), any())).thenReturn(page(List.of()));

        mockMvc.perform(get("/api/public/companies").param("companyType", "ARCHITECT")).andExpect(status().isOk());

        verify(companyPublicService).publicList(eq("ARCHITECT"), any());
    }

    @Test
    void listSortsByPriorityAscendingThenIdDescending() throws Exception {
        when(companyPublicService.publicList(isNull(), any())).thenReturn(page(List.of()));

        mockMvc.perform(get("/api/public/companies")).andExpect(status().isOk());

        verify(companyPublicService).publicList(isNull(), argThat(p -> {
            var orders = p.getSort().toList();
            return orders.size() == 2
                && orders.get(0).getProperty().equals("priority") && orders.get(0).isAscending()
                && orders.get(1).getProperty().equals("id") && orders.get(1).isDescending();
        }));
    }

    // ── Response envelope shape ──────────────────────────────────────────────

    @Test
    void listReturnsThePageEnvelopeWithContentAndMetadata() throws Exception {
        when(companyPublicService.publicList(isNull(), any()))
            .thenReturn(new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/public/companies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].id", is(701)))
            .andExpect(jsonPath("$.content[0].name", is("Meridian Architects")))
            .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    void detailReturnsTheCompanyResponseDirectly() throws Exception {
        when(companyPublicService.publicGet(701L)).thenReturn(sample());

        mockMvc.perform(get("/api/public/companies/701"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(701)))
            .andExpect(jsonPath("$.slug", is("meridian-architects")))
            .andExpect(jsonPath("$.companyType", is("ARCHITECT")));
    }

    // ── Error behavior ────────────────────────────────────────────────────────

    @Test
    void unknownCompanyIdReturnsASafeApiErrorEnvelopeWithNoStackTrace() throws Exception {
        when(companyPublicService.publicGet(999L))
            .thenThrow(new ResponseStatusException(NOT_FOUND, "Company not found"));

        mockMvc.perform(get("/api/public/companies/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status", is(404)))
            .andExpect(jsonPath("$.message", is("Company not found")))
            .andExpect(jsonPath("$.error", is("Not Found")))
            .andExpect(jsonPath("$.path", is("/api/public/companies/999")))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.stackTrace").doesNotExist())
            .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void emptyListingResultReturnsAnEmptyContentArrayNotAnError() throws Exception {
        when(companyPublicService.publicList(isNull(), any())).thenReturn(page(List.of()));

        mockMvc.perform(get("/api/public/companies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)));
    }

    /**
     * GAP-032 (existing, documented) reconfirmed against a third call site.
     * A non-numeric {companyId} path segment fails Spring's own
     * @PathVariable Long conversion (MethodArgumentTypeMismatchException)
     * before the controller method body ever runs; GlobalExceptionHandler
     * has no specific handler for that exception type, so it falls through
     * to the generic catch-all and returns 500 instead of 400. This
     * reconfirms the same shared-GlobalExceptionHandler root cause already
     * recorded for /api/builders/{builderId}/credibility (Phase 6D) and
     * /api/public/companies's page/size params (Phase 7A) - not a new gap.
     */
    @Test
    void nonNumericCompanyIdProducesAGeneric500NotA400_reconfirmsExistingGap032() throws Exception {
        mockMvc.perform(get("/api/public/companies/not-a-number"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status", is(500)))
            .andExpect(jsonPath("$.message", is("Internal server error")))
            .andExpect(jsonPath("$.stackTrace").doesNotExist());

        verifyNoInteractions(companyPublicService);
    }

    @Test
    void requestIdIsPresentOnTheErrorEnvelopeWhenSupplied() throws Exception {
        when(companyPublicService.publicGet(999L))
            .thenThrow(new ResponseStatusException(NOT_FOUND, "Company not found"));

        mockMvc.perform(get("/api/public/companies/999").header("X-Request-Id", "test-req-abc123"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.requestId", is("test-req-abc123")));
    }
}
