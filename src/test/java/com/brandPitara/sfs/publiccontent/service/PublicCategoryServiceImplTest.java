package com.brandPitara.sfs.publiccontent.service;

import com.brandPitara.sfs.publiccontent.repository.PublicCategoryRepository;
import com.brandPitara.sfs.publiccontent.repository.PublicCategorySummaryView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicCategoryServiceImplTest {

    @Mock private PublicCategoryRepository repository;
    @Mock private PublicCategorySummaryView marketInsights;
    @Mock private PublicCategorySummaryView emptyCategory;
    @Mock private PublicCategorySummaryView zeroCountAsNull;

    private PublicCategoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PublicCategoryServiceImpl(repository);
    }

    @Test
    void publishedContentCountCountsOnlyPublishedContentAndCategoryWithZeroIsExcluded() {
        stub(marketInsights, 2L, "Market Insights", "market-insights", "Market news", 4L);
        stub(emptyCategory, 3L, "D1.1 Smoke Category", "d1-1-smoke-category", null, 0L);
        when(repository.findActiveCategorySummaries()).thenReturn(List.of(marketInsights, emptyCategory));

        var result = service.list();

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).id()).isEqualTo(2L);
        assertThat(result.items().get(0).name()).isEqualTo("Market Insights");
        assertThat(result.items().get(0).slug()).isEqualTo("market-insights");
        assertThat(result.items().get(0).publishedContentCount()).isEqualTo(4L);
    }

    @Test
    void nullPublishedContentCountIsTreatedAsExcludedNotAnError() {
        stub(zeroCountAsNull, 5L, "No Posts Yet", "no-posts-yet", null, null);
        when(repository.findActiveCategorySummaries()).thenReturn(List.of(zeroCountAsNull));

        var result = service.list();

        assertThat(result.items()).isEmpty();
    }

    @Test
    void noExtraInternalFieldsAreExposedOnTheDto() {
        stub(marketInsights, 2L, "Market Insights", "market-insights", "Market news", 4L);
        when(repository.findActiveCategorySummaries()).thenReturn(List.of(marketInsights));

        var item = service.list().items().get(0);

        assertThat(item.toString())
                .doesNotContain("active", "draft", "owner", "revision");
    }

    @Test
    void repositoryOrderingIsPreservedNotReSorted() {
        stub(marketInsights, 2L, "Market Insights", "market-insights", "d", 1L);
        var zTopic = org.mockito.Mockito.mock(PublicCategorySummaryView.class);
        stub(zTopic, 9L, "Zebra Topics", "zebra-topics", "d", 1L);
        // Repository already orders name asc, id asc; "Market Insights" sorts
        // before "Zebra Topics" - the service must return them in that order
        // as given, not re-sort.
        when(repository.findActiveCategorySummaries()).thenReturn(List.of(marketInsights, zTopic));

        var result = service.list();

        assertThat(result.items()).extracting("slug")
                .containsExactly("market-insights", "zebra-topics");
    }

    private void stub(PublicCategorySummaryView view, Long id, String name, String slug, String description, Long count) {
        lenient().when(view.getId()).thenReturn(id);
        lenient().when(view.getName()).thenReturn(name);
        lenient().when(view.getSlug()).thenReturn(slug);
        lenient().when(view.getDescription()).thenReturn(description);
        lenient().when(view.getPublishedContentCount()).thenReturn(count);
    }
}
