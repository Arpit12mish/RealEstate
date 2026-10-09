package com.brandPitara.sfs.marketplace;

import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.marketplace.dto.DealerWorkerLinkCreateRequest;
import com.brandPitara.sfs.dashboard.marketplace.dto.OfferingsReplaceRequest;
import com.brandPitara.sfs.dashboard.marketplace.dto.OpeningHoursReplaceRequest;
import com.brandPitara.sfs.dashboard.marketplace.dto.RecommendationReviewRequest;
import com.brandPitara.sfs.dashboard.marketplace.service.DashboardDealerService;
import com.brandPitara.sfs.dashboard.marketplace.service.impl.DashboardDealerServiceImpl;
import com.brandPitara.sfs.dashboard.marketplace.service.impl.DashboardWorkerServiceImpl;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.exception.GlobalExceptionHandler;
import com.brandPitara.sfs.marketplace.controller.publicapi.DealerPublicController;
import com.brandPitara.sfs.marketplace.controller.publicapi.WorkerPublicController;
import com.brandPitara.sfs.marketplace.dto.DealerReviewCreateRequest;
import com.brandPitara.sfs.marketplace.dto.DealerReviewModerationRequest;
import com.brandPitara.sfs.marketplace.entity.BusinessMediaEntity;
import com.brandPitara.sfs.marketplace.enums.BusinessOfferingType;
import com.brandPitara.sfs.marketplace.enums.BusinessReviewStatus;
import com.brandPitara.sfs.marketplace.enums.WorkerRecommendationStatus;
import com.brandPitara.sfs.marketplace.repository.DealerRepository;
import com.brandPitara.sfs.marketplace.service.DealerCardAssembler;
import com.brandPitara.sfs.marketplace.service.DealerReviewService;
import com.brandPitara.sfs.marketplace.service.WorkerCardAssembler;
import com.brandPitara.sfs.marketplace.service.impl.DealerPublicServiceImpl;
import com.brandPitara.sfs.marketplace.service.impl.DealerReviewServiceImpl;
import com.brandPitara.sfs.marketplace.service.impl.WorkerPublicServiceImpl;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.provider.dto.WorkerRateRequest;
import com.brandPitara.sfs.provider.dto.WorkerRatesReplaceRequest;
import com.brandPitara.sfs.provider.dto.WorkerServicesReplaceRequest;
import com.brandPitara.sfs.provider.entity.ProviderProfileEntity;
import com.brandPitara.sfs.provider.enums.ProviderRateType;
import com.brandPitara.sfs.provider.enums.ProviderRateUnit;
import com.brandPitara.sfs.provider.repository.ProviderProfileRepository;
import com.brandPitara.sfs.provider.service.WorkerProfileMutationService;
import com.brandPitara.sfs.provider.service.impl.WorkerProfileMutationServiceImpl;
import com.brandPitara.sfs.repository.CategoryRepository;
import com.brandPitara.sfs.marketplace.service.DealerPublicService;
import com.brandPitara.sfs.marketplace.service.WorkerPublicService;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the real Flyway history (B134 + forward migrations incl. V174) and validates the Hibernate
 * mapping against it, then exercises the public dealer/worker contracts, visibility rules,
 * relationship integrity, review moderation and query counts against PostgreSQL.
 */
