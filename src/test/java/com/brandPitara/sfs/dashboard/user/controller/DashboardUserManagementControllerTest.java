package com.brandPitara.sfs.dashboard.user.controller;

import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.exception.DashboardExceptionHandler;
import com.brandPitara.sfs.dashboard.user.service.DashboardUserManagementService;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardUserManagementControllerTest {

    private AnnotationConfigApplicationContext context;
    private DashboardUserManagementController securedController;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(MethodSecurityTestConfig.class);
        securedController = context.getBean(DashboardUserManagementController.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void allAccountManagementMethodsRequireAdminRole() {
        assertDeniedWithoutAuthentication();
        assertDeniedFor("ROLE_CONTENT_STAFF");
        assertDeniedFor("ROLE_REVIEWER");
        assertDeniedFor("ROLE_DATA_ENTRY");

        authenticate("ROLE_ADMIN");
        assertThatCode(() -> securedController.get(1L)).doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidEmailUnknownProfileAndArbitrarySecurityField() throws Exception {
        DashboardUserManagementService service = mock(DashboardUserManagementService.class);
        MockMvc mockMvc = standaloneMvc(service);

        mockMvc.perform(post("/api/dashboard/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson().replace("writer@squarefootstory.com", "not-an-email")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/dashboard/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson().replace("WRITER", "SUPERUSER")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/dashboard/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson().replace("\n}", ",\n  \"permissions\": [\"CMS_USER_MANAGE\"]\n}")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void validationEnvelopeNeverEchoesPassword() throws Exception {
        DashboardUserManagementService service = mock(DashboardUserManagementService.class);
        MockMvc mockMvc = standaloneMvc(service);

        mockMvc.perform(put("/api/dashboard/users/9/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].rejectedValue").value("[REDACTED]"));

        verifyNoInteractions(service);
    }

    @Test
    void userListIsBoundedAndUsesOnlyAllowlistedSortFields() throws Exception {
        DashboardUserManagementService service = mock(DashboardUserManagementService.class);
        when(service.list(isNull(), isNull(), isNull(), any(Pageable.class))).thenAnswer(invocation ->
                new PageImpl<>(java.util.List.of(), invocation.getArgument(3), 0)
        );
        MockMvc mockMvc = standaloneMvc(service);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/dashboard/users")
                        .param("page", "-4")
                        .param("size", "5000")
                        .param("sortBy", "email")
                        .param("sortDirection", "asc"))
                .andExpect(status().isOk());

        var pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(service).list(isNull(), isNull(), isNull(), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(pageable.getPageNumber()).isZero();
        org.assertj.core.api.Assertions.assertThat(pageable.getPageSize()).isEqualTo(50);
        org.assertj.core.api.Assertions.assertThat(pageable.getSort().getOrderFor("email").isAscending()).isTrue();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/dashboard/users")
                        .param("sortBy", "passwordHash"))
                .andExpect(status().isBadRequest());
    }

    private void assertDeniedWithoutAuthentication() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> securedController.get(1L))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    private void assertDeniedFor(String authority) {
        authenticate(authority);
        assertThatThrownBy(() -> securedController.get(1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    private void authenticate(String authority) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "dashboard-user",
                        "unused",
                        AuthorityUtils.createAuthorityList(authority)
                )
        );
    }

    private MockMvc standaloneMvc(DashboardUserManagementService service) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(new DashboardUserManagementController(
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

    private String createJson() {
        return """
                {
                  "email": "writer@squarefootstory.com",
                  "displayName": "Content Writer",
                  "role": "CONTENT_STAFF",
                  "permissionProfiles": ["WRITER"],
                  "initialPassword": "Strong!Pass123"
                }
                """;
    }

    @Configuration
    @EnableMethodSecurity(proxyTargetClass = true)
    static class MethodSecurityTestConfig {
        @Bean
        DashboardUserManagementService userService() {
            return mock(DashboardUserManagementService.class);
        }

        @Bean
        DashboardActionAuditService auditService() {
            return mock(DashboardActionAuditService.class);
        }

        @Bean
        DashboardUserManagementController controller(
                DashboardUserManagementService userService,
                DashboardActionAuditService auditService
        ) {
            return new DashboardUserManagementController(userService, auditService);
        }
    }
}
