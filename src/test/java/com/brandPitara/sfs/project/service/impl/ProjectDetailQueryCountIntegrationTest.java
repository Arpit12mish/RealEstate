package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.buildercredibility.service.impl.BuilderCredibilityServiceImpl;
import com.brandPitara.sfs.builderhighlight.repository.BuilderHighlightItemRepository;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.project.service.ProjectConnectivityService;
import com.brandPitara.sfs.project.service.ProjectFavoriteService;
import com.brandPitara.sfs.project.service.ProjectFloorPlanService;
import com.brandPitara.sfs.project.service.ProjectMasterPlanService;
import com.brandPitara.sfs.project.service.ProjectService;
import com.brandPitara.sfs.projectmeter.entity.ProjectMeterSnapshotEntity;
import com.brandPitara.sfs.projectmeter.repository.ProjectMeterSnapshotRepository;
import com.brandPitara.sfs.projectmeter.service.impl.ProjectMeterServiceImpl;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ProjectDetailQueryCountIntegrationTest.TestApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false",
                "spring.jpa.properties.hibernate.generate_statistics=true",
                "spring.flyway.enabled=false",
                "app.logging.path=target/test-logs"
        }
)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProjectDetailQueryCountIntegrationTest {

    private static final long MAX_PROJECT_DETAIL_STATEMENTS = 20;

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
    private final EntityManager entityManager;
    private final TransactionTemplate transactions;
    private Long projectId;

    @Autowired
    ProjectDetailQueryCountIntegrationTest(
            ProjectService projectService,
            EntityManager entityManager,
            PlatformTransactionManager transactionManager
    ) {
        this.projectService = projectService;
        this.entityManager = entityManager;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @MockitoBean private ContentVersionService contentVersionService;
    @MockitoBean private ProjectFavoriteService projectFavoriteService;
    @MockitoBean private ProjectConnectivityService projectConnectivityService;
    @MockitoBean private ProjectFloorPlanService projectFloorPlanService;
    @MockitoBean private ProjectMasterPlanService projectMasterPlanService;
    @MockitoBean private BuilderHighlightItemRepository builderHighlightItemRepository;

    @BeforeEach
    void seedVisibleProject() {
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
                    .slug("incident-test-project")
                    .active(true)
                    .published(true)
                    .deleted(false)
                    .priority(0)
                    .reviewStatus(ReviewStatus.APPROVED)
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
        assertThat(statementCount)
                .as("project-detail SQL statement count")
                .isLessThanOrEqualTo(MAX_PROJECT_DETAIL_STATEMENTS);
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
            ProjectDetailComposerImpl.class,
            ProjectMeterServiceImpl.class,
            BuilderCredibilityServiceImpl.class,
            ProjectPublicVisibilityPolicy.class
    })
    static class TestApplication {
    }
}