@SpringBootTest(
        classes = MarketplaceDetailsPostgresIntegrationTest.TestApplication.class,
        properties = {
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "spring.jpa.properties.hibernate.generate_statistics=true",
                "sfs.log.dir=target/test-logs"
        }
)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MarketplaceDetailsPostgresIntegrationTest {

    /** Wednesday 2026-10-07 12:00 IST. */
    static final Instant NOW = Instant.parse("2026-10-07T06:30:00Z");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("marketplace_details")
            .withUsername("marketplace_test")
            .withPassword("marketplace_test");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @MockitoBean private DashboardCurrentUserService dashboardCurrentUserService;
    @MockitoBean private DashboardActionAuditService auditService;
    // Root-package repository whose queries reference project entities outside this narrowed scan.
    @MockitoBean private com.brandPitara.sfs.repository.CityRepository cityRepository;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;
    @Autowired private DealerPublicService dealerPublicService;
    @Autowired private WorkerPublicService workerPublicService;
    @Autowired private DashboardDealerService dashboardDealerService;
    @Autowired private WorkerProfileMutationService workerMutationService;
    @Autowired private DealerReviewService dealerReviewService;

    private MockMvc mvc;
    private Fixture f;

    record Fixture(long cityId, long otherCityId, long paintsCategoryId, long siblingCategoryId,
                   long dealerId, long similarSameCategoryId, long similarSiblingId, long inactiveDealerId,
                   long otherCityDealerId, long verifiedWorkerId, long unverifiedWorkerId, long brandProviderId,
                   long workerListingId, long reviewerUserId) {
    }

    @BeforeEach
    void seed() {
        mvc = MockMvcBuilders.standaloneSetup(
                new DealerPublicController(dealerPublicService),
                new WorkerPublicController(workerPublicService)
        ).setControllerAdvice(new GlobalExceptionHandler(new LogSanitizer())).build();

        DashboardUserEntity reviewer = mock(DashboardUserEntity.class);
        when(reviewer.getId()).thenReturn(77L);
        when(dashboardCurrentUserService.getCurrentUserOrThrow()).thenReturn(reviewer);

        jdbc.execute("""
                TRUNCATE business_review, business_worker_link, business_media, business_offering_item,
                         business_offering_group, business_opening_hours, provider_rate, provider_service_offering,
                         provider_service_area, provider_media, provider_profile, business, users
                RESTART IDENTITY CASCADE
                """);
        f = seedFixture();
    }

    private Fixture seedFixture() {
        long cityId = jdbc.queryForObject("SELECT id FROM city ORDER BY id LIMIT 1", Long.class);
        long otherCityId = jdbc.queryForObject("SELECT id FROM city WHERE id <> ? ORDER BY id LIMIT 1", Long.class, cityId);
        // Two active children of the same parent: the dealer's category and a sibling.
        List<java.util.Map<String, Object>> siblings = jdbc.queryForList("""
                SELECT c.id, c.parent_id FROM category c
                WHERE c.parent_id IS NOT NULL AND c.active
                  AND (SELECT count(*) FROM category s WHERE s.parent_id = c.parent_id AND s.active) >= 2
                ORDER BY c.parent_id, c.priority, c.id LIMIT 2
                """);
        long paints = ((Number) siblings.get(0).get("id")).longValue();
        long sibling = ((Number) siblings.get(1).get("id")).longValue();
        long unrelated = jdbc.queryForObject(
                "SELECT id FROM category WHERE parent_id IS DISTINCT FROM ? AND id NOT IN (?, ?) AND active ORDER BY id LIMIT 1",
                Long.class, siblings.get(0).get("parent_id"), paints, sibling);

        long dealer = business("Sharma Paints & Hardware", cityId, paints, true, 4.0, 2, "+919876543210", "+919876543210");
        jdbc.update("""
                UPDATE business SET description = 'Local paint supplier.', locality = 'Sector 26',
                       established_year = 2008, timezone = 'Asia/Kolkata' WHERE id = ?
                """, dealer);
        long sameCategory = business("Elite Home Solutions", cityId, paints, true, 4.5, 10, "0124-4567890", null);
        long siblingDealer = business("ProPaint Hub", cityId, sibling, true, 4.9, 3, "12345", null);
        long inactive = business("Closed Store", cityId, paints, false, 5.0, 1, null, null);
        long otherCity = business("Other City Paints", otherCityId, paints, true, 5.0, 1, null, null);
        business("Unrelated Category", cityId, unrelated, true, 5.0, 1, null, null);

        long workerUser = user("+919811111111", "WORKER");
        long unverifiedUser = user("+919822222222", "WORKER");
        long brandUser = user("+919833333333", "BRAND");
        long reviewerUser = user("+919844444444", "CUSTOMER");

        long workerListing = business("Manish Singh", cityId, paints, true, 0, 0, "+919811111111", "+919811111111");
        long unverifiedListing = business("Unverified Worker", cityId, paints, true, 0, 0, null, null);
        long brandListing = business("Brand Store", cityId, paints, true, 0, 0, null, null);

        long verifiedWorker = provider(workerUser, workerListing, "WORKER", "Manish Singh", paints, "VERIFIED", 11);
        long unverifiedWorker = provider(unverifiedUser, unverifiedListing, "WORKER", "Unverified Worker", paints, "PENDING", 3);
        long brandProvider = provider(brandUser, brandListing, "BRAND", "Brand Store", paints, "VERIFIED", 5);
        jdbc.update("UPDATE provider_profile SET bio = 'Painter with 11 years of experience.', gst_number = 'GST-SECRET', "
                + "availability_status = 'AVAILABLE' WHERE id = ?", verifiedWorker);
        jdbc.update("INSERT INTO provider_service_area (provider_id, city_id, locality) VALUES (?, ?, 'Sector 14'), (?, ?, 'Ashok Vihar'), (?, ?, 'Sector 14')",
                verifiedWorker, cityId, verifiedWorker, cityId, verifiedWorker, cityId);

        return new Fixture(cityId, otherCityId, paints, sibling, dealer, sameCategory, siblingDealer, inactive, otherCity,
                verifiedWorker, unverifiedWorker, brandProvider, workerListing, reviewerUser);
    }

    // ---------------------------------------------------------------------------------------
    // Dealer details contract
    // ---------------------------------------------------------------------------------------

    @Test
    void dealerDetailReturnsStructuredPersistedData() throws Exception {
        populateDealer();

        mvc.perform(get("/api/public/dealers/{id}", f.dealerId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sharma Paints & Hardware"))
                .andExpect(jsonPath("$.locationText").value(org.hamcrest.Matchers.startsWith("Sector 26, ")))
                .andExpect(jsonPath("$.description").value("Local paint supplier."))
                .andExpect(jsonPath("$.yearsInBusiness").value(18))
                .andExpect(jsonPath("$.openingHours.status").value("OPEN"))
                .andExpect(jsonPath("$.openingHours.statusText").value("Closes at 10:00 PM"))
                .andExpect(jsonPath("$.openingHours.todayText").value("9:00 am to 10:00 pm"))
                .andExpect(jsonPath("$.openingHours.weekly[6].closed").value(true))
                .andExpect(jsonPath("$.heroMedia[*].url").value(contains(
                        "https://cdn.example.com/hero-1.jpg", "https://cdn.example.com/hero-2.jpg")))
                .andExpect(jsonPath("$.galleryMedia", hasSize(1)))
                .andExpect(jsonPath("$.productGroups[0].title").value("Paints"))
                .andExpect(jsonPath("$.productGroups[0].items[*].name").value(contains("Interior Paint", "Exterior Paint")))
                .andExpect(jsonPath("$.productGroups[1].title").value("Hardware"))
                .andExpect(jsonPath("$.services[*].name").value(contains("Color Consultation", "Home Delivery")))
                .andExpect(jsonPath("$.contact.callUrl").value("tel:+919876543210"))
                .andExpect(jsonPath("$.contact.whatsappUrl").value("https://wa.me/919876543210"))
                .andExpect(jsonPath("$.rating.average").value(4.0))
                .andExpect(jsonPath("$.rating.count").value(2))
                .andExpect(jsonPath("$", not(hasKey("ownerUserId"))));
    }

    @Test
    void inactiveDealersAndWorkerListingsAreNotDealers() throws Exception {
        mvc.perform(get("/api/public/dealers/{id}", f.inactiveDealerId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/public/dealers/{id}", f.workerListingId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/public/dealers/{id}", 999_999)).andExpect(status().isNotFound());
        mvc.perform(get("/api/public/dealers/{id}/similar", f.inactiveDealerId())).andExpect(status().isNotFound());
    }

    @Test
    void similarStoresAreSameCityRelatedCategoryDeterministicAndPaginated() throws Exception {
        mvc.perform(get("/api/public/dealers/{id}/similar", f.dealerId()).param("size", "10"))
                .andExpect(status().isOk())
                // exact category first, then sibling; excludes self, inactive, other city, unrelated, worker listings
                .andExpect(jsonPath("$.content[*].id").value(contains(
                        (int) f.similarSameCategoryId(), (int) brandListingId(), (int) f.similarSiblingId())))
                .andExpect(jsonPath("$.totalElements").value(3));

        mvc.perform(get("/api/public/dealers/{id}/similar", f.dealerId()).param("size", "1").param("page", "1"))
                .andExpect(jsonPath("$.content[0].id").value((int) brandListingId()))
                .andExpect(jsonPath("$.totalPages").value(3));

        // A landline is callable but never offered on WhatsApp; an invalid number yields no action.
        mvc.perform(get("/api/public/dealers/{id}/similar", f.dealerId()))
                .andExpect(jsonPath("$.content[0].contact.callUrl").value("tel:+911244567890"))
                .andExpect(jsonPath("$.content[0].contact.whatsappUrl").doesNotExist())
                .andExpect(jsonPath("$.content[2].contact.callUrl").doesNotExist());
    }

    @Test
    void globalDealerListingSpansCitiesButExcludesInactiveAndWorkerListings() {
        var all = dealerPublicService.listDealers(null, null, 0, 20).getContent();

        assertThat(all).extracting(c -> c.id())
                .contains(f.dealerId(), f.otherCityDealerId(), brandListingId())
                .doesNotContain(f.inactiveDealerId(), f.workerListingId());
        assertThat(dealerPublicService.listDealers(f.cityId(), null, 0, 20).getContent())
                .extracting(c -> c.id())
                .doesNotContain(f.otherCityDealerId());
    }

    @Test
    void similarStoreCardsUseAFixedNumberOfQueriesRegardlessOfPageSize() {
        populateDealer();
        for (int n = 0; n < 8; n++) {
            long id = business("Extra Store " + n, f.cityId(), f.paintsCategoryId(), true, 3.0, 1, null, null);
            jdbc.update("INSERT INTO business_media (business_id, usage_type, media_url, sort_order) VALUES (?, 'HERO', ?, 0)",
                    id, "https://cdn.example.com/extra-" + n + ".jpg");
        }
        Statistics stats = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();

        stats.clear();
        dealerPublicService.similarDealers(f.dealerId(), 0, 2);
        long small = stats.getPrepareStatementCount();

        stats.clear();
        var page = dealerPublicService.similarDealers(f.dealerId(), 0, 20);
        long large = stats.getPrepareStatementCount();

        assertThat(page.getContent()).hasSizeGreaterThan(8);
        // Spring Data skips the count query when the first page is not full, hence <= rather than ==.
        assertThat(large).as("statements for a 20-card page").isLessThanOrEqualTo(small).isLessThanOrEqualTo(8);
    }

    // ---------------------------------------------------------------------------------------
    // Workers, links, recommendations
    // ---------------------------------------------------------------------------------------

    @Test
    void connectedWorkersShowOnlyVerifiedWorkersWithTypedRates() throws Exception {
        populateWorker();
        dashboardDealerService.linkWorker(f.dealerId(), new DealerWorkerLinkCreateRequest(f.verifiedWorkerId(), null, 0));
        dashboardDealerService.linkWorker(f.dealerId(), new DealerWorkerLinkCreateRequest(f.unverifiedWorkerId(), null, 1));

        mvc.perform(get("/api/public/dealers/{id}/workers", f.dealerId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value((int) f.verifiedWorkerId()))
                .andExpect(jsonPath("$.content[0].initials").value("MS"))
                .andExpect(jsonPath("$.content[0].availability").value("AVAILABLE"))
                .andExpect(jsonPath("$.content[0].primaryRate.type").value("VISITING_CHARGE"))
                .andExpect(jsonPath("$.content[0].primaryRate.label").value("Visiting Charge"))
                .andExpect(jsonPath("$.content[0].primaryRate.amount").value(150.00))
                .andExpect(jsonPath("$.content[0].primaryRate.currency").value("INR"))
                .andExpect(jsonPath("$.content[0].experienceYears").value(11));
    }

    @Test
    void workerDetailIsPublicOnlyWhenVerifiedAndNeverLeaksPrivateFields() throws Exception {
        populateWorker();

        mvc.perform(get("/api/public/workers/{id}", f.verifiedWorkerId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Manish Singh"))
                .andExpect(jsonPath("$.areaSummary").value("Serves Sector 14 & nearby"))
                .andExpect(jsonPath("$.serviceAreas[*].label").value(contains("Sector 14", "Ashok Vihar")))
                .andExpect(jsonPath("$.services[*].name").value(contains("Interior Painting", "Waterproofing")))
                .andExpect(jsonPath("$.rates[*].label").value(contains("Visiting Charge", "Material Cost")))
                .andExpect(jsonPath("$.contact.whatsappUrl").value("https://wa.me/919811111111"))
                .andExpect(jsonPath("$", not(hasKey("userId"))))
                .andExpect(jsonPath("$", not(hasKey("gstNumber"))))
                .andExpect(jsonPath("$", not(hasKey("verificationStatus"))));

        mvc.perform(get("/api/public/workers/{id}", f.unverifiedWorkerId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/public/workers/{id}", f.brandProviderId())).andExpect(status().isNotFound());
    }

    @Test
    void recommendationsAppearOnlyAfterReviewerVerification() throws Exception {
        populateDealer();
        var link = dashboardDealerService.linkWorker(f.dealerId(),
                new DealerWorkerLinkCreateRequest(f.verifiedWorkerId(), "Recommended for interior work.", 0));
        assertThat(link.recommendationStatus()).isEqualTo(WorkerRecommendationStatus.PENDING);

        mvc.perform(get("/api/public/workers/{id}/recommendations", f.verifiedWorkerId()))
                .andExpect(jsonPath("$.content", hasSize(0)));

        dashboardDealerService.reviewRecommendation(f.dealerId(), link.id(),
                new RecommendationReviewRequest(WorkerRecommendationStatus.VERIFIED));

        mvc.perform(get("/api/public/workers/{id}/recommendations", f.verifiedWorkerId()))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].dealerId").value((int) f.dealerId()))
                .andExpect(jsonPath("$.content[0].dealerName").value("Sharma Paints & Hardware"))
                .andExpect(jsonPath("$.content[0].dealerImageUrl").value("https://cdn.example.com/hero-1.jpg"))
                .andExpect(jsonPath("$.content[0].note").value("Recommended for interior work."));
    }

    @Test
    void databaseRejectsInvalidRelationshipTargetsAndDuplicates() {
        // provider must be a WORKER
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO business_worker_link (business_id, provider_id) VALUES (?, ?)", f.dealerId(), f.brandProviderId()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("not a WORKER provider");
        // business must be a dealer, not a worker's own listing
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO business_worker_link (business_id, provider_id) VALUES (?, ?)", f.workerListingId(), f.verifiedWorkerId()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("worker listing");
        // a VERIFIED recommendation must record who reviewed it
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO business_worker_link (business_id, provider_id, recommendation_status, recommendation_note) VALUES (?, ?, 'VERIFIED', 'x')",
                f.dealerId(), f.verifiedWorkerId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        // one charge per type, non-negative amounts
        jdbc.update("INSERT INTO provider_rate (provider_id, rate_type, amount, unit) VALUES (?, 'SERVICE_FEE', 450.00, 'PER_JOB')", f.verifiedWorkerId());
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO provider_rate (provider_id, rate_type, amount, unit) VALUES (?, 'SERVICE_FEE', 10, 'PER_JOB')", f.verifiedWorkerId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO provider_rate (provider_id, rate_type, amount, unit) VALUES (?, 'VISITING_CHARGE', -1, 'PER_VISIT')", f.verifiedWorkerId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        // plain-http media is rejected
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO business_media (business_id, usage_type, media_url) VALUES (?, 'HERO', 'http://insecure/x.jpg')", f.dealerId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------------------------------------------------------------------------------------
    // Reviews
    // ---------------------------------------------------------------------------------------

    @Test
    void reviewIsHiddenUntilApprovedThenUpdatesRatingAggregate() throws Exception {
        jdbc.update("UPDATE business SET avg_rating = 0, total_ratings = 0 WHERE id = ?", f.dealerId());
        User reviewer = new User();
        reviewer.setId(f.reviewerUserId());

        var submitted = dealerReviewService.submit(f.dealerId(),
                new DealerReviewCreateRequest(4, "Rahul Mehta", "Gurugram", "Helpful staff and genuine products."), reviewer);
        assertThat(submitted.status()).isEqualTo(BusinessReviewStatus.PENDING);

        mvc.perform(get("/api/public/dealers/{id}/reviews", f.dealerId())).andExpect(jsonPath("$.content", hasSize(0)));
        assertThatThrownBy(() -> dealerReviewService.submit(f.dealerId(),
                new DealerReviewCreateRequest(5, "Rahul", null, "Trying to review a second time."), reviewer))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(dealerReviewService.findMine(f.dealerId(), reviewer)).get()
                .extracting(r -> r.status()).isEqualTo(BusinessReviewStatus.PENDING);

        dealerReviewService.moderate(submitted.id(), new DealerReviewModerationRequest(BusinessReviewStatus.APPROVED, null));

        mvc.perform(get("/api/public/dealers/{id}/reviews", f.dealerId()))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].reviewerName").value("Rahul Mehta"))
                .andExpect(jsonPath("$.content[0].reviewerLocation").value("Gurugram"))
                .andExpect(jsonPath("$.content[0]", not(hasKey("userId"))));
        mvc.perform(get("/api/public/dealers/{id}", f.dealerId()))
                .andExpect(jsonPath("$.rating.average").value(4.0))
                .andExpect(jsonPath("$.rating.count").value(1));
    }

    // ---------------------------------------------------------------------------------------
    // fixtures
    // ---------------------------------------------------------------------------------------

    private void populateDealer() {
        List<OpeningHoursReplaceRequest.Interval> hours = new java.util.ArrayList<>();
        for (int d = 1; d <= 6; d++) {
            hours.add(new OpeningHoursReplaceRequest.Interval(d, LocalTime.of(9, 0), LocalTime.of(22, 0)));
        }
        dashboardDealerService.replaceOpeningHours(f.dealerId(), new OpeningHoursReplaceRequest(hours));
        dashboardDealerService.replaceOfferings(f.dealerId(), BusinessOfferingType.PRODUCT, new OfferingsReplaceRequest(List.of(
                new OfferingsReplaceRequest.Group("Paints", List.of("Interior Paint", "Exterior Paint")),
                new OfferingsReplaceRequest.Group("Hardware", List.of("Locks")))));
        dashboardDealerService.replaceOfferings(f.dealerId(), BusinessOfferingType.SERVICE, new OfferingsReplaceRequest(List.of(
                new OfferingsReplaceRequest.Group("Services", List.of("Color Consultation", "Home Delivery")))));
        jdbc.update("""
                INSERT INTO business_media (business_id, usage_type, media_url, sort_order) VALUES
                    (?, 'HERO', 'https://cdn.example.com/hero-2.jpg', 2),
                    (?, 'HERO', 'https://cdn.example.com/hero-1.jpg', 1),
                    (?, 'GALLERY', 'https://cdn.example.com/gallery-1.jpg', 0)
                """, f.dealerId(), f.dealerId(), f.dealerId());
        jdbc.update("""
                INSERT INTO business_media (business_id, usage_type, media_url, deleted) VALUES
                    (?, 'GALLERY', 'https://cdn.example.com/deleted.jpg', TRUE)
                """, f.dealerId());
    }

    private void populateWorker() {
        workerMutationService.replaceServices(f.verifiedWorkerId(),
                new WorkerServicesReplaceRequest(List.of("Interior Painting", "Waterproofing")));
        workerMutationService.replaceRates(f.verifiedWorkerId(), new WorkerRatesReplaceRequest(List.of(
                new WorkerRateRequest(ProviderRateType.VISITING_CHARGE, new BigDecimal("150.00"), "INR", ProviderRateUnit.PER_VISIT, null),
                new WorkerRateRequest(ProviderRateType.MATERIAL_COST, new BigDecimal("18.50"), null, ProviderRateUnit.PER_SQFT, "Primer + 2 coats"))));
    }

    private long brandListingId() {
        return jdbc.queryForObject("SELECT id FROM business WHERE name = 'Brand Store'", Long.class);
    }

    private long business(String name, long cityId, long categoryId, boolean active, double rating, int count,
                          String phone, String whatsapp) {
        return jdbc.queryForObject("""
                INSERT INTO business (name, city_id, category_id, is_active, avg_rating, total_ratings, primary_phone, whatsapp_phone)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, name, cityId, categoryId, active, rating, count, phone, whatsapp);
    }

    private long user(String phone, String role) {
        return jdbc.queryForObject("INSERT INTO users (phone_number, role) VALUES (?, ?) RETURNING id", Long.class, phone, role);
    }

    private long provider(long userId, long businessId, String type, String name, long categoryId, String verification, int years) {
        return jdbc.queryForObject("""
                INSERT INTO provider_profile (user_id, business_id, provider_type, display_name, primary_category_id,
                                              verification_status, experience_years)
                VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, userId, businessId, type, name, categoryId, verification, years);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            BusinessEntity.class,
            ProviderProfileEntity.class,
            BusinessMediaEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            DealerRepository.class,
            ProviderProfileRepository.class,
            CategoryRepository.class
    })
    @Import({
            DealerPublicServiceImpl.class,
            WorkerPublicServiceImpl.class,
            DealerReviewServiceImpl.class,
            DashboardDealerServiceImpl.class,
            DashboardWorkerServiceImpl.class,
            WorkerProfileMutationServiceImpl.class,
            DealerCardAssembler.class,
            WorkerCardAssembler.class
    })
    static class TestApplication {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
