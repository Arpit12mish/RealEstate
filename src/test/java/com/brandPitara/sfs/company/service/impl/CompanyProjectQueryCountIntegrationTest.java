package com.brandPitara.sfs.company.service.impl;

import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.repository.BrandCollaborationRepository;
import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.company.dto.ArchitectDesignerDetailResponse;
import com.brandPitara.sfs.company.dto.CompanyProjectCardDto;
import com.brandPitara.sfs.company.dto.CompanyProjectResponse;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.entity.CompanyProjectEntity;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.company.service.ArchitectDesignerPublicService;
import com.brandPitara.sfs.company.service.CompanyConnectedBrandPublicService;
import com.brandPitara.sfs.company.service.CompanyProjectPublicService;
import com.brandPitara.sfs.distributor.entity.DistributorEntity;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.publicreview.dto.PublicReviewSignalResponse;
import com.brandPitara.sfs.publicreview.enums.PublicReviewSourceType;
import com.brandPitara.sfs.publicreview.enums.PublicReviewTargetType;
import com.brandPitara.sfs.publicreview.service.PublicReviewService;
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
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Regression coverage for the LazyInitializationException that made
 * GET /api/public/architect-designers/{id}, /api/public/company-projects/{id}, and
 * /api/public/companies/{id}/projects return 500 for an existing, visible company: the public
 * read services called a repository method that returned without eagerly fetching
 * CompanyProjectEntity.company/.city, then a mapper dereferenced those LAZY proxies after the
 * repository call's own session had already closed (open-in-view is disabled). Runs against a
 * real Postgres session (Testcontainers) rather than Mockito, since a mock never produces a real
 * Hibernate proxy and therefore cannot reproduce this failure mode.
 */
