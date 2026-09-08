package com.brandPitara.sfs.publiccontent.controller;

import com.brandPitara.sfs.cms.content.document.ContentDocument;
import com.brandPitara.sfs.cms.content.domain.ContentType;
import com.brandPitara.sfs.publiccontent.dto.*;
import com.brandPitara.sfs.publiccontent.exception.PublicContentApiException;
import com.brandPitara.sfs.publiccontent.exception.PublicContentExceptionHandler;
import com.brandPitara.sfs.publiccontent.service.PublicCategoryService;
import com.brandPitara.sfs.publiccontent.service.PublicContentResult;
import com.brandPitara.sfs.publiccontent.service.PublicContentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PublicContentControllerTest {
    @Mock private PublicContentService service;
    @Mock private PublicCategoryService categoryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PublicContentController(service, categoryService))
                .setControllerAdvice(new PublicContentExceptionHandler())
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(
                        new ObjectMapper().findAndRegisterModules()
                ))
                .build();
    }

    @Test
    void detailReturnsPublicCacheContractEtagAndSeparatedDto() throws Exception {
        OffsetDateTime published = OffsetDateTime.parse("2026-08-19T12:00:00Z");
        var body = new PublicContentDetailResponse(
                42L, "published-title", ContentType.ARTICLE, "Published title", "Excerpt",
                new PublicContentSeoResponse(null, null, null, true, true),
                ContentDocument.empty(), published, published.minusHours(1), Map.of()
        );
        when(service.getBySlug("published-title", null))
                .thenReturn(new PublicContentResult("\"cms-etag\"", false, body));

        mockMvc.perform(get("/api/public/content/published-title"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, PublicContentController.CACHE_CONTROL))
                .andExpect(header().string(HttpHeaders.ETAG, "\"cms-etag\""))
                .andExpect(jsonPath("$.title").value("Published title"))
                .andExpect(jsonPath("$.document.schemaVersion").value(ContentDocument.CURRENT_SCHEMA_VERSION))
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.revisionId").doesNotExist())
                .andExpect(jsonPath("$.publishedByDashboardUserId").doesNotExist())
                .andExpect(jsonPath("$.storageBucket").doesNotExist());
    }

    @Test
    void conditionalDetailReturns304WithNoBody() throws Exception {
        when(service.getBySlug("published-title", "\"cms-etag\""))
                .thenReturn(new PublicContentResult("\"cms-etag\"", true, null));

        mockMvc.perform(get("/api/public/content/published-title")
                        .header(HttpHeaders.IF_NONE_MATCH, "\"cms-etag\""))
                .andExpect(status().isNotModified())
                .andExpect(header().string(HttpHeaders.ETAG, "\"cms-etag\""))
                .andExpect(content().string(""));
    }

    @Test
    void listIsLightweightBoundedAndCacheable() throws Exception {
        var item = new PublicContentListItemResponse(
                42L, "published-title", ContentType.ARTICLE, "Published title", "Excerpt",
                OffsetDateTime.parse("2026-08-19T12:00:00Z")
        );
        when(service.list(ContentType.ARTICLE, 0, 20))
                .thenReturn(new PublicContentPageResponse(List.of(item), 0, 20, 1, 1, true));

        mockMvc.perform(get("/api/public/content").param("contentType", "ARTICLE"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, PublicContentController.CACHE_CONTROL))
                .andExpect(jsonPath("$.content[0].title").value("Published title"))
                .andExpect(jsonPath("$.content[0].document").doesNotExist())
                .andExpect(jsonPath("$.content[0].media").doesNotExist());
    }

    @Test
    void notPubliclyAvailableUsesOpaque404AndNoStore() throws Exception {
        when(service.getBySlug("not-public", null)).thenThrow(PublicContentApiException.notFound());

        mockMvc.perform(get("/api/public/content/not-public"))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.code").value("CONTENT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Published content was not found."));
    }

    @Test
    void invalidDependencyUsesOpaque503AndNoStore() throws Exception {
        when(service.getBySlug("broken", null))
                .thenThrow(PublicContentApiException.temporarilyUnavailable());

        mockMvc.perform(get("/api/public/content/broken"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.code").value("CONTENT_TEMPORARILY_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Published content is temporarily unavailable."));
    }

    @Test
    void invalidContentTypeUsesPublic400Contract() throws Exception {
        mockMvc.perform(get("/api/public/content").param("contentType", "NOT_A_TYPE"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.code").value("CONTENT_INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("A public content query parameter is invalid."));
    }

    @Test
    void categoriesRouteResolvesToCategoriesHandlerNotSlugDetailRegression() throws Exception {
        var market = new PublicCategoryResponse(2L, "Market Insights", "market-insights", "desc", 4L);
        when(categoryService.list()).thenReturn(new PublicCategoryListResponse(List.of(market)));

        mockMvc.perform(get("/api/public/content/categories"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, PublicContentController.CACHE_CONTROL))
                .andExpect(jsonPath("$.items[0].slug").value("market-insights"))
                .andExpect(jsonPath("$.items[0].publishedContentCount").value(4));

        verify(categoryService).list();
        // The literal /categories segment must never be mistaken for {slug} -
        // if it were, getBySlug("categories", ...) would have been invoked.
        verify(service, never()).getBySlug(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void categoriesRouteRequiresNoAuthenticationHeader() throws Exception {
        when(categoryService.list()).thenReturn(new PublicCategoryListResponse(List.of()));

        mockMvc.perform(get("/api/public/content/categories"))
                .andExpect(status().isOk());
    }

    @Test
    void categorySlugFilterIsForwardedToService() throws Exception {
        when(service.list(null, "market-insights", null, null, null, 0, 20))
                .thenReturn(new PublicContentPageResponse(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/public/content").param("categorySlug", "market-insights"))
                .andExpect(status().isOk());

        verify(service).list(null, "market-insights", null, null, null, 0, 20);
    }

    @Test
    void searchQueryIsForwardedToService() throws Exception {
        when(service.list(null, null, null, null, "builder", 0, 20))
                .thenReturn(new PublicContentPageResponse(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/public/content").param("q", "builder"))
                .andExpect(status().isOk());

        verify(service).list(null, null, null, null, "builder", 0, 20);
    }

    @Test
    void combinedSearchAndCategoryFilterComposeWithAndSemantics() throws Exception {
        when(service.list(null, "market-insights", null, null, "builder", 0, 20))
                .thenReturn(new PublicContentPageResponse(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/public/content")
                        .param("q", "builder")
                        .param("categorySlug", "market-insights"))
                .andExpect(status().isOk());

        verify(service).list(null, "market-insights", null, null, "builder", 0, 20);
    }

    @Test
    void paginationParamsStillForwardCorrectlyWithNoFilters() throws Exception {
        // No filter param is present, so the controller takes the unfiltered
        // 3-arg overload (matches its pre-existing branching for contentType-only
        // requests) - not the 7-arg filtered one.
        when(service.list(null, 1, 5))
                .thenReturn(new PublicContentPageResponse(List.of(), 1, 5, 0, 0, true));

        mockMvc.perform(get("/api/public/content").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void paginationParamsStillForwardCorrectlyAlongsideNewFilters() throws Exception {
        when(service.list(null, "market-insights", null, null, null, 1, 5))
                .thenReturn(new PublicContentPageResponse(List.of(), 1, 5, 0, 0, true));

        mockMvc.perform(get("/api/public/content")
                        .param("categorySlug", "market-insights")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5));
    }
}
