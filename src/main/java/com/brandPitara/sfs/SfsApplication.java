package com.brandPitara.sfs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.brandPitara.sfs.config.TwilioProperties;

@SpringBootApplication
@EnableConfigurationProperties(TwilioProperties.class)
@EnableScheduling
public class SfsApplication {

	public static void main(String[] args) {
		SpringApplication.run(SfsApplication.class, args);
	}

}
