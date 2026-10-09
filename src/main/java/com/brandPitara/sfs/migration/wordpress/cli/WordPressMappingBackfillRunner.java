package com.brandPitara.sfs.migration.wordpress.cli;

import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import com.brandPitara.sfs.migration.wordpress.audit.WordPressDataset;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressDumpReader;
import com.brandPitara.sfs.migration.wordpress.importer.WordPressMappingBackfillConflictException;
import com.brandPitara.sfs.migration.wordpress.importer.WordPressMappingBackfillProperties;
import com.brandPitara.sfs.migration.wordpress.importer.WordPressMappingBackfillRow;
import com.brandPitara.sfs.migration.wordpress.importer.WordPressMappingBackfillValidator;
import com.brandPitara.sfs.migration.wordpress.importer.WordPressMappingBackfillWriter;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The actual orchestration behind {@link WordPressMappingBackfillCli} - deliberately thin: every
 * per-pair check (identity, target existence/status, conflicting mappings, source-vs-target
 * content comparison) lives in {@link WordPressMappingBackfillValidator}, a real {@code @Service}
 * whose {@code @Transactional(readOnly = true)} boundary is required to read {@code
 * ContentPostEntity}'s lazy associations at all - this class is a plain object with no
 * transaction of its own. Dry-run and apply share this exact same validation; apply differs only
 * in that a real conflict aborts the whole operation instead of merely being reported, and only
 * rows planned {@code INSERT} or {@code ADOPT_EXISTING_WITH_DIFFERENCES} are handed to {@link
 * WordPressMappingBackfillWriter}.
 */
final class WordPressMappingBackfillRunner {

    static final int EXIT_SUCCESS = 0;
    static final int EXIT_ACTOR_NOT_FOUND = 3;
    static final int EXIT_AUTHORIZATION_FAILED = 4;
    static final int EXIT_DUMP_READ_FAILED = 5;
    static final int EXIT_VALIDATION_BLOCKED = 6;

    private final DashboardUserRepository dashboardUserRepository;
    private final WordPressMappingBackfillValidator validator;
    private final WordPressMappingBackfillProperties backfillProperties;
    private final WordPressMappingBackfillWriter writer;

    WordPressMappingBackfillRunner(
            DashboardUserRepository dashboardUserRepository,
            WordPressMappingBackfillValidator validator,
            WordPressMappingBackfillProperties backfillProperties,
            WordPressMappingBackfillWriter writer
    ) {
        this.dashboardUserRepository = dashboardUserRepository;
        this.validator = validator;
        this.backfillProperties = backfillProperties;
        this.writer = writer;
    }

    static WordPressMappingBackfillRunner fromContext(ApplicationContext context) {
        return new WordPressMappingBackfillRunner(
                context.getBean(DashboardUserRepository.class),
                context.getBean(WordPressMappingBackfillValidator.class),
                context.getBean(WordPressMappingBackfillProperties.class),
                context.getBean(WordPressMappingBackfillWriter.class)
        );
    }

