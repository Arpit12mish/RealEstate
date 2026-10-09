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
 * The one and only entry point that can drive a <strong>real</strong> WordPress migration write -
 * deliberately never invoked by normal application startup ({@link SfsApplication#main} is the
 * only thing a deployment's default {@code java -jar} / launcher actually runs). This class has
 * no Spring stereotype annotation itself and is reachable only by explicitly naming it on the
 * command line, mirroring the existing {@code WordPressAuditCli} convention.
 *
 * <h2>Usage</h2>
 * <pre>
 *   mvn -q -o compile
 *   mvn -q -o dependency:build-classpath -Dmdep.outputFile=.cp   # once
 *
 *   # Dry run (default) - reads and indexes files, computes intended keys/checksums, reports
 *   # what WOULD happen, makes zero database or S3 writes. Scope defaults to the complete audit
 *   # (every eligible post) unless --post-ids narrows it:
 *   java -cp target/classes:$(cat .cp) com.brandPitara.sfs.migration.wordpress.cli.WordPressProductionImportCli \
 *       --dump=/path/to/dump.sql --uploads-zip=/path/to/uploads.zip --actor-email=you@example.com
 *
 *   # Real write, scoped to a pilot subset - requires the five sfs.migration.wordpress.media.*
 *   # environment variables to already be set (see WordPressMigrationMediaProperties), PLUS
 *   # --apply, PLUS --confirm repeating that exact same production-confirmation-token value,
 *   # PLUS an explicit scope. An unscoped --apply is always refused - there is no implicit
 *   # "apply to everything" path:
 *   java -cp target/classes:$(cat .cp) com.brandPitara.sfs.migration.wordpress.cli.WordPressProductionImportCli \
 *       --dump=/path/to/dump.sql --uploads-zip=/path/to/uploads.zip --actor-email=you@example.com \
 *       --apply --confirm=&lt;token&gt; --post-ids=10043,10054
 *
 *   # Real write across the full corpus - requires a SECOND, distinct explicit confirmation on
 *   # top of --confirm, so a full-corpus apply can never be triggered by a copy-pasted pilot
 *   # command or a missing flag:
 *   java -cp target/classes:$(cat .cp) com.brandPitara.sfs.migration.wordpress.cli.WordPressProductionImportCli \
 *       --dump=/path/to/dump.sql --uploads-zip=/path/to/uploads.zip --actor-email=you@example.com \
 *       --apply --confirm=&lt;token&gt; --all-posts --confirm-full-corpus=&lt;same token&gt;
 * </pre>
 * Every underlying write (media upload, content-post insert) is already idempotent and safe to
 * rerun - see {@link com.brandPitara.sfs.migration.wordpress.importer.WordPressContentImportService}
 * and {@link com.brandPitara.sfs.migration.wordpress.media.WordPressMediaImportService}. This CLI
 * does not add its own idempotency; it relies entirely on theirs.
 *
 * <h2>Boot safety</h2>
 * This process boots the real {@code SfsApplication} context (same beans, same wiring the deployed
 * app uses) but as {@link WebApplicationType#NONE} - no HTTP listener is ever opened, so there is
 * nothing here for anything external to connect to, on any port. That required splitting {@code
 * PasswordEncoder} out of {@code SecurityConfig} into its own always-on {@code
 * PasswordEncoderConfig}, since {@code SecurityConfig}'s two {@code SecurityFilterChain} beans
 * (the only beans that actually need a servlet {@code HttpSecurity}) are now {@code
 * @ConditionalOnWebApplication(SERVLET)} and simply don't exist under {@code NONE} - nothing this
 * CLI calls needs them. On top of that, every boot (dry-run and apply alike - schema migration is
 * the deployed application's job, never this CLI's, regardless of mode) forces:
 * <ul>
 *   <li>{@code spring.flyway.enabled=false} - this process must never run a schema migration;</li>
 *   <li>{@code spring.jpa.hibernate.ddl-auto=validate} - fails fast on a schema mismatch, mutates nothing;</li>
 *   <li>every {@code @Scheduled} cron in the app forced to {@code "-"} (Spring's documented
 *       disabled-cron sentinel) so no background job - token cleanup, media cleanup, Instagram
 *       sync, analytics aggregation/partition maintenance - can fire mid-run;</li>
 *   <li>{@code ScrapeSessionStore}'s fixed-delay eviction pushed out to effectively never, since a
 *       fixed-delay trigger has no "-" equivalent;</li>
 *   <li>{@code dashboard.seed.enabled=false} and the DB-connection debug runner disabled, so no
 *       {@code CommandLineRunner} writes or prints anything regardless of the active profile's
 *       own defaults.</li>
 * </ul>
 * See {@code WordPressMigrationInertByDefaultTest} for the Spring-context-level proof that this
 * package is never constructed by a normal application boot in the first place.
 */
public final class WordPressProductionImportCli {

    private WordPressProductionImportCli() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /**
     * Pure, testable entry point - parses arguments, boots the context, runs the import, and
     * returns the exit code instead of terminating the JVM. {@code main} is the only caller that
     * should ever call {@link System#exit}; tests call this method directly.
     */
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
        out.println("WordPress migration CLI - mode: " + (parsed.apply() ? "APPLY (real write)" : "DRY RUN (no writes)"));
        out.println("dump: " + parsed.dumpPath());
        out.println("uploads.zip: " + parsed.uploadsZipPath());
        out.println("actor: " + parsed.actorEmail());
        out.println("scope: " + describeScope(parsed));
        out.println("========================================================");

        applySafetySystemProperties();
        ConfigurableApplicationContext context = new SpringApplicationBuilder(SfsApplication.class)
                .web(WebApplicationType.NONE)
                .build()
                .run();
        try {
            return WordPressProductionImportRunner.fromContext(context).run(parsed, out, err);
        } finally {
            context.close();
        }
    }

    private static String describeScope(CliArgs args) {
        if (args.allPosts()) {
            return "full corpus (--all-posts)";
        }
        if (args.postIds() != null) {
            return "scoped to " + args.postIds();
        }
        return "full corpus (dry-run default)";
    }

    /**
     * Every property forced regardless of the active profile's own defaults - see the class-level
     * "Boot safety" doc for why each one is here. Applied via {@link System#setProperty} rather
     * than {@code SpringApplicationBuilder.properties(Map)}: the latter registers them as the
     * <em>lowest</em>-priority "default properties" source, which - verified empirically against
     * this exact app (a {@code spring.flyway.enabled=false} default was silently ignored and
     * Flyway ran anyway; the identical value via {@code -Dspring.flyway.enabled=false} correctly
     * suppressed it) - is not reliably higher priority than whatever the active profile's own
     * property sources already contribute. A JVM system property sits in Spring Boot's standard
     * {@code SystemPropertiesPropertySource}, near the top of the precedence chain, so it cannot
     * be silently outranked by profile config. Not profile-conditional: a CLI boot must behave
     * identically no matter which profile (local-staging, prod, ...) supplies the datasource, so
     * none of this is allowed to depend on ambient configuration the operator might get wrong.
     * Package-visible so every CLI in this package (e.g. {@code WordPressMappingBackfillCli})
     * shares exactly this one boot-safety implementation, never a second hand-copied one.
     */
    static void applySafetySystemProperties() {
        System.setProperty("spring.main.banner-mode", "off");
        System.setProperty("server.port", "0");

        // Schema state is exclusively the deployed application's responsibility.
        System.setProperty("spring.flyway.enabled", "false");
        System.setProperty("spring.jpa.hibernate.ddl-auto", "validate");

        // Two schedulers gate themselves behind their own properties-level enabled flag - use
        // that real lever rather than the cron string, since RefreshTokenCleanupProperties'
        // @AssertTrue validator rejects Spring's "-" disabled-cron sentinel as "not a valid cron
        // expression" (it validates with CronExpression.isValidExpression, which has no notion of
        // that ScheduledAnnotationBeanPostProcessor-specific convention) and would otherwise fail
        // context startup outright.
        System.setProperty("sfs.refresh-token.cleanup.enabled", "false");
        System.setProperty("sfs.instagram.meta.sync-enabled", "false");
        // The remaining three have no properties-level enabled flag and no custom cron
        // validation, so Spring's own "-" disabled-cron sentinel is the correct, direct lever.
        System.setProperty("app.cms.media.cleanup-cron", "-");
        System.setProperty("sfs.analytics.partition-maintenance.schedule", "-");
        System.setProperty("sfs.analytics.aggregation.schedule", "-");
        // fixedDelay has no "-" equivalent; push it out past any conceivable CLI run instead.
        System.setProperty("sfs.scrape-session.cleanup-delay-ms", String.valueOf(Long.MAX_VALUE / 2));

        // Unconditional CommandLineRunner writes/output, regardless of the active profile.
        System.setProperty("dashboard.seed.enabled", "false");
        System.setProperty("sfs.debug.db-connection-runner.enabled", "false");
    }

    private static void printUsage(PrintStream err) {
        err.println("""
                Usage:
                  WordPressProductionImportCli --dump=<path> --uploads-zip=<path> --actor-email=<email> \
                [--apply --confirm=<token> (--post-ids=<id,id,...> | --all-posts --confirm-full-corpus=<token>)] \
                [--post-ids=<id,id,...>]

                Without --apply, runs a read-only dry run: indexes the dump/zip, classifies every post, \
                computes intended object keys/checksums, and reports what would happen - no database or \
                S3 writes. Dry-run scope defaults to the complete audit unless --post-ids narrows it.

                --apply always requires an explicit scope - there is no unscoped apply:
                  --post-ids=<id,id,...>              a limited pilot, e.g. --post-ids=10043,10054
                  --all-posts --confirm-full-corpus=<token>   the full corpus, gated by a second,
                                                       distinct confirmation matching the same
                                                       production-confirmation-token value.
                """);
    }

    record CliArgs(
            Path dumpPath, Path uploadsZipPath, String actorEmail, boolean apply, String confirm,
            Set<Long> postIds, boolean allPosts, String confirmFullCorpus
    ) {
        static CliArgs parse(String[] args) {
            Map<String, String> flags = new LinkedHashMap<>();
            boolean apply = false;
            boolean allPosts = false;
            for (String arg : args) {
                if ("--apply".equals(arg)) {
                    apply = true;
                    continue;
                }
                if ("--all-posts".equals(arg)) {
                    allPosts = true;
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
            String uploadsZip = require(flags, "uploads-zip");
            String actorEmail = require(flags, "actor-email");
            Path dumpPath = Path.of(dump);
            Path uploadsZipPath = Path.of(uploadsZip);
            if (!Files.exists(dumpPath)) {
                throw new IllegalArgumentException("dump file does not exist: " + dumpPath);
            }
            if (!Files.exists(uploadsZipPath)) {
                throw new IllegalArgumentException("uploads-zip file does not exist: " + uploadsZipPath);
            }
            Set<Long> postIds = null;
            if (flags.containsKey("post-ids")) {
                postIds = new LinkedHashSet<>();
                for (String id : flags.get("post-ids").split(",")) {
                    postIds.add(Long.parseLong(id.trim()));
                }
            }
            String confirmFullCorpus = flags.get("confirm-full-corpus");

            if (apply) {
                String confirm = flags.get("confirm");
                if (confirm == null || confirm.isBlank()) {
                    throw new IllegalArgumentException(
                            "--apply requires --confirm=<production confirmation token> - refusing to proceed.");
                }
                if (allPosts && postIds != null) {
                    throw new IllegalArgumentException(
                            "--all-posts and --post-ids are mutually exclusive - pick exactly one scope.");
                }
                if (!allPosts && postIds == null) {
                    throw new IllegalArgumentException(
                            "--apply requires an explicit scope: --post-ids=<id,id,...> for a pilot, "
                                    + "or --all-posts --confirm-full-corpus=<token> for the full corpus. "
                                    + "There is no unscoped apply.");
                }
                if (postIds != null && postIds.isEmpty()) {
                    throw new IllegalArgumentException("--post-ids must name at least one post id.");
                }
                if (allPosts && (confirmFullCorpus == null || confirmFullCorpus.isBlank())) {
                    throw new IllegalArgumentException(
                            "--all-posts requires --confirm-full-corpus=<token> - a second, distinct "
                                    + "confirmation, on top of --confirm, before a full-corpus apply can run.");
                }
            }

            return new CliArgs(
                    dumpPath, uploadsZipPath, actorEmail, apply, flags.get("confirm"),
                    postIds, allPosts, confirmFullCorpus
            );
        }

        private static final Set<String> RECOGNIZED_KEYS = Set.of(
                "dump", "uploads-zip", "actor-email", "confirm", "post-ids", "confirm-full-corpus"
        );

        private static String require(Map<String, String> flags, String key) {
            String value = flags.get(key);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("--" + key + " is required");
            }
            return value;
        }
    }
}
