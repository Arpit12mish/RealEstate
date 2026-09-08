package com.brandPitara.sfs.media.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.Duration;
@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class AwsS3Config {

    /**
     * The SDK's own default chain (env vars incl. AWS_SESSION_TOKEN, ~/.aws/credentials
     * and config profiles, container credentials, EC2 instance role) already resolves
     * both long-lived and temporary/session credentials correctly. A prior custom
     * StaticCredentialsProvider+AwsBasicCredentials branch here discarded any session
     * token whenever AWS_ACCESS_KEY_ID was set, which broke presigned S3 PUTs for any
     * temporary credential source (aws login, SSO, assumed role) with
     * 403 InvalidAccessKeyId - AwsBasicCredentials has no field for a session token.
     */
    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        return DefaultCredentialsProvider.create();
    }

    @Bean
    public S3Presigner s3Presigner(
            S3Properties props,
            AwsCredentialsProvider credentialsProvider
    ) {
        return S3Presigner.builder()
                .region(Region.of(props.getRegion()))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    @Bean
    public S3Client s3Client(
            S3Properties props,
            AwsCredentialsProvider credentialsProvider
    ) {
        // Bounded so a hung S3 call (network partition, regional outage) can
        // never block the calling thread indefinitely - the SDK default has no
        // overall apiCallTimeout.
        ClientOverrideConfiguration overrideConfiguration = ClientOverrideConfiguration.builder()
                .apiCallTimeout(Duration.ofMillis(props.getApiCallTimeoutMs()))
                .apiCallAttemptTimeout(Duration.ofMillis(props.getApiCallAttemptTimeoutMs()))
                .build();

        return S3Client.builder()
                .region(Region.of(props.getRegion()))
                .credentialsProvider(credentialsProvider)
                .overrideConfiguration(overrideConfiguration)
                .build();
    }
}