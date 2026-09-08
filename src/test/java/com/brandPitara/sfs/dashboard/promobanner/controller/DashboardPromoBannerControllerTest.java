package com.brandPitara.sfs.dashboard.promobanner.controller;

import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.exception.DashboardExceptionHandler;
import com.brandPitara.sfs.dashboard.promobanner.service.DashboardPromoBannerService;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardPromoBannerControllerTest {

    private DashboardPromoBannerService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(DashboardPromoBannerService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new DashboardPromoBannerController(
                        service,
                        mock(DashboardActionAuditService.class)
                ))
                .setControllerAdvice(new DashboardExceptionHandler(new LogSanitizer()))
                .setValidator(validator)
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(
                        new ObjectMapper().findAndRegisterModules()
                ))
                .build();
    }

    @Test
    void endpointsUseDashboardRoleConventions() {
        assertThat(preAuthorize("list")).contains("ADMIN", "REVIEWER", "DATA_ENTRY");
        assertThat(preAuthorize("get")).contains("ADMIN", "REVIEWER", "DATA_ENTRY");
        assertThat(preAuthorize("create")).contains("ADMIN", "DATA_ENTRY");
        assertThat(preAuthorize("update")).contains("ADMIN", "DATA_ENTRY");
        assertThat(preAuthorize("setActive")).isEqualTo("hasRole('ADMIN')");
        assertThat(preAuthorize("delete")).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void invalidMediaTypeReturns400() throws Exception {
        mockMvc.perform(put("/api/dashboard/promo-banners/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson().replace("\"VIDEO\"", "\"MP4\"")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void blankMediaUrlReturns400() throws Exception {
        mockMvc.perform(put("/api/dashboard/promo-banners/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson().replace("https://cdn.example.com/hero.mp4", " ")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    private String validJson() {
        return """
                {
                  "categoryId": 0,
                  "slotKey": "HERO",
                  "title": "Hero",
                  "mediaType": "VIDEO",
                  "mediaUrl": "https://cdn.example.com/hero.mp4",
                  "priority": 1,
                  "active": true
                }
                """;
    }

    private String preAuthorize(String methodName) {
        for (var method : DashboardPromoBannerController.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
                return annotation != null ? annotation.value() : null;
            }
        }
        throw new AssertionError("Method not found: " + methodName);
    }
}
