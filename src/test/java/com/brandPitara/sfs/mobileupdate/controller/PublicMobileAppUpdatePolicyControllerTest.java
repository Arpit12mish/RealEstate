package com.brandPitara.sfs.mobileupdate.controller;

import com.brandPitara.sfs.mobileupdate.MobilePlatform;
import com.brandPitara.sfs.mobileupdate.UpdateStatus;
import com.brandPitara.sfs.mobileupdate.dto.MobileAppUpdatePolicyResponse;
import com.brandPitara.sfs.mobileupdate.exception.MobileUpdatePolicyExceptionHandler;
import com.brandPitara.sfs.mobileupdate.service.MobileAppUpdatePolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PublicMobileAppUpdatePolicyControllerTest {

    private MobileAppUpdatePolicyService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(MobileAppUpdatePolicyService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PublicMobileAppUpdatePolicyController(service))
                .setControllerAdvice(new MobileUpdatePolicyExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void returnsPolicyWithNoStoreCacheHeader() throws Exception {
        when(service.evaluate(MobilePlatform.ANDROID, 20))
                .thenReturn(new MobileAppUpdatePolicyResponse(
                        MobilePlatform.ANDROID, UpdateStatus.OPTIONAL, "2.1.0",
                        21L, 18L, "Update available", "Please update", null, 24,
                        "https://play.google.com/store/apps/details?id=com.squarefootstory.app"));

        mockMvc.perform(get("/api/public/mobile-app/update-policy")
                        .param("platform", "ANDROID")
                        .param("currentBuild", "20"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.status").value("OPTIONAL"))
                .andExpect(jsonPath("$.latestBuild").value(21));
    }

    @Test
    void missingBuildReturnsStable400Code() throws Exception {
        mockMvc.perform(get("/api/public/mobile-app/update-policy")
                        .param("platform", "ANDROID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UPDATE_POLICY_REQUEST"));
    }

    @Test
    void malformedBuildReturnsStable400Code() throws Exception {
        mockMvc.perform(get("/api/public/mobile-app/update-policy")
                        .param("platform", "ANDROID")
                        .param("currentBuild", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UPDATE_POLICY_REQUEST"));
    }

    @Test
    void unsupportedPlatformReturnsStable400Code() throws Exception {
        mockMvc.perform(get("/api/public/mobile-app/update-policy")
                        .param("platform", "WINDOWS")
                        .param("currentBuild", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UPDATE_POLICY_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    void missingPolicyReturnsControlled503() throws Exception {
        when(service.evaluate(MobilePlatform.IOS, 20)).thenThrow(
                new com.brandPitara.sfs.mobileupdate.exception.MobileUpdatePolicyException(
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                        "UPDATE_POLICY_UNAVAILABLE", "Update policy is temporarily unavailable"));

        mockMvc.perform(get("/api/public/mobile-app/update-policy")
                        .param("platform", "IOS")
                        .param("currentBuild", "20"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value("UPDATE_POLICY_UNAVAILABLE"));
    }
}
