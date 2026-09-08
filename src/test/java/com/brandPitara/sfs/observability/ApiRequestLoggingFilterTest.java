package com.brandPitara.sfs.observability;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiRequestLoggingFilterTest {

    private final LogSanitizer sanitizer = new LogSanitizer();
    private final ApiRequestLoggingFilter requestFilter = new ApiRequestLoggingFilter(sanitizer);
    private final CorrelationIdFilter correlationFilter = new CorrelationIdFilter(sanitizer);
    private final Logger infoLogger = (Logger) LoggerFactory.getLogger(LoggingConstants.LOGGER_API);
    private final Logger reliableLogger = (Logger) LoggerFactory.getLogger(LoggingConstants.LOGGER_API_RELIABLE);
    private final PreparingListAppender info = new PreparingListAppender();
    private final PreparingListAppender reliable = new PreparingListAppender();
    private boolean originalReliableAdditivity;

    @BeforeEach
    void attach() {
        ReflectionTestUtils.setField(requestFilter, "slowApiThresholdMs", Long.MAX_VALUE);
        info.start();
        reliable.start();
        originalReliableAdditivity = reliableLogger.isAdditive();
        reliableLogger.setAdditive(false);
        infoLogger.addAppender(info);
        reliableLogger.addAppender(reliable);
    }

    @AfterEach
    void detach() {
        infoLogger.detachAppender(info);
        reliableLogger.detachAppender(reliable);
        reliableLogger.setAdditive(originalReliableAdditivity);
        info.stop();
        reliable.stop();
    }

    @Test
    void successfulRequestUsesInfoPathAndPreservesStructuredContract() throws Exception {
        MockHttpServletRequest request = request("/api/projects/42");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/projects/{id}");
        request.addHeader(LoggingConstants.HEADER_REQUEST_ID, "release-4b1-correlation");
        request.setQueryString("view=summary&token=never-log-this-token");
        request.addHeader("Authorization", "Bearer never-log-this-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentLength(321);

        invoke(request, response, (req, res) -> { });

        assertThat(info.list).hasSize(1);
        assertThat(reliable.list).isEmpty();
        String rendered = info.list.get(0).getFormattedMessage();
        assertThat(rendered)
                .contains("api_request_completed", "method=GET", "path=/api/projects/42")
                .contains("route=/api/projects/{id}", "status=200", "responseSizeBytes=321")
                .contains("principalType=anonymous", "token=****")
                .doesNotContain("never-log-this-token", "never-log-this-jwt", "Authorization");
        assertThat(info.list.get(0).getMDCPropertyMap())
                .containsEntry(LoggingConstants.MDC_REQUEST_ID, "release-4b1-correlation");
        assertThat(response.getHeader(LoggingConstants.HEADER_REQUEST_ID))
                .isEqualTo("release-4b1-correlation");
    }

    @Test
    void routineClientErrorsUseInfoQueueWhileServerErrorsRemainReliable() throws Exception {
        MockHttpServletRequest warningRequest = request("/api/missing");
        MockHttpServletResponse warningResponse = new MockHttpServletResponse();
        warningResponse.setStatus(404);
        invoke(warningRequest, warningResponse, (req, res) -> { });

        MockHttpServletRequest errorRequest = request("/api/failure");
        MockHttpServletResponse errorResponse = new MockHttpServletResponse();
        errorResponse.setStatus(500);
        invoke(errorRequest, errorResponse, (req, res) -> { });

        assertThat(info.list).hasSize(1);
        assertThat(reliable.list).hasSize(1);
        assertThat(reliable.list).extracting(ILoggingEvent::getLevel)
                .containsExactly(ch.qos.logback.classic.Level.ERROR);
    }

    @Test
    void exceptionClassificationIsSanitizedOnReliablePath() {
        MockHttpServletRequest request = request("/api/failure");
        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            invoke(request, response, (req, res) -> {
                throw new ServletException("failure\nwithout sensitive body");
            });
        } catch (Exception expected) {
            // The logging filter must preserve the original exception contract.
        }

        assertThat(reliable.list).hasSize(1);
        assertThat(reliable.list.get(0).getFormattedMessage())
                .contains("exceptionClass=ServletException")
                .contains("failure_without sensitive body")
                .doesNotContain("\n");
    }

    @Test
    void successfulSlowRequestUsesBoundedInfoPathAndPreservesSlowContract() throws Exception {
        ReflectionTestUtils.setField(requestFilter, "slowApiThresholdMs", -1L);
        MockHttpServletRequest request = request("/api/projects/42");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/projects/{id}");
        request.addHeader(LoggingConstants.HEADER_REQUEST_ID, "slow-request-correlation");
        MockHttpServletResponse response = new MockHttpServletResponse();

        invoke(request, response, (req, res) -> { });

        assertThat(reliable.list).isEmpty();
        assertThat(info.list).hasSize(2);
        ILoggingEvent slowEvent = info.list.get(1);
        assertThat(slowEvent.getFormattedMessage())
                .contains("event=slow_api", "slow=true", "durationMs=", "route=/api/projects/{id}", "status=200");
        assertThat(slowEvent.getMDCPropertyMap())
                .containsEntry(LoggingConstants.MDC_REQUEST_ID, "slow-request-correlation");
    }

    @Test
    void failedSlowRequestKeepsActualFailureAndSlowMarkerOnReliablePath() throws Exception {
        ReflectionTestUtils.setField(requestFilter, "slowApiThresholdMs", -1L);
        MockHttpServletRequest request = request("/api/failure");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);

        invoke(request, response, (req, res) -> { });

        assertThat(info.list).isEmpty();
        assertThat(reliable.list).hasSize(2);
        assertThat(reliable.list.get(0).getLevel()).isEqualTo(ch.qos.logback.classic.Level.ERROR);
        assertThat(reliable.list.get(1).getFormattedMessage()).contains("event=slow_api", "slow=true", "status=500");
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRemoteAddr("10.20.30.40");
        request.addHeader("User-Agent", "JUnit");
        return request;
    }

    private void invoke(
            MockHttpServletRequest request,
            MockHttpServletResponse response,
            FilterChain terminalChain
    ) throws ServletException, IOException {
        correlationFilter.doFilter(request, response,
                (correlatedRequest, correlatedResponse) -> requestFilter.doFilter(
                        correlatedRequest, correlatedResponse, terminalChain));
    }

    private static final class PreparingListAppender extends ListAppender<ILoggingEvent> {
        @Override
        protected void append(ILoggingEvent eventObject) {
            eventObject.prepareForDeferredProcessing();
            super.append(eventObject);
        }
    }
}
