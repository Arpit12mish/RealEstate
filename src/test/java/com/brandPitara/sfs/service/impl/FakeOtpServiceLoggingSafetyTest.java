package com.brandPitara.sfs.service.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.brandPitara.sfs.config.LocalFakeOtpProperties;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FakeOtpServiceLoggingSafetyTest {

    @Test
    void localFakeOtpLogsNeitherOtpNorAnyPhoneIdentifier() {
        Logger logger = (Logger) LoggerFactory.getLogger(FakeOtpService.class);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> events = new ListAppender<>();
        events.start();
        logger.setLevel(Level.INFO);
        logger.addAppender(events);
        try {
            LocalFakeOtpProperties properties = new LocalFakeOtpProperties();
            properties.setEnabled(true);
            properties.setFixedOtp("123456");
            properties.setPhoneNumbers(Set.of("+919876543210"));
            FakeOtpService service = new FakeOtpService(properties);
            service.sendOtp("9876543210");
            service.verifyOtp("9876543210", "123456");

            assertThat(events.list).hasSize(2);
            assertThat(events.list).extracting(ILoggingEvent::getFormattedMessage)
                    .allSatisfy(message -> assertThat(message)
                            .doesNotContain("9876543210", "123456", "98******10", "phone"));
        } finally {
            logger.detachAppender(events);
            logger.setLevel(previousLevel);
            events.stop();
        }
    }
}
