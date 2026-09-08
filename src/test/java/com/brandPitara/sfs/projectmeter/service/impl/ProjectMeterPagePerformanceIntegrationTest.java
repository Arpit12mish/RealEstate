package com.brandPitara.sfs.projectmeter.service.impl;

import com.brandPitara.sfs.SfsApplication;
import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.entity.UserFavoriteEntity;
import com.brandPitara.sfs.enums.FavoriteTargetType;
import com.brandPitara.sfs.enums.OnboardingStatus;
import com.brandPitara.sfs.enums.Role;
import com.brandPitara.sfs.project.entity.*;
import com.brandPitara.sfs.project.enums.*;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterDetailResponse;
import com.brandPitara.sfs.projectmeter.entity.*;
import com.brandPitara.sfs.projectmeter.enums.*;
import com.brandPitara.sfs.projectmeter.service.ProjectMeterService;
import com.brandPitara.sfs.security.identity.MobileAuthenticationUserSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    classes = SfsApplication.class,
    properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.flyway.enabled=false",
        "sfs.local-staging.fake-otp.enabled=true",
        "spring.datasource.hikari.maximum-pool-size=3",
        "spring.datasource.hikari.minimum-idle=3",
        "spring.datasource.hikari.connection-timeout=1000",
        "spring.task.scheduling.enabled=false",
        "dashboard.seed.enabled=false",
        "dashboard.seed.update-passwords=false",
        "jwt.secret=meter-page-test-secret-meter-page-test-secret",
        "jwt.expiration.ms=3600000",
        "dashboard.jwt.secret=meter-dashboard-test-secret-meter-dashboard-test",
        "dashboard.jwt.access-expiration-ms=3600000",
        "dashboard.refresh.expiration-days=7",
        "elasticsearch.url=http://localhost:9200",
        "elasticsearch.api-key=meter-page-test-key",
        "app.media.s3.bucket=meter-page-test",
        "app.media.s3.region=ap-south-1",
        "aws.credentials.access-key=meter-page-test",
        "aws.credentials.secret-key=meter-page-test",
        "logging.level.org.hibernate.SQL=OFF",
        "sfs.log.dir=target/test-logs"
    }
)
@ActiveProfiles({"test", "local-fake-otp"})
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProjectMeterPagePerformanceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("project_meter_page")
        .withUsername("meter_test")
        .withPassword("meter_test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired private ProjectMeterService projectMeterService;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private javax.sql.DataSource dataSource;

    private Long projectId;
    private Long userId;

    @BeforeEach
    void seedMeterPage() {
        SecurityContextHolder.clearContext();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            List<ProjectEntity> existingProjects = entityManager.createQuery(
                "select p from ProjectEntity p where p.slug = :slug", ProjectEntity.class)
                .setParameter("slug", "meter-project")
                .getResultList();
            if (!existingProjects.isEmpty()) {
                projectId = existingProjects.get(0).getId();
                userId = entityManager.createQuery(
                    "select u.id from User u where u.phoneNumber = :phone", Long.class)
                    .setParameter("phone", "9000000001")
                    .getSingleResult();
                return;
            }
            CityEntity city = CityEntity.builder().name("Gurugram").slug("meter-gurugram").active(true).build();
            entityManager.persist(city);
            BuilderEntity builder = BuilderEntity.builder().name("Meter Builder").city(city)
                .active(true).published(true).deleted(false).build();
            entityManager.persist(builder);
            ProjectEntity project = ProjectEntity.builder()
                .builder(builder).city(city).name("Meter Project").slug("meter-project")
                .description("Meter page project").addressLine("Sector 1")
                .latitude(28.45).longitude(77.02).priceMin(10_000_000L).priceMax(20_000_000L)
                .monthlyEmiMin(50_000L).monthlyEmiMax(100_000L).averagePricePerSqft(12_000L)
                .status(ProjectStatus.UNDER_CONSTRUCTION)
                .propertyTypes(new HashSet<>(Set.of(PropertyType.APARTMENT)))
                .active(true).published(true).deleted(false).reviewStatus(ReviewStatus.APPROVED).build();
            entityManager.persist(project);
            projectId = project.getId();

            entityManager.persist(ProjectMediaEntity.builder().project(project).mediaType(ProjectMediaType.IMAGE)
                .url("https://img/project.jpg").sortOrder(0).active(true).deleted(false).build());
            entityManager.persist(ProjectMediaEntity.builder().project(project).mediaType(ProjectMediaType.BROCHURE_PDF)
                .url("https://docs/brochure.pdf").sortOrder(1).active(true).deleted(false).build());
            entityManager.persist(ProjectMeterSnapshotEntity.builder().project(project)
                .constructionProgressPercent(60).delayDays(0).amenityScore(70).locationScore(8.0)
                .launchPrice(10_000_000L).currentPrice(12_000_000L).averageAreaPrice(12_000L)
                .priceAppreciationPercent(20.0).estimatedCostTotal(15_000_000L)
                .verified(true).computedAt(OffsetDateTime.now()).build());
            entityManager.persist(ProjectConstructionStageEntity.builder().project(project)
                .stageCode(ProjectConstructionStageCode.FOUNDATION).stageLabel("Foundation")
                .displayOrder(1).weightPercent(100).progressPercent(60)
                .status(ProjectStageStatus.IN_PROGRESS).verified(true).build());
            entityManager.persist(compliance(project, ProjectComplianceGroup.LAND_LICENSE, "LAND"));
            entityManager.persist(compliance(project, ProjectComplianceGroup.APPROVAL_NOC, "NOC"));
            entityManager.persist(ProjectPriceHistoryEntity.builder().project(project).yearLabel("2026")
                .projectPrice(12_000_000L).averageAreaPrice(12_000L).displayOrder(1).verified(true).build());
            entityManager.persist(ProjectPaymentMilestoneEntity.builder().project(project)
                .milestoneCode("BOOKING").milestoneLabel("Booking").percentageValue(10)
                .displayOrder(1).active(true).build());
            entityManager.persist(ProjectCostBreakdownEntity.builder().project(project)
                .landCost(5_000_000L).constructionCost(8_000_000L).totalCost(13_000_000L).verified(true).build());
            entityManager.persist(ProjectLandUtilizationEntity.builder().project(project)
                .totalLandAreaSqm(1000.0).residentialAreaSqm(600.0).openAreaSqm(400.0).build());
            entityManager.persist(ProjectLocationScoreEntity.builder().project(project)
                .metroScore(8.0).educationScore(7.0).finalScore(7.5).verified(true).build());
            entityManager.persist(ProjectAmenityProgressEntity.builder().project(project)
                .amenityCode("POOL").amenityLabel("Pool").status(ProjectAmenityStatus.COMPLETED)
                .progressPercent(100).weightPercent(100).displayOrder(1)
                .category(ProjectAmenityCategory.LIFESTYLE).categoryDisplayOrder(1)
                .active(true).publicVisible(true).available(true).verified(true).build());

            ProjectConnectivityEntity connectivity = ProjectConnectivityEntity.builder().project(project)
                .title("Connectivity").mapImageUrl("https://img/map.jpg").defaultRadiusMeters(5000)
                .searchEnabled(true).active(true).deleted(false).build();
            entityManager.persist(connectivity);
            entityManager.persist(ProjectConnectivityPlaceEntity.builder().project(project).connectivity(connectivity)
                .placeName("Metro").placeType(ProjectConnectivityType.METRO)
                .category(ProjectConnectivityCategory.TRANSIT).latitude(28.46).longitude(77.03)
                .distanceMeters(500).distanceLabel("500 m").sortOrder(1)
                .active(true).deleted(false).verified(true).featured(true).build());
            entityManager.persist(ProjectMasterPlanEntity.builder().project(project).title("Master Plan")
                .masterPlanImageUrl("https://img/master.jpg").totalUnits(100).active(true).deleted(false).build());
            entityManager.persist(ProjectFloorPlanEntity.builder().project(project).title("2 BHK")
                .imageUrl("https://img/2bhk.jpg").unitConfigurationType(UnitConfigurationType.BHK_2)
                .sortOrder(1).active(true).deleted(false).featured(true).build());

            User user = new User();
            user.setPhoneNumber("9000000001");
            user.setRole(Role.CUSTOMER);
            user.setVerified(true);
            user.setOnboardingStatus(OnboardingStatus.CUSTOMER_READY);
            entityManager.persist(user);
            userId = user.getId();
            entityManager.persist(UserFavoriteEntity.builder().user(user).targetType(FavoriteTargetType.PROJECT)
                .targetId(projectId).build());
            entityManager.flush();
        });
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anonymousMeterPageStaysWithinTwentyStatementsAndSerializesDetached() throws Exception {
        RouteSample sample = measure(() -> projectMeterService.publicGetMeterDetail(projectId));

        assertCompleteContract(sample.response());
        assertThat(sample.response().getProject().getIsFavorite()).isFalse();
        assertThat(sample.response().getProject().getFavoriteCount()).isEqualTo(1L);
        assertThat(sample.statements()).isLessThanOrEqualTo(20);
        assertPoolReleased();
        System.out.printf("project-meter anonymousSql=%d durationMs=%d payloadBytes=%d%n",
            sample.statements(), sample.durationMillis(), sample.payloadBytes());
    }

    @Test
    void authenticatedMeterPageUsesSnapshotUserIdAndStaysWithinTwentyTwoStatements() throws Exception {
        authenticate();
        RouteSample sample = measure(() -> projectMeterService.publicGetMeterDetail(projectId));

        assertCompleteContract(sample.response());
        assertThat(sample.response().getProject().getIsFavorite()).isTrue();
        assertThat(sample.statements()).isLessThanOrEqualTo(22);
        assertPoolReleased();
        System.out.printf("project-meter authenticatedSql=%d durationMs=%d payloadBytes=%d%n",
            sample.statements(), sample.durationMillis(), sample.payloadBytes());
    }

    @Test
    void sixConcurrentRequestsCompleteWithPoolOfThree() throws Exception {
        assertConcurrentRequestsComplete(6);
    }

    @Test
    void tenConcurrentRequestsCompleteWithPoolOfThree() throws Exception {
        assertConcurrentRequestsComplete(10);
    }

    private void assertConcurrentRequestsComplete(int clientCount) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(clientCount);
        CountDownLatch start = new CountDownLatch(1);
        HikariDataSource hikari = dataSource.unwrap(HikariDataSource.class);
        AtomicBoolean sampling = new AtomicBoolean(true);
        AtomicInteger peakActive = new AtomicInteger();
        AtomicInteger peakPending = new AtomicInteger();
        Thread sampler = new Thread(() -> {
            while (sampling.get()) {
                peakActive.accumulateAndGet(hikari.getHikariPoolMXBean().getActiveConnections(), Math::max);
                peakPending.accumulateAndGet(hikari.getHikariPoolMXBean().getThreadsAwaitingConnection(), Math::max);
                Thread.onSpinWait();
            }
        }, "project-meter-pool-sampler");
        List<Future<ProjectMeterDetailResponse>> futures = new java.util.ArrayList<>();
        sampler.start();
        try {
            for (int i = 0; i < clientCount; i++) {
                futures.add(executor.submit(() -> {
                    SecurityContextHolder.clearContext();
                    start.await();
                    return projectMeterService.publicGetMeterDetail(projectId);
                }));
            }
            start.countDown();
            for (Future<ProjectMeterDetailResponse> future : futures) {
                assertCompleteContract(future.get(30, TimeUnit.SECONDS));
            }
        } finally {
            sampling.set(false);
            sampler.join(2000);
            executor.shutdownNow();
        }
        assertThat(peakActive).hasValueLessThanOrEqualTo(3);
        assertPoolReleased();
        System.out.printf("project-meter concurrency=%d pool=3 peakActive=%d peakPending=%d timeouts=0%n",
            clientCount, peakActive.get(), peakPending.get());
    }

    private RouteSample measure(Callable<ProjectMeterDetailResponse> action) throws Exception {
        Statistics statistics = statistics();
        statistics.clear();
        long started = System.nanoTime();
        ProjectMeterDetailResponse response = action.call();
        long durationMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();
        byte[] payload = objectMapper.writeValueAsBytes(response);
        return new RouteSample(response, statistics.getPrepareStatementCount(), durationMillis, payload.length);
    }

    private void assertCompleteContract(ProjectMeterDetailResponse response) {
        assertThat(response.getProject()).isNotNull();
        assertThat(response.getProject().getBrochureUrl()).isEqualTo("https://docs/brochure.pdf");
        assertThat(response.getMedia().getItems()).hasSize(1);
        assertThat(response.getSummary()).isNotNull();
        assertThat(response.getConstruction().getStages()).hasSize(1);
        assertThat(response.getLandLicense().getItems()).hasSize(1);
        assertThat(response.getApprovals().getItems()).hasSize(1);
        assertThat(response.getMasterPlan()).isNotNull();
        assertThat(response.getFloorPlanGroups()).hasSize(1);
        assertThat(response.getConnectivity().getPlaces()).hasSize(1);
    }

    private void authenticate() {
        MobileAuthenticationUserSnapshot principal =
            new MobileAuthenticationUserSnapshot(userId, "9000000001", Role.CUSTOMER, true);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private void assertPoolReleased() {
        HikariDataSource hikari;
        try {
            hikari = dataSource.unwrap(HikariDataSource.class);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        assertThat(hikari.getHikariPoolMXBean().getActiveConnections()).isZero();
        assertThat(hikari.getHikariPoolMXBean().getThreadsAwaitingConnection()).isZero();
    }

    private Statistics statistics() {
        return entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
    }

    private ProjectComplianceItemEntity compliance(
        ProjectEntity project,
        ProjectComplianceGroup group,
        String key
    ) {
        return ProjectComplianceItemEntity.builder().project(project).itemGroup(group).itemKey(key)
            .itemLabel(key).status(ProjectComplianceStatus.APPROVED).displayOrder(1).verified(true).build();
    }

    private record RouteSample(
        ProjectMeterDetailResponse response,
        long statements,
        long durationMillis,
        int payloadBytes
    ) {
    }
}
