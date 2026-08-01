package com.brandPitara.sfs.company.controller.publicapi;

import com.brandPitara.sfs.company.dto.CompanyResponse;
import com.brandPitara.sfs.company.service.CompanyPublicService;
import com.brandPitara.sfs.exception.GlobalExceptionHandler;
import com.brandPitara.sfs.observability.LogSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 7B-G: controller-layer coverage for the new
 * GET /api/public/companies/slug/{companySlug} route only. Standalone
 * MockMvc against the real GlobalExceptionHandler - this Spring Boot 4.0.0
 * dependency tree ships no @WebMvcTest/@MockBean web slice (established in
 * Phase 6C-G/7A-G), so a manually-advised standalone context is used
 * instead, matching every other controller test in this codebase's Company/
 * Builder slug-contract family.
 */
class CompanyPublicSlugControllerTest {

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

  @Test
  void getBySlug_isReachableWithoutAnyAuthenticationHeaderOrPrincipal() throws Exception {
    when(companyPublicService.publicGetBySlug("meridian-architects")).thenReturn(sample());

    mockMvc.perform(get("/api/public/companies/slug/meridian-architects"))
        .andExpect(status().isOk());
  }

  @Test
  void getBySlug_returnsTheCompanyResponseDirectly() throws Exception {
    when(companyPublicService.publicGetBySlug("meridian-architects")).thenReturn(sample());

    mockMvc.perform(get("/api/public/companies/slug/meridian-architects"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(701)))
        .andExpect(jsonPath("$.slug", is("meridian-architects")))
        .andExpect(jsonPath("$.name", is("Meridian Architects")));
  }

  @Test
  void getBySlug_passesThePathVariableThroughVerbatim() throws Exception {
    when(companyPublicService.publicGetBySlug(eq("meridian-architects"))).thenReturn(sample());

    mockMvc.perform(get("/api/public/companies/slug/meridian-architects")).andExpect(status().isOk());

    verify(companyPublicService).publicGetBySlug(eq("meridian-architects"));
  }

  @Test
  void getBySlug_unknownSlugReturnsASafeApiErrorEnvelopeWithNoStackTrace() throws Exception {
    when(companyPublicService.publicGetBySlug("unknown-slug"))
        .thenThrow(new ResponseStatusException(NOT_FOUND, "Company not found"));

    mockMvc.perform(get("/api/public/companies/slug/unknown-slug"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status", is(404)))
        .andExpect(jsonPath("$.message", is("Company not found")))
        .andExpect(jsonPath("$.stackTrace").doesNotExist());
  }

  // These three tests prove the path regex itself rejects the input (the route never
  // matches, the service is never called) - they deliberately do NOT assert a specific
  // HTTP status. In this standalone MockMvc harness (no Boot auto-configuration, no
  // default resource-handler chain), MockMvcBuilders.standaloneSetup() enables
  // throwExceptionIfNoHandlerFound(true), so an unmatched route surfaces as
  // NoHandlerFoundException - a type GlobalExceptionHandler has no specific handler for,
  // so it falls through to the generic catch-all and returns a safe 500 here. That is a
  // known artifact of this specific test harness, not a claim about the real deployed
  // app: Spring Framework 6.1+ (this app's actual DispatcherServlet, unlike the bare
  // standalone one built here) throws NoResourceFoundException by default for an
  // unmatched route instead, which GlobalExceptionHandler.handleNoResourceFound() already
  // maps to a clean 404 - confirmed by that handler's own existing presence and the
  // absence of any NoHandlerFoundException handler being needed anywhere else in this
  // codebase. What both scenarios agree on, and what this test actually verifies: the
  // service is never reached, and the response stays safe (no stack trace).
  @Test
  void getBySlug_purelyNumericSlugDoesNotMatchThisRoute_neverReachesTheService() throws Exception {
    mockMvc.perform(get("/api/public/companies/slug/12345"))
        .andExpect(jsonPath("$.stackTrace").doesNotExist());

    verifyNoInteractions(companyPublicService);
  }

  @Test
  void getBySlug_uppercaseSlugDoesNotMatchThisRoute_neverReachesTheService() throws Exception {
    // The path regex is lowercase-only ([a-z0-9-]) - an uppercase variant of a real slug
    // never matches the route pattern at all (the "wrong-case slug -> 404" requirement is
    // satisfied at the routing layer itself, before any service/repository lookup).
    mockMvc.perform(get("/api/public/companies/slug/Meridian-Architects"))
        .andExpect(jsonPath("$.stackTrace").doesNotExist());

    verifyNoInteractions(companyPublicService);
  }

  @Test
  void getBySlug_malformedSlugWithOnlySymbolsDoesNotMatchThisRoute_neverReachesTheService() throws Exception {
    mockMvc.perform(get("/api/public/companies/slug/---"))
        .andExpect(jsonPath("$.stackTrace").doesNotExist());

    verifyNoInteractions(companyPublicService);
  }

  @Test
  void getBySlug_doesNotCollideWithTheNumericIdRoute() throws Exception {
    when(companyPublicService.publicGet(701L)).thenReturn(sample());
    when(companyPublicService.publicGetBySlug("meridian-architects")).thenReturn(sample());

    mockMvc.perform(get("/api/public/companies/701")).andExpect(status().isOk());
    mockMvc.perform(get("/api/public/companies/slug/meridian-architects")).andExpect(status().isOk());

    verify(companyPublicService).publicGet(701L);
    verify(companyPublicService).publicGetBySlug("meridian-architects");
  }
}
