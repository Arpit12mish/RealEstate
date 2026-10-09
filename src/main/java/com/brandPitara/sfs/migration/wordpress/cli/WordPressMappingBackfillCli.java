package com.brandPitara.sfs.migration.wordpress.cli;

import com.brandPitara.sfs.SfsApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A narrowly-scoped companion to {@link WordPressProductionImportCli}: reconciles WordPress source
 * posts that were already migrated to this CMS <strong>by hand</strong> (a dashboard operator
 * re-authored and published the article directly, never through this importer) before the real
 * importer ever ran against production. Writes <strong>only</strong> to {@code
 * wordpress_migration_mapping} - never touches {@code content_post}, never touches {@code
 * cms_media_asset}, never calls S3. Its entire purpose is to make {@link
 * WordPressProductionImportCli}'s own idempotency check ({@code
 * WordPressContentImportService#importPost}'s "existing mapping, fingerprint matches -> skip")
 * correctly recognize these posts as already-done, so a later real import run does not create
 * duplicate published articles for content that already exists.
 *
 * <p>Every pair is explicit and operator-supplied - this tool never guesses a WordPress-post-ID to
 * content-post-ID correspondence by title/slug similarity itself; get that wrong and a real source
 * post silently gets "marked done" against the wrong target forever (or vice versa). It does,
 * however, independently re-verify every supplied pair (slug/title identity, uniqueness, target
 * existence and status, conflicting mappings) before writing anything - see {@link
 * WordPressMappingBackfillRunner} for exactly what is checked and how a real content difference
 * between the WordPress source and the manually-created target is reported rather than hidden.
 *
 * <h2>Usage</h2>
 * <pre>
 *   # Dry run (default) - resolves each pair, computes the real fingerprint, compares WordPress
 *   # source against the current target, and reports exactly what would be written; makes zero
 *   # writes:
 *   java -cp target/classes:$(cat .cp) com.brandPitara.sfs.migration.wordpress.cli.WordPressMappingBackfillCli \
 *       --dump=/path/to/dump.sql --actor-email=you@example.com \
 *       --pairs=10054:15,10043:14,9923:10
 *
 *   # Real write - a backfill-specific confirmation token (sfs.migration.wordpress.mapping-backfill.*),
 *   # deliberately never the media-import token:
 *   java -cp target/classes:$(cat .cp) com.brandPitara.sfs.migration.wordpress.cli.WordPressMappingBackfillCli \
 *       --dump=/path/to/dump.sql --actor-email=you@example.com \
 *       --pairs=10054:15,10043:14,9923:10 --apply --confirm=&lt;backfill token&gt;
 * </pre>
 *
 * <p>Boot safety is identical to {@link WordPressProductionImportCli}: {@code
 * WebApplicationType.NONE}, Flyway/ddl-auto/every {@code @Scheduled} job/both unconditional
 * runners forced off - see {@link WordPressProductionImportCli#applySafetySystemProperties()},
 * reused here unchanged, never re-implemented.
 */
public final class WordPressMappingBackfillCli {

    private WordPressMappingBackfillCli() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        CliArgs parsed;
        try {
            parsed = CliArgs.parse(args);
        } catch (IllegalArgumentException invalid) {
            err.println("Invalid arguments: " + invalid.getMessage());
            printUsage(err);
            return 2;
        }

        out.println("========================================================");
        out.println("WordPress mapping backfill CLI - mode: " + (parsed.apply() ? "APPLY (real write)" : "DRY RUN (no writes)"));
        out.println("dump: " + parsed.dumpPath());
        out.println("actor: " + parsed.actorEmail());
        out.println("pairs: " + parsed.pairs());
        out.println("========================================================");

        WordPressProductionImportCli.applySafetySystemProperties();
        ConfigurableApplicationContext context = new SpringApplicationBuilder(SfsApplication.class)
                .web(WebApplicationType.NONE)
                .build()
                .run();
        try {
            return WordPressMappingBackfillRunner.fromContext(context).run(parsed, out, err);
        } finally {
            context.close();
        }
    }

    private static void printUsage(PrintStream err) {
        err.println("""
                Usage:
                  WordPressMappingBackfillCli --dump=<path> --actor-email=<email> \
                --pairs=<wpPostId>:<contentPostId>,... [--apply --confirm=<backfill token>]

                Without --apply, runs a read-only dry run: resolves each pair against the dump and \
                the current content_post row, computes the real idempotency fingerprint, compares \
                source against target, and reports exactly what would be written - no database writes.

                Every pair must be matched by a human first (title/slug comparison) - this tool never \
                infers a correspondence itself, only independently verifies and records one you supply.

                If any pair fails validation (identity mismatch, duplicate id in the manifest, a \
                conflicting existing mapping, an unsupported target status), --apply writes nothing \
                at all - the whole operation is all-or-nothing.
                """);
    }

    record CliArgs(
            Path dumpPath, String actorEmail, Map<Long, Long> pairs, boolean apply, String confirm
    ) {
        static CliArgs parse(String[] args) {
            Map<String, String> flags = new LinkedHashMap<>();
            boolean apply = false;
            for (String arg : args) {
                if ("--apply".equals(arg)) {
                    apply = true;
                    continue;
                }
                int eq = arg.indexOf('=');
                if (!arg.startsWith("--") || eq < 0) {
                    throw new IllegalArgumentException("Unrecognized argument: " + arg);
                }
                String key = arg.substring(2, eq);
                if (!RECOGNIZED_KEYS.contains(key)) {
                    throw new IllegalArgumentException("Unrecognized argument: --" + key);
                }
                flags.put(key, arg.substring(eq + 1));
            }
            String dump = require(flags, "dump");
            String actorEmail = require(flags, "actor-email");
            String pairsRaw = require(flags, "pairs");
            Path dumpPath = Path.of(dump);
            if (!Files.exists(dumpPath)) {
                throw new IllegalArgumentException("dump file does not exist: " + dumpPath);
            }

            Map<Long, Long> pairs = new LinkedHashMap<>();
            Set<Long> seenTargets = new LinkedHashSet<>();
            for (String pair : pairsRaw.split(",")) {
                String[] parts = pair.split(":");
                if (parts.length != 2) {
                    throw new IllegalArgumentException(
                            "Malformed pair '" + pair + "' - expected <wpPostId>:<contentPostId>");
                }
                long wpId;
                long contentId;
                try {
                    wpId = Long.parseLong(parts[0].trim());
                    contentId = Long.parseLong(parts[1].trim());
                } catch (NumberFormatException notNumeric) {
                    throw new IllegalArgumentException("Malformed pair '" + pair + "' - both sides must be numeric");
                }
                if (pairs.containsKey(wpId)) {
                    throw new IllegalArgumentException("WordPress post id " + wpId + " appears more than once in --pairs");
                }
                if (!seenTargets.add(contentId)) {
                    throw new IllegalArgumentException(
                            "content_post id " + contentId + " appears more than once in --pairs - "
                                    + "two different WordPress posts cannot map to the same target.");
                }
                pairs.put(wpId, contentId);
            }
            if (pairs.isEmpty()) {
                throw new IllegalArgumentException("--pairs must name at least one pair");
            }

            if (apply) {
                String confirm = flags.get("confirm");
                if (confirm == null || confirm.isBlank()) {
                    throw new IllegalArgumentException(
                            "--apply requires --confirm=<backfill confirmation token> - refusing to proceed.");
                }
            }

            return new CliArgs(dumpPath, actorEmail, pairs, apply, flags.get("confirm"));
        }

        private static final Set<String> RECOGNIZED_KEYS = Set.of("dump", "actor-email", "pairs", "confirm");

        private static String require(Map<String, String> flags, String key) {
            String value = flags.get(key);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("--" + key + " is required");
            }
            return value;
        }
    }
}