    int run(WordPressMappingBackfillCli.CliArgs args, PrintStream out, PrintStream err) {
        DashboardUserEntity actor = dashboardUserRepository.findByEmailIgnoreCase(args.actorEmail()).orElse(null);
        if (actor == null) {
            err.println("No dashboard user found with email '" + args.actorEmail()
                    + "' - refusing to attribute a backfill run to a fabricated account. Aborting.");
            return EXIT_ACTOR_NOT_FOUND;
        }
        out.println("Resolved actor: " + actor.getName() + " (id=" + actor.getId() + ")");

        WordPressDataset dataset;
        try {
            WordPressDataset.Builder builder = WordPressDataset.builder();
            new WordPressDumpReader().read(args.dumpPath(), builder);
            dataset = builder.build();
        } catch (IOException e) {
            err.println("Failed to read the WordPress dump: " + e.getMessage());
            return EXIT_DUMP_READ_FAILED;
        }
        out.println("Dataset loaded: " + dataset.postsById().size() + " total rows.");

        List<WordPressMappingBackfillRow> rows = validator.validateAll(args.pairs(), dataset);

        printReport(rows, out);

        long blockedCount = rows.stream()
                .filter(r -> r.plannedAction() == WordPressMappingBackfillRow.PlannedAction.BLOCKED).count();

        if (!args.apply()) {
            out.println("No writes were made (dry run). " + blockedCount
                    + " of " + rows.size() + " pairs would currently block a real apply.");
            return EXIT_SUCCESS;
        }

        if (blockedCount > 0) {
            err.println(blockedCount + " of " + rows.size() + " pairs are BLOCKED - "
                    + "the whole backfill is all-or-nothing, so nothing was written. Fix the "
                    + "blocking pair(s) above and rerun.");
            return EXIT_VALIDATION_BLOCKED;
        }

        boolean authorized = backfillProperties.isAuthorized(args.confirm());
        if (!authorized) {
            err.println("Real-write authorization check failed:");
            err.println("  sfs.migration.wordpress.mapping-backfill.* gate authorized: " + backfillProperties.isEnabled());
            err.println("  --confirm matches the configured backfill confirmation token: "
                    + (backfillProperties.isEnabled() && args.confirm() != null
                            && args.confirm().equals(backfillProperties.getConfirmationToken())));
            err.println("Aborting - no database writes were attempted.");
            return EXIT_AUTHORIZATION_FAILED;
        }

        List<WordPressMappingBackfillWriter.PendingInsert> pendingInserts = rows.stream()
                .filter(r -> r.plannedAction() == WordPressMappingBackfillRow.PlannedAction.INSERT
                        || r.plannedAction() == WordPressMappingBackfillRow.PlannedAction.ADOPT_EXISTING_WITH_DIFFERENCES)
                .map(r -> new WordPressMappingBackfillWriter.PendingInsert(
                        r.wordPressPostId(), r.targetContentPostId(), r.fingerprint()))
                .toList();
        long alreadyMappedCount = rows.stream()
                .filter(r -> r.plannedAction() == WordPressMappingBackfillRow.PlannedAction.ALREADY_MAPPED).count();

        if (pendingInserts.isEmpty()) {
            out.println("Nothing to insert - every pair is already correctly mapped (" + alreadyMappedCount + " of "
                    + rows.size() + "). Idempotent success.");
            return EXIT_SUCCESS;
        }

        try {
            writer.applyBackfill(pendingInserts);
        } catch (WordPressMappingBackfillConflictException conflict) {
            err.println("Apply aborted, all inserts in this call rolled back: " + conflict.getMessage());
            return EXIT_VALIDATION_BLOCKED;
        }

        out.println("========== APPLY complete ==========");
        out.println("Inserted: " + pendingInserts.size() + " new mapping row(s).");
        out.println("Already correctly mapped (no-op): " + alreadyMappedCount);
        out.println("=====================================");
        return EXIT_SUCCESS;
    }

    private void printReport(List<WordPressMappingBackfillRow> rows, PrintStream out) {
        out.println();
        out.println("========== BACKFILL VALIDATION REPORT (" + rows.size() + " pairs) ==========");
        for (WordPressMappingBackfillRow row : rows) {
            out.println("--------------------------------------------------------------");
            out.println("WordPress ID: " + row.wordPressPostId() + "   -> content_post ID: " + row.targetContentPostId());
            out.println("  WordPress title: " + row.wordPressTitle());
            out.println("  Target title:    " + row.targetTitle());
            out.println("  WordPress slug:  " + row.wordPressSlug());
            out.println("  Target slug:     " + row.targetSlug());
            out.println("  Target status:   " + row.targetStatus());
            out.println("  Fingerprint:     " + row.fingerprint());
            out.println("  Identity result: " + row.identityResult());
            if (row.contentComparison() != null) {
                out.println("  Content equivalence: " + (row.contentComparison().isFullyEquivalent()
                        ? "EQUIVALENT" : "DIFFERENT " + row.contentComparison().differingFields()));
            } else {
                out.println("  Content equivalence: (not evaluated - blocked before comparison)");
            }
            out.println("  Planned action:  " + row.plannedAction());
            if (row.blockReason() != null) {
                out.println("  Reason:          " + row.blockReason());
            }
        }
        out.println("--------------------------------------------------------------");
        Map<WordPressMappingBackfillRow.PlannedAction, Long> counts = rows.stream()
                .collect(Collectors.groupingBy(WordPressMappingBackfillRow::plannedAction, Collectors.counting()));
        out.println("Summary: " + counts);
        out.println("================================================================");
        out.println();
    }
}
