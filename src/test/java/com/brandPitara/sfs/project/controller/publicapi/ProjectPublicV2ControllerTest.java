package com.brandPitara.sfs.project.controller.publicapi;

import com.brandPitara.sfs.project.service.ProjectPublicCoreService;
import com.brandPitara.sfs.project.service.model.ProjectPublicCoreData;
import com.brandPitara.sfs.exception.GlobalExceptionHandler;
import com.brandPitara.sfs.observability.LogSanitizer;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectPublicV2ControllerTest {

    @Test
    void delegatesOnlyToDeterministicCore() {
        ProjectPublicCoreService coreService = mock(ProjectPublicCoreService.class);
        when(coreService.getById(27L)).thenReturn(ProjectPublicCoreData.builder()
                .id(27L)
                .favoriteCount(4L)
                .propertyTypes(Set.of())
                .floorPlanGroups(List.of())
                .glimpses(List.of())
                .failedSections(Set.of())
                .build());

        var response = new ProjectPublicV2Controller(coreService).get(27L);

        assertThat(response.getHeaders().getCacheControl())
                .isEqualTo(ProjectPublicV2Controller.PROJECT_DETAIL_CACHE_CONTROL);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(27L);
        assertThat(response.getBody().getFavoriteCount()).isEqualTo(4L);
        verify(coreService).getById(27L);
    }

    @Test
    void refusesToReturnCacheableDegradedProjectDetail() {
        for (var failedSection : com.brandPitara.sfs.project.service.model.ProjectPublicSectionFailure.values()) {
            ProjectPublicCoreService coreService = mock(ProjectPublicCoreService.class);
            when(coreService.getById(27L)).thenReturn(ProjectPublicCoreData.builder()
                    .id(27L)
                    .failedSections(Set.of(failedSection))
                    .build());

            assertThatThrownBy(() -> new ProjectPublicV2Controller(coreService).get(27L))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                    .satisfies(error -> assertThat(
                            ((org.springframework.web.server.ResponseStatusException) error)
                                    .getStatusCode().value())
                            .isEqualTo(503));
        }
    }

    @Test
    void degradedProjectDetailIs503WithoutThePublicCachePolicy() throws Exception {
        ProjectPublicCoreService coreService = mock(ProjectPublicCoreService.class);
        when(coreService.getById(27L)).thenReturn(ProjectPublicCoreData.builder()
                .id(27L)
                .failedSections(Set.of(
                        com.brandPitara.sfs.project.service.model.ProjectPublicSectionFailure.ANALYTICS))
                .build());
        var mvc = MockMvcBuilders.standaloneSetup(new ProjectPublicV2Controller(coreService))
                .setControllerAdvice(new GlobalExceptionHandler(new LogSanitizer()))
                .build();

        mvc.perform(get("/api/v2/public/projects/27"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().doesNotExist("Cache-Control"));
    }
}
