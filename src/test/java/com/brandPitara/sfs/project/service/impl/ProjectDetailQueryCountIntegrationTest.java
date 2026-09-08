package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.buildercredibility.service.impl.BuilderCredibilityServiceImpl;
import com.brandPitara.sfs.builderhighlight.repository.BuilderHighlightItemRepository;
import com.brandPitara.sfs.cdn.event.ProjectPublicCacheEvictionPublisher;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.dashboard.project.controller.DashboardProjectController;
import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.project.service.DashboardProjectOwnershipService;
import com.brandPitara.sfs.dashboard.project.service.DashboardProjectWorkspaceService;
import com.brandPitara.sfs.dashboard.validator.DashboardProjectValidator;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.exception.GlobalExceptionHandler;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.project.controller.publicapi.ProjectPublicController;
import com.brandPitara.sfs.project.controller.publicapi.ProjectPublicV2Controller;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.enums.PropertyType;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.repository.ProjectMediaRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.project.service.ProjectConnectivityService;
import com.brandPitara.sfs.project.service.ProjectFavoriteService;
import com.brandPitara.sfs.project.service.ProjectFloorPlanService;
import com.brandPitara.sfs.project.service.ProjectMasterPlanService;
import com.brandPitara.sfs.project.service.ProjectMediaService;
import com.brandPitara.sfs.project.service.ProjectService;
import com.brandPitara.sfs.project.service.ProjectPublicCoreService;
import com.brandPitara.sfs.project.service.reader.ProjectDetailAnalyticsReader;
import com.brandPitara.sfs.project.service.reader.ProjectPublicBaseReader;
import com.brandPitara.sfs.project.service.reader.ProjectPublicContentReader;
import com.brandPitara.sfs.repository.UserFavoriteRepository;
import com.brandPitara.sfs.projectmeter.entity.ProjectMeterSnapshotEntity;
import com.brandPitara.sfs.projectmeter.repository.ProjectMeterSnapshotRepository;
import com.brandPitara.sfs.projectmeter.service.ProjectMeterService;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = ProjectDetailQueryCountIntegrationTest.TestApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false",
                "spring.jpa.properties.hibernate.generate_statistics=true",
                "spring.flyway.enabled=false",
                "sfs.log.dir=target/test-logs"
        }
)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProjectDetailQueryCountIntegrationTest {

    private static final long MAX_PROJECT_DETAIL_STATEMENTS = 12;

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("project_detail_query_count")
            .withUsername("project_test")
            .withPassword("project_test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    private final ProjectService projectService;
    private final ProjectPublicCoreService projectPublicCoreService;
    private final EntityManager entityManager;
    private final TransactionTemplate transactions;
    private final ProjectRepository projectRepository;
    private MockMvc mockMvc;
    private Long projectId;

    @Autowired
    ProjectDetailQueryCountIntegrationTest(
            ProjectService projectService,
            ProjectPublicCoreService projectPublicCoreService,
            EntityManager entityManager,
            PlatformTransactionManager transactionManager,
            ProjectRepository projectRepository
    ) {
        this.projectService = projectService;
        this.projectPublicCoreService = projectPublicCoreService;
        this.entityManager = entityManager;
        this.transactions = new TransactionTemplate(transactionManager);
        this.projectRepository = projectRepository;
    }

    @MockitoBean private ContentVersionService contentVersionService;
    @MockitoBean private ProjectFavoriteService projectFavoriteService;
    @MockitoBean private ProjectConnectivityService projectConnectivityService;
    @MockitoBean private ProjectFloorPlanService projectFloorPlanService;
    @MockitoBean private ProjectMasterPlanService projectMasterPlanService;
    @MockitoBean private BuilderHighlightItemRepository builderHighlightItemRepository;
    @MockitoBean private ProjectMeterService projectMeterService;
    @MockitoBean private ProjectDetailAnalyticsReader projectDetailAnalyticsReader;
    @MockitoBean private UserFavoriteRepository userFavoriteRepository;
    @MockitoBean private ProjectPublicCacheEvictionPublisher projectPublicCacheEvictionPublisher;

    @BeforeEach
    void seedVisibleProject() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ProjectPublicController(projectService, mock(ProjectMediaService.class)),
                new ProjectPublicV2Controller(projectPublicCoreService),
                new DashboardProjectController(
                        projectService,
                        mock(DashboardProjectOwnershipService.class),
                        mock(DashboardActionAuditService.class),
                        mock(DashboardProjectWorkspaceService.class),
                        mock(DashboardProjectValidator.class)
                )
        ).setControllerAdvice(new GlobalExceptionHandler(new LogSanitizer())).build();

        projectId = transactions.execute(status -> {
            BuilderEntity builder = BuilderEntity.builder()
                    .name("Incident Test Builder")
                    .active(true)
                    .published(true)
                    .deleted(false)
                    .priority(0)
                    .build();
            entityManager.persist(builder);

            ProjectEntity project = ProjectEntity.builder()
                    .builder(builder)
                    .name("Incident Test Project")
                    .slug("incident-test-project-" + java.util.UUID.randomUUID())
                    .active(true)
                    .published(true)
                    .deleted(false)
                    .priority(0)
                    .reviewStatus(ReviewStatus.APPROVED)
                    .propertyTypes(new java.util.HashSet<>(
                            java.util.Set.of(PropertyType.APARTMENT, PropertyType.VILLA)))
                    .build();
            entityManager.persist(project);
            entityManager.flush();
            return project.getId();
        });
    }

    @Test
    void publicProjectDetailHasABoundedSqlStatementCountAndNoLazySerialization() {
        Statistics statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.clear();

        long started = System.nanoTime();
        var response = projectService.publicGet(projectId);
        long durationMillis = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - started);
        long statementCount = statistics.getPrepareStatementCount();

        System.out.printf("project-detail statements=%d durationMs=%d%n", statementCount, durationMillis);

        assertThat(response.getId()).isEqualTo(projectId);
        assertThat(response.getPropertyTypes())
                .containsExactlyInAnyOrder(PropertyType.APARTMENT, PropertyType.VILLA);
        assertThat(response.getPropertyTypes().getClass().getName()).doesNotContain("Persistent");
        assertThat(statementCount)
                .as("project-detail SQL statement count")
                .isLessThanOrEqualTo(MAX_PROJECT_DETAIL_STATEMENTS);
    }

    @Test
    void publicProjectDetailEndpointSerializesDetachedPropertyTypes() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertyTypes").isArray())
                .andExpect(jsonPath("$.propertyTypes.length()").value(2))
                .andExpect(jsonPath("$.propertyTypes").value(
                        org.hamcrest.Matchers.containsInAnyOrder("APARTMENT", "VILLA")));
    }

    @Test
    void publicProjectDetailV2IsDeterministicAndOmitsViewerMembership() throws Exception {
        String anonymous = mockMvc.perform(get("/api/v2/public/projects/{projectId}", projectId))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control",
                        "public, max-age=0, s-maxage=30, stale-while-revalidate=30, stale-if-error=60"))
                .andExpect(jsonPath("$.favoriteCount").isNumber())
                .andExpect(jsonPath("$.isFavorite").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String userA = mockMvc.perform(get("/api/v2/public/projects/{projectId}", projectId)
                        .header("Authorization", "Bearer user-a"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String userB = mockMvc.perform(get("/api/v2/public/projects/{projectId}", projectId)
                        .header("Authorization", "Bearer user-b"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String malformed = mockMvc.perform(get("/api/v2/public/projects/{projectId}", projectId)
                        .header("Authorization", "malformed"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(userA).isEqualTo(anonymous);
        assertThat(userB).isEqualTo(anonymous);
        assertThat(malformed).isEqualTo(anonymous);
    }

    @Test
    void publicProjectDetailV2HeadUsesTheSameCacheContract() throws Exception {
        mockMvc.perform(head("/api/v2/public/projects/{projectId}", projectId))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control",
                        "public, max-age=0, s-maxage=30, stale-while-revalidate=30, stale-if-error=60"));
    }

    @Test
    void legacyProjectDetailDoesNotReceiveThePublicSharedCachePolicy() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}", projectId))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Cache-Control"));
    }

    @Test
    void missingV2ProjectDoesNotReceiveThePublicSuccessCachePolicy() throws Exception {
        mockMvc.perform(get("/api/v2/public/projects/{projectId}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(header().doesNotExist("Cache-Control"));
    }

    @Test
    void dashboardProjectPageSerializesPropertyTypesWithBoundedQueriesAndPreservedPagination()
            throws Exception {
        transactions.executeWithoutResult(status -> {
            BuilderEntity builder = entityManager.find(ProjectEntity.class, projectId).getBuilder();
            persistProject(builder, "Priority One", -2, PropertyType.PLOT);
            persistProject(builder, "Priority Two", -1, PropertyType.COMMERCIAL);
        });

        long expectedTotal = projectRepository.countByDeletedFalse();
        Statistics statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.clear();

        MvcResult result = mockMvc.perform(get("/api/dashboard/projects")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Priority One"))
                .andExpect(jsonPath("$.content[0].propertyTypes").isArray())
                .andExpect(jsonPath("$.content[0].propertyTypes[0]").value("PLOT"))
                .andExpect(jsonPath("$.content[1].name").value("Priority Two"))
                .andExpect(jsonPath("$.totalElements").value(expectedTotal))
                .andReturn();

        assertThat(result.getResolvedException()).isNull();
        assertThat(statistics.getPrepareStatementCount())
                .as("dashboard page, count, and one bulk property-types query")
                .isLessThanOrEqualTo(3);
    }

    @Test
    void publicProjectCollectionExcludesProjectWhoseBuilderIsUnpublished() {
        transactions.executeWithoutResult(status -> {
            ProjectEntity project = entityManager.find(ProjectEntity.class, projectId);
            project.getBuilder().setPublished(false);
            entityManager.flush();
        });

        var page = projectRepository
                .findByPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
                        ReviewStatus.APPROVED,
                        org.springframework.data.domain.PageRequest.of(0, 20));

        assertThat(page.getContent()).noneMatch(project -> project.getId().equals(projectId));
    }

    @Test
    void publicProjectCollectionExcludesProjectWhoseBuilderIsInactiveOrDeleted() {
        transactions.executeWithoutResult(status -> {
            ProjectEntity project = entityManager.find(ProjectEntity.class, projectId);
            project.getBuilder().setActive(false);
            entityManager.flush();
        });

        var inactivePage = projectRepository
                .findByPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
                        ReviewStatus.APPROVED,
                        org.springframework.data.domain.PageRequest.of(0, 20));
        assertThat(inactivePage.getContent()).noneMatch(project -> project.getId().equals(projectId));

        transactions.executeWithoutResult(status -> {
            ProjectEntity project = entityManager.find(ProjectEntity.class, projectId);
            project.getBuilder().setActive(true);
            project.getBuilder().setDeleted(true);
            entityManager.flush();
        });

        var deletedPage = projectRepository
                .findByPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
                        ReviewStatus.APPROVED,
                        org.springframework.data.domain.PageRequest.of(0, 20));
        assertThat(deletedPage.getContent()).noneMatch(project -> project.getId().equals(projectId));
    }

    private void persistProject(BuilderEntity builder, String name, int priority, PropertyType propertyType) {
        ProjectEntity project = ProjectEntity.builder()
                .builder(builder)
                .name(name)
                .slug(name.toLowerCase().replace(' ', '-'))
                .active(true)
                .published(true)
                .deleted(false)
                .priority(priority)
                .reviewStatus(ReviewStatus.APPROVED)
                .propertyTypes(new java.util.HashSet<>(java.util.Set.of(propertyType)))
                .build();
        entityManager.persist(project);
        entityManager.flush();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            ProjectEntity.class,
            ProjectMeterSnapshotEntity.class,
            BuilderEntity.class,
            CityEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            ProjectRepository.class,
            ProjectMeterSnapshotRepository.class,
            BuilderRepository.class
    })
    @Import({
            ProjectServiceImpl.class,
            ProjectPublicCoreServiceImpl.class,
            ProjectPublicBaseReader.class,
            ProjectPublicContentReader.class,
            ProjectDetailComposerImpl.class,
            BuilderCredibilityServiceImpl.class,
            ProjectPublicVisibilityPolicy.class
    })
    static class TestApplication {
    }
}
