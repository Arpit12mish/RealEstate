package com.brandPitara.sfs.home.service.impl;

import com.brandPitara.sfs.SfsApplication;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builderhighlight.entity.BuilderHighlightItemEntity;
import com.brandPitara.sfs.builderhighlight.enums.*;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.distributor.entity.DistributorEntity;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.entity.PromoBannerEntity;
import com.brandPitara.sfs.home.dto.HomeFeedRequest;
import com.brandPitara.sfs.home.entity.*;
import com.brandPitara.sfs.home.enums.HomeSectionItemType;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.repository.HomeSectionConfigRepository;
import com.brandPitara.sfs.home.repository.HomeSectionItemRepository;
import com.brandPitara.sfs.home.repository.ProjectPlanRepository;
import com.brandPitara.sfs.home.service.HomeFeedService;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.SectionContext;
import com.brandPitara.sfs.instagram.entity.InstagramReelEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import com.brandPitara.sfs.project.enums.ProjectMediaType;
import com.brandPitara.sfs.project.enums.PropertyType;
import com.brandPitara.sfs.projectmeter.entity.ProjectMeterSnapshotEntity;
import com.brandPitara.sfs.projectcompare.dto.request.ProjectComparisonRequest;
import com.brandPitara.sfs.projectcompare.service.ProjectComparisonService;
import com.brandPitara.sfs.buildercredibility.service.BuilderCredibilityService;
import com.brandPitara.sfs.home.service.section.impl.ComparePropertiesSectionLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

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
        "jwt.secret=release-2-test-secret-release-2-test-secret",
        "jwt.expiration.ms=3600000",
        "dashboard.jwt.secret=release-2-dashboard-test-secret-release-2",
        "dashboard.jwt.access-expiration-ms=3600000",
        "dashboard.refresh.expiration-days=7",
        "elasticsearch.url=http://localhost:9200",
        "elasticsearch.api-key=release-2-test-key",
        "app.media.s3.bucket=release-2-test",
        "app.media.s3.region=ap-south-1",
        "aws.credentials.access-key=release-2-test",
        "aws.credentials.secret-key=release-2-test",
        "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.springframework.jdbc=OFF",
        "app.logging.path=target/test-logs"
    }
)
@ActiveProfiles({"test", "local-fake-otp"})
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class HomeFeedPerformanceIntegrationTest {

    private static final List<HomeSectionType> PRODUCTION_SECTIONS = List.of(
        HomeSectionType.TOP_PROJECTS,
        HomeSectionType.TOP_BUILDERS,
        HomeSectionType.CONNECTED_BRANDS,
        HomeSectionType.TOP_CATEGORIES,
        HomeSectionType.ARCHITECTS_AND_DESIGNERS,
        HomeSectionType.ARCHITECTS,
        HomeSectionType.DESIGNERS,
        HomeSectionType.TOP_DISTRIBUTORS,
        HomeSectionType.PROJECT_PLAN,
        HomeSectionType.PROJECT_ANALYTICS,
        HomeSectionType.NEARBY_LISTINGS,
        HomeSectionType.INSTAGRAM_REELS,
        HomeSectionType.TRENDING_CITIES,
        HomeSectionType.SMART_CALCULATORS,
        HomeSectionType.COMPANIES,
        HomeSectionType.FEATURED_CAROUSEL,
        HomeSectionType.GENERIC_CARDS,
        HomeSectionType.COMPARE_PROPERTIES,
        HomeSectionType.BUILDER_CREDIBILITY_CARDS
    );

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("home_feed_release_2")
        .withUsername("home_test")
        .withPassword("home_test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired private HomeFeedService homeFeedService;
    @Autowired private ProjectComparisonService projectComparisonService;
    @Autowired private BuilderCredibilityService builderCredibilityService;
    @Autowired private HomeSectionConfigRepository configRepository;
    @Autowired private HomeSectionItemRepository itemRepository;
    @Autowired private ProjectPlanRepository projectPlanRepository;
    @Autowired private List<HomeSectionLoader> loaders;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private javax.sql.DataSource dataSource;
    @MockitoSpyBean private ComparePropertiesSectionLoader comparePropertiesSectionLoader;

    private Long cityId;
    private Long builderId;

    @BeforeEach
    void seedFullyConfiguredHome() {
        if (Boolean.TRUE.equals(jdbcTemplate.queryForObject(
            "select exists(select 1 from category where id = 0)", Boolean.class))) {
            cityId = jdbcTemplate.queryForObject("select id from city order by id limit 1", Long.class);
            builderId = jdbcTemplate.queryForObject("select id from builder order by priority, id limit 1", Long.class);
            return;
        }
        jdbcTemplate.update("""
            insert into category (id, name, slug, priority, active, created_at, updated_at)
            values (0, 'All Home', 'all-home', 0, false, now(), now())
            """);

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            CategoryEntity homeCategory = entityManager.find(CategoryEntity.class, 0L);
            CategoryEntity publicCategory = CategoryEntity.builder()
                .name("Residential").slug("residential").priority(1).active(true).build();
            entityManager.persist(publicCategory);

            CityEntity city = CityEntity.builder()
                .name("Gurugram").slug("gurugram").active(true).homepageFeatured(true)
                .displayOrder(1).growthPercent(12.5).coverImageUrl("https://img/city.jpg").build();
            entityManager.persist(city);
            cityId = city.getId();

            List<BuilderEntity> builders = new ArrayList<>();
            List<ProjectEntity> projects = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                BuilderEntity builder = BuilderEntity.builder()
                    .name("Builder " + i).logoUrl("https://img/builder-" + i + ".png")
                    .city(city).active(true).published(true).deleted(false).priority(i).build();
                entityManager.persist(builder);
                builders.add(builder);
                if (i == 0) builderId = builder.getId();

                ProjectEntity project = ProjectEntity.builder()
                    .builder(builder).city(city).name("Project " + i).slug("project-" + i)
                    .addressLine("Sector " + i).latitude(28.45 + i / 1000.0).longitude(77.02 + i / 1000.0)
                    .priceMin(10_000_000L + i).priceMax(20_000_000L + i)
                    .propertyTypes(new HashSet<>(Set.of(PropertyType.APARTMENT)))
                    .active(true).published(true).deleted(false).priority(i).reviewStatus(ReviewStatus.APPROVED).build();
                entityManager.persist(project);
                projects.add(project);
                entityManager.persist(ProjectMediaEntity.builder()
                    .project(project).mediaType(ProjectMediaType.IMAGE).url("https://img/project-" + i + ".jpg")
                    .sortOrder(0).active(true).deleted(false).build());
                entityManager.persist(ProjectMeterSnapshotEntity.builder()
                    .project(project).constructionProgressPercent(60 + i).delayDays(i)
                    .amenityScore(70).verified(true).computedAt(OffsetDateTime.now()).build());

                entityManager.persist(BuilderHighlightItemEntity.builder()
                    .builder(builder).project(project).city(city)
                    .highlightType(BuilderHighlightType.BUILDER_UPDATE)
                    .sourceType(BuilderHighlightSourceType.SFS_EDITORIAL)
                    .mediaType(BuilderHighlightMediaType.IMAGE)
                    .title("Highlight " + i).status(BuilderHighlightStatus.PUBLISHED)
                    .publicVisible(true).active(true).build());
            }

            List<BrandEntity> brands = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                BrandEntity brand = BrandEntity.builder()
                    .name("Brand " + i).slug("brand-" + i).logoUrl("https://img/brand-" + i + ".png")
                    .active(true).published(true).deleted(false).priority(i).build();
                entityManager.persist(brand);
                brands.add(brand);
            }

            for (int i = 0; i < 6; i++) {
                entityManager.persist(CompanyEntity.builder()
                    .name("Company " + i).slug("company-" + i)
                    .companyType(i % 2 == 0 ? "ARCHITECT" : "DESIGNER")
                    .logoUrl("https://img/company-" + i + ".png")
                    .coverImageUrl("https://img/company-cover-" + i + ".jpg")
                    .city(city).active(true).published(true).deleted(false).priority(i).build());
            }

            for (int i = 0; i < 6; i++) {
                entityManager.persist(InstagramReelEntity.builder()
                    .instagramMediaId("media-" + i).title("Reel " + i)
                    .instagramUrl("https://instagram/reel-" + i)
                    .cachedThumbnailUrl("https://cdn/reel-" + i + ".jpg")
                    .publishedAt(OffsetDateTime.now().minusDays(i)).active(true).deleted(false).build());
                entityManager.persist(DistributorEntity.builder()
                    .name("Distributor " + i).cityId(city.getId()).active(true).deleted(false).build());
            }

            for (int i = 0; i < 20; i++) {
                entityManager.persist(CategoryEntity.builder()
                    .name("Category " + i).slug("category-" + i).priority(i).active(true).build());
                entityManager.persist(ProjectPlanEntity.builder()
                    .category(homeCategory).title("Plan " + i).description("Plan description")
                    .imageUrl("https://img/plan-" + i + ".jpg").priority(i).active(true).deleted(false).build());
                entityManager.persist(PromoBannerEntity.builder()
                    .category(homeCategory).title("Banner " + i).imageUrl("https://img/banner-" + i + ".jpg")
                    .slotKey("HERO").priority(i).active(true).build());
            }

            Map<HomeSectionType, HomeSectionConfigEntity> configs = new EnumMap<>(HomeSectionType.class);
            int order = 1;
            for (HomeSectionType type : PRODUCTION_SECTIONS) {
                HomeSectionConfigEntity config = HomeSectionConfigEntity.builder()
                    .homeCategory(homeCategory).sectionType(type).title(type.name())
                    .subtitle(type.name() + " subtitle").enabled(true).sortOrder(order++)
                    .maxItems(4)
                    .param1(type == HomeSectionType.COMPARE_PROPERTIES ? "https://cdn/compare.json"
                        : type == HomeSectionType.GENERIC_CARDS ? "release-2" : null)
                    .build();
                entityManager.persist(config);
                configs.put(type, config);
            }

            for (int i = 0; i < 20; i++) {
                BrandEntity brand = brands.get(i % brands.size());
                entityManager.persist(HomeSectionItemEntity.builder()
                    .homeCategory(homeCategory).config(configs.get(HomeSectionType.COMPANIES))
                    .sectionType(HomeSectionType.COMPANIES).itemType(HomeSectionItemType.BRAND)
                    .refId(brand.getId()).sortOrder(i).active(true).deleted(false).build());
                entityManager.persist(HomeSectionItemEntity.builder()
                    .homeCategory(homeCategory).config(configs.get(HomeSectionType.GENERIC_CARDS))
                    .sectionType(HomeSectionType.GENERIC_CARDS).itemType(HomeSectionItemType.PROJECT)
                    .refId(projects.get(i % projects.size()).getId()).imageUrl("https://img/generic-" + i + ".jpg")
                    .groupKey("release-2").sortOrder(i).active(true).deleted(false).build());
            }

            for (int i = 0; i < 3; i++) {
                entityManager.persist(FeaturedCarouselConfigEntity.builder()
                    .cityId(city.getId()).categoryId(homeCategory.getId())
                    .variant(List.of("TALL", "SMALL_TOP", "SMALL_BOTTOM").get(i))
                    .position(i + 1).title("Featured " + i).imageUrl("https://img/featured-" + i + ".jpg")
                    .active(true).priority(i).build());
            }

            entityManager.persist(PromoBannerSlotConfigEntity.builder()
                .screen(com.brandPitara.sfs.feed.enums.FeedScreen.HOME)
                .homeCategory(homeCategory).slotKey("HERO").maxItems(4).priority(1).active(true).build());
            entityManager.flush();
        });
    }

    @Test
    void fullyConfiguredHomeReportsSqlTimingPoolAndPayloadBaseline() throws Exception {
        Statistics statistics = statistics();
        statistics.clear();
        PoolSample pool = samplePool(() -> homeFeedService.getHome(request()));
        var response = pool.result;
        long statements = statistics.getPrepareStatementCount();
        int payloadBytes = objectMapper.writeValueAsBytes(response).length;

        Map<HomeSectionType, Long> perSection = measurePerSectionQueries();
        System.out.printf(
            "home-release2 totalSql=%d durationMs=%d activeSampleMs=%d peakActive=%d peakPending=%d payloadBytes=%d sections=%s%n",
            statements, pool.durationMillis, pool.activeSampleMillis, pool.peakActive, pool.peakPending,
            payloadBytes, perSection);

        assertThat(response.getSections()).isNotEmpty();
        assertThat(response.getSections()).extracting(section -> section.getType())
            .containsAll(PRODUCTION_SECTIONS);
        assertThat(statements).isLessThanOrEqualTo(54);
        assertThat(perSection.get(HomeSectionType.BUILDER_CREDIBILITY_CARDS)).isLessThanOrEqualTo(6);
        assertThat(poolAfter().active()).isZero();
        assertThat(poolAfter().pending()).isZero();
    }

    @Test
    void comparisonForTwoToFourProjectsHasConstantBoundedSql() throws Exception {
        List<Long> projectIds = jdbcTemplate.queryForList(
            "select id from project order by priority, id limit 4", Long.class);
        List<Long> queryCounts = new ArrayList<>();
        for (int projectCount = 2; projectCount <= 4; projectCount++) {
            Statistics statistics = statistics();
            statistics.clear();
            long started = System.nanoTime();
            ProjectComparisonRequest request = new ProjectComparisonRequest();
            request.setProjectIds(projectIds.subList(0, projectCount));
            var response = projectComparisonService.compare(request);
            long durationMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();
            long statements = statistics.getPrepareStatementCount();
            queryCounts.add(statements);
            int payloadBytes = objectMapper.writeValueAsBytes(response).length;
            System.out.printf("comparison-release2 projects=%d totalSql=%d durationMs=%d payloadBytes=%d%n",
                projectCount, statements, durationMillis, payloadBytes);
            assertThat(response.getProjects()).hasSize(projectCount);
            assertThat(statements).isLessThanOrEqualTo(16);
        }
        assertThat(queryCounts).containsOnly(queryCounts.get(0));
    }

    @Test
    void builderCredibilityCardsReportQueryBaseline() {
        Statistics statistics = statistics();
        statistics.clear();
        var oneCard = builderCredibilityService.publicListCredibilityCards(cityId, 1);
        long oneBuilderStatements = statistics.getPrepareStatementCount();
        statistics.clear();
        var cards = builderCredibilityService.publicListCredibilityCards(cityId, 4);
        long statements = statistics.getPrepareStatementCount();

        System.out.printf("builder-credibility-release2 builders=4 totalSql=%d%n", statements);
        assertThat(oneCard).hasSize(1);
        assertThat(cards).hasSize(4);
        assertThat(statements).isEqualTo(oneBuilderStatements).isLessThanOrEqualTo(6);
    }

    @Test
    void delayedNonDatabaseSectionRetainsNoConnection() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean transactionActive = new AtomicBoolean(true);
        doAnswer(invocation -> {
            transactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
            entered.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            return invocation.callRealMethod();
        }).when(comparePropertiesSectionLoader).load(any(), any());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> future = executor.submit(() -> homeFeedService.getHome(request()));
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(transactionActive).isFalse();
            assertThat(poolAfter().active()).isZero();
            assertThat(poolAfter().pending()).isZero();
            release.countDown();
            future.get(10, TimeUnit.SECONDS);
            assertThat(poolAfter().active()).isZero();
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void tenClientsCompleteWithPoolSizeThreeAndReturnAllConnections() throws Exception {
        int clientCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(clientCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
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
        }, "home-concurrency-pool-sampler");
        sampler.start();
        try {
            for (int i = 0; i < clientCount; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return homeFeedService.getHome(request());
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                assertThat(future.get(30, TimeUnit.SECONDS)).isNotNull();
            }
        } finally {
            sampling.set(false);
            sampler.join(2000);
            executor.shutdownNow();
        }
        System.out.printf("home-concurrency-release2 clients=10 pool=3 peakActive=%d peakPending=%d timeouts=0%n",
            peakActive.get(), peakPending.get());
        assertThat(peakActive).hasValueLessThanOrEqualTo(3);
        assertThat(poolAfter().active()).isZero();
        assertThat(poolAfter().pending()).isZero();
    }

    @Test
    void databaseQueriesEnforceConfiguredLimits() {
        Long genericConfigId = config(HomeSectionType.GENERIC_CARDS).getId();
        var curated = itemRepository
            .findByConfig_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(
                genericConfigId, PageRequest.of(0, 4));
        var plans = projectPlanRepository
            .findByCategory_IdAndBuilderIsNullAndActiveTrueAndDeletedFalseOrderByPriorityAscIdAsc(
                0L, PageRequest.of(0, 4));

        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from home_section_item where config_id = ?", Long.class, genericConfigId))
            .isGreaterThan(4);
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from project_plan where category_id = 0", Long.class)).isGreaterThan(4);
        assertThat(curated).hasSize(4);
        assertThat(plans).hasSize(4);
    }

    @Test
    void osivDisabledResponseSerializesAfterServiceReturns() throws Exception {
        var response = homeFeedService.getHome(request());
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(objectMapper.writeValueAsBytes(response)).isNotEmpty();
    }

    private Map<HomeSectionType, Long> measurePerSectionQueries() {
        Map<HomeSectionType, HomeSectionLoader> loaderByType = new EnumMap<>(HomeSectionType.class);
        loaders.forEach(loader -> loaderByType.put(loader.supports(), loader));
        Map<HomeSectionType, Long> counts = new LinkedHashMap<>();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setReadOnly(true);
        for (HomeSectionType type : PRODUCTION_SECTIONS) {
            HomeSectionLoader loader = loaderByType.get(type);
            if (loader == null) continue;
            long count = tx.execute(status -> {
                HomeSectionConfigEntity config = configRepository
                    .findByHomeCategory_IdAndEnabledTrueOrderBySortOrderAscIdAsc(0L).stream()
                    .filter(candidate -> candidate.getSectionType() == type).findFirst().orElseThrow();
                Statistics stats = statistics();
                stats.clear();
                loader.load(config, context());
                return stats.getPrepareStatementCount();
            });
            counts.put(type, count);
        }
        return counts;
    }

    private PoolSample samplePool(Callable<com.brandPitara.sfs.home.dto.HomeFeedResponse> action) throws Exception {
        HikariDataSource hikari = dataSource.unwrap(HikariDataSource.class);
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicInteger peakActive = new AtomicInteger();
        AtomicInteger peakPending = new AtomicInteger();
        AtomicInteger activeSamples = new AtomicInteger();
        Thread sampler = new Thread(() -> {
            while (running.get()) {
                int active = hikari.getHikariPoolMXBean().getActiveConnections();
                int pending = hikari.getHikariPoolMXBean().getThreadsAwaitingConnection();
                peakActive.accumulateAndGet(active, Math::max);
                peakPending.accumulateAndGet(pending, Math::max);
                if (active > 0) activeSamples.incrementAndGet();
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "home-pool-sampler");
        sampler.start();
        long start = System.nanoTime();
        var result = action.call();
        long duration = Duration.ofNanos(System.nanoTime() - start).toMillis();
        running.set(false);
        sampler.join(2000);
        return new PoolSample(result, duration, activeSamples.get(), peakActive.get(), peakPending.get());
    }

    private PoolState poolAfter() {
        HikariDataSource hikari;
        try {
            hikari = dataSource.unwrap(HikariDataSource.class);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return new PoolState(
            hikari.getHikariPoolMXBean().getActiveConnections(),
            hikari.getHikariPoolMXBean().getThreadsAwaitingConnection());
    }

    private Statistics statistics() {
        return entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
    }

    private HomeFeedRequest request() {
        return HomeFeedRequest.builder().cityId(cityId).categoryId(0L).builderId(builderId).build();
    }

    private SectionContext context() {
        return SectionContext.builder().cityId(cityId).categoryId(0L).builderId(builderId)
            .resolvedCityName("Gurugram").build();
    }

    private HomeSectionConfigEntity config(HomeSectionType type) {
        return configRepository.findByHomeCategory_IdAndEnabledTrueOrderBySortOrderAscIdAsc(0L).stream()
            .filter(candidate -> candidate.getSectionType() == type)
            .findFirst()
            .orElseThrow();
    }

    private record PoolSample(
        com.brandPitara.sfs.home.dto.HomeFeedResponse result,
        long durationMillis,
        int activeSampleMillis,
        int peakActive,
        int peakPending
    ) {}

    private record PoolState(int active, int pending) {}
}
