package com.brandPitara.sfs.media.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.S3Client;
@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class AwsS3Config {

    @Value("${aws.credentials.access-key:}")
    private String accessKey;

    @Value("${aws.credentials.secret-key:}")
    private String secretKey;

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        if (accessKey != null && !accessKey.isBlank()) {
            return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
            );
        }
        // fallback for EC2 IAM role later
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
        return S3Client.builder()
                .region(Region.of(props.getRegion()))
                .credentialsProvider(credentialsProvider)
                .build();
    }
}