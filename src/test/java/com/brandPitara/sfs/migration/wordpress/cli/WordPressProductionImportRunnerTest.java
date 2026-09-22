package com.brandPitara.sfs.migration.wordpress.cli;

import com.brandPitara.sfs.cms.content.repository.ContentPostRepository;
import com.brandPitara.sfs.cms.media.config.CmsMediaProperties;
import com.brandPitara.sfs.cms.media.repository.CmsMediaAssetRepository;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import com.brandPitara.sfs.migration.wordpress.importer.WordPressContentImportService;
import com.brandPitara.sfs.migration.wordpress.importer.WordPressImportOutcome;
import com.brandPitara.sfs.migration.wordpress.importer.WordPressImportRequest;
import com.brandPitara.sfs.migration.wordpress.importer.WordPressMigrationMappingRepository;
import com.brandPitara.sfs.migration.wordpress.importer.WordPressMigrationState;
import com.brandPitara.sfs.migration.wordpress.media.WordPressMediaImportOutcome;
import com.brandPitara.sfs.migration.wordpress.media.WordPressMediaImportRequest;
import com.brandPitara.sfs.migration.wordpress.media.WordPressMediaImportService;
import com.brandPitara.sfs.migration.wordpress.media.WordPressMediaMappingRepository;
import com.brandPitara.sfs.migration.wordpress.media.WordPressMigrationMediaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit-level tests for {@link WordPressProductionImportRunner} - every dependency is a Mockito
 * mock (no Spring context, no database, no S3), except the dump/zip reading path, which uses tiny
 * synthetic local fixtures built per-test so these run in any environment, unlike the opt-in
 * verification classes that require the real ~154MB export. Exercises exactly the checklist this
 * class exists to prove: invalid actor fails before any import call, dry-run never mutates,
 * authorization gating is all-or-nothing, and a scoped apply touches only the requested posts.
 */
class WordPressProductionImportRunnerTest {

    private WordPressContentImportService contentImportService;
    private WordPressMediaImportService mediaImportService;
    private WordPressMigrationMappingRepository mappingRepository;
    private WordPressMediaMappingRepository mediaMappingRepository;
    private DashboardUserRepository dashboardUserRepository;
    private ContentPostRepository contentPostRepository;
    private CmsMediaAssetRepository mediaAssetRepository;
    private CmsMediaProperties cmsMediaProperties;
    private WordPressMigrationMediaProperties migrationMediaProperties;
    private WordPressProductionImportRunner runner;

    private final DashboardUserEntity actor = DashboardUserEntity.builder()
            .id(1L).name("Test Actor").email("actor@example.com").build();

