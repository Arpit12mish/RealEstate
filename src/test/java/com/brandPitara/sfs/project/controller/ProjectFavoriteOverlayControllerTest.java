package com.brandPitara.sfs.project.controller;

import com.brandPitara.sfs.project.dto.ProjectFavoriteOverlayResponse;
import com.brandPitara.sfs.project.service.ProjectFavoriteService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectFavoriteOverlayControllerTest {

    @Test
    void acceptsCommaSeparatedProjectIdsAndReturnsOverlay() throws Exception {
        ProjectFavoriteService service = mock(ProjectFavoriteService.class);
        when(service.getCurrentViewerFavoriteOverlay(List.of(27L, 28L, 29L)))
                .thenReturn(new ProjectFavoriteOverlayResponse(List.of()));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProjectFavoriteOverlayController(service)).build();

        mvc.perform(get("/api/me/project-favorites").param("projectIds", "27,28,29"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Cache-Control"))
                .andExpect(jsonPath("$.items").isArray());

        verify(service).getCurrentViewerFavoriteOverlay(List.of(27L, 28L, 29L));
    }

    @Test
    void missingProjectIdsIsBadRequestBeforeServiceInvocation() throws Exception {
        ProjectFavoriteService service = mock(ProjectFavoriteService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProjectFavoriteOverlayController(service)).build();

        mvc.perform(get("/api/me/project-favorites"))
                .andExpect(status().isBadRequest());
    }
}
