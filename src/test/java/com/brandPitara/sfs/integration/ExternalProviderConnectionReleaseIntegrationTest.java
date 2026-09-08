package com.brandPitara.sfs.integration;

import com.brandPitara.sfs.cdn.event.ProjectPublicCacheEvictionPublisher;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.project.connectivity.provider.NearbyPlaceProvider;
import com.brandPitara.sfs.project.dto.ConnectivityProviderSearchRequest;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.enums.ProjectConnectivityCategory;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.repository.ProjectConnectivityPlaceRepository;
import com.brandPitara.sfs.project.repository.ProjectConnectivityRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.project.service.impl.ProjectConnectivityServiceImpl;
import com.brandPitara.sfs.publicreview.client.GooglePlaceDetailsResponse;
import com.brandPitara.sfs.publicreview.entity.PublicReviewPlaceEntity;
import com.brandPitara.sfs.publicreview.enums.GoogleReviewFetchStatus;
import com.brandPitara.sfs.publicreview.enums.PublicReviewTargetType;
import com.brandPitara.sfs.publicreview.provider.ReviewPlaceProvider;
import com.brandPitara.sfs.publicreview.repository.ProjectReviewRepository;
import com.brandPitara.sfs.publicreview.repository.PublicReviewPlaceRepository;
import com.brandPitara.sfs.publicreview.repository.PublicReviewSampleRepository;
import com.brandPitara.sfs.publicreview.repository.PublicReviewSummaryRepository;
import com.brandPitara.sfs.publicreview.service.impl.PublicReviewServiceImpl;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Testcontainers
class ExternalProviderConnectionReleaseIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static HikariDataSource dataSource;
    private static ExternalProviderTransactions transactions;

    @BeforeAll
    static void startPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(POSTGRES.getJdbcUrl());
        config.setUsername(POSTGRES.getUsername());
        config.setPassword(POSTGRES.getPassword());
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(1_000);
        config.setPoolName("ExternalProviderBoundaryPool");
        dataSource = new HikariDataSource(config);
        transactions = new ExternalProviderTransactions(new DataSourceTransactionManager(dataSource));
    }

    @AfterAll
    static void stopPool() {
        if (dataSource != null) dataSource.close();
    }

    @Test
    void delayedPublicReviewProviderRetainsNoHikariConnection() throws Exception {
        PublicReviewPlaceRepository placeRepository = mock(PublicReviewPlaceRepository.class);
        PublicReviewSummaryRepository summaryRepository = mock(PublicReviewSummaryRepository.class);
        PublicReviewSampleRepository sampleRepository = mock(PublicReviewSampleRepository.class);
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        ReviewPlaceProvider provider = mock(ReviewPlaceProvider.class);
        ContentVersionService contentVersionService = mock(ContentVersionService.class);

        PublicReviewPlaceEntity place = PublicReviewPlaceEntity.builder()
            .id(11L)
            .targetType(PublicReviewTargetType.PROJECT)
            .targetId(21L)
            .googlePlaceId("delayed-place")
            .fetchStatus(GoogleReviewFetchStatus.NOT_FETCHED)
            .oneTimeFetched(false)
            .build();
        when(projectRepository.findByIdAndDeletedFalse(21L))
            .thenReturn(Optional.of(ProjectEntity.builder().id(21L).build()));
        when(placeRepository.findByIdAndTargetTypeAndTargetIdAndDeletedFalse(
            11L, PublicReviewTargetType.PROJECT, 21L)).thenReturn(Optional.of(place));
        when(placeRepository.findByIdAndTargetTypeAndTargetIdAndDeletedFalseForUpdate(
            11L, PublicReviewTargetType.PROJECT, 21L)).thenReturn(Optional.of(place));
        when(summaryRepository.findByReviewPlaceId(11L)).thenReturn(Optional.empty());
        when(placeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CountDownLatch providerStarted = new CountDownLatch(1);
        CountDownLatch releaseProvider = new CountDownLatch(1);
        GooglePlaceDetailsResponse response = new GooglePlaceDetailsResponse();
        response.setRating(BigDecimal.valueOf(4.4));
        response.setUserRatingCount(12);
        response.setReviews(List.of());
        when(provider.fetchPlaceDetails("delayed-place")).thenAnswer(invocation -> {
            providerStarted.countDown();
            assertThat(releaseProvider.await(5, TimeUnit.SECONDS)).isTrue();
            return response;
        });

        PublicReviewServiceImpl service = new PublicReviewServiceImpl(
            placeRepository,
            summaryRepository,
            sampleRepository,
            mock(ProjectReviewRepository.class),
            projectRepository,
            mock(BuilderRepository.class),
            mock(CompanyRepository.class),
            mock(ProjectPublicVisibilityPolicy.class),
            provider,
            contentVersionService,
            transactions,
            new com.brandPitara.sfs.publicreview.config.GooglePlacesProperties()
        );

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> call = executor.submit(() ->
                service.syncGoogleReviews(PublicReviewTargetType.PROJECT, 21L, 11L));
            assertThat(providerStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertPoolIdle("public-review");
            releaseProvider.countDown();
            call.get(5, TimeUnit.SECONDS);
            awaitPoolIdle();
        } finally {
            releaseProvider.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void delayedConnectivityProviderRetainsNoHikariConnection() throws Exception {
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        ProjectConnectivityPlaceRepository placeRepository = mock(ProjectConnectivityPlaceRepository.class);
        NearbyPlaceProvider provider = mock(NearbyPlaceProvider.class);
        ProjectEntity project = ProjectEntity.builder()
            .id(31L)
            .latitude(28.4595)
            .longitude(77.0266)
            .active(true)
            .published(true)
            .deleted(false)
            .reviewStatus(ReviewStatus.APPROVED)
            .build();
        when(projectRepository.findByIdAndDeletedFalse(31L)).thenReturn(Optional.of(project));
        when(provider.getMaxRadiusMeters()).thenReturn(5000);
        when(placeRepository.findAllProviderExternalIdKeysByProjectId(31L)).thenReturn(Set.of());
        when(placeRepository.findAllNameTypeKeysByProjectId(31L)).thenReturn(Set.of());
        when(placeRepository.findAllNameCategoryKeysByProjectId(31L)).thenReturn(Set.of());

        CountDownLatch providerStarted = new CountDownLatch(1);
        CountDownLatch releaseProvider = new CountDownLatch(1);
        when(provider.searchNearby(any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            providerStarted.countDown();
            assertThat(releaseProvider.await(5, TimeUnit.SECONDS)).isTrue();
            return List.of();
        });

        ProjectConnectivityServiceImpl service = new ProjectConnectivityServiceImpl(
            projectRepository,
            mock(ProjectConnectivityRepository.class),
            placeRepository,
            mock(ContentVersionService.class),
            mock(ProjectPublicVisibilityPolicy.class),
            provider,
            transactions,
            mock(ProjectPublicCacheEvictionPublisher.class)
        );
        ConnectivityProviderSearchRequest request = ConnectivityProviderSearchRequest.builder()
            .category(ProjectConnectivityCategory.TRANSIT)
            .radiusMeters(3000)
            .build();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> call = executor.submit(() -> service.providerSearch(31L, request));
            assertThat(providerStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertPoolIdle("connectivity");
            releaseProvider.countDown();
            call.get(5, TimeUnit.SECONDS);
            awaitPoolIdle();
        } finally {
            releaseProvider.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void providerEntryPointsSuspendAnyCallerTransaction() throws Exception {
        assertNotSupported(PublicReviewServiceImpl.class.getMethod(
            "syncGoogleReviews", PublicReviewTargetType.class, Long.class, Long.class));
        assertNotSupported(PublicReviewServiceImpl.class.getMethod(
            "searchGooglePlaces", Long.class, String.class));
        assertNotSupported(ProjectConnectivityServiceImpl.class.getMethod(
            "providerSearch", Long.class, ConnectivityProviderSearchRequest.class));
    }

    private static void assertNotSupported(java.lang.reflect.Method method) {
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.NOT_SUPPORTED);
    }

    private static void assertPoolIdle(String flow) {
        int active = dataSource.getHikariPoolMXBean().getActiveConnections();
        int idle = dataSource.getHikariPoolMXBean().getIdleConnections();
        int pending = dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection();
        System.out.printf("delayed-provider flow=%s active=%d idle=%d pending=%d%n",
            flow, active, idle, pending);
        assertThat(active).isZero();
        assertThat(idle).isEqualTo(1);
        assertThat(pending).isZero();
    }

    private static void awaitPoolIdle() throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (System.nanoTime() < deadline) {
            if (dataSource.getHikariPoolMXBean().getActiveConnections() == 0) return;
            Thread.sleep(10);
        }
        assertThat(dataSource.getHikariPoolMXBean().getActiveConnections()).isZero();
    }
}
