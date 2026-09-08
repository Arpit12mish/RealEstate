package com.brandPitara.sfs.migration;

import com.brandPitara.sfs.SfsApplication;
import com.brandPitara.sfs.cms.content.domain.ContentStatus;
import com.brandPitara.sfs.cms.content.domain.ContentType;
import com.brandPitara.sfs.cms.content.dto.ContentPostListResponse;
import com.brandPitara.sfs.cms.content.document.ContentBlock;
import com.brandPitara.sfs.cms.content.document.ContentDocument;
import com.brandPitara.sfs.cms.content.document.InlineNode;
import com.brandPitara.sfs.cms.content.document.TextMark;
import com.brandPitara.sfs.cms.content.entity.ContentPostEntity;
import com.brandPitara.sfs.cms.content.repository.ContentPostRepository;
import com.brandPitara.sfs.cms.content.service.ContentPostService;
import com.brandPitara.sfs.cms.workflow.domain.ContentRevisionReason;
import com.brandPitara.sfs.cms.workflow.entity.ContentPostRevisionEntity;
import com.brandPitara.sfs.cms.workflow.repository.ContentPostRevisionRepository;
import com.brandPitara.sfs.cms.security.CmsPermissionProfile;
import com.brandPitara.sfs.dashboard.auth.security.DashboardUserDetails;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import com.brandPitara.sfs.security.identity.DashboardAuthenticationUserSnapshot;
import com.brandPitara.sfs.publiccontent.service.PublicContentService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import com.brandPitara.sfs.cms.metadata.service.CmsMetadataService;
import com.brandPitara.sfs.cms.metadata.dto.*;
import com.brandPitara.sfs.cms.media.repository.CmsMediaAssetRepository;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.cms.media.domain.*;
import com.brandPitara.sfs.cms.content.service.ContentDocumentService;
import com.brandPitara.sfs.cms.content.dto.*;
import com.brandPitara.sfs.cms.workflow.service.ContentWorkflowService;
import com.brandPitara.sfs.cms.workflow.dto.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = SfsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "spring.jpa.properties.hibernate.generate_statistics=true",
                "spring.task.scheduling.enabled=false",
                "sfs.local-staging.fake-otp.enabled=true",
                "sfs.rate-limit.trusted-proxies[0]=127.0.0.1",
                "jwt.secret=release-5am-mobile-secret-release-5am-mobile-secret",
                "dashboard.jwt.secret=release-5am-dashboard-secret-release-5am-dashboard-secret",
                "dashboard.seed.enabled=false",
                "sfs.search.enabled=false",
                "google.maps.places.enabled=false",
                "sfs.instagram.meta.sync-enabled=false",
                "logging.level.org.springframework.web=INFO",
                "sfs.log.dir=target/test-logs"
                ,"app.cms.media.public-delivery.base-url=https://media.example.test"
        }
)
@ActiveProfiles({"test", "local-staging"})
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ForwardOnlyFlywayApplicationStartupIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sfs_forward_startup")
            .withUsername("sfs_test")
            .withPassword("sfs_test");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Autowired private DataSource dataSource;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private Flyway flyway;
    @Autowired private WebApplicationContext applicationContext;
    @Autowired private ContentPostRepository contentPostRepository;
    @Autowired private ContentPostService contentPostService;
    @Autowired private DashboardUserRepository dashboardUserRepository;
    @Autowired private ContentPostRevisionRepository contentPostRevisionRepository;
    @Autowired private PublicContentService publicContentService;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private CmsMetadataService cmsMetadataService;
    @Autowired private CmsMediaAssetRepository cmsMediaAssetRepository;
    @Autowired private ContentDocumentService contentDocumentService;
    @Autowired private ContentWorkflowService contentWorkflowService;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    private MockMvc mockMvc;
    private MockMvc securedMockMvc;

    @BeforeEach
    void configureMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
        securedMockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .addFilters(applicationContext.getBean("springSecurityFilterChain", Filter.class))
                .build();
    }

    @Test
    void emptyPostgresMigratesValidatesHibernateAndStartsApplication() throws Exception {
        assertThat(entityManagerFactory.isOpen()).isTrue();
        flyway.validate();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT version, success
                     FROM flyway_schema_history
                     ORDER BY installed_rank DESC
                     LIMIT 1
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(Integer.parseInt(result.getString("version"))).isGreaterThanOrEqualTo(141);
            assertThat(result.getBoolean("success")).isTrue();
        }

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT count(*)
                     FROM flyway_schema_history
                     WHERE version IN ('140', '141') AND success
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getLong(1)).isEqualTo(2);
        }
    }

    @Test
    void freshBaselineSupportsCoreGuestAndPublicReadFlows() throws Exception {
        mockMvc.perform(post("/api/auth/guest/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "installationId": "release-5am2-fresh-smoke",
                                  "platform": "ANDROID",
                                  "appVersion": "5am2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestSessionId").isNumber())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.installationId").value("release-5am2-fresh-smoke"));

        mockMvc.perform(get("/api/public/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").isNumber())
                .andExpect(jsonPath("$.sections").isArray());
        mockMvc.perform(get("/api/public/home/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug == 'architects')]").exists())
                .andExpect(jsonPath("$[?(@.slug == 'interior-designers')]").exists());
        mockMvc.perform(get("/api/public/cities/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        mockMvc.perform(get("/api/projects/feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
        mockMvc.perform(get("/api/public/search/suggest").param("q", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections").isArray());
    }

    @Test
    void contentPostJpaIncrementsVersionAndListProjectionDoesNotCauseNPlusOne() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Long postId = transaction.execute(status -> {
            DashboardUserEntity owner = dashboardUserRepository.saveAndFlush(
                    DashboardUserEntity.builder()
                            .name("Phase 2 Writer")
                            .email("phase2-writer@example.com")
                            .passwordHash("encoded")
                            .role(DashboardRole.CONTENT_STAFF)
                            .active(true)
                            .build()
            );
            ContentPostEntity post = contentPostRepository.saveAndFlush(ContentPostEntity.builder()
                    .contentType(ContentType.ARTICLE)
                    .status(ContentStatus.DRAFT)
                    .title("Initial Phase 2 Title")
                    .slug("initial-phase-2-title")
                    .contentOwner(owner)
                    .createdBy(owner)
                    .updatedBy(owner)
                    .robotsIndex(true)
                    .robotsFollow(true)
                    .contentDocument(largeDocument())
                    .contentDocumentSchemaVersion((short) 1)
                    .build());
            assertThat(post.getVersion()).isZero();

            post.setTitle("Updated Phase 2 Title");
            contentPostRepository.flush();
            assertThat(post.getVersion()).isOne();

            contentPostRepository.saveAndFlush(ContentPostEntity.builder()
                    .contentType(ContentType.BLOG)
                    .status(ContentStatus.DRAFT)
                    .title("Second Phase 2 Title")
                    .slug("second-phase-2-title")
                    .contentOwner(owner)
                    .createdBy(owner)
                    .updatedBy(owner)
                    .robotsIndex(true)
                    .robotsFollow(true)
                    .build());
            return post.getId();
        });

        Long persistedVersion = transaction.execute(status ->
                contentPostRepository.findDetailedById(postId).orElseThrow().getVersion()
        );
        assertThat(persistedVersion).isOne();

        ContentDocument persistedDocument = transaction.execute(status ->
                contentPostRepository.findDetailedById(postId).orElseThrow().getContentDocument()
        );
        InlineNode.Text persistedText = (InlineNode.Text) ((ContentBlock.Paragraph)
                persistedDocument.blocks().get(0)).content().get(0);
        assertThat(persistedText.text()).contains("गुरुग्राम");
        assertThat(((TextMark.Link) persistedText.marks().get(0)).href())
                .isEqualTo("https://squarefootstory.com/gurgaon");

        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        sessionFactory.getStatistics().clear();
        transaction.executeWithoutResult(status -> {
            var page = contentPostRepository.findList(
                    null, null, null, null,
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt"))
            );
            assertThat(page.getContent())
                    .hasSizeGreaterThanOrEqualTo(2)
                    .allSatisfy(post -> {
                        ContentPostListResponse response = ContentPostListResponse.from(post);
                        assertThat(response.contentOwnerDisplayName()).isNotBlank();
                        assertThat(response.updatedByDisplayName()).isNotBlank();
                    });
        });

        long dashboardListQueries = sessionFactory.getStatistics().getPrepareStatementCount();
        System.out.println("CMS_PHASE8_QUERIES dashboardContentList=" + dashboardListQueries);
        assertThat(dashboardListQueries)
                .as("paged content query plus count query, without per-row owner/actor selects")
                .isLessThanOrEqualTo(2);
        assertThat(java.util.Arrays.asList(sessionFactory.getStatistics().getQueries()))
                .filteredOn(query -> query.contains("ContentPostEntity"))
                .as("list projection must not select the JSONB document")
                .isNotEmpty()
                .allMatch(query -> !query.contains("contentDocument"));
    }

    @Test
    void publicContentReadsPublishedRevisionWithBoundedQueriesAndMetadataOnlyList() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            DashboardUserEntity publisher = dashboardUserRepository.saveAndFlush(
                    DashboardUserEntity.builder()
                            .name("Public Contract Publisher")
                            .email("public-contract-publisher@example.com")
                            .passwordHash("encoded")
                            .role(DashboardRole.ADMIN)
                            .active(true)
                            .build()
            );
            ContentPostEntity post = contentPostRepository.saveAndFlush(ContentPostEntity.builder()
                    .contentType(ContentType.ARTICLE)
                    .status(ContentStatus.DRAFT)
                    .title("Mutable draft title")
                    .slug("public-contract-article")
                    .excerpt("Mutable draft excerpt")
                    .contentOwner(publisher)
                    .createdBy(publisher)
                    .updatedBy(publisher)
                    .robotsIndex(false)
                    .robotsFollow(false)
                    .contentDocument(ContentDocument.empty())
                    .contentDocumentSchemaVersion((short) ContentDocument.CURRENT_SCHEMA_VERSION)
                    .build());
            ContentPostRevisionEntity revision = contentPostRevisionRepository.saveAndFlush(
                    ContentPostRevisionEntity.builder()
                            .contentPost(post)
                            .revisionNumber(1)
                            .contentType(ContentType.ARTICLE)
                            .title("Immutable published title")
                            .slug("public-contract-article")
                            .excerpt("Immutable published excerpt")
                            .seoTitle("Immutable SEO")
                            .robotsIndex(true)
                            .robotsFollow(true)
                            .contentDocument(ContentDocument.empty())
                            .contentDocumentSchemaVersion((short) ContentDocument.CURRENT_SCHEMA_VERSION)
                            .createdFromPostVersion(post.getVersion())
                            .createdBy(publisher)
                            .revisionReason(ContentRevisionReason.REVIEW_SUBMISSION)
                            .build()
            );
            post.setStatus(ContentStatus.PUBLISHED);
            post.setCurrentReviewRevision(revision);
            post.setApprovedRevision(revision);
            post.setCurrentPublishedRevision(revision);
            post.setPublishedAt(OffsetDateTime.now());
            post.setPublishedBy(publisher);
            contentPostRepository.flush();
        });

        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        sessionFactory.getStatistics().clear();
        var detail = publicContentService.getBySlug("public-contract-article", null).body();
        assertThat(detail.title()).isEqualTo("Immutable published title");
        assertThat(detail.excerpt()).isEqualTo("Immutable published excerpt");
        assertThat(detail.seo().title()).isEqualTo("Immutable SEO");
        long publicDetailQueries = sessionFactory.getStatistics().getPrepareStatementCount();
        System.out.println("CMS_PHASE8_QUERIES publicDetail=" + publicDetailQueries);
        assertThat(publicDetailQueries)
                .as("published detail projection plus optional one bulk media query")
                .isLessThanOrEqualTo(2);

        sessionFactory.getStatistics().clear();
        var page = publicContentService.list(ContentType.ARTICLE, 0, 20);
        assertThat(page.content()).anySatisfy(item ->
                assertThat(item.title()).isEqualTo("Immutable published title"));
        long publicListQueries = sessionFactory.getStatistics().getPrepareStatementCount();
        System.out.println("CMS_PHASE8_QUERIES publicList=" + publicListQueries);
        assertThat(publicListQueries)
                .as("published list projection plus count")
                .isLessThanOrEqualTo(2);
        assertThat(java.util.Arrays.asList(sessionFactory.getStatistics().getQueries()))
                .filteredOn(query -> query.contains("currentPublishedRevision"))
                .isNotEmpty()
                .allMatch(query -> !query.contains("contentDocument"));
    }

    @Test
    void fullEditorialApiContractFreezesAuthorTaxonomyCoverAndServesPublishedRevision() {
        DashboardUserEntity writer = dashboardUserRepository.saveAndFlush(
                dashboardUser("Acceptance Writer", "acceptance-writer@example.com"));
        DashboardUserEntity editor = dashboardUserRepository.saveAndFlush(
                dashboardUser("Acceptance Editor", "acceptance-editor@example.com"));
        DashboardUserEntity publisher = dashboardUserRepository.saveAndFlush(
                dashboardUser("Acceptance Publisher", "acceptance-publisher@example.com"));
        CmsMediaAssetEntity cover = cmsMediaAssetRepository.saveAndFlush(CmsMediaAssetEntity.builder()
                .mediaType(CmsMediaType.IMAGE).status(CmsMediaStatus.READY)
                .storageBucket("private-test").storageKey("cms/images/2026/08/acceptance-cover.jpg")
                .originalFilename("cover.jpg").contentType("image/jpeg")
                .declaredSizeBytes(1200L).sizeBytes(1200L).width(1200).height(630)
                .createdBy(writer).readyAt(OffsetDateTime.now()).build());

        CmsAuthorResponse author = cmsMetadataService.createAuthor(new CmsAuthorRequest(
                "Acceptance Analyst", null, "Public biography", "Senior Analyst", null, true, null));
        CmsCategoryResponse category = cmsMetadataService.createCategory(new CmsCategoryRequest(
                "Acceptance Insights", null, "Market reporting", true, null));
        CmsTagResponse tag = cmsMetadataService.createTag(new CmsTagRequest(
                "Acceptance Gurgaon", null, true, null));

        var writerAuth = authentication(writer.getId(), writer.getEmail(), writer.getName(),
                CmsPermissionProfile.WRITER.permissions());
        SecurityContextHolder.getContext().setAuthentication(writerAuth);
        ContentPostDetailResponse created = contentPostService.create(new ContentPostCreateRequest(
                ContentType.ARTICLE, "Acceptance Market Guide", null, "Acceptance excerpt",
                null, null, null, true, true, author.id(), category.id(), Set.of(tag.id()),
                cover.getId(), "Gurgaon skyline", 6), writerAuth);
        ContentDocument document = new ContentDocument(2, java.util.List.of(
                new ContentBlock.Paragraph(java.util.List.of(new InlineNode.Text(
                        "A complete acceptance article", java.util.List.of())))));
        var documentSaved = contentDocumentService.update(created.id(),
                new ContentDocumentUpdateRequest(created.version(), document), writerAuth);
        var submitted = contentWorkflowService.submit(created.id(),
                new SubmitReviewRequest(documentSaved.version()), writerAuth);

        var editorAuth = authentication(editor.getId(), editor.getEmail(), editor.getName(),
                CmsPermissionProfile.EDITOR.permissions());
        SecurityContextHolder.getContext().setAuthentication(editorAuth);
        var approved = contentWorkflowService.approve(created.id(),
                new ApproveContentRequest(submitted.version()), editorAuth);

        var publisherAuth = authentication(publisher.getId(), publisher.getEmail(), publisher.getName(),
                CmsPermissionProfile.PUBLISHER.permissions());
        SecurityContextHolder.getContext().setAuthentication(publisherAuth);
        contentWorkflowService.publish(created.id(), new PublishContentRequest(approved.version()), publisherAuth);

        var publicDetail = publicContentService.getBySlug(created.slug(), null).body();
        assertThat(publicDetail.author().displayName()).isEqualTo("Acceptance Analyst");
        assertThat(publicDetail.category().slug()).isEqualTo("acceptance-insights");
        assertThat(publicDetail.tags()).extracting("slug").containsExactly("acceptance-gurgaon");
        assertThat(publicDetail.cover().mediaAssetId()).isEqualTo(cover.getId());
        assertThat(publicDetail.cover().altText()).isEqualTo("Gurgaon skyline");
        assertThat(publicContentService.list(ContentType.ARTICLE, "acceptance-insights", null,
                "acceptance-gurgaon", null, 0, 20).content()).extracting("id").contains(created.id());
    }

    @Test
    void phase8BearerAuthenticatedHttpWorkflowPublishesTheExactSecondRevision() throws Exception {
        String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        String password = "Strong!Pass123";
        String adminEmail = "phase8-admin-" + suffix + "@example.com";
        dashboardUserRepository.saveAndFlush(DashboardUserEntity.builder()
                .name("Phase 8 Admin").email(adminEmail).passwordHash(passwordEncoder.encode(password))
                .role(DashboardRole.ADMIN).active(true).build());

        String adminToken = login(adminEmail, password);
        String writerEmail = "phase8-writer-" + suffix + "@example.com";
        String editorEmail = "phase8-editor-" + suffix + "@example.com";
        String publisherEmail = "phase8-publisher-" + suffix + "@example.com";
        createStaff(adminToken, writerEmail, "WRITER", password);
        createStaff(adminToken, editorEmail, "EDITOR", password);
        createStaff(adminToken, publisherEmail, "PUBLISHER", password);
        String writerToken = login(writerEmail, password);
        String editorToken = login(editorEmail, password);
        String publisherToken = login(publisherEmail, password);

        securedMockMvc.perform(post("/api/dashboard/cms/content")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        long authorId = responseId(securedMockMvc.perform(post("/api/dashboard/cms/authors")
                        .header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Phase 8 Analyst","bio":"Certified public biography",
                                 "designation":"Senior Analyst","active":true}
                                """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long categoryId = responseId(securedMockMvc.perform(post("/api/dashboard/cms/categories")
                        .header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Phase 8 Market Insights","description":"Certified category","active":true}
                                """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long tagId = responseId(securedMockMvc.perform(post("/api/dashboard/cms/tags")
                        .header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Phase 8 Gurgaon","active":true}
                                """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());

        DashboardUserEntity writer = dashboardUserRepository.findByEmailIgnoreCase(writerEmail).orElseThrow();
        CmsMediaAssetEntity cover = readyMedia(writer, CmsMediaType.IMAGE,
                "cms/images/2026/08/phase8-cover-" + suffix + ".jpg", "cover.jpg", "image/jpeg");
        cover.setWidth(1600); cover.setHeight(900);
        cover = cmsMediaAssetRepository.saveAndFlush(cover);
        cmsMetadataService.createAuthor(new CmsAuthorRequest(
                "Phase 8 Profiled Analyst " + suffix, null, null, "Analyst",
                cover.getId(), true, null));
        SessionFactory metadataSessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        metadataSessionFactory.getStatistics().clear();
        assertThat(cmsMetadataService.authors(true, "phase 8", PageRequest.of(0, 20)).getContent())
                .hasSizeGreaterThanOrEqualTo(2);
        long authorLookupQueries = metadataSessionFactory.getStatistics().getPrepareStatementCount();
        System.out.println("CMS_PHASE8_QUERIES authorLookup=" + authorLookupQueries);
        assertThat(authorLookupQueries)
                .as("author selector page plus count, including profile-media IDs without N+1")
                .isLessThanOrEqualTo(2);
        CmsMediaAssetEntity video = readyMedia(writer, CmsMediaType.VIDEO,
                "cms/videos/2026/08/phase8-video-" + suffix + ".mp4", "interview.mp4", "video/mp4");
        video.setDurationMillis(42_000L);
        video = cmsMediaAssetRepository.saveAndFlush(video);

        String slug = "phase-8-certified-" + suffix;
        JsonNode created = json(securedMockMvc.perform(post("/api/dashboard/cms/content")
                        .header("Authorization", bearer(writerToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentType":"ARTICLE","title":"Phase 8 Certified Market Guide",
                                 "slug":"%s","excerpt":"The certified immutable-revision contract.",
                                 "seoTitle":"Certified Market Guide","seoDescription":"Certified CMS response.",
                                 "robotsIndex":true,"robotsFollow":true,"publicAuthorId":%d,
                                 "categoryId":%d,"tagIds":[%d],"coverMediaAssetId":%d,
                                 "coverAltText":"Gurgaon skyline at sunset"}
                                """.formatted(slug, authorId, categoryId, tagId, cover.getId())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString());
        long contentId = created.path("id").asLong();
        long initialVersion = created.path("version").asLong();

        securedMockMvc.perform(post("/api/dashboard/cms/content")
                        .header("Authorization", bearer(publisherToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentType":"BLOG","title":"Publisher Cannot Draft","robotsIndex":true,"robotsFollow":true}
                                """))
                .andExpect(status().isForbidden());

        String document = """
                {"version":%d,"document":{"schemaVersion":2,"blocks":[
                  {"type":"HEADING","level":"H2","content":[{"type":"TEXT","text":"Market overview","marks":[]}]},
                  {"type":"PARAGRAPH","content":[
                    {"type":"TEXT","text":"Trusted ","marks":[{"type":"BOLD"}]},
                    {"type":"TEXT","text":"analysis","marks":[{"type":"ITALIC"},{"type":"LINK","href":"https://squarefootstory.com/gurgaon","openInNewTab":false,"nofollow":false,"sponsored":false}]}]},
                  {"type":"BULLET_LIST","items":[{"content":[{"type":"TEXT","text":"One bounded item","marks":[]}]}]},
                  {"type":"BLOCKQUOTE","content":[{"type":"TEXT","text":"Exact reviewed words","marks":[]}]},
                  {"type":"IMAGE","mediaAssetId":%d,"decorative":false,"altText":"Gurgaon skyline",
                   "caption":[{"type":"TEXT","text":"Certified cover","marks":[]}],"layout":"WIDE"},
                  {"type":"VIDEO","mediaAssetId":%d,"posterMediaAssetId":%d,
                   "caption":[{"type":"TEXT","text":"Analyst interview","marks":[]}]},
                  {"type":"EMBED","provider":"YOUTUBE","externalId":"dQw4w9WgXcQ","caption":[]},
                  {"type":"DIVIDER"}
                ]}}
                """.formatted(initialVersion, cover.getId(), video.getId(), cover.getId());
        JsonNode documentSaved = json(securedMockMvc.perform(put("/api/dashboard/cms/content/{id}/document", contentId)
                        .header("Authorization", bearer(writerToken)).contentType(MediaType.APPLICATION_JSON)
                        .content(document))
                .andExpect(status().isOk()).andExpect(jsonPath("$.document.blocks[4].mediaAssetId").value(cover.getId()))
                .andReturn().getResponse().getContentAsString());
        long documentVersion = documentSaved.path("version").asLong();

        securedMockMvc.perform(put("/api/dashboard/cms/content/{id}", contentId)
                        .header("Authorization", bearer(editorToken)).contentType(MediaType.APPLICATION_JSON)
                        .content(metadataUpdate(initialVersion, slug, authorId, categoryId, tagId, cover.getId(), "stale")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONTENT_VERSION_CONFLICT"));

        JsonNode submitted1 = workflow(contentId, "submit", writerToken, documentVersion, null);
        long revision1 = submitted1.path("currentReviewRevisionId").asLong();
        JsonNode changes = workflow(contentId, "request-changes", editorToken,
                submitted1.path("version").asLong(), "Clarify the market conclusion.");
        JsonNode metadata2 = json(securedMockMvc.perform(put("/api/dashboard/cms/content/{id}", contentId)
                        .header("Authorization", bearer(writerToken)).contentType(MediaType.APPLICATION_JSON)
                        .content(metadataUpdate(changes.path("version").asLong(), slug, authorId, categoryId,
                                tagId, cover.getId(), "Revised immutable excerpt")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        JsonNode submitted2 = workflow(contentId, "submit", writerToken, metadata2.path("version").asLong(), null);
        long revision2 = submitted2.path("currentReviewRevisionId").asLong();
        assertThat(revision2).isNotEqualTo(revision1);

        securedMockMvc.perform(get("/api/dashboard/cms/content/{id}/revisions/{revisionId}", contentId, revision1)
                        .header("Authorization", bearer(editorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.excerpt").value("The certified immutable-revision contract."));
        JsonNode approved = workflow(contentId, "approve", editorToken, submitted2.path("version").asLong(), null);
        JsonNode published = workflow(contentId, "publish", publisherToken, approved.path("version").asLong(), null);
        assertThat(published.path("approvedRevisionId").asLong()).isEqualTo(revision2);
        assertThat(published.path("currentPublishedRevisionId").asLong()).isEqualTo(revision2);

        var publicResult = securedMockMvc.perform(get("/api/public/content/{slug}", slug))
                .andExpect(status().isOk()).andExpect(jsonPath("$.excerpt").value("Revised immutable excerpt"))
                .andExpect(jsonPath("$.media['" + cover.getId() + "'].deliveryUrl").exists())
                .andReturn();
        String etag1 = publicResult.getResponse().getHeader("ETag");
        securedMockMvc.perform(get("/api/public/content/{slug}", slug).header("If-None-Match", etag1))
                .andExpect(status().isNotModified());

        JsonNode unpublished = workflow(contentId, "unpublish", publisherToken,
                published.path("version").asLong(), null);
        securedMockMvc.perform(get("/api/public/content/{slug}", slug))
                .andExpect(status().isNotFound()).andExpect(result ->
                        assertThat(result.getResponse().getHeader("Cache-Control")).isEqualTo("no-store"));
        JsonNode republished = workflow(contentId, "publish", publisherToken,
                unpublished.path("version").asLong(), null);
        String etag2 = securedMockMvc.perform(get("/api/public/content/{slug}", slug))
                .andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");
        assertThat(etag2).isNotEqualTo(etag1);
        assertThat(republished.path("currentPublishedRevisionId").asLong()).isEqualTo(revision2);

        LoadResult detailLoad = concurrentPublicGets("/api/public/content/" + slug, 50);
        LoadResult listLoad = concurrentPublicGets("/api/public/content?contentType=ARTICLE&page=0&size=20", 50);
        assertThat(detailLoad.successes()).isEqualTo(50);
        assertThat(listLoad.successes()).isEqualTo(50);
        assertThat(detailLoad.serverErrors() + listLoad.serverErrors()).isZero();
        System.out.printf("CMS_PHASE8_LOAD detail=%s list=%s%n", detailLoad, listLoad);
    }

    @Test
    void contentListEnforcesWriterOwnershipAndEditorFilters() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Long[] userIds = transaction.execute(status -> {
            DashboardUserEntity writer = dashboardUserRepository.saveAndFlush(
                    dashboardUser("Visibility Writer", "visibility-writer@example.com")
            );
            DashboardUserEntity other = dashboardUserRepository.saveAndFlush(
                    dashboardUser("Visibility Other", "visibility-other@example.com")
            );
            contentPostRepository.saveAndFlush(contentPost(
                    writer, ContentType.ARTICLE, "Writer Visible Shell", "writer-visible-shell"
            ));
            contentPostRepository.saveAndFlush(contentPost(
                    other, ContentType.BLOG, "Editor Visible Shell", "editor-visible-shell"
            ));
            return new Long[]{writer.getId(), other.getId()};
        });

        try {
            var writerAuthentication = authentication(
                    userIds[0], "visibility-writer@example.com", "Visibility Writer",
                    CmsPermissionProfile.WRITER.permissions()
            );
            SecurityContextHolder.getContext().setAuthentication(writerAuthentication);
            var writerPage = contentPostService.list(
                    null, ContentStatus.DRAFT, userIds[1], null,
                    PageRequest.of(0, 20), writerAuthentication
            );
            assertThat(writerPage.getContent())
                    .isNotEmpty()
                    .allSatisfy(item ->
                            assertThat(item.contentOwnerDashboardUserId()).isEqualTo(userIds[0])
                    );

            var editorAuthentication = authentication(
                    userIds[1], "visibility-other@example.com", "Visibility Other",
                    CmsPermissionProfile.EDITOR.permissions()
            );
            SecurityContextHolder.getContext().setAuthentication(editorAuthentication);
            var editorPage = contentPostService.list(
                    ContentType.BLOG, ContentStatus.DRAFT, userIds[1], "editor visible",
                    PageRequest.of(0, 20), editorAuthentication
            );
            assertThat(editorPage.getContent())
                    .extracting(item -> item.slug())
                    .containsExactly("editor-visible-shell");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String login(String email, String password) throws Exception {
        String body = securedMockMvc.perform(post("/api/dashboard/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        return json(body).path("accessToken").asText();
    }

    private void createStaff(String adminToken, String email, String profile, String password) throws Exception {
        securedMockMvc.perform(post("/api/dashboard/users")
                        .header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","displayName":"Phase 8 %s","role":"CONTENT_STAFF",
                                 "permissionProfiles":["%s"],"initialPassword":"%s"}
                                """.formatted(email, profile, profile, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.permissions").isArray())
                .andExpect(jsonPath("$.initialPassword").doesNotExist());
    }

    private JsonNode workflow(long contentId, String command, String token, long version, String comment)
            throws Exception {
        String payload = comment == null
                ? "{\"version\":" + version + "}"
                : objectMapper.writeValueAsString(java.util.Map.of("version", version, "comment", comment));
        return json(securedMockMvc.perform(post("/api/dashboard/cms/content/{id}/workflow/{command}",
                        contentId, command).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private String metadataUpdate(long version, String slug, long authorId, long categoryId,
                                  long tagId, long coverId, String excerpt) {
        return """
                {"version":%d,"contentType":"ARTICLE","title":"Phase 8 Certified Market Guide",
                 "slug":"%s","excerpt":"%s","seoTitle":"Certified Market Guide",
                 "seoDescription":"Certified CMS response.","robotsIndex":true,"robotsFollow":true,
                 "publicAuthorId":%d,"categoryId":%d,"tagIds":[%d],"coverMediaAssetId":%d,
                 "coverAltText":"Gurgaon skyline at sunset"}
                """.formatted(version, slug, excerpt, authorId, categoryId, tagId, coverId);
    }

    private CmsMediaAssetEntity readyMedia(DashboardUserEntity creator, CmsMediaType type,
                                            String key, String filename, String contentType) {
        return CmsMediaAssetEntity.builder().mediaType(type).status(CmsMediaStatus.READY)
                .storageBucket("private-test").storageKey(key).originalFilename(filename)
                .contentType(contentType).declaredSizeBytes(1_200L).sizeBytes(1_200L)
                .createdBy(creator).readyAt(OffsetDateTime.now()).build();
    }

    private long responseId(String body) throws Exception {
        return json(body).path("id").asLong();
    }

    private JsonNode json(String body) throws Exception {
        return objectMapper.readTree(body);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private LoadResult concurrentPublicGets(String path, int requests) throws Exception {
        com.zaxxer.hikari.HikariDataSource hikari = dataSource.unwrap(com.zaxxer.hikari.HikariDataSource.class);
        java.util.concurrent.atomic.AtomicInteger maxActive = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger maxPending = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicBoolean sampling = new java.util.concurrent.atomic.AtomicBoolean(true);
        Thread sampler = new Thread(() -> {
            while (sampling.get()) {
                var pool = hikari.getHikariPoolMXBean();
                if (pool != null) {
                    maxActive.accumulateAndGet(pool.getActiveConnections(), Math::max);
                    maxPending.accumulateAndGet(pool.getThreadsAwaitingConnection(), Math::max);
                }
                java.util.concurrent.locks.LockSupport.parkNanos(1_000_000L);
            }
        }, "cms-phase8-hikari-sampler");
        sampler.start();

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(requests);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.List<java.util.concurrent.Future<long[]>> futures = new java.util.ArrayList<>();
        for (int index = 0; index < requests; index++) {
            futures.add(executor.submit(() -> {
                start.await();
                long before = System.nanoTime();
                int status = securedMockMvc.perform(get(path)).andReturn().getResponse().getStatus();
                return new long[]{status, System.nanoTime() - before};
            }));
        }
        start.countDown();
        java.util.List<Long> latencies = new java.util.ArrayList<>();
        int successes = 0, serverErrors = 0;
        for (var future : futures) {
            long[] result = future.get(30, java.util.concurrent.TimeUnit.SECONDS);
            if (result[0] == 200) successes++;
            if (result[0] >= 500) serverErrors++;
            latencies.add(java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(result[1]));
        }
        executor.shutdownNow();
        sampling.set(false);
        sampler.join(1_000L);
        latencies.sort(Long::compareTo);
        return new LoadResult(successes, serverErrors, percentile(latencies, 0.50),
                percentile(latencies, 0.95), percentile(latencies, 0.99),
                maxActive.get(), maxPending.get());
    }

    private long percentile(java.util.List<Long> values, double percentile) {
        int index = Math.min(values.size() - 1, Math.max(0, (int) Math.ceil(values.size() * percentile) - 1));
        return values.get(index);
    }

    private record LoadResult(int successes, int serverErrors, long p50Ms, long p95Ms, long p99Ms,
                              int maxHikariActive, int maxHikariPending) { }

    private DashboardUserEntity dashboardUser(String name, String email) {
        return DashboardUserEntity.builder()
                .name(name)
                .email(email)
                .passwordHash("encoded")
                .role(DashboardRole.CONTENT_STAFF)
                .active(true)
                .build();
    }

    private ContentPostEntity contentPost(
            DashboardUserEntity owner,
            ContentType type,
            String title,
            String slug
    ) {
        return ContentPostEntity.builder()
                .contentType(type)
                .status(ContentStatus.DRAFT)
                .title(title)
                .slug(slug)
                .contentOwner(owner)
                .createdBy(owner)
                .updatedBy(owner)
                .robotsIndex(true)
                .robotsFollow(true)
                .build();
    }

    private ContentDocument largeDocument() {
        return new ContentDocument(1, java.util.List.of(new ContentBlock.Paragraph(java.util.List.of(
                new InlineNode.Text(
                        "गुरुग्राम ".repeat(10_000),
                        java.util.List.of(new TextMark.Link(
                                "https://squarefootstory.com/gurgaon", false, false, false
                        ))
                )
        ))));
    }

    private UsernamePasswordAuthenticationToken authentication(
            Long userId,
            String email,
            String name,
            Set<com.brandPitara.sfs.dashboard.common.enums.DashboardPermission> permissions
    ) {
        DashboardUserDetails details = new DashboardUserDetails(
                new DashboardAuthenticationUserSnapshot(
                        userId, email, name, DashboardRole.CONTENT_STAFF, true, permissions
                )
        );
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }
}
