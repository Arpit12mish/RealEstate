package com.brandPitara.sfs.cdn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;

import java.time.Duration;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class CdnInvalidationConfiguration {

    @Bean(name = "projectPublicCacheEvictionExecutor")
    public TaskExecutor projectPublicCacheEvictionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("cdn-project-evict-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    @Bean
    @ConditionalOnProperty(prefix = "sfs.cdn", name = "enabled", havingValue = "true")
    public CloudFrontClient cloudFrontClient(
            CdnProperties properties,
            AwsCredentialsProvider credentialsProvider
    ) {
        ClientOverrideConfiguration overrides = ClientOverrideConfiguration.builder()
                .apiCallTimeout(Duration.ofSeconds(5))
                .apiCallAttemptTimeout(Duration.ofSeconds(3))
                // Retry is owned by CloudFrontCdnInvalidationGateway so attempts are observable.
                .retryPolicy(RetryPolicy.none())
                .build();
        return CloudFrontClient.builder()
                .region(Region.AWS_GLOBAL)
                .credentialsProvider(credentialsProvider)
                .overrideConfiguration(overrides)
                .build();
    }
}
