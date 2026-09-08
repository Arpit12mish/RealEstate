package com.brandPitara.sfs.company.controller.publicapi;

import com.brandPitara.sfs.company.dto.ArchitectDesignerDetailResponse;
import com.brandPitara.sfs.company.dto.ArchitectDesignerListItemResponse;
import com.brandPitara.sfs.company.enums.ArchitectDesignerType;
import com.brandPitara.sfs.company.service.ArchitectDesignerPublicService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 8A-G (GAP-037 list, GAP-003B slug detail): controller-layer coverage
 * for the two new routes, plus a non-collision check against the existing
 * numeric detail route. Standalone MockMvc against the real
 * GlobalExceptionHandler, matching CompanyPublicControllerTest/
 * CompanyPublicSlugControllerTest's own established convention exactly (this
 * Spring Boot 4.0.0 dependency tree ships no @WebMvcTest/@MockBean web
 * slice).
 */
class ArchitectDesignerPublicControllerTest {

  private ArchitectDesignerPublicService architectDesignerPublicService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    architectDesignerPublicService = mock(ArchitectDesignerPublicService.class);
    mockMvc = MockMvcBuilders
        .standaloneSetup(new ArchitectDesignerPublicController(architectDesignerPublicService))
        .setControllerAdvice(new GlobalExceptionHandler(new LogSanitizer()))
        .build();
  }

  private ArchitectDesignerDetailResponse detailSample() {
    return ArchitectDesignerDetailResponse.builder()
        .companyId(701L).name("Morphogenesis").slug("morphogenesis").companyType("ARCHITECT&DESIGNERS")
        .logoUrl("https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/companies/morphogenesis/logo.png")
        .description("Award-winning architecture studio.")
        .topProjects(List.of()).stats(List.of()).awardsAndPublications(List.of())
        .certificates(List.of()).connectedBrands(List.of())
        .build();
  }

  private ArchitectDesignerListItemResponse listItemSample() {
    return ArchitectDesignerListItemResponse.builder()
        .companyId(701L).slug("morphogenesis").name("Morphogenesis").type("ARCHITECT")
        .logoUrl("https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/companies/morphogenesis/logo.png")
        .build();
  }

  // page() mirrors CompanyPublicControllerTest's own helper - PageImpl's
  // convenience constructors default to Pageable.unpaged(), whose
  // getOffset() throws UnsupportedOperationException that Jackson trips
  // over during serialization, so every mocked page here carries an
  // explicit Pageable too.
  private <T> PageImpl<T> page(List<T> content) {
    return new PageImpl<>(content, PageRequest.of(0, 20), content.size());
  }

  // ── Unauthenticated access ────────────────────────────────────────────────

  @Test
  void numericDetailIsReachableWithoutAnyAuthenticationHeaderOrPrincipal() throws Exception {
    when(architectDesignerPublicService.getDetail(701L)).thenReturn(detailSample());

    mockMvc.perform(get("/api/public/architect-designers/701")).andExpect(status().isOk());
  }

  @Test
  void listIsReachableWithoutAnyAuthenticationHeaderOrPrincipal() throws Exception {
    when(architectDesignerPublicService.list(eq(ArchitectDesignerType.ARCHITECT), any()))
        .thenReturn(page(List.of(listItemSample())));

    mockMvc.perform(get("/api/public/architect-designers").param("type", "ARCHITECT"))
        .andExpect(status().isOk());
  }

  @Test
  void slugDetailIsReachableWithoutAnyAuthenticationHeaderOrPrincipal() throws Exception {
    when(architectDesignerPublicService.getDetailBySlug("morphogenesis", ArchitectDesignerType.ARCHITECT))
        .thenReturn(detailSample());

    mockMvc.perform(get("/api/public/architect-designers/slug/morphogenesis").param("type", "ARCHITECT"))
        .andExpect(status().isOk());
  }

  // ── Listing: type parameter handling ─────────────────────────────────────

  @Test
  void list_missingTypeReturnsASafe400_neverReachesTheService() throws Exception {
    mockMvc.perform(get("/api/public/architect-designers"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("ARCHITECT")))
        .andExpect(jsonPath("$.stackTrace").doesNotExist());

    verifyNoInteractions(architectDesignerPublicService);
  }

  @Test
  void list_invalidTypeReturnsASafe400_neverReachesTheService() throws Exception {
    mockMvc.perform(get("/api/public/architect-designers").param("type", "BUILDER"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.stackTrace").doesNotExist());

    verifyNoInteractions(architectDesignerPublicService);
  }

  @Test
  void list_lowercaseTypeIsAccepted() throws Exception {
    when(architectDesignerPublicService.list(eq(ArchitectDesignerType.ARCHITECT), any()))
        .thenReturn(page(List.of()));

    mockMvc.perform(get("/api/public/architect-designers").param("type", "architect"))
        .andExpect(status().isOk());

    verify(architectDesignerPublicService).list(eq(ArchitectDesignerType.ARCHITECT), any());
  }

  @Test
  void list_interiorDesignerTypeResolvesToTheCorrectNormalizedType() throws Exception {
    when(architectDesignerPublicService.list(eq(ArchitectDesignerType.INTERIOR_DESIGNER), any()))
        .thenReturn(page(List.of()));

    mockMvc.perform(get("/api/public/architect-designers").param("type", "INTERIOR_DESIGNER"))
        .andExpect(status().isOk());

    verify(architectDesignerPublicService).list(eq(ArchitectDesignerType.INTERIOR_DESIGNER), any());
  }

  @Test
  void list_rejectsARawStorageValueAsType() throws Exception {
    // "ARCHITECT&DESIGNERS" is a real storage value but not a public
    // normalized type - callers must use ARCHITECT/INTERIOR_DESIGNER.
    mockMvc.perform(get("/api/public/architect-designers").param("type", "ARCHITECT&DESIGNERS"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(architectDesignerPublicService);
  }

  // ── Listing: pagination (controller-owned logic) ─────────────────────────

  @Test
  void list_withNoPageParamsRequestsPageZeroSizeTwentyFromTheService() throws Exception {
    when(architectDesignerPublicService.list(eq(ArchitectDesignerType.ARCHITECT), any()))
        .thenReturn(page(List.of()));

    mockMvc.perform(get("/api/public/architect-designers").param("type", "ARCHITECT"))
        .andExpect(status().isOk());

    verify(architectDesignerPublicService).list(eq(ArchitectDesignerType.ARCHITECT), argThat(p ->
        p.getPageNumber() == 0 && p.getPageSize() == 20));
  }

  @Test
  void list_capsAnOversizedSizeParamAtTwenty() throws Exception {
    when(architectDesignerPublicService.list(eq(ArchitectDesignerType.ARCHITECT), any()))
        .thenReturn(page(List.of()));

    mockMvc.perform(get("/api/public/architect-designers").param("type", "ARCHITECT").param("size", "500"))
        .andExpect(status().isOk());

    verify(architectDesignerPublicService).list(eq(ArchitectDesignerType.ARCHITECT), argThat(p -> p.getPageSize() == 20));
  }

  @Test
  void list_honorsAnExplicitInBoundsSizeParam() throws Exception {
    when(architectDesignerPublicService.list(eq(ArchitectDesignerType.ARCHITECT), any()))
        .thenReturn(page(List.of()));

    mockMvc.perform(get("/api/public/architect-designers").param("type", "ARCHITECT").param("size", "5"))
        .andExpect(status().isOk());

    verify(architectDesignerPublicService).list(eq(ArchitectDesignerType.ARCHITECT), argThat(p -> p.getPageSize() == 5));
  }

  @Test
  void list_honorsAnExplicitPageParam() throws Exception {
    when(architectDesignerPublicService.list(eq(ArchitectDesignerType.ARCHITECT), any()))
        .thenReturn(page(List.of()));

    mockMvc.perform(get("/api/public/architect-designers").param("type", "ARCHITECT").param("page", "2"))
        .andExpect(status().isOk());

    verify(architectDesignerPublicService).list(eq(ArchitectDesignerType.ARCHITECT), argThat(p -> p.getPageNumber() == 2));
  }

  @Test
  void list_sortsByPriorityAscendingThenIdDescending() throws Exception {
    when(architectDesignerPublicService.list(eq(ArchitectDesignerType.ARCHITECT), any()))
        .thenReturn(page(List.of()));

    mockMvc.perform(get("/api/public/architect-designers").param("type", "ARCHITECT"))
        .andExpect(status().isOk());

    verify(architectDesignerPublicService).list(eq(ArchitectDesignerType.ARCHITECT), argThat(p -> {
      var orders = p.getSort().toList();
      return orders.size() == 2
          && orders.get(0).getProperty().equals("priority") && orders.get(0).isAscending()
          && orders.get(1).getProperty().equals("id") && orders.get(1).isDescending();
    }));
  }

  // ── Listing: response envelope shape ─────────────────────────────────────

  @Test
  void list_returnsThePageEnvelopeWithContentAndMetadata() throws Exception {
    when(architectDesignerPublicService.list(eq(ArchitectDesignerType.ARCHITECT), any()))
        .thenReturn(new PageImpl<>(List.of(listItemSample()), PageRequest.of(0, 20), 1));

    mockMvc.perform(get("/api/public/architect-designers").param("type", "ARCHITECT"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].companyId", is(701)))
        .andExpect(jsonPath("$.content[0].slug", is("morphogenesis")))
        .andExpect(jsonPath("$.content[0].type", is("ARCHITECT")))
        .andExpect(jsonPath("$.content[0].phone").doesNotExist())
        .andExpect(jsonPath("$.content[0].whatsapp").doesNotExist())
        .andExpect(jsonPath("$.content[0].email").doesNotExist())
        .andExpect(jsonPath("$.totalElements", is(1)));
  }

  // ── Numeric detail (existing route, unchanged) ───────────────────────────

  @Test
  void numericDetail_returnsTheDetailResponseDirectly() throws Exception {
    when(architectDesignerPublicService.getDetail(701L)).thenReturn(detailSample());

    mockMvc.perform(get("/api/public/architect-designers/701"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.companyId", is(701)))
        .andExpect(jsonPath("$.slug", is("morphogenesis")));
  }

  @Test
  void numericDetail_unknownIdReturnsASafeApiErrorEnvelopeWithNoStackTrace() throws Exception {
    when(architectDesignerPublicService.getDetail(999L))
        .thenThrow(new ResponseStatusException(NOT_FOUND, "Architect/Designer not found"));

    mockMvc.perform(get("/api/public/architect-designers/999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status", is(404)))
        .andExpect(jsonPath("$.message", is("Architect/Designer not found")))
        .andExpect(jsonPath("$.stackTrace").doesNotExist());
  }

  // ── Slug detail: type parameter handling ─────────────────────────────────

  @Test
  void slugDetail_missingTypeReturnsASafe400_neverReachesTheService() throws Exception {
    mockMvc.perform(get("/api/public/architect-designers/slug/morphogenesis"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.stackTrace").doesNotExist());

    verifyNoInteractions(architectDesignerPublicService);
  }

  @Test
  void slugDetail_invalidTypeReturnsASafe400_neverReachesTheService() throws Exception {
    mockMvc.perform(get("/api/public/architect-designers/slug/morphogenesis").param("type", "NOT_A_TYPE"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.stackTrace").doesNotExist());

    verifyNoInteractions(architectDesignerPublicService);
  }

  @Test
  void slugDetail_passesSlugAndTypeThroughVerbatim() throws Exception {
    when(architectDesignerPublicService.getDetailBySlug("morphogenesis", ArchitectDesignerType.ARCHITECT))
        .thenReturn(detailSample());

    mockMvc.perform(get("/api/public/architect-designers/slug/morphogenesis").param("type", "ARCHITECT"))
        .andExpect(status().isOk());

    verify(architectDesignerPublicService).getDetailBySlug("morphogenesis", ArchitectDesignerType.ARCHITECT);
  }

  @Test
  void slugDetail_returnsTheRichDetailResponseDirectly() throws Exception {
    when(architectDesignerPublicService.getDetailBySlug("morphogenesis", ArchitectDesignerType.ARCHITECT))
        .thenReturn(detailSample());

    mockMvc.perform(get("/api/public/architect-designers/slug/morphogenesis").param("type", "ARCHITECT"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.companyId", is(701)))
        .andExpect(jsonPath("$.slug", is("morphogenesis")))
        .andExpect(jsonPath("$.name", is("Morphogenesis")))
        .andExpect(jsonPath("$.phone").doesNotExist())
        .andExpect(jsonPath("$.whatsapp").doesNotExist())
        .andExpect(jsonPath("$.email").doesNotExist());
  }

  @Test
  void slugDetail_unknownSlugReturnsASafeApiErrorEnvelopeWithNoStackTrace() throws Exception {
    when(architectDesignerPublicService.getDetailBySlug("unknown-studio", ArchitectDesignerType.ARCHITECT))
        .thenThrow(new ResponseStatusException(NOT_FOUND, "Architect/Designer not found"));

    mockMvc.perform(get("/api/public/architect-designers/slug/unknown-studio").param("type", "ARCHITECT"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status", is(404)))
        .andExpect(jsonPath("$.message", is("Architect/Designer not found")))
        .andExpect(jsonPath("$.stackTrace").doesNotExist());
  }

  @Test
  void slugDetail_typeMismatchPropagatesTheServiceLayersNotFound() throws Exception {
    // A real Interior Designer slug requested with type=ARCHITECT - the
    // service (backed by a query scoped to ARCHITECT's storage values)
    // reports not-found exactly like an unknown slug; the controller must
    // not mask or alter that.
    when(architectDesignerPublicService.getDetailBySlug("interior-studio", ArchitectDesignerType.ARCHITECT))
        .thenThrow(new ResponseStatusException(NOT_FOUND, "Architect/Designer not found"));

    mockMvc.perform(get("/api/public/architect-designers/slug/interior-studio").param("type", "ARCHITECT"))
        .andExpect(status().isNotFound());
  }

  // These three tests prove the path regex itself rejects the input (the route never
  // matches, the service is never called) - same reasoning and same known test-harness
  // artifact documented in CompanyPublicSlugControllerTest (standalone MockMvc surfaces
  // an unmatched route as a generic 500 here; the real deployed app's DispatcherServlet
  // maps it to a clean 404 via GlobalExceptionHandler.handleNoResourceFound()). What both
  // scenarios agree on, and what this test actually verifies: the service is never
  // reached, and the response stays safe (no stack trace).
  @Test
  void slugDetail_purelyNumericSlugDoesNotMatchThisRoute_neverReachesTheService() throws Exception {
    mockMvc.perform(get("/api/public/architect-designers/slug/12345").param("type", "ARCHITECT"))
        .andExpect(jsonPath("$.stackTrace").doesNotExist());

    verifyNoInteractions(architectDesignerPublicService);
  }

  @Test
  void slugDetail_uppercaseSlugDoesNotMatchThisRoute_neverReachesTheService() throws Exception {
    mockMvc.perform(get("/api/public/architect-designers/slug/Morphogenesis").param("type", "ARCHITECT"))
        .andExpect(jsonPath("$.stackTrace").doesNotExist());

    verifyNoInteractions(architectDesignerPublicService);
  }

  @Test
  void slugDetail_malformedSlugWithOnlySymbolsDoesNotMatchThisRoute_neverReachesTheService() throws Exception {
    mockMvc.perform(get("/api/public/architect-designers/slug/---").param("type", "ARCHITECT"))
        .andExpect(jsonPath("$.stackTrace").doesNotExist());

    verifyNoInteractions(architectDesignerPublicService);
  }

  @Test
  void slugDetail_doesNotCollideWithTheNumericIdRoute() throws Exception {
    when(architectDesignerPublicService.getDetail(701L)).thenReturn(detailSample());
    when(architectDesignerPublicService.getDetailBySlug("morphogenesis", ArchitectDesignerType.ARCHITECT))
        .thenReturn(detailSample());

    mockMvc.perform(get("/api/public/architect-designers/701")).andExpect(status().isOk());
    mockMvc.perform(get("/api/public/architect-designers/slug/morphogenesis").param("type", "ARCHITECT"))
        .andExpect(status().isOk());

    verify(architectDesignerPublicService).getDetail(701L);
    verify(architectDesignerPublicService).getDetailBySlug("morphogenesis", ArchitectDesignerType.ARCHITECT);
  }

  @Test
  void slugDetail_doesNotCollideWithTheListRoute() throws Exception {
    when(architectDesignerPublicService.list(eq(ArchitectDesignerType.ARCHITECT), any()))
        .thenReturn(page(List.of()));
    when(architectDesignerPublicService.getDetailBySlug("morphogenesis", ArchitectDesignerType.ARCHITECT))
        .thenReturn(detailSample());

    mockMvc.perform(get("/api/public/architect-designers").param("type", "ARCHITECT")).andExpect(status().isOk());
    mockMvc.perform(get("/api/public/architect-designers/slug/morphogenesis").param("type", "ARCHITECT"))
        .andExpect(status().isOk());

    verify(architectDesignerPublicService).list(eq(ArchitectDesignerType.ARCHITECT), any());
    verify(architectDesignerPublicService).getDetailBySlug("morphogenesis", ArchitectDesignerType.ARCHITECT);
  }
}