    @BeforeEach
    void setUp() {
        contentImportService = mock(WordPressContentImportService.class);
        mediaImportService = mock(WordPressMediaImportService.class);
        mappingRepository = mock(WordPressMigrationMappingRepository.class);
        mediaMappingRepository = mock(WordPressMediaMappingRepository.class);
        dashboardUserRepository = mock(DashboardUserRepository.class);
        contentPostRepository = mock(ContentPostRepository.class);
        mediaAssetRepository = mock(CmsMediaAssetRepository.class);
        cmsMediaProperties = mock(CmsMediaProperties.class);
        migrationMediaProperties = mock(WordPressMigrationMediaProperties.class);

        runner = new WordPressProductionImportRunner(
                contentImportService, mediaImportService, mappingRepository, mediaMappingRepository,
                dashboardUserRepository, contentPostRepository, mediaAssetRepository,
                cmsMediaProperties, migrationMediaProperties
        );

        when(mappingRepository.findBySourceSystemAndSourcePostId(anyString(), org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(Optional.empty());
        when(mediaMappingRepository.findBySourceSystemAndWordPressAttachmentId(anyString(), org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(Optional.empty());
    }

    @Test
    void invalidActorFailsBeforeAnyImportCall(@TempDir Path dir) throws IOException {
        when(dashboardUserRepository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());
        var args = cliArgs(dir, false, null, false, null, null);

        int exitCode = runner.run(args, out(), out());

        assertThat(exitCode).isEqualTo(WordPressProductionImportRunner.EXIT_ACTOR_NOT_FOUND);
        verifyNoInteractions(contentImportService, mediaImportService);
    }

    @Test
    void dryRunNeverCallsContentOrMediaImportServices(@TempDir Path dir) throws IOException {
        when(dashboardUserRepository.findByEmailIgnoreCase("actor@example.com")).thenReturn(Optional.of(actor));
        Path dump = writeDump(dir, twoPublishedPostsSql());
        Path zip = writeZip(dir);
        var args = new WordPressProductionImportCli.CliArgs(
                dump, zip, "actor@example.com", false, null, null, false, null
        );

        int exitCode = runner.run(args, out(), out());

        assertThat(exitCode).isEqualTo(WordPressProductionImportRunner.EXIT_SUCCESS);
        verifyNoInteractions(contentImportService, mediaImportService);
    }

    @Test
    void applyWithoutAuthorizationGateFailsAndWritesNothing(@TempDir Path dir) throws IOException {
        when(dashboardUserRepository.findByEmailIgnoreCase("actor@example.com")).thenReturn(Optional.of(actor));
        when(migrationMediaProperties.isRealUploadAuthorized(any())).thenReturn(false);
        Path dump = writeDump(dir, twoPublishedPostsSql());
        Path zip = writeZip(dir);
        var args = new WordPressProductionImportCli.CliArgs(
                dump, zip, "actor@example.com", true, "correct-token", java.util.Set.of(601L), false, null
        );

        int exitCode = runner.run(args, out(), out());

        assertThat(exitCode).isEqualTo(WordPressProductionImportRunner.EXIT_AUTHORIZATION_FAILED);
        verifyNoInteractions(contentImportService, mediaImportService);
    }

    @Test
    void applyWithWrongConfirmTokenFailsEvenIfGateIsAuthorized(@TempDir Path dir) throws IOException {
        when(dashboardUserRepository.findByEmailIgnoreCase("actor@example.com")).thenReturn(Optional.of(actor));
        when(migrationMediaProperties.isRealUploadAuthorized(any())).thenReturn(true);
        when(migrationMediaProperties.getProductionConfirmationToken()).thenReturn("correct-token");
        Path dump = writeDump(dir, twoPublishedPostsSql());
        Path zip = writeZip(dir);
        var args = new WordPressProductionImportCli.CliArgs(
                dump, zip, "actor@example.com", true, "wrong-token", java.util.Set.of(601L), false, null
        );

        int exitCode = runner.run(args, out(), out());

        assertThat(exitCode).isEqualTo(WordPressProductionImportRunner.EXIT_AUTHORIZATION_FAILED);
        verifyNoInteractions(contentImportService, mediaImportService);
    }

    @Test
    void allPostsApplyRequiresItsOwnMatchingFullCorpusConfirmation(@TempDir Path dir) throws IOException {
        when(dashboardUserRepository.findByEmailIgnoreCase("actor@example.com")).thenReturn(Optional.of(actor));
        when(migrationMediaProperties.isRealUploadAuthorized(any())).thenReturn(true);
        when(migrationMediaProperties.getProductionConfirmationToken()).thenReturn("correct-token");
        Path dump = writeDump(dir, twoPublishedPostsSql());
        Path zip = writeZip(dir);
        // --confirm matches, but --confirm-full-corpus does not.
        var args = new WordPressProductionImportCli.CliArgs(
                dump, zip, "actor@example.com", true, "correct-token", null, true, "wrong-token"
        );

        int exitCode = runner.run(args, out(), out());

        assertThat(exitCode).isEqualTo(WordPressProductionImportRunner.EXIT_AUTHORIZATION_FAILED);
        verifyNoInteractions(contentImportService, mediaImportService);
    }

    @Test
    void correctlyAuthorizedApplyImportsOnlyTheScopedPosts(@TempDir Path dir) throws IOException {
        when(dashboardUserRepository.findByEmailIgnoreCase("actor@example.com")).thenReturn(Optional.of(actor));
        when(migrationMediaProperties.isRealUploadAuthorized(any())).thenReturn(true);
        when(migrationMediaProperties.getProductionConfirmationToken()).thenReturn("correct-token");
        when(contentImportService.importPost(any())).thenAnswer(invocation -> {
            WordPressImportRequest request = invocation.getArgument(0);
            return new WordPressImportOutcome(request.post().id(), WordPressMigrationState.PUBLISHED, 900L, false, null);
        });

        Path dump = writeDump(dir, twoPublishedPostsSql());
        Path zip = writeZip(dir);
        // Corpus has posts 601 and 602; scope this apply to 601 only.
        var args = new WordPressProductionImportCli.CliArgs(
                dump, zip, "actor@example.com", true, "correct-token", java.util.Set.of(601L), false, null
        );

        int exitCode = runner.run(args, out(), out());

        assertThat(exitCode).isEqualTo(WordPressProductionImportRunner.EXIT_SUCCESS);
        verify(contentImportService, times(1))
                .importPost(argThat(req -> req.post().id() == 601L));
        verify(contentImportService, never())
                .importPost(argThat(req -> req.post().id() == 602L));
    }

    @Test
    void unresolvedDumpPathReturnsDumpReadFailureExitCode(@TempDir Path dir) throws IOException {
        when(dashboardUserRepository.findByEmailIgnoreCase("actor@example.com")).thenReturn(Optional.of(actor));
        // A directory (not a file) makes WordPressDumpReader.read throw IOException.
        Path dump = dir.resolve("not-a-real-dump-dir");
        Files.createDirectory(dump);
        Path zip = writeZip(dir);
        var args = new WordPressProductionImportCli.CliArgs(
                dump, zip, "actor@example.com", false, null, null, false, null
        );

        int exitCode = runner.run(args, out(), out());

        assertThat(exitCode).isEqualTo(WordPressProductionImportRunner.EXIT_DUMP_READ_FAILED);
        verifyNoInteractions(contentImportService, mediaImportService);
    }

    private WordPressProductionImportCli.CliArgs cliArgs(
            Path dir, boolean apply, String confirm, boolean allPosts, java.util.Set<Long> postIds, String confirmFullCorpus
    ) throws IOException {
        Path dump = writeDump(dir, twoPublishedPostsSql());
        Path zip = writeZip(dir);
        return new WordPressProductionImportCli.CliArgs(
                dump, zip, "nobody@example.com", apply, confirm, postIds, allPosts, confirmFullCorpus
        );
    }

    private String twoPublishedPostsSql() {
        return """
                INSERT INTO `wp_posts` (`ID`, `post_author`, `post_date`, `post_date_gmt`, `post_content`,
                    `post_title`, `post_excerpt`, `post_status`, `comment_status`, `ping_status`,
                    `post_password`, `post_name`, `to_ping`, `pinged`, `post_modified`, `post_modified_gmt`,
                    `post_content_filtered`, `post_parent`, `guid`, `menu_order`, `post_type`,
                    `post_mime_type`, `comment_count`) VALUES
                (601, 1, '2024-01-01 00:00:00', '2024-01-01 00:00:00', '<p>First post body.</p>', 'First Post',
                    '', 'publish', 'closed', 'closed', '', 'first-post', '', '', '2024-01-01 00:00:00',
                    '2024-01-01 00:00:00', '', 0, 'https://example.com/?p=601', 0, 'post', '', 0),
                (602, 1, '2024-01-01 00:00:00', '2024-01-01 00:00:00', '<p>Second post body.</p>', 'Second Post',
                    '', 'publish', 'closed', 'closed', '', 'second-post', '', '', '2024-01-01 00:00:00',
                    '2024-01-01 00:00:00', '', 0, 'https://example.com/?p=602', 0, 'post', '', 0);
                """;
    }

    private Path writeDump(Path dir, String sql) throws IOException {
        Path dump = dir.resolve("dump-" + System.nanoTime() + ".sql");
        Files.writeString(dump, sql, StandardCharsets.UTF_8);
        return dump;
    }

    private Path writeZip(Path dir) throws IOException {
        Path zip = dir.resolve("uploads-" + System.nanoTime() + ".zip");
        try (var out = new ZipOutputStream(Files.newOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("placeholder.txt"));
            out.write("unused".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return zip;
    }

    private PrintStream out() {
        return new PrintStream(new ByteArrayOutputStream());
    }
}
