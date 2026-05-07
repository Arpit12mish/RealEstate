package com.brandPitara.sfs;

import com.brandPitara.sfs.config.AppReviewLoginProperties;
import com.brandPitara.sfs.config.TwilioProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({
        TwilioProperties.class,
        AppReviewLoginProperties.class
})
@EnableScheduling
public class SfsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SfsApplication.class, args);
    }
}