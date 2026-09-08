package com.brandPitara.sfs.controller;

import com.brandPitara.sfs.dto.PublicCityDetailResponse;
import com.brandPitara.sfs.dto.TrendingCityCardResponse;
import com.brandPitara.sfs.exception.GlobalExceptionHandler;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.service.PublicCityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GAP-017 controller test — this codebase has no prior @WebMvcTest/MockMvc
 * convention for a public controller specifically, so this mirrors the
 * closest existing precedent (AdminBrandControllerTest /
 * DashboardPromoBannerControllerTest's MockMvcBuilders.standaloneSetup +
 * an explicit GlobalExceptionHandler as controller advice) rather than
 * introducing a new pattern.
 */
@ExtendWith(MockitoExtension.class)
class PublicCityControllerTest {

    @Mock private PublicCityService publicCityService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PublicCityController(publicCityService))
                .setControllerAdvice(new GlobalExceptionHandler(new LogSanitizer()))
                .build();
    }

    @Test
    void knownActiveCityReturns200WithMappedFields() throws Exception {
        PublicCityDetailResponse city = PublicCityDetailResponse.builder()
                .id(7L)
                .slug("mumbai")
                .name("Mumbai")
                .state("Maharashtra")
                .countryCode("IN")
                .coverImageUrl("https://cdn.sfs.com/cities/mumbai.webp")
                .growthPercent(12.4)
                .updatedAt(OffsetDateTime.parse("2026-07-01T10:00:00Z"))
                .build();
        when(publicCityService.getCityBySlug(eq("mumbai"))).thenReturn(city);

        mockMvc.perform(get("/api/public/cities/mumbai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.slug").value("mumbai"))
                .andExpect(jsonPath("$.name").value("Mumbai"))
                .andExpect(jsonPath("$.state").value("Maharashtra"))
                .andExpect(jsonPath("$.growthPercent").value(12.4))
                // Public DTO does not expose internal/admin-only fields.
                .andExpect(jsonPath("$.active").doesNotExist())
                .andExpect(jsonPath("$.homepageFeatured").doesNotExist())
                .andExpect(jsonPath("$.displayOrder").doesNotExist())
                .andExpect(jsonPath("$.latitude").doesNotExist())
                .andExpect(jsonPath("$.longitude").doesNotExist());
    }

    @Test
    void unknownSlugReturns404WithStandardApiErrorShape() throws Exception {
        when(publicCityService.getCityBySlug(eq("nowhere-city")))
                .thenThrow(new NotFoundException("City not found: nowhere-city"));

        mockMvc.perform(get("/api/public/cities/nowhere-city"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("City not found: nowhere-city"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void trendingRouteIsNotShadowedByTheNewSlugRoute() throws Exception {
        when(publicCityService.getTrendingCities(null)).thenReturn(
                List.of(TrendingCityCardResponse.builder().id(1L).name("Mumbai").slug("mumbai").build())
        );

        mockMvc.perform(get("/api/public/cities/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("mumbai"));
    }
}
