package com.brandPitara.sfs.publicreview.service.impl;

import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.integration.ExternalProviderTransactions;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.publicreview.client.GooglePlaceDetailsResponse;
import com.brandPitara.sfs.publicreview.config.GooglePlacesProperties;
import com.brandPitara.sfs.publicreview.dto.SyncGooglePublicReviewsResponse;
import com.brandPitara.sfs.publicreview.entity.PublicReviewPlaceEntity;
import com.brandPitara.sfs.publicreview.enums.GoogleReviewFetchStatus;
import com.brandPitara.sfs.publicreview.enums.PublicReviewTargetType;
import com.brandPitara.sfs.publicreview.provider.ReviewPlaceProvider;
import com.brandPitara.sfs.publicreview.repository.ProjectReviewRepository;
import com.brandPitara.sfs.publicreview.repository.PublicReviewPlaceRepository;
import com.brandPitara.sfs.publicreview.repository.PublicReviewSampleRepository;
import com.brandPitara.sfs.publicreview.repository.PublicReviewSummaryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves the Google-sync reservation race is closed under real concurrency
 * (real threads, a real Postgres row lock via
 * PublicReviewPlaceRepository#findByIdAndTargetTypeAndTargetIdAndDeletedFalseForUpdate)
 * rather than the single-threaded Mockito simulation used elsewhere in
 * PublicReviewServiceImplTest. Two threads race to sync the same place; the
 * PESSIMISTIC_WRITE lock serializes prepareGoogleSync, so exactly one caller
 * reserves the fetch and calls the provider, and the other observes the
 * winner's committed FETCHING+fresh state and is rejected with 409 before
 * ever reaching the provider.
 */
@SpringBootTest(
    classes = PublicReviewSyncConcurrencyIntegrationTest.TestApplication.class,
    properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "google.places.fetch-lease-seconds=180"
    }
)
@ActiveProfiles({"test", "dev"})
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PublicReviewSyncConcurrencyIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("sfs_review_sync_test")
        .withUsername("sfs_test")
        .withPassword("sfs_test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Autowired
    private PublicReviewServiceImpl reviewService;

    @Autowired
    private PublicReviewPlaceRepository placeRepository;

    @MockitoBean
    private ReviewPlaceProvider reviewPlaceProvider;

    @MockitoBean
    private ProjectRepository projectRepository;

    @MockitoBean
    private BuilderRepository builderRepository;

    @MockitoBean
    private CompanyRepository companyRepository;

    @MockitoBean
    private PublicReviewSummaryRepository summaryRepository;

    @MockitoBean
    private PublicReviewSampleRepository sampleRepository;

    @MockitoBean
    private ProjectReviewRepository projectReviewRepository;

    @MockitoBean
    private ProjectPublicVisibilityPolicy projectPublicVisibilityPolicy;

    @MockitoBean
    private ContentVersionService contentVersionService;

    @Test
    void concurrentSyncAttemptsForTheSamePlaceCallTheProviderExactlyOnce() throws Exception {
        Long targetId = 900L;
        when(projectRepository.findByIdAndDeletedFalse(targetId))
            .thenReturn(Optional.of(ProjectEntity.builder().id(targetId).build()));

        PublicReviewPlaceEntity place = placeRepository.save(PublicReviewPlaceEntity.builder()
            .targetType(PublicReviewTargetType.PROJECT)
            .targetId(targetId)
            .googlePlaceId("ChIJ-real-concurrency")
            .fetchStatus(GoogleReviewFetchStatus.NOT_FETCHED)
            .oneTimeFetched(false)
            .build());
        Long placeId = place.getId();

        GooglePlaceDetailsResponse googleResponse = new GooglePlaceDetailsResponse();
        googleResponse.setRating(BigDecimal.valueOf(4.6));
        googleResponse.setUserRatingCount(17);
        googleResponse.setReviews(List.of());
        when(reviewPlaceProvider.fetchPlaceDetails(anyString())).thenReturn(googleResponse);

        int attempts = 2;
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<OutcomeOrRejection>> futures = IntStream.range(0, attempts)
            .mapToObj(i -> executor.submit(() -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                try {
                    return new OutcomeOrRejection(
                        reviewService.syncGoogleReviews(PublicReviewTargetType.PROJECT, targetId, placeId), null);
                } catch (ResponseStatusException ex) {
                    return new OutcomeOrRejection(null, ex);
                }
            }))
            .collect(Collectors.toList());

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        for (Future<OutcomeOrRejection> future : futures) {
            OutcomeOrRejection outcome = future.get(10, TimeUnit.SECONDS);
            if (outcome.result() != null) {
                assertThat(outcome.result().getReviewPlaceId()).isEqualTo(placeId);
                succeeded.incrementAndGet();
            } else {
                assertThat(outcome.rejection().getStatusCode().value()).isEqualTo(409);
                rejected.incrementAndGet();
            }
        }
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(succeeded.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(1);
        // The core regression check: the provider is invoked exactly once even
        // though two real threads raced for the same place - the loser is
        // rejected by the row lock before it ever reaches the provider.
        verify(reviewPlaceProvider, times(1)).fetchPlaceDetails(anyString());

        PublicReviewPlaceEntity persisted = placeRepository.findById(placeId).orElseThrow();
        assertThat(persisted.getFetchStatus()).isEqualTo(GoogleReviewFetchStatus.FETCHED);
        assertThat(persisted.getFetchStartedAt()).isNull();
    }

    private record OutcomeOrRejection(
        SyncGooglePublicReviewsResponse result,
        ResponseStatusException rejection
    ) {
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableConfigurationProperties(GooglePlacesProperties.class)
    @EntityScan(basePackageClasses = PublicReviewPlaceEntity.class)
    @EnableJpaRepositories(
        basePackageClasses = PublicReviewPlaceRepository.class,
        excludeFilters = @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.brandPitara\\.sfs\\.publicreview\\.repository\\.(?!PublicReviewPlaceRepository$).*"
        )
    )
    @Import({PublicReviewServiceImpl.class, ExternalProviderTransactions.class})
    static class TestApplication {
    }
}
