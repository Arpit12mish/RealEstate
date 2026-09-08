package com.brandPitara.sfs.buildercredibility.controller.publicapi;

import com.brandPitara.sfs.buildercredibility.dto.BuilderCredibilityCardResponse;
import com.brandPitara.sfs.buildercredibility.dto.BuilderCredibilityResponse;
import com.brandPitara.sfs.buildercredibility.service.BuilderCredibilityService;
import com.brandPitara.sfs.exception.GlobalExceptionHandler;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.observability.LogSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-slice test for GAP-030: proves routing, public response shape
 * and error-envelope mapping for the two Builder Credibility public
 * endpoints. Built with plain standalone MockMvc (no Spring context boot) -
 * this project's resolved Spring Boot 4.0.0 dependency tree does not carry
 * the org.springframework.boot.test.autoconfigure.web.servlet /
 * org.springframework.boot.test.mock.mockito packages that older Boot
 * versions shipped (@WebMvcTest / @MockBean are unavailable here), so this
 * uses vanilla spring-test's MockMvcBuilders.standaloneSetup() instead - a
 * lower-level, Boot-slice-independent mechanism that only needs the real
 * controller instances, the real GlobalExceptionHandler and a mocked
 * service. Unauthenticated-access / security-filter-chain coverage is
 * handled separately in BuilderCredibilitySecurityAndRateLimitRegressionTest,
 * since standalone MockMvc deliberately does not run the servlet filter
 * chain (SecurityConfig) at all. Aggregation/scoring correctness is
 * BuilderCredibilityServiceImplTest's job; the service is mocked here on
 * purpose so this class is only about what the HTTP layer does with
 * whatever the service returns or throws.
 */
class BuilderCredibilityPublicControllerTest {

    private BuilderCredibilityService builderCredibilityService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        builderCredibilityService = mock(BuilderCredibilityService.class);

        BuilderCredibilityPublicController detailController =
            new BuilderCredibilityPublicController(builderCredibilityService);
        BuilderCredibilityCardPublicController cardController =
            new BuilderCredibilityCardPublicController(builderCredibilityService);

        mockMvc = MockMvcBuilders
            .standaloneSetup(detailController, cardController)
            .setControllerAdvice(new GlobalExceptionHandler(new LogSanitizer()))
            .build();
    }

    @Test
    void getCredibilityForAKnownPublicBuilderReturns200WithTheServiceResponse() throws Exception {
        when(builderCredibilityService.publicGetCredibility(42L)).thenReturn(
            BuilderCredibilityResponse.builder()
                .builderId(42L)
                .builderName("M3M")
                .credibilityScore(66)
                .credibilityLabel("Reliable")
                .trackedProjectsCount(3)
                .metrics(List.of())
                .scoreBreakdown(List.of())
                .recentProjectEvidence(List.of())
                .positiveIndicators(List.of())
                .observedRisks(List.of())
                .build()
        );

        mockMvc.perform(get("/api/builders/42/credibility"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.builderId").value(42))
            .andExpect(jsonPath("$.builderName").value("M3M"))
            .andExpect(jsonPath("$.credibilityScore").value(66))
            .andExpect(jsonPath("$.credibilityLabel").value("Reliable"));
    }

    @Test
    void getCredibilityForAnUnknownBuilderReturns404WithSafeErrorEnvelope() throws Exception {
        when(builderCredibilityService.publicGetCredibility(99999999L))
            .thenThrow(new NotFoundException("Builder not found: 99999999"));

        // Request-ID assignment normally happens in a filter that standalone
        // MockMvc deliberately doesn't run (see class javadoc) - passing the
        // header directly exercises GlobalExceptionHandler's real
        // resolveRequestId() header fallback instead of skipping the field.
        mockMvc.perform(get("/api/builders/99999999/credibility")
                .header("X-Request-Id", "test-request-id-123"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.path").value("/api/builders/99999999/credibility"))
            .andExpect(jsonPath("$.requestId").value("test-request-id-123"));
    }

    @Test
    void getCredibilityWithANonNumericBuilderIdIsRejectedSafelyWithoutLeakingAStackTrace() throws Exception {
        mockMvc.perform(get("/api/builders/not-a-number/credibility"))
            .andExpect(status().is5xxServerError())
            .andExpect(jsonPath("$.message").value("Internal server error"))
            .andExpect(jsonPath("$.message").value(not(containsString("java.lang"))));
    }

    @Test
    void getCredibilityCardsReturns200WithAJsonArray() throws Exception {
        when(builderCredibilityService.publicListCredibilityCards(isNull(), eq(12))).thenReturn(List.of(
            BuilderCredibilityCardResponse.builder()
                .builderId(1L)
                .builderName("Builder 1")
                .credibilityScore(80)
                .credibilityLabel("Reliable")
                .highlightsAvailable(true)
                .highlightCtaLabel("Highlights")
                .build()
        ));

        mockMvc.perform(get("/api/builders/credibility/cards"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].builderId").value(1))
            .andExpect(jsonPath("$[0].highlightsAvailable").value(true));
    }

    @Test
    void getCredibilityCardsHonorsTheOptionalCityIdQueryParameter() throws Exception {
        when(builderCredibilityService.publicListCredibilityCards(eq(7L), anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/builders/credibility/cards").param("cityId", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getCredibilityCardsDefaultsLimitTo12WhenNotProvided() throws Exception {
        when(builderCredibilityService.publicListCredibilityCards(isNull(), eq(12))).thenReturn(List.of());

        mockMvc.perform(get("/api/builders/credibility/cards"))
            .andExpect(status().isOk());
    }
}
