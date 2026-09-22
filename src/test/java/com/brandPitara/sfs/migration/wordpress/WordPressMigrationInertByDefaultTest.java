package com.brandPitara.sfs.migration.wordpress;

import com.brandPitara.sfs.cms.author.repository.CmsPublicAuthorRepository;
import com.brandPitara.sfs.cms.content.slug.ContentSlugService;
import com.brandPitara.sfs.cms.metadata.service.CmsMetadataService;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * The WordPress migration tool must be inert by default: merely being on the classpath and
 * registered as Spring beans must never read the source SQL dump, index the media archive,
 * create a CMS author, write a PostgreSQL row, or make an S3 request. Every one of those actions
 * only happens through an explicit method call {@code resolve()}/{@code reconcile()}) that a
 * future CLI entry point makes - there is no automatic trigger here. This proves that guarantee
 * at the Spring-context level (not just by code inspection), and locks in that neither class ever
 * grows a runner/lifecycle hook that would silently break the guarantee.
 */
@SpringBootTest(classes = WordPressMigrationInertByDefaultTest.TestApplication.class)
class WordPressMigrationInertByDefaultTest {

    @MockitoBean
    private CmsMetadataService metadataService;
    @MockitoBean
    private CmsPublicAuthorRepository authorRepository;
    @MockitoBean
    private ContentSlugService slugService;

    @Test
    void registeringTheMigrationBeansPerformsNoWorkOnItsOwn() {
        verifyNoInteractions(metadataService, authorRepository, slugService);
    }

    @Test
    void neitherBeanIsARunnerOrHasALifecycleAutoTrigger() {
        for (Class<?> type : new Class<?>[]{WordPressMigrationProperties.class, WordPressAuthorResolver.class}) {
            assertThat(CommandLineRunner.class.isAssignableFrom(type)).as(type + " must not be a CommandLineRunner")
                    .isFalse();
            assertThat(ApplicationRunner.class.isAssignableFrom(type)).as(type + " must not be an ApplicationRunner")
                    .isFalse();
            for (Method method : type.getDeclaredMethods()) {
                assertThat(method.getAnnotation(PostConstruct.class))
                        .as(type + "." + method.getName() + " must not run automatically via @PostConstruct")
                        .isNull();
                assertThat(method.getAnnotation(EventListener.class))
                        .as(type + "." + method.getName() + " must not run automatically via @EventListener")
                        .isNull();
            }
        }
    }

    @Test
    void noClassInTheDumpGutenbergAuditOrCliPackagesCarriesASpringStereotypeAnnotation() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> true);

        List<Class<?>> stereotypeOffenders = new ArrayList<>();
        List<Class<?>> runnerOffenders = new ArrayList<>();
        // Unlike `importer`/`media` (real @Service beans, legitimately Spring-managed - inert
        // means "never auto-triggered", not "no annotation at all"), `cli` must have ZERO Spring
        // stereotypes: WordPressProductionImportCli/Runner/CliArgs are deliberately plain classes
        // reachable only by explicitly naming WordPressProductionImportCli on a command line - see
        // that class's own "Boot safety" doc.
        for (String basePackage : Set.of(
                "com.brandPitara.sfs.migration.wordpress.dump",
                "com.brandPitara.sfs.migration.wordpress.gutenberg",
                "com.brandPitara.sfs.migration.wordpress.audit",
                "com.brandPitara.sfs.migration.wordpress.cli"
        )) {
            for (var candidate : scanner.findCandidateComponents(basePackage)) {
                Class<?> type;
                try {
                    type = Class.forName(candidate.getBeanClassName());
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(e);
                }
                for (Class<? extends Annotation> stereotype : List.of(
                        org.springframework.stereotype.Component.class,
                        org.springframework.stereotype.Service.class,
                        org.springframework.context.annotation.Configuration.class,
                        org.springframework.stereotype.Repository.class,
                        org.springframework.boot.context.properties.ConfigurationProperties.class
                )) {
                    if (type.isAnnotationPresent(stereotype)) {
                        stereotypeOffenders.add(type);
                    }
                }
                if (CommandLineRunner.class.isAssignableFrom(type) || ApplicationRunner.class.isAssignableFrom(type)) {
                    runnerOffenders.add(type);
                }
            }
        }
        assertThat(stereotypeOffenders).as("classes carrying a Spring stereotype annotation").isEmpty();
        assertThat(runnerOffenders).as("classes implementing CommandLineRunner/ApplicationRunner").isEmpty();
    }

    @SpringBootConfiguration
    @Import({WordPressMigrationProperties.class, WordPressAuthorResolver.class})
    static class TestApplication {
    }
}
