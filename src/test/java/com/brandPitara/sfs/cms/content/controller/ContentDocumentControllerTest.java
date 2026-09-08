package com.brandPitara.sfs.cms.content.controller;

import com.brandPitara.sfs.cms.content.document.ContentBlock;
import com.brandPitara.sfs.cms.content.document.ContentDocument;
import com.brandPitara.sfs.cms.content.document.InlineNode;
import com.brandPitara.sfs.cms.content.dto.ContentDocumentResponse;
import com.brandPitara.sfs.cms.content.service.ContentDocumentService;
import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.common.exception.DashboardExceptionHandler;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContentDocumentControllerTest {

    @Test
    void getAndPutExposeAutosaveContractAndAuditOnlyIdentifier() throws Exception {
        ContentDocumentService service = mock(ContentDocumentService.class);
        DashboardActionAuditService audit = mock(DashboardActionAuditService.class);
        ContentDocumentResponse response = response(9L, 5L);
        when(service.get(9L, null)).thenReturn(response);
        when(service.update(org.mockito.ArgumentMatchers.eq(9L), any(), isNull()))
                .thenReturn(response);
        MockMvc mockMvc = mvc(service, audit);

        mockMvc.perform(get("/api/dashboard/cms/content/9/document"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentId").value(9))
                .andExpect(jsonPath("$.version").value(5))
                .andExpect(jsonPath("$.wordCount").value(3))
                .andExpect(jsonPath("$.document.schemaVersion").value(ContentDocument.CURRENT_SCHEMA_VERSION))
                .andExpect(jsonPath("$.media").isMap());

        mockMvc.perform(put("/api/dashboard/cms/content/9/document")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(5));

        verify(audit).record(
                DashboardAuditAction.CONTENT_DOCUMENT_UPDATED,
                ReviewEntityType.CONTENT_POST,
                9L,
                null
        );
    }

    @Test
    void rejectsUnknownBlocksMarksPropertiesMediaRawHtmlAndH1UsingDocumentErrorEnvelope()
            throws Exception {
        ContentDocumentService service = mock(ContentDocumentService.class);
        MockMvc mockMvc = mvc(service, mock(DashboardActionAuditService.class));

        for (String block : List.of(
                "{\"type\":\"IMAGE\",\"assetId\":1}",
                "{\"type\":\"IMAGE\",\"mediaAssetId\":1,\"decorative\":false,\"altText\":\"x\",\"src\":\"https://evil.example/x.jpg\"}",
                "{\"type\":\"VIDEO\",\"assetId\":1}",
                "{\"type\":\"EMBED\",\"provider\":\"INSTAGRAM\",\"externalId\":\"x\"}",
                "{\"type\":\"EMBED\",\"provider\":\"YOUTUBE\",\"externalId\":\"x\",\"html\":\"<iframe></iframe>\"}",
                "{\"type\":\"RAW_HTML\",\"html\":\"<script>x</script>\"}",
                "{\"type\":\"SCRIPT\",\"code\":\"alert(1)\"}",
                "{\"type\":\"HEADING\",\"level\":\"H1\",\"content\":[]}",
                "{\"type\":\"DIVIDER\",\"style\":\"arbitrary\"}",
                "{\"type\":\"PARAGRAPH\",\"content\":[{\"type\":\"TEXT\",\"text\":\"x\",\"marks\":[{\"type\":\"COLOR\"}]}]}"
        )) {
            mockMvc.perform(put("/api/dashboard/cms/content/9/document")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson(block)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("CONTENT_DOCUMENT_INVALID"));
        }

        verifyNoInteractions(service);
    }

    @Test
    void rejectsUnknownTopLevelSecurityFields() throws Exception {
        ContentDocumentService service = mock(ContentDocumentService.class);
        MockMvc mockMvc = mvc(service, mock(DashboardActionAuditService.class));

        mockMvc.perform(put("/api/dashboard/cms/content/9/document")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateJson().replace(
                                "\n}", ",\n  \"status\": \"PUBLISHED\"\n}"
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CONTENT_DOCUMENT_INVALID"));

        verifyNoInteractions(service);
    }

    @Test
    void deeplyNestedUnknownInputIsRejectedBeforeServiceExecution() throws Exception {
        ContentDocumentService service = mock(ContentDocumentService.class);
        MockMvc mockMvc = mvc(service, mock(DashboardActionAuditService.class));
        String deepValue = "{\"nested\":".repeat(1_100) + "0" + "}".repeat(1_100);
        String body = """
                {
                  "version": 4,
                  "document": {
                    "schemaVersion": 1,
                    "blocks": [],
                    "unknown": %s
                  }
                }
                """.formatted(deepValue);

        mockMvc.perform(put("/api/dashboard/cms/content/9/document")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CONTENT_DOCUMENT_INVALID"));

        verifyNoInteractions(service);
    }

    @Test
    void rawDocumentRequestOverOneMibReturns413BeforeServiceExecution() throws Exception {
        ContentDocumentService service = mock(ContentDocumentService.class);
        MockMvc mockMvc = mvc(service, mock(DashboardActionAuditService.class));
        String oversized = " ".repeat(
                com.brandPitara.sfs.cms.content.document.ContentDocumentLimits.MAX_REQUEST_BYTES + 1
        );

        mockMvc.perform(put("/api/dashboard/cms/content/9/document")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oversized))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("CONTENT_DOCUMENT_TOO_LARGE"));

        verifyNoInteractions(service);
    }

    private MockMvc mvc(ContentDocumentService service, DashboardActionAuditService audit) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(new ContentDocumentController(service, audit))
                .setControllerAdvice(
                        new ContentDocumentRequestBodyAdvice(),
                        new DashboardExceptionHandler(new LogSanitizer())
                )
                .setValidator(validator)
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(
                        new ObjectMapper().findAndRegisterModules()
                ))
                .build();
    }

    private ContentDocumentResponse response(Long id, Long version) {
        return new ContentDocumentResponse(
                id,
                version,
                OffsetDateTime.now(),
                new ContentDocument(ContentDocument.CURRENT_SCHEMA_VERSION, List.of(new ContentBlock.Paragraph(List.of(
                        new InlineNode.Text("Gurgaon market guide", List.of())
                )))),
                3,
                java.util.Map.of()
        );
    }

    private String validUpdateJson() {
        return updateJson("""
                {"type":"PARAGRAPH","content":[
                  {"type":"TEXT","text":"Gurgaon market guide","marks":[]}
                ]}
                """);
    }

    private String updateJson(String block) {
        return """
                {
                  "version": 4,
                  "document": {
                    "schemaVersion": 1,
                    "blocks": [%s]
                  }
                }
                """.formatted(block);
    }
}