@SpringBootTest(
        classes = CompanyProjectQueryCountIntegrationTest.TestApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false",
                "spring.jpa.properties.hibernate.generate_statistics=true",
                "spring.flyway.enabled=false"
        }
)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CompanyProjectQueryCountIntegrationTest {

    private static final long MAX_ARCHITECT_DETAIL_STATEMENTS = 12;
    private static final long MAX_COMPANY_PROJECT_DETAIL_STATEMENTS = 4;
    private static final long MAX_COMPANY_PROJECT_LIST_STATEMENTS = 2;

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("company_project_query_count")
            .withUsername("company_test")
            .withPassword("company_test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    private final ArchitectDesignerPublicService architectDesignerPublicService;
    private final CompanyProjectPublicService companyProjectPublicService;
    private final EntityManager entityManager;
    private final TransactionTemplate transactions;

    @Autowired
    CompanyProjectQueryCountIntegrationTest(
            ArchitectDesignerPublicService architectDesignerPublicService,
            CompanyProjectPublicService companyProjectPublicService,
            EntityManager entityManager,
            PlatformTransactionManager transactionManager
    ) {
        this.architectDesignerPublicService = architectDesignerPublicService;
        this.companyProjectPublicService = companyProjectPublicService;
        this.entityManager = entityManager;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @MockitoBean private CompanyConnectedBrandPublicService companyConnectedBrandPublicService;
    @MockitoBean private PublicReviewService publicReviewService;
    @MockitoBean private BrandCollaborationRepository brandCollaborationRepository;

    private Long companyId;
    private Long firstProjectId;

    @BeforeEach
    void seedVisibleCompanyWithProjectsAcrossCities() {
        when(companyConnectedBrandPublicService.getConnectedBrands(anyLong())).thenReturn(List.of());
        when(publicReviewService.getPublicSignal(any(PublicReviewTargetType.class), anyLong()))
                .thenReturn(emptySignal());
        when(brandCollaborationRepository.findPublicByCompanyProjectId(anyLong(), any(Pageable.class)))
                .thenReturn(List.of());

        companyId = transactions.execute(status -> {
            CityEntity delhi = CityEntity.builder()
                    .name("New Delhi").slug("new-delhi-" + UUID.randomUUID())
                    .active(true).build();
            entityManager.persist(delhi);

            CityEntity gurugram = CityEntity.builder()
                    .name("Gurugram").slug("gurugram-" + UUID.randomUUID())
                    .active(true).build();
            entityManager.persist(gurugram);

            CompanyEntity company = CompanyEntity.builder()
                    .name("Regression Test Architects")
                    .slug("regression-test-architects-" + UUID.randomUUID())
                    .companyType("ARCHITECT&DESIGNERS")
                    .active(true).published(true).deleted(false)
                    .build();
            entityManager.persist(company);

            for (int i = 0; i < 3; i++) {
                CompanyProjectEntity project = CompanyProjectEntity.builder()
                        .company(company)
                        .city(i % 2 == 0 ? delhi : gurugram)
                        .name("Regression Project " + i)
                        .published(true).active(true).deleted(false)
                        .priority(i)
                        .build();
                entityManager.persist(project);
            }
            entityManager.flush();
            return company.getId();
        });

        firstProjectId = entityManager.createQuery(
                        "select p.id from CompanyProjectEntity p where p.company.id = :companyId order by p.id asc",
                        Long.class)
                .setParameter("companyId", companyId)
                .setMaxResults(1)
                .getSingleResult();
    }

    @Test
    void architectDetailPopulatesCompanyAndCityWithoutLazyInitializationExceptionAndBoundedQueries() {
        Statistics statistics = statistics();
        statistics.clear();

        ArchitectDesignerDetailResponse response = architectDesignerPublicService.getDetail(companyId);

        assertThat(response.getName()).isEqualTo("Regression Test Architects");
        assertThat(response.getTopProjects()).hasSize(3);
        assertThat(response.getTopProjects())
                .extracting(CompanyProjectCardDto::getCompanyName)
                .containsOnly("Regression Test Architects");
        assertThat(response.getTopProjects())
                .extracting(CompanyProjectCardDto::getCityName)
                .containsExactlyInAnyOrder("New Delhi", "Gurugram", "New Delhi");

        assertThat(statistics.getPrepareStatementCount())
                .as("architect-designer detail SQL statement count")
                .isLessThanOrEqualTo(MAX_ARCHITECT_DETAIL_STATEMENTS);
    }

    @Test
    void companyProjectDetailPopulatesCompanyAndCityWithoutLazyInitializationExceptionAndBoundedQueries() {
        Statistics statistics = statistics();
        statistics.clear();

        CompanyProjectResponse response = companyProjectPublicService.publicGet(firstProjectId);

        assertThat(response.getCompanyName()).isEqualTo("Regression Test Architects");
        assertThat(response.getCityName()).isNotBlank();

        assertThat(statistics.getPrepareStatementCount())
                .as("company-project detail SQL statement count")
                .isLessThanOrEqualTo(MAX_COMPANY_PROJECT_DETAIL_STATEMENTS);
    }

    @Test
    void companyProjectListPopulatesCompanyAndCityWithBoundedQueriesRegardlessOfRowCount() {
        Statistics statistics = statistics();
        statistics.clear();

        Pageable pageable = PageRequest.of(0, 20, Sort.by("priority").ascending());
        Page<CompanyProjectCardDto> page = companyProjectPublicService.publicListByCompany(companyId, pageable);

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent())
                .extracting(CompanyProjectCardDto::getCompanyName)
                .containsOnly("Regression Test Architects");
        assertThat(page.getContent())
                .extracting(CompanyProjectCardDto::getCityName)
                .doesNotContainNull();

        // Bounded by the number of association types fetched (company+city, joined into the
        // primary query), not by row count - proves this is not a per-row N+1.
        assertThat(statistics.getPrepareStatementCount())
                .as("company-project list SQL statement count")
                .isLessThanOrEqualTo(MAX_COMPANY_PROJECT_LIST_STATEMENTS);
    }

    @Test
    void architectDetailReturns404ForMissingCompany() {
        assertThatThrownBy(() -> architectDesignerPublicService.getDetail(Long.MAX_VALUE))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);
    }

    @Test
    void architectDetailReturns404ForUnpublishedCompany() {
        Long unpublishedCompanyId = transactions.execute(status -> {
            CompanyEntity unpublished = CompanyEntity.builder()
                    .name("Hidden Company")
                    .slug("hidden-company-" + UUID.randomUUID())
                    .companyType("ARCHITECT&DESIGNERS")
                    .active(true).published(false).deleted(false)
                    .build();
            entityManager.persist(unpublished);
            entityManager.flush();
            return unpublished.getId();
        });

        assertThatThrownBy(() -> architectDesignerPublicService.getDetail(unpublishedCompanyId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);
    }

    @Test
    void companyProjectDetailReturns404ForMissingProject() {
        assertThatThrownBy(() -> companyProjectPublicService.publicGet(Long.MAX_VALUE))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(404);
    }

    private Statistics statistics() {
        return entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
    }

    private PublicReviewSignalResponse emptySignal() {
        return PublicReviewSignalResponse.builder()
                .targetType(PublicReviewTargetType.COMPANY)
                .sourceType(PublicReviewSourceType.GOOGLE_PLACES)
                .rating(null)
                .userRatingCount(0)
                .sourceLabel("Google Maps")
                .places(List.of())
                .samples(List.of())
                .build();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            CompanyEntity.class,
            CityEntity.class,
            BrandEntity.class,
            BuilderEntity.class,
            ProjectEntity.class,
            DistributorEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            CompanyRepository.class
    })
    @Import({
            ArchitectDesignerPublicServiceImpl.class,
            CompanyProjectPublicServiceImpl.class
    })
    static class TestApplication {
    }
}
