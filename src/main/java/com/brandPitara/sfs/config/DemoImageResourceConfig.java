package com.brandPitara.sfs.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

/**
 * Serves the bundled demo photos (src/main/resources/demo-images) at
 * {@code /api/public/demo-images/**}. They are the Figma "Spotlight" photos the demo seed
 * (marketplace/demo-seed.sql) points demo workers and stores at, hosted by the backend itself so a
 * deploy is all that is needed - no bucket upload. The path sits under /api/public, which is
 * already public and routed to the backend.
 */
@Configuration
public class DemoImageResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/public/demo-images/**")
                .addResourceLocations("classpath:/demo-images/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
    }
}
