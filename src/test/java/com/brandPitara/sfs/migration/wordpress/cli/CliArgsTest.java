package com.brandPitara.sfs.migration.wordpress.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure argument-parsing/validation unit tests for {@link WordPressProductionImportCli.CliArgs} -
 * no Spring context, no database, no network. Every one of these must fail (or succeed) before
 * {@link WordPressProductionImportCli#run} ever attempts to boot the application context.
 */
class CliArgsTest {

    @Test
    void noApplyFlagDefaultsToDryRunWithNoScopeRequired(@TempDir Path dir) throws IOException {
        var args = parse(dir, "--dump=%s", "--uploads-zip=%s", "--actor-email=you@example.com");

        assertThat(args.apply()).isFalse();
        assertThat(args.postIds()).isNull();
        assertThat(args.allPosts()).isFalse();
    }

    @Test
    void dryRunMayStillBeScopedByPostIds(@TempDir Path dir) throws IOException {
        var args = parse(dir, "--dump=%s", "--uploads-zip=%s", "--actor-email=you@example.com", "--post-ids=10043,10054");

        assertThat(args.apply()).isFalse();
        assertThat(args.postIds()).containsExactly(10043L, 10054L);
    }

    @Test
    void applyWithoutConfirmFails(@TempDir Path dir) throws IOException {
        assertThatThrownBy(() -> parse(dir, "--dump=%s", "--uploads-zip=%s", "--actor-email=you@example.com",
                "--apply", "--post-ids=597"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--confirm");
    }

    @Test
    void applyWithBlankConfirmFails(@TempDir Path dir) throws IOException {
        assertThatThrownBy(() -> parse(dir, "--dump=%s", "--uploads-zip=%s", "--actor-email=you@example.com",
                "--apply", "--confirm=", "--post-ids=597"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--confirm");
    }

    @Test
    void applyWithConfirmButNoScopeFails(@TempDir Path dir) throws IOException {
        assertThatThrownBy(() -> parse(dir, "--dump=%s", "--uploads-zip=%s", "--actor-email=you@example.com",
                "--apply", "--confirm=tok"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("explicit scope");
    }

    @Test
    void applyWithPostIdsScopeSucceedsWithoutFullCorpusConfirmation(@TempDir Path dir) throws IOException {
        var args = parse(dir, "--dump=%s", "--uploads-zip=%s", "--actor-email=you@example.com",
                "--apply", "--confirm=tok", "--post-ids=10043,10054");

        assertThat(args.apply()).isTrue();
        assertThat(args.allPosts()).isFalse();
        assertThat(args.postIds()).containsExactly(10043L, 10054L);
    }

    @Test
    void applyWithEmptyPostIdsListFails(@TempDir Path dir) throws IOException {
        assertThatThrownBy(() -> parse(dir, "--dump=%s", "--uploads-zip=%s", "--actor-email=you@example.com",
                "--apply", "--confirm=tok", "--post-ids="))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void applyWithAllPostsButNoFullCorpusConfirmationFails(@TempDir Path dir) throws IOException {
        assertThatThrownBy(() -> parse(dir, "--dump=%s", "--uploads-zip=%s", "--actor-email=you@example.com",
                "--apply", "--confirm=tok", "--all-posts"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("confirm-full-corpus");
    }

    @Test
    void applyWithAllPostsAndFullCorpusConfirmationSucceeds(@TempDir Path dir) throws IOException {
        var args = parse(dir, "--dump=%s", "--uploads-zip=%s", "--actor-email=you@example.com",
                "--apply", "--confirm=tok", "--all-posts", "--confirm-full-corpus=tok");

        assertThat(args.apply()).isTrue();
        assertThat(args.allPosts()).isTrue();
        assertThat(args.postIds()).isNull();
        assertThat(args.confirmFullCorpus()).isEqualTo("tok");
    }

    @Test
    void postIdsAndAllPostsAreMutuallyExclusive(@TempDir Path dir) throws IOException {
        assertThatThrownBy(() -> parse(dir, "--dump=%s", "--uploads-zip=%s", "--actor-email=you@example.com",
                "--apply", "--confirm=tok", "--post-ids=1", "--all-posts", "--confirm-full-corpus=tok"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mutually exclusive");
    }

    @Test
    void unrecognizedFlagFailsClosed(@TempDir Path dir) throws IOException {
        assertThatThrownBy(() -> parse(dir, "--dump=%s", "--uploads-zip=%s", "--actor-email=you@example.com",
                "--wipe-production-database=true"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unrecognized argument");
    }

    @Test
    void malformedFlagWithoutEqualsSignFailsClosed(@TempDir Path dir) throws IOException {
        assertThatThrownBy(() -> parse(dir, "--dump=%s", "--uploads-zip=%s", "--actor-email"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void missingRequiredFlagsFail(@TempDir Path dir) throws IOException {
        assertThatThrownBy(() -> parse(dir, "--dump=%s"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uploads-zip");
    }

    @Test
    void nonexistentDumpFileFails(@TempDir Path dir) throws IOException {
        Path uploadsZip = dir.resolve("uploads.zip");
        Files.createFile(uploadsZip);

        assertThatThrownBy(() -> WordPressProductionImportCli.CliArgs.parse(new String[] {
                "--dump=" + dir.resolve("does-not-exist.sql"),
                "--uploads-zip=" + uploadsZip,
                "--actor-email=you@example.com"
        })).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("dump file does not exist");
    }

    @Test
    void nonexistentUploadsZipFails(@TempDir Path dir) throws IOException {
        Path dump = dir.resolve("dump.sql");
        Files.createFile(dump);

        assertThatThrownBy(() -> WordPressProductionImportCli.CliArgs.parse(new String[] {
                "--dump=" + dump,
                "--uploads-zip=" + dir.resolve("does-not-exist.zip"),
                "--actor-email=you@example.com"
        })).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("uploads-zip file does not exist");
    }

    /** Builds real (empty but existing) dump/zip files under {@code dir} and parses the given template args. */
    private WordPressProductionImportCli.CliArgs parse(Path dir, String... argTemplates) throws IOException {
        Path dump = dir.resolve("dump.sql");
        Path uploadsZip = dir.resolve("uploads.zip");
        if (!Files.exists(dump)) Files.createFile(dump);
        if (!Files.exists(uploadsZip)) Files.createFile(uploadsZip);

        String[] args = new String[argTemplates.length];
        for (int i = 0; i < argTemplates.length; i++) {
            String template = argTemplates[i];
            if (template.contains("--dump=%s")) {
                args[i] = "--dump=" + dump;
            } else if (template.contains("--uploads-zip=%s")) {
                args[i] = "--uploads-zip=" + uploadsZip;
            } else {
                args[i] = template;
            }
        }
        return WordPressProductionImportCli.CliArgs.parse(args);
    }
}
