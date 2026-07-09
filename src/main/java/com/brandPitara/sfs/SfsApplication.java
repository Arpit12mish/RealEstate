package com.brandPitara.sfs;

import com.brandPitara.sfs.config.AppReviewLoginProperties;
import com.brandPitara.sfs.config.TwilioProperties;
import com.brandPitara.sfs.instagram.config.AppInstagramProperties;
import com.brandPitara.sfs.instagram.config.InstagramMetaProperties;
import com.brandPitara.sfs.ratelimit.config.RateLimitProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({
        TwilioProperties.class,
        AppReviewLoginProperties.class,
        AppInstagramProperties.class,
        InstagramMetaProperties.class,
        RateLimitProperties.class
})
@EnableScheduling
public class SfsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SfsApplication.class, args);
    }
}
