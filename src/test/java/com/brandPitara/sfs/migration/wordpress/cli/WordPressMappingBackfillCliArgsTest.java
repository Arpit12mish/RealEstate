package com.brandPitara.sfs.migration.wordpress.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure argument-parsing/validation unit tests for {@link WordPressMappingBackfillCli.CliArgs} - no
 * Spring context, no database.
 */
class WordPressMappingBackfillCliArgsTest {

    @Test
    void noApplyFlagDefaultsToDryRun(@TempDir Path dir) throws IOException {
        var args = parse(dir, "--pairs=10054:15");

        assertThat(args.apply()).isFalse();
        assertThat(args.pairs()).containsEntry(10054L, 15L);
    }

    @Test
    void parsesMultiplePairs(@TempDir Path dir) throws IOException {
        var args = parse(dir, "--pairs=10054:15,10043:14,9923:10");

        assertThat(args.pairs()).containsOnly(
                java.util.Map.entry(10054L, 15L),
                java.util.Map.entry(10043L, 14L),
                java.util.Map.entry(9923L, 10L)
        );
    }

    @Test
    void duplicateSourceIdInManifestFails(@TempDir Path dir) throws IOException {
        assertThatThrownBy(() -> parse(dir, "--pairs=10054:15,10054:99"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10054")
                .hasMessageContaining("more than once");
    }

    @Test
    void duplicateTargetIdInManifestFails(@TempDir Path dir) throws IOException {
        assertThatThrownBy(() -> parse(dir, "--pairs=10054:15,10043:15"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("15")
                .hasMessageContaining("cannot map to the same target");
    }

    @Test
    void malformedPairFailsClosed(@TempDir Path dir) throws IOException {
        assertThatThrownBy(() -> parse(dir, "--pairs=10054-15"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Malformed pair");
    }

    @Test
    void nonNumericPairFailsClosed(@TempDir Path dir) throws IOException {
        assertThatThrownBy(() -> parse(dir, "--pairs=abc:15"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("numeric");
    }

    @Test
    void emptyPairsFails(@TempDir Path dir) throws IOException {
        assertThatThrownBy(() -> parse(dir, "--pairs="))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void applyWithoutConfirmFails(@TempDir Path dir) throws IOException {
        assertThatThrownBy(() -> parse(dir, "--pairs=10054:15", "--apply"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--confirm");
    }

    @Test
    void applyWithBlankConfirmFails(@TempDir Path dir) throws IOException {
        assertThatThrownBy(() -> parse(dir, "--pairs=10054:15", "--apply", "--confirm="))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--confirm");
    }

    @Test
    void applyWithConfirmSucceeds(@TempDir Path dir) throws IOException {
        var args = parse(dir, "--pairs=10054:15", "--apply", "--confirm=tok");

        assertThat(args.apply()).isTrue();
        assertThat(args.confirm()).isEqualTo("tok");
    }

    @Test
    void unrecognizedFlagFailsClosed(@TempDir Path dir) throws IOException {
        assertThatThrownBy(() -> parse(dir, "--pairs=10054:15", "--wipe-everything=true"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unrecognized argument");
    }

    @Test
    void missingRequiredFlagsFail(@TempDir Path dir) throws IOException {
        Path dump = dir.resolve("dump.sql");
        Files.createFile(dump);
        assertThatThrownBy(() -> WordPressMappingBackfillCli.CliArgs.parse(new String[] {
                "--dump=" + dump
        })).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("actor-email");
    }

    @Test
    void nonexistentDumpFileFails(@TempDir Path dir) {
        assertThatThrownBy(() -> WordPressMappingBackfillCli.CliArgs.parse(new String[] {
                "--dump=" + dir.resolve("does-not-exist.sql"),
                "--actor-email=you@example.com",
                "--pairs=10054:15"
        })).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("dump file does not exist");
    }

    private WordPressMappingBackfillCli.CliArgs parse(Path dir, String... extraArgs) throws IOException {
        Path dump = dir.resolve("dump.sql");
        if (!Files.exists(dump)) {
            Files.createFile(dump);
        }
        String[] args = new String[extraArgs.length + 2];
        args[0] = "--dump=" + dump;
        args[1] = "--actor-email=you@example.com";
        System.arraycopy(extraArgs, 0, args, 2, extraArgs.length);
        return WordPressMappingBackfillCli.CliArgs.parse(args);
    }
}
