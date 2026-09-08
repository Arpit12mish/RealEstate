package com.brandPitara.sfs.observability;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLoggingTopologyTest {

    @Test
    void productionTopologySeparatesLossyInfoReliableErrorsSecurityAndAudit() throws Exception {
        String xml = Files.readString(
                new ClassPathResource("logback-spring.xml").getFile().toPath(), StandardCharsets.UTF_8);

        String async = appender(xml, "API_ASYNC");
        assertThat(async)
                .contains("BoundedRequestAsyncAppender")
                .contains("<queueSize>${REQUEST_LOG_QUEUE_SIZE}</queueSize>")
                .contains("<discardingThreshold>${REQUEST_LOG_DISCARDING_THRESHOLD}</discardingThreshold>")
                .contains("<neverBlock>${REQUEST_LOG_NEVER_BLOCK}</neverBlock>")
                .contains("<includeCallerData>${REQUEST_LOG_INCLUDE_CALLER_DATA}</includeCallerData>")
                .contains("<maxFlushTime>${REQUEST_LOG_MAX_FLUSH_TIME_MS}</maxFlushTime>")
                .contains("appender-ref ref=\"API_FILE\"");

        String prod = profile(xml, "!local");
        assertThat(logger(prod, LoggingConstants.LOGGER_API))
                .contains("additivity=\"false\"")
                .contains("appender-ref ref=\"API_ASYNC\"")
                .doesNotContain("API_RELIABLE_FILE", "SECURITY_FILE", "AUDIT_FILE");
        assertThat(logger(prod, LoggingConstants.LOGGER_API_RELIABLE))
                .contains("level=\"WARN\"", "additivity=\"false\"", "API_RELIABLE_FILE")
                .doesNotContain("API_ASYNC");
        assertThat(logger(prod, LoggingConstants.LOGGER_SECURITY))
                .contains("additivity=\"false\"", "SECURITY_FILE").doesNotContain("API_ASYNC");
        assertThat(logger(prod, LoggingConstants.LOGGER_AUDIT))
                .contains("additivity=\"false\"", "AUDIT_FILE").doesNotContain("API_ASYNC");
    }

    @Test
    void rollingAndRetentionRemainBoundedForBothRequestPaths() throws Exception {
        String xml = Files.readString(
                new ClassPathResource("logback-spring.xml").getFile().toPath(), StandardCharsets.UTF_8);

        assertThat(appender(xml, "API_FILE"))
                .contains("SizeAndTimeBasedRollingPolicy", "<maxFileSize>100MB</maxFileSize>")
                .contains("<maxHistory>14</maxHistory>", "<totalSizeCap>2GB</totalSizeCap>");
        assertThat(appender(xml, "API_RELIABLE_FILE"))
                .contains("SizeAndTimeBasedRollingPolicy", "<maxFileSize>50MB</maxFileSize>")
                .contains("<maxHistory>30</maxHistory>", "<totalSizeCap>2GB</totalSizeCap>");
    }

    @Test
    void securityAndAuditEventsDoNotReachRequestLogger() {
        Logger request = (Logger) LoggerFactory.getLogger(LoggingConstants.LOGGER_API);
        Logger security = (Logger) LoggerFactory.getLogger(LoggingConstants.LOGGER_SECURITY);
        Logger audit = (Logger) LoggerFactory.getLogger(LoggingConstants.LOGGER_AUDIT);
        ListAppender<ILoggingEvent> requestEvents = new ListAppender<>();
        ListAppender<ILoggingEvent> securityEvents = new ListAppender<>();
        ListAppender<ILoggingEvent> auditEvents = new ListAppender<>();
        requestEvents.start();
        securityEvents.start();
        auditEvents.start();
        request.addAppender(requestEvents);
        security.addAppender(securityEvents);
        audit.addAppender(auditEvents);
        try {
            security.warn("security-isolated");
            audit.info("audit-isolated");
            assertThat(requestEvents.list).isEmpty();
            assertThat(securityEvents.list).hasSize(1);
            assertThat(auditEvents.list).hasSize(1);
        } finally {
            request.detachAppender(requestEvents);
            security.detachAppender(securityEvents);
            audit.detachAppender(auditEvents);
        }
    }

    private String appender(String xml, String name) {
        int start = xml.indexOf("<appender name=\"" + name + "\"");
        int end = xml.indexOf("</appender>", start);
        return xml.substring(start, end);
    }

    private String profile(String xml, String name) {
        int start = xml.indexOf("<springProfile name=\"" + name + "\"");
        int end = xml.indexOf("</springProfile>", start);
        return xml.substring(start, end);
    }

    private String logger(String profile, String name) {
        int start = profile.indexOf("<logger name=\"" + name + "\"");
        int end = profile.indexOf("</logger>", start);
        return profile.substring(start, end);
    }
}
