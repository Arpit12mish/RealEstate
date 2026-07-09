package com.brandPitara.sfs.brand.controller.admin;

import com.brandPitara.sfs.brand.dto.BrandResponse;
import com.brandPitara.sfs.brand.dto.BrandUpsertRequest;
import com.brandPitara.sfs.brand.service.BrandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminBrandControllerTest {

  @Mock private BrandService brandService;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    mockMvc = MockMvcBuilders
        .standaloneSetup(new AdminBrandController(brandService))
        .setValidator(validator)
        .build();
    objectMapper = new ObjectMapper().findAndRegisterModules();
  }

  @Test
  void update_acceptsFullBodyWithPublicStats_andReturnsStats() throws Exception {
    when(brandService.update(eq(29L), org.mockito.ArgumentMatchers.any(BrandUpsertRequest.class)))
        .thenReturn(BrandResponse.builder()
            .id(29L)
            .name("Incor")
            .slug("incor")
            .logoUrl("https://example.com/incor.png")
            .heroImageUrl("https://example.com/incor-hero.png")
            .shortDescription("Short")
            .description("Description")
            .foundedYear(2012)
            .yearsInIndustry(Year.now().getValue() - 2012)
            .customerRating(new BigDecimal("4.7"))
            .customerRatingCount(350)
            .categoryIds(List.of())
            .active(true)
            .published(true)
            .priority(0)
            .build());

    mockMvc.perform(put("/api/admin/brands/29")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Incor",
                  "slug": "incor",
                  "shortDescription": "Short",
                  "description": "Description",
                  "logoUrl": "https://example.com/incor.png",
                  "heroImageUrl": "https://example.com/incor-hero.png",
                  "foundedYear": 2012,
                  "customerRating": 4.7,
                  "customerRatingCount": 350,
                  "active": true,
                  "published": true,
                  "priority": 0
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.foundedYear", is(2012)))
        .andExpect(jsonPath("$.yearsInIndustry", is(Year.now().getValue() - 2012)))
        .andExpect(jsonPath("$.customerRating", is(4.7)))
        .andExpect(jsonPath("$.customerRatingCount", is(350)));

    ArgumentCaptor<BrandUpsertRequest> requestCaptor = ArgumentCaptor.forClass(BrandUpsertRequest.class);
    verify(brandService).update(eq(29L), requestCaptor.capture());
    BrandUpsertRequest request = requestCaptor.getValue();
    assertThat(request.getFoundedYear()).isEqualTo(2012);
    assertThat(request.getCustomerRating()).isEqualByComparingTo("4.7");
    assertThat(request.getCustomerRatingCount()).isEqualTo(350);
    assertThat(request.isFoundedYearPresent()).isTrue();
    assertThat(request.isCustomerRatingPresent()).isTrue();
    assertThat(request.isCustomerRatingCountPresent()).isTrue();
  }

  @Test
  void update_acceptsExplicitNullPublicStatsForClearing() throws Exception {
    when(brandService.update(eq(29L), org.mockito.ArgumentMatchers.any(BrandUpsertRequest.class)))
        .thenReturn(BrandResponse.builder()
            .id(29L)
            .name("Incor")
            .slug("incor")
            .categoryIds(List.of())
            .active(true)
            .published(true)
            .build());

    mockMvc.perform(put("/api/admin/brands/29")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Incor",
                  "foundedYear": null,
                  "customerRating": null,
                  "customerRatingCount": null
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.foundedYear").doesNotExist())
        .andExpect(jsonPath("$.yearsInIndustry").doesNotExist())
        .andExpect(jsonPath("$.customerRating").doesNotExist())
        .andExpect(jsonPath("$.customerRatingCount").doesNotExist());

    ArgumentCaptor<BrandUpsertRequest> requestCaptor = ArgumentCaptor.forClass(BrandUpsertRequest.class);
    verify(brandService).update(eq(29L), requestCaptor.capture());
    BrandUpsertRequest request = requestCaptor.getValue();
    assertThat(request.getFoundedYear()).isNull();
    assertThat(request.getCustomerRating()).isNull();
    assertThat(request.getCustomerRatingCount()).isNull();
    assertThat(request.isFoundedYearPresent()).isTrue();
    assertThat(request.isCustomerRatingPresent()).isTrue();
    assertThat(request.isCustomerRatingCountPresent()).isTrue();
  }

  @Test
  void update_preservesStatsWhenFieldsAreOmitted() throws Exception {
    when(brandService.update(eq(29L), org.mockito.ArgumentMatchers.any(BrandUpsertRequest.class)))
        .thenReturn(BrandResponse.builder()
            .id(29L)
            .name("Incor")
            .slug("incor")
            .foundedYear(2012)
            .yearsInIndustry(Year.now().getValue() - 2012)
            .customerRating(new BigDecimal("4.7"))
            .customerRatingCount(350)
            .categoryIds(List.of())
            .active(true)
            .published(true)
            .build());

    mockMvc.perform(put("/api/admin/brands/29")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Incor"
                }
                """))
        .andExpect(status().isOk());

    ArgumentCaptor<BrandUpsertRequest> requestCaptor = ArgumentCaptor.forClass(BrandUpsertRequest.class);
    verify(brandService).update(eq(29L), requestCaptor.capture());
    BrandUpsertRequest request = requestCaptor.getValue();
    assertThat(request.isFoundedYearPresent()).isFalse();
    assertThat(request.isCustomerRatingPresent()).isFalse();
    assertThat(request.isCustomerRatingCountPresent()).isFalse();
  }

  @Test
  void update_rejectsInvalidPublicStatsValues() throws Exception {
    assertInvalid("""
        {"name":"Incor","foundedYear":1799}
        """);
    assertInvalid("""
        {"name":"Incor","foundedYear":%d}
        """.formatted(Year.now().getValue() + 1));
    assertInvalid("""
        {"name":"Incor","customerRating":5.1}
        """);
    assertInvalid("""
        {"name":"Incor","customerRating":4.75}
        """);
    assertInvalid("""
        {"name":"Incor","customerRatingCount":-1}
        """);

    verifyNoInteractions(brandService);
  }

  private void assertInvalid(String body) throws Exception {
    mockMvc.perform(put("/api/admin/brands/29")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
  }
}
