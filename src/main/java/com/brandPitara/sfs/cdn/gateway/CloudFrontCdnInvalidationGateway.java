package com.brandPitara.sfs.cdn.gateway;

import com.brandPitara.sfs.cdn.config.CdnProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.CloudFrontException;
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationRequest;
import software.amazon.awssdk.services.cloudfront.model.InvalidationBatch;
import software.amazon.awssdk.services.cloudfront.model.Paths;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sfs.cdn", name = "enabled", havingValue = "true")
public class CloudFrontCdnInvalidationGateway implements CdnInvalidationGateway {

    static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MILLIS = 100L;

    private final CloudFrontClient cloudFrontClient;
    private final CdnProperties properties;

    @Override
    public void invalidate(Collection<String> paths) {
        List<String> exactPaths = paths == null ? List.of() : paths.stream().distinct().toList();
        if (exactPaths.isEmpty()) throw new IllegalArgumentException("At least one invalidation path is required");
        exactPaths.forEach(this::validatePath);

        String callerReference = "sfs-project-detail-" + UUID.randomUUID();
        CreateInvalidationRequest request = CreateInvalidationRequest.builder()
                .distributionId(properties.getDistributionId())
                .invalidationBatch(InvalidationBatch.builder()
                        .callerReference(callerReference)
                        .paths(Paths.builder().quantity(exactPaths.size()).items(exactPaths).build())
                        .build())
                .build();

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                cloudFrontClient.createInvalidation(request);
                log.info("event=cdn_invalidation provider=cloudfront attempt={} result=success paths={}",
                        attempt, exactPaths);
                return;
            } catch (RuntimeException failure) {
                boolean retryable = isRetryable(failure);
                log.warn("event=cdn_invalidation provider=cloudfront attempt={} result=failure retryable={} paths={}",
                        attempt, retryable, exactPaths, failure);
                if (!retryable || attempt == MAX_ATTEMPTS) {
                    throw new CdnInvalidationException("CloudFront invalidation failed", failure);
                }
                backoff(attempt);
            }
        }
    }

    private boolean isRetryable(RuntimeException failure) {
        if (failure instanceof SdkClientException) return true;
        if (failure instanceof CloudFrontException cloudFrontFailure) {
            int status = cloudFrontFailure.statusCode();
            return status == 429 || status >= 500;
        }
        return false;
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(BASE_BACKOFF_MILLIS * attempt);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new CdnInvalidationException("CloudFront invalidation retry interrupted", interrupted);
        }
    }

    private void validatePath(String path) {
        if (path == null || !path.matches("/api/v2/public/projects/[1-9][0-9]*")) {
            throw new IllegalArgumentException("Invalid Project Detail invalidation path");
        }
    }
}
