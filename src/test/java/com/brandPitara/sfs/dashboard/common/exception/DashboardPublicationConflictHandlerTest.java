package com.brandPitara.sfs.dashboard.common.exception;

import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.project.exception.PublicationConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardPublicationConflictHandlerTest {

    @Test
    void returnsStableStructuredConflictCode() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "PATCH", "/api/dashboard/projects/130/published");
        DashboardExceptionHandler handler = new DashboardExceptionHandler(new LogSanitizer());

        var response = handler.handlePublicationConflict(
                new PublicationConflictException(
                        "PROJECT_BUILDER_NOT_PUBLISHED",
                        "Publish the builder first."),
                request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSuccess()).isFalse();
        assertThat(response.getBody().getCode()).isEqualTo("PROJECT_BUILDER_NOT_PUBLISHED");
        assertThat(response.getBody().getMessage()).isEqualTo("Publish the builder first.");
    }
}
