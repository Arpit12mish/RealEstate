package com.brandPitara.sfs.migration.wordpress.audit;

import com.brandPitara.sfs.migration.wordpress.dump.WordPressPostMetaRow;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressPostRow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WordPressMediaReferenceAnalyzerTest {

    private final WordPressMediaReferenceAnalyzer analyzer = new WordPressMediaReferenceAnalyzer();

    @Test
    void reconcilesRawOccurrencesDownToNormalizedOwnDomainAndExternalCounts() {
        WordPressPostRow post = post(1, """
                <p><img src="https://squarefootstory.com/wp-content/uploads/2024/05/photo-1024x576.jpg"></p>
                <p><img src="https://squarefootstory.com/wp-content/uploads/2024/05/photo-300x169.jpg"></p>
                <p><img src="https://squarefootstory.com/wp-content/uploads/2024/05/photo-1024x576.jpg"></p>
                <p><img src="https://squarefootstory.com/wp-content/uploads/2024/06/second.jpg"></p>
                <p><img src="https://external.example/hotlinked.jpg"></p>
                """);
        WordPressDataset.Builder builder = WordPressDataset.builder();
        builder.onPost(post);
        builder.onPostMeta(new WordPressPostMetaRow(1, 900, "_wp_attached_file", "2024/05/photo.jpg"));
        WordPressPostRow attachment = attachment(900, "photo.jpg");
        builder.onPost(attachment);

        MediaReferenceReconciliation result = analyzer.analyze(List.of(post), builder.build());

        assertThat(result.rawImageOccurrences()).isEqualTo(5);
        assertThat(result.uniqueOriginalUrls()).isEqualTo(4);
        assertThat(result.normalizedUniqueUrls()).isEqualTo(3); // photo.jpg (from two size variants) + second.jpg + hotlinked.jpg
        assertThat(result.ownDomainUrls()).isEqualTo(2);
        assertThat(result.externalUrls()).isEqualTo(1);
        assertThat(result.attachmentBackedReferences()).isEqualTo(1); // photo.jpg matches the attachment
        assertThat(result.unresolvedReferences()).isEqualTo(1); // second.jpg has no attachment row
    }

    @Test
    void explanationTextNamesEveryCategoryAndTheirRelationships() {
        MediaReferenceReconciliation result = new MediaReferenceReconciliation(10, 8, 5, 4, 1, 3, 1);
        String explanation = result.explanation();
        assertThat(explanation).contains("rawImageOccurrences (10)")
                .contains("uniqueOriginalUrls (8)")
                .contains("normalizedUniqueUrls (5)")
                .contains("attachmentBackedReferences (3)")
                .contains("unresolvedReferences (1)");
    }

    private WordPressPostRow post(long id, String content) {
        return new WordPressPostRow(id, 1L, "d", "d", "m", content, "Title", "", "publish", "slug", null, "post", "", "guid");
    }

    private WordPressPostRow attachment(long id, String filename) {
        return new WordPressPostRow(id, 1L, "d", "d", "m", "", filename, "", "inherit", filename, null,
                "attachment", "image/jpeg", "guid");
    }
}
