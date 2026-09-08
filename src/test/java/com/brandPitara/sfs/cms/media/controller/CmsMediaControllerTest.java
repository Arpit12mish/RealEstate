package com.brandPitara.sfs.cms.media.controller;

import com.brandPitara.sfs.cms.media.dto.CmsMediaAssetResponse;
import com.brandPitara.sfs.cms.media.service.CmsMediaService;
import com.brandPitara.sfs.dashboard.common.exception.DashboardExceptionHandler;
import com.brandPitara.sfs.observability.LogSanitizer;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CmsMediaControllerTest {
    @Test
    void listIsBoundedFilteredAndDoesNotExposeStorageInternals() throws Exception {
        CmsMediaService service = mock(CmsMediaService.class);
        when(service.list(any(), any(), any(), any(), any())).thenAnswer(invocation ->
                new PageImpl<>(List.of(response()), invocation.getArgument(4), 1));
        MockMvc mvc = mvc(service);

        String json = mvc.perform(get("/api/dashboard/cms/media")
                        .param("mediaType", "IMAGE").param("status", "READY")
                        .param("createdBy", "11").param("search", "cover")
                        .param("page", "-1").param("size", "1000")
                        .param("sortBy", "filename").param("sortDirection", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(71))
                .andReturn().getResponse().getContentAsString();

        assertThat(json).doesNotContain("storageBucket", "storageKey", "uploadUrl", "credentials");
        var pageable = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(service).list(any(), any(), eq(11L), eq("cover"), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
        assertThat(pageable.getValue().getSort().getOrderFor("originalFilename").isAscending()).isTrue();

        mvc.perform(get("/api/dashboard/cms/media").param("sortBy", "storageBucket"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void detailIncludesOnlyShortLivedPreviewContract() throws Exception {
        CmsMediaService service = mock(CmsMediaService.class);
        when(service.get(71L)).thenReturn(response());
        String json = mvc(service).perform(get("/api/dashboard/cms/media/71"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previewExpiresInSeconds").value(300))
                .andReturn().getResponse().getContentAsString();
        assertThat(json).contains("https://signed.example/read").doesNotContain("private-cms-bucket", "cms/images/");
    }

    private MockMvc mvc(CmsMediaService service) {
        return MockMvcBuilders.standaloneSetup(new CmsMediaController(service))
                .setControllerAdvice(new DashboardExceptionHandler(new LogSanitizer())).build();
    }

    private CmsMediaAssetResponse response() {
        OffsetDateTime now = OffsetDateTime.now();
        return new CmsMediaAssetResponse(71L, "IMAGE", "READY", "cover.jpg", "image/jpeg",
                1024L, 1200, 630, null, 11L, "Writer", now, now, now,
                null, "https://signed.example/read", 300);
    }
}
