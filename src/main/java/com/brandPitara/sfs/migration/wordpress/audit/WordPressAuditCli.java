package com.brandPitara.sfs.migration.wordpress.audit;

import java.nio.file.Path;

/**
 * The one and only entry point that actually runs a WordPress migration audit. Deliberately a
 * plain {@code main()} class with no Spring annotation whatsoever, so it can never be picked up
 * by component scanning, never boots as part of {@code SfsApplication}, and is invisible to
 * Spring context tests - the only way this code ever runs is a human explicitly invoking it from
 * the command line. It never inserts into PostgreSQL and never contacts S3; it only reads the
 * two local files and writes report files to the given output directory.
 * <p>
 * Usage:
 * <pre>
 *   mvn -q -o compile
 *   java -cp target/classes:$(cat .cp) \
 *       com.brandPitara.sfs.migration.wordpress.audit.WordPressAuditCli \
 *       /Users/mac/Downloads/u427251222_Ue8rA.sql \
 *       target/wordpress-migration-reports
 * </pre>
 * (Build {@code .cp} once with {@code mvn -q dependency:build-classpath -Dmdep.outputFile=.cp}.)
 */
public final class WordPressAuditCli {

    private WordPressAuditCli() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println(
                    "Usage: WordPressAuditCli <dump.sql|dump.sql.gz> <outputDir>"
            );
            System.exit(2);
            return;
        }
        Path dumpPath = Path.of(args[0]);
        Path outputDir = Path.of(args[1]);

        System.out.println("Reading and converting: " + dumpPath);
        WordPressAuditRunner.AuditResult result = new WordPressAuditRunner().audit(dumpPath);

        System.out.println("Writing reports to: " + outputDir);
        new WordPressAuditReportWriter().writeAll(result, outputDir);

        AuditSummary summary = result.summary();
        System.out.println("Total posts: " + summary.totalPosts()
                + " (published=" + summary.published() + ", drafts=" + summary.drafts() + ")");
        System.out.println("Classification counts: " + summary.classificationCounts());
        System.out.println("Classification by status: " + summary.classificationCountsByStatus());
        System.out.println("Gallery: blockPosts=" + summary.galleryBlockPosts()
                + " resolvable=" + summary.resolvableGalleryPosts()
                + " unresolved=" + summary.unresolvedGalleryPosts()
                + " " + summary.unresolvedGalleryPostIds());
        System.out.println("Elementor: anyMetadata=" + summary.elementorAnyMetadataCount()
                + " editModeOrDataKey=" + summary.elementorEditModeOrDataKeyCount()
                + " editModeBuilder=" + summary.elementorEditModeBuilderCount()
                + " meaningfulPayload=" + summary.elementorMeaningfulPayloadCount());
        System.out.println("Done.");
    }
}
