package com.brandPitara.sfs.publiccontent.service;

import com.brandPitara.sfs.cms.content.document.CalloutVariant;
import com.brandPitara.sfs.cms.content.document.CmsMediaReferenceService;
import com.brandPitara.sfs.cms.content.document.ContentBlock;
import com.brandPitara.sfs.cms.content.document.ContentDocument;
import com.brandPitara.sfs.cms.content.document.EmbedProvider;
import com.brandPitara.sfs.cms.content.document.InlineNode;
import com.brandPitara.sfs.cms.content.document.TableRowType;
import com.brandPitara.sfs.cms.content.domain.ContentType;
import com.brandPitara.sfs.cms.content.exception.CmsContentApiException;
import com.brandPitara.sfs.cms.media.domain.CmsMediaStatus;
import com.brandPitara.sfs.cms.media.domain.CmsMediaType;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.publiccontent.exception.PublicContentApiException;
import com.brandPitara.sfs.publiccontent.media.PublicMediaDeliveryException;
import com.brandPitara.sfs.publiccontent.media.PublicMediaUrlResolver;
import com.brandPitara.sfs.publiccontent.repository.PublicContentDetailView;
import com.brandPitara.sfs.publiccontent.repository.PublicContentListView;
import com.brandPitara.sfs.publiccontent.repository.PublicContentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicContentServiceImplTest {
    @Mock private PublicContentRepository repository;
    @Mock private CmsMediaReferenceService mediaReferenceService;
    @Mock private PublicContentDetailView detail;
    @Mock private PublicContentListView listItem;
    @Mock private PublicMediaUrlResolver publicMediaUrlResolver;

    private PublicContentServiceImpl service;
    private final OffsetDateTime publishedAt = OffsetDateTime.parse("2026-08-19T12:00:00Z");

    @BeforeEach
    void setUp() {
        service = new PublicContentServiceImpl(
                repository, mediaReferenceService, new PublicContentEtag(), publicMediaUrlResolver
        );
    }

    @Test
    void detailIsBuiltOnlyFromPublishedRevisionProjectionAndSafeMediaMetadata() {
        ContentDocument document = new ContentDocument(2, List.of(
                new ContentBlock.Embed(EmbedProvider.YOUTUBE, "dQw4w9WgXcQ", List.of())
        ));
        stubDetail(document);
        when(detail.getReadingTimeMinutes()).thenReturn(6);
        CmsMediaAssetEntity image = CmsMediaAssetEntity.builder()
                .id(81L).mediaType(CmsMediaType.IMAGE).status(CmsMediaStatus.READY)
                .storageBucket("private-bucket").storageKey("cms/images/private.jpg")
                .originalFilename("private.jpg").contentType("image/jpeg")
                .declaredSizeBytes(100L).sizeBytes(100L).width(1920).height(1080).build();
        when(mediaReferenceService.validateAndResolve(document)).thenReturn(Map.of(81L, image));
        when(publicMediaUrlResolver.resolve(image))
                .thenReturn("https://media.example.com/cms/images/private.jpg");

        var result = service.getBySlug("published-title", null);

        assertThat(result.body().title()).isEqualTo("Published title");
        assertThat(result.body().excerpt()).isEqualTo("Published excerpt");
        assertThat(result.body().readingTimeMinutes()).isEqualTo(6);
        assertThat(result.body().seo().title()).isEqualTo("Published SEO");
        assertThat(result.body().document()).isSameAs(document);
        assertThat(result.body().media().get(81L).width()).isEqualTo(1920);
        assertThat(result.body().media().get(81L).deliveryUrl())
                .isEqualTo("https://media.example.com/cms/images/private.jpg");
        assertThat(result.body().toString())
                .doesNotContain("private-bucket", "storageKey=", "storageBucket=", "presign");
        verify(repository).findPublishedDetailBySlug("published-title");
        verify(mediaReferenceService).validateAndResolve(document);
    }

    @Test
    void legacyRevisionWithNullReadingTimeIsReturnedAsNullNotZero() {
        ContentDocument document = new ContentDocument(2, List.of());
        stubDetail(document);
        when(detail.getReadingTimeMinutes()).thenReturn(null);
        when(mediaReferenceService.validateAndResolve(document)).thenReturn(Map.of());

        var result = service.getBySlug("published-title", null);

        assertThat(result.body().readingTimeMinutes()).isNull();
    }

    @Test
    void matchingEtagReturnsNotModifiedWithoutMediaQuery() {
        ContentDocument document = new ContentDocument(1, List.of());
        stubDetail(document);
        String etag = new PublicContentEtag().create(90L, publishedAt);

        var result = service.getBySlug("published-title", etag);

        assertThat(result.notModified()).isTrue();
        assertThat(result.body()).isNull();
        verifyNoInteractions(mediaReferenceService, publicMediaUrlResolver);
    }

    @Test
    void mediaDeliveryConfigurationFailureFailsArticleClosed() {
        ContentDocument document = new ContentDocument(2, List.of());
        stubDetail(document);
        CmsMediaAssetEntity image = CmsMediaAssetEntity.builder()
                .id(82L).mediaType(CmsMediaType.IMAGE).status(CmsMediaStatus.READY)
                .storageKey("cms/images/image.jpg").contentType("image/jpeg").build();
        when(mediaReferenceService.validateAndResolve(document)).thenReturn(Map.of(82L, image));
        when(publicMediaUrlResolver.resolve(image))
                .thenThrow(new PublicMediaDeliveryException("not configured"));

        assertPublicCode(() -> service.getBySlug("published-title", null),
                "CONTENT_TEMPORARILY_UNAVAILABLE");
    }

    @Test
    void missingOrInvalidPublishedDependencyFailsWithoutLeakingEditorialState() {
        when(repository.findPublishedDetailBySlug("draft-or-missing")).thenReturn(Optional.empty());
        assertPublicCode(() -> service.getBySlug("draft-or-missing", null), "CONTENT_NOT_FOUND");

        ContentDocument document = new ContentDocument(2, List.of());
        stubDetail(document);
        when(mediaReferenceService.validateAndResolve(document))
                .thenThrow(CmsContentApiException.mediaNotReady(81L));
        assertPublicCode(() -> service.getBySlug("published-title", null),
                "CONTENT_TEMPORARILY_UNAVAILABLE");
    }

    @Test
    void allReadableDocumentVersionsAreAcceptedAndUnsupportedVersionFailsClosed() {
        for (int version = ContentDocument.MINIMUM_READABLE_SCHEMA_VERSION;
             version <= ContentDocument.CURRENT_SCHEMA_VERSION; version++) {
            reset(repository, mediaReferenceService, detail);
            ContentDocument document = new ContentDocument(version, List.of());
            stubDetail(document);
            when(mediaReferenceService.validateAndResolve(document)).thenReturn(Map.of());
            assertThat(service.getBySlug("published-title", null).body().document().schemaVersion())
                    .isEqualTo(version);
        }

        reset(repository, mediaReferenceService, detail);
        ContentDocument unsupported = new ContentDocument(ContentDocument.CURRENT_SCHEMA_VERSION + 1, List.of());
        stubDetail(unsupported);
        assertPublicCode(() -> service.getBySlug("published-title", null),
                "CONTENT_TEMPORARILY_UNAVAILABLE");
        verifyNoInteractions(mediaReferenceService);
    }

    @Test
    void publicApiReturnsSchemaVersion3DocumentWithTableCalloutAndCheckListBlocksIntact() {
        ContentDocument document = new ContentDocument(3, List.of(
                new ContentBlock.CheckList(List.of(
                        new ContentBlock.ListItem(List.of(new InlineNode.Text("Smart switches", List.of())))
                )),
                new ContentBlock.Callout(
                        CalloutVariant.VERDICT, "SFS Verdict",
                        List.of(new InlineNode.Text("Strong pick.", List.of()))
                ),
                new ContentBlock.Table(
                        "Project Snapshot",
                        List.of(new ContentBlock.TableColumn("Package"), new ContentBlock.TableColumn("Estimated Cost")),
                        List.of(new ContentBlock.TableRow(TableRowType.NORMAL, List.of(
                                List.of(new InlineNode.Text("Towers", List.of())),
                                List.of(new InlineNode.Text("7", List.of()))
                        )))
                )
        ));
        stubDetail(document);
        when(mediaReferenceService.validateAndResolve(document)).thenReturn(Map.of());

        ContentDocument returned = service.getBySlug("published-title", null).body().document();

        assertThat(returned).isEqualTo(document);
        assertThat(returned.blocks()).hasSize(3)
                .hasOnlyElementsOfTypes(ContentBlock.CheckList.class, ContentBlock.Callout.class, ContentBlock.Table.class);
    }

    @Test
    void listUsesPublishedRevisionProjectionAndBoundedUnsortedPageRequest() {
        when(listItem.getPostId()).thenReturn(42L);
        when(listItem.getSlug()).thenReturn("published-title");
        when(listItem.getContentType()).thenReturn(ContentType.ARTICLE);
        when(listItem.getTitle()).thenReturn("Published title");
        when(listItem.getExcerpt()).thenReturn("Published excerpt");
        when(listItem.getReadingTimeMinutes()).thenReturn(4);
        when(listItem.getPublishedAt()).thenReturn(publishedAt);
        when(repository.findPublishedList(eq(ContentType.ARTICLE), any()))
                .thenReturn(new PageImpl<>(List.of(listItem)));

        var response = service.list(ContentType.ARTICLE, 0, 20);

        assertThat(response.content()).singleElement().satisfies(item -> {
            assertThat(item.title()).isEqualTo("Published title");
            assertThat(item.excerpt()).isEqualTo("Published excerpt");
            assertThat(item.readingTimeMinutes()).isEqualTo(4);
        });
        verify(repository).findPublishedList(eq(ContentType.ARTICLE), argThat(pageable ->
                pageable.getPageNumber() == 0 && pageable.getPageSize() == 20
                        && pageable.getSort().isUnsorted()));
        assertThatThrownBy(() -> service.list(null, 0, 51))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void categorySlugFilterIsNormalizedAndForwardedToFilteredQuery() {
        when(repository.findPublishedListFiltered(
                eq(ContentType.ARTICLE), eq("market-insights"), eq(null), eq(null), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.list(ContentType.ARTICLE, "Market-Insights", null, null, null, 0, 20);

        verify(repository).findPublishedListFiltered(
                eq(ContentType.ARTICLE), eq("market-insights"), eq(null), eq(null), eq(null), any());
        verify(repository, never()).findPublishedList(any(), any());
    }

    @Test
    void searchQueryIsTrimmedLowercasedAndLikeWildcardsAreEscaped() {
        when(repository.findPublishedListFiltered(
                eq(null), eq(null), eq(null), eq(null), eq("%50\\%\\_off%"), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.list(null, null, null, null, "  50%_off  ", 0, 20);

        verify(repository).findPublishedListFiltered(
                eq(null), eq(null), eq(null), eq(null), eq("%50\\%\\_off%"), any());
    }

    @Test
    void blankSearchQueryBehavesLikeNoSearchFilter() {
        when(repository.findPublishedList(eq(null), any())).thenReturn(new PageImpl<>(List.of()));

        service.list(null, null, null, null, "   ", 0, 20);

        verify(repository).findPublishedList(eq(null), any());
        verify(repository, never()).findPublishedListFiltered(any(), any(), any(), any(), any(), any());
    }

    @Test
    void searchQueryOverMaxLengthIsRejected() {
        String tooLong = "a".repeat(PublicContentServiceImpl.MAX_SEARCH_QUERY_LENGTH + 1);

        assertThatThrownBy(() -> service.list(null, null, null, null, tooLong, 0, 20))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void searchAndCategoryFilterComposeWithAndSemanticsInASingleQuery() {
        when(repository.findPublishedListFiltered(
                eq(ContentType.ARTICLE), eq("market-insights"), eq(null), eq(null), eq("%builder%"), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.list(ContentType.ARTICLE, "market-insights", null, null, "builder", 0, 20);

        verify(repository).findPublishedListFiltered(
                eq(ContentType.ARTICLE), eq("market-insights"), eq(null), eq(null), eq("%builder%"), any());
        verify(repository, times(1)).findPublishedListFiltered(any(), any(), any(), any(), any(), any());
    }

    @Test
    void paginationIsPreservedWhenFiltersArePresent() {
        when(repository.findPublishedListFiltered(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.list(null, "market-insights", null, null, null, 2, 5);

        verify(repository).findPublishedListFiltered(any(), any(), any(), any(), any(),
                argThat(pageable -> pageable.getPageNumber() == 2 && pageable.getPageSize() == 5));
    }

    @Test
    void listOnlyEverReadsThroughThePublishedOnlyRepositoryMethodsRegardlessOfFilters() {
        when(repository.findPublishedList(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(repository.findPublishedListFiltered(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.list(null, 0, 20);
        service.list(null, "market-insights", null, null, "builder", 0, 20);

        // PublicContentRepository exposes no other read method - draft/unpublished
        // content has no code path into the public list at all.
        verifyNoMoreInteractions(repository);
    }

    private void stubDetail(ContentDocument document) {
        lenient().when(repository.findPublishedDetailBySlug("published-title")).thenReturn(Optional.of(detail));
        lenient().when(detail.getPostId()).thenReturn(42L);
        lenient().when(detail.getRevisionId()).thenReturn(90L);
        lenient().when(detail.getContentType()).thenReturn(ContentType.ARTICLE);
        lenient().when(detail.getTitle()).thenReturn("Published title");
        lenient().when(detail.getSlug()).thenReturn("published-title");
        lenient().when(detail.getExcerpt()).thenReturn("Published excerpt");
        lenient().when(detail.getSeoTitle()).thenReturn("Published SEO");
        lenient().when(detail.getRobotsIndex()).thenReturn(true);
        lenient().when(detail.getRobotsFollow()).thenReturn(true);
        lenient().when(detail.getContentDocument()).thenReturn(document);
        lenient().when(detail.getContentDocumentSchemaVersion()).thenReturn((short) document.schemaVersion());
        lenient().when(detail.getPublishedAt()).thenReturn(publishedAt);
        lenient().when(detail.getRevisionCreatedAt()).thenReturn(publishedAt.minusHours(1));
    }

    private void assertPublicCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable, String code) {
        assertThatThrownBy(callable)
                .isInstanceOf(PublicContentApiException.class)
                .extracting(error -> ((PublicContentApiException) error).getCode())
                .isEqualTo(code);
    }
}
