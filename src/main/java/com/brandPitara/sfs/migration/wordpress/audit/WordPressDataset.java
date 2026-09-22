package com.brandPitara.sfs.migration.wordpress.audit;

import com.brandPitara.sfs.migration.wordpress.dump.WordPressDumpVisitor;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressPostMetaRow;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressPostRow;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressTermRelationshipRow;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressTermRow;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressTermTaxonomyRow;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressUserRow;
import org.jsoup.parser.Parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An in-memory join of the seven streamed WordPress tables, built once by
 * {@link #builder()}'s {@link WordPressDumpVisitor}. Holding ~3,000 posts and ~14,000 postmeta
 * rows as plain Java objects is a bounded, reasonable amount of structured data - this is
 * distinct from (and does not reintroduce) the "never load the raw dump file into memory"
 * constraint on {@code MySqlStatementReader}, which this class does not touch.
 */
public final class WordPressDataset {

    private final Map<Long, WordPressPostRow> postsById = new LinkedHashMap<>();
    private final Map<Long, List<WordPressPostMetaRow>> metaByPostId = new LinkedHashMap<>();
    private final Map<Long, WordPressTermRow> termsById = new LinkedHashMap<>();
    private final Map<Long, WordPressTermTaxonomyRow> taxonomyById = new LinkedHashMap<>();
    private final Map<Long, List<Long>> taxonomyIdsByObjectId = new LinkedHashMap<>();
    private final Map<Long, WordPressUserRow> usersById = new LinkedHashMap<>();

    public static Builder builder() {
        return new Builder();
    }

    public Map<Long, WordPressPostRow> postsById() {
        return postsById;
    }

    public List<WordPressPostMetaRow> metaFor(long postId) {
        return metaByPostId.getOrDefault(postId, List.of());
    }

    public String metaValue(long postId, String key) {
        for (WordPressPostMetaRow row : metaFor(postId)) {
            if (row.metaKey().equals(key)) {
                return row.metaValue();
            }
        }
        return null;
    }

    public List<String> categoriesFor(long postId) {
        return taxonomyNamesFor(postId, "category");
    }

    public List<String> tagsFor(long postId) {
        return taxonomyNamesFor(postId, "post_tag");
    }

    public WordPressUserRow userById(long id) {
        return usersById.get(id);
    }

    private List<String> taxonomyNamesFor(long postId, String taxonomy) {
        List<String> names = new ArrayList<>();
        for (Long taxonomyId : taxonomyIdsByObjectId.getOrDefault(postId, List.of())) {
            WordPressTermTaxonomyRow tt = taxonomyById.get(taxonomyId);
            if (tt == null || !taxonomy.equals(tt.taxonomy())) {
                continue;
            }
            WordPressTermRow term = termsById.get(tt.termId());
            if (term != null) {
                names.add(Parser.unescapeEntities(term.name(), false));
            }
        }
        return names;
    }

    public static final class Builder implements WordPressDumpVisitor {
        private final WordPressDataset dataset = new WordPressDataset();

        @Override
        public void onPost(WordPressPostRow row) {
            dataset.postsById.put(row.id(), row);
        }

        @Override
        public void onPostMeta(WordPressPostMetaRow row) {
            dataset.metaByPostId.computeIfAbsent(row.postId(), id -> new ArrayList<>()).add(row);
        }

        @Override
        public void onTerm(WordPressTermRow row) {
            dataset.termsById.put(row.termId(), row);
        }

        @Override
        public void onTermTaxonomy(WordPressTermTaxonomyRow row) {
            dataset.taxonomyById.put(row.termTaxonomyId(), row);
        }

        @Override
        public void onTermRelationship(WordPressTermRelationshipRow row) {
            dataset.taxonomyIdsByObjectId.computeIfAbsent(row.objectId(), id -> new ArrayList<>())
                    .add(row.termTaxonomyId());
        }

        @Override
        public void onUser(WordPressUserRow row) {
            dataset.usersById.put(row.id(), row);
        }

        public WordPressDataset build() {
            return dataset;
        }
    }
}
