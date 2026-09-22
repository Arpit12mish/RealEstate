package com.brandPitara.sfs.migration.wordpress.audit;

import com.brandPitara.sfs.migration.wordpress.dump.WordPressPostMetaRow;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressPostRow;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressTermRelationshipRow;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressTermRow;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressTermTaxonomyRow;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressUserRow;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WordPressDatasetTest {

    @Test
    void joinsTermRelationshipsThroughTaxonomyToTermNamesSeparatedByTaxonomy() {
        WordPressDataset.Builder builder = WordPressDataset.builder();
        builder.onPost(new WordPressPostRow(10, 1L, "d", "d", "m", "<p>x</p>", "Title", "", "publish", "slug", null, "post", "", "guid"));
        builder.onTerm(new WordPressTermRow(1, "Real Estate", "real-estate"));
        builder.onTerm(new WordPressTermRow(2, "Trends", "trends"));
        builder.onTermTaxonomy(new WordPressTermTaxonomyRow(100, 1, "category", 0L));
        builder.onTermTaxonomy(new WordPressTermTaxonomyRow(200, 2, "post_tag", 0L));
        builder.onTermRelationship(new WordPressTermRelationshipRow(10, 100));
        builder.onTermRelationship(new WordPressTermRelationshipRow(10, 200));

        WordPressDataset dataset = builder.build();
        assertThat(dataset.categoriesFor(10)).containsExactly("Real Estate");
        assertThat(dataset.tagsFor(10)).containsExactly("Trends");
    }

    @Test
    void decodesHtmlEntitiesInTermNames() {
        WordPressDataset.Builder builder = WordPressDataset.builder();
        builder.onTerm(new WordPressTermRow(1, "Furnishings &amp; Upholstery", "furnishings-upholstery"));
        builder.onTermTaxonomy(new WordPressTermTaxonomyRow(100, 1, "category", 0L));
        builder.onTermRelationship(new WordPressTermRelationshipRow(5, 100));

        assertThat(builder.build().categoriesFor(5)).containsExactly("Furnishings & Upholstery");
    }

    @Test
    void postWithNoTermRelationshipsHasNoCategoriesOrTags() {
        WordPressDataset dataset = WordPressDataset.builder().build();
        assertThat(dataset.categoriesFor(999)).isEmpty();
        assertThat(dataset.tagsFor(999)).isEmpty();
    }

    @Test
    void groupsPostmetaByPostIdAndLooksUpByKey() {
        WordPressDataset.Builder builder = WordPressDataset.builder();
        builder.onPostMeta(new WordPressPostMetaRow(1, 10, "_thumbnail_id", "55"));
        builder.onPostMeta(new WordPressPostMetaRow(2, 10, "_edit_last", "1"));
        builder.onPostMeta(new WordPressPostMetaRow(3, 11, "_thumbnail_id", "56"));

        WordPressDataset dataset = builder.build();
        assertThat(dataset.metaFor(10)).hasSize(2);
        assertThat(dataset.metaValue(10, "_thumbnail_id")).isEqualTo("55");
        assertThat(dataset.metaValue(11, "_thumbnail_id")).isEqualTo("56");
        assertThat(dataset.metaValue(10, "_missing_key")).isNull();
    }

    @Test
    void resolvesUsersById() {
        WordPressDataset.Builder builder = WordPressDataset.builder();
        builder.onUser(new WordPressUserRow(1, "Square Foot Story"));

        assertThat(builder.build().userById(1).displayName()).isEqualTo("Square Foot Story");
    }
}
