package com.brandPitara.sfs.cms.content.document;

import com.brandPitara.sfs.cms.content.exception.CmsContentApiException;
import com.brandPitara.sfs.cms.media.domain.CmsMediaStatus;
import com.brandPitara.sfs.cms.media.domain.CmsMediaType;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.cms.media.repository.CmsMediaAssetRepository;
import com.brandPitara.sfs.media.service.MediaObjectStorageService;
import com.brandPitara.sfs.media.service.PresignedReadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CmsMediaReferenceServiceTest {

    @Mock private CmsMediaAssetRepository repository;
    @Mock private MediaObjectStorageService storage;

    private CmsMediaReferenceService service;

    @BeforeEach
    void setUp() {
        service = new CmsMediaReferenceService(repository, storage);
    }

    @Test
    void validatesMultipleAndDuplicateReferencesWithOneDeduplicatedQuery() {
        CmsMediaAssetEntity image = asset(10L, CmsMediaType.IMAGE, CmsMediaStatus.READY);
        CmsMediaAssetEntity video = asset(20L, CmsMediaType.VIDEO, CmsMediaStatus.READY);
        when(repository.findAllByIdIn(any())).thenReturn(List.of(image, video));

        ContentDocument document = document(
                new ContentBlock.Image(10L, false, "Lobby", List.of(), ImageLayout.STANDARD, null),
                new ContentBlock.Image(10L, false, "Lobby detail", List.of(), ImageLayout.WIDE, null),
                new ContentBlock.Video(20L, 10L, List.of())
        );
        Map<Long, CmsMediaAssetEntity> resolved = service.validateAndResolve(document);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> ids = ArgumentCaptor.forClass(Collection.class);
        verify(repository).findAllByIdIn(ids.capture());
        assertThat(ids.getValue()).containsExactlyInAnyOrder(10L, 20L);
        assertThat(resolved).containsOnlyKeys(10L, 20L);
    }

    @Test
    void distinguishesMissingNotReadyAndTypeMismatch() {
        when(repository.findAllByIdIn(any())).thenReturn(List.of());
        assertCode(() -> service.validateAndResolve(document(image(10L))), "CONTENT_MEDIA_NOT_FOUND");

        when(repository.findAllByIdIn(any())).thenReturn(List.of(
                asset(10L, CmsMediaType.IMAGE, CmsMediaStatus.PENDING_UPLOAD)
        ));
        assertCode(() -> service.validateAndResolve(document(image(10L))), "CONTENT_MEDIA_NOT_READY");

        when(repository.findAllByIdIn(any())).thenReturn(List.of(
                asset(10L, CmsMediaType.VIDEO, CmsMediaStatus.READY)
        ));
        assertCode(() -> service.validateAndResolve(document(image(10L))), "CONTENT_MEDIA_TYPE_MISMATCH");
    }

    @Test
    void rejectsVideoPosterThatIsNotReadyImage() {
        when(repository.findAllByIdIn(any())).thenReturn(List.of(
                asset(20L, CmsMediaType.VIDEO, CmsMediaStatus.READY),
                asset(21L, CmsMediaType.VIDEO, CmsMediaStatus.READY)
        ));
        assertCode(() -> service.validateAndResolve(document(
                new ContentBlock.Video(20L, 21L, List.of())
        )), "CONTENT_MEDIA_TYPE_MISMATCH");
    }

    @Test
    void emptyOrEmbedOnlyDocumentDoesNotQueryMedia() {
        assertThat(service.validateAndResolve(document(
                new ContentBlock.Embed(EmbedProvider.YOUTUBE, "dQw4w9WgXcQ", List.of())
        ))).isEmpty();
        verify(repository, never()).findAllByIdIn(any());
    }

    @Test
    void checkListCalloutAndTableDoNotIntroduceMediaReferences() {
        ContentDocument document = new ContentDocument(ContentDocument.CURRENT_SCHEMA_VERSION, List.of(
                new ContentBlock.CheckList(List.of(new ContentBlock.ListItem(List.of()))),
                new ContentBlock.Callout(CalloutVariant.INFO, null, List.of()),
                new ContentBlock.Table(
                        "Snapshot",
                        List.of(new ContentBlock.TableColumn("A")),
                        List.of(new ContentBlock.TableRow(TableRowType.NORMAL, List.of(List.of())))
                )
        ));

        assertThat(service.validateAndResolve(document)).isEmpty();
        verify(repository, never()).findAllByIdIn(any());
    }

    @Test
    void findsImageMediaReferencesNestedInsideALayoutChild() {
        CmsMediaAssetEntity image = asset(10L, CmsMediaType.IMAGE, CmsMediaStatus.READY);
        when(repository.findAllByIdIn(any())).thenReturn(List.of(image));

        ContentDocument document = document(new ContentBlock.Layout(2, List.of(
                image(10L),
                new ContentBlock.Table(
                        null, "Cost",
                        List.of(new ContentBlock.TableColumn("A")),
                        List.of(new ContentBlock.TableRow(TableRowType.NORMAL, List.of(List.of())))
                )
        )));
        Map<Long, CmsMediaAssetEntity> resolved = service.validateAndResolve(document);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> ids = ArgumentCaptor.forClass(Collection.class);
        verify(repository).findAllByIdIn(ids.capture());
        assertThat(ids.getValue()).containsExactly(10L);
        assertThat(resolved).containsOnlyKeys(10L);
    }

    @Test
    void layoutWithOnlyTableChildrenDoesNotQueryMedia() {
        ContentDocument document = document(new ContentBlock.Layout(2, List.of(
                new ContentBlock.Table(
                        null, null, List.of(new ContentBlock.TableColumn("A")),
                        List.of(new ContentBlock.TableRow(TableRowType.NORMAL, List.of(List.of())))
                ),
                new ContentBlock.Table(
                        null, null, List.of(new ContentBlock.TableColumn("B")),
                        List.of(new ContentBlock.TableRow(TableRowType.NORMAL, List.of(List.of())))
                )
        )));

        assertThat(service.validateAndResolve(document)).isEmpty();
        verify(repository, never()).findAllByIdIn(any());
    }

    @Test
    void createsSafeDynamicPreviewMapWithoutStorageCoordinates() {
        CmsMediaAssetEntity image = asset(10L, CmsMediaType.IMAGE, CmsMediaStatus.READY);
        when(storage.createPresignedRead("private-bucket", "cms/images/10.jpg"))
                .thenReturn(new PresignedReadResult("https://signed.example/10", 300));

        var previews = service.createPreviewMap(Map.of(10L, image));

        assertThat(previews.get(10L).previewUrl()).isEqualTo("https://signed.example/10");
        assertThat(previews.get(10L).previewExpiresInSeconds()).isEqualTo(300);
        assertThat(previews.get(10L).toString()).doesNotContain("private-bucket", "cms/images");
    }

    @Test
    void previewSigningFailureDoesNotMakeCanonicalDocumentUnavailable() {
        CmsMediaAssetEntity image = asset(10L, CmsMediaType.IMAGE, CmsMediaStatus.READY);
        when(storage.createPresignedRead(any(), any())).thenThrow(new IllegalStateException("AWS detail"));

        var preview = service.createPreviewMap(Map.of(10L, image)).get(10L);

        assertThat(preview.previewUrl()).isNull();
        assertThat(preview.previewExpiresInSeconds()).isNull();
    }

    private ContentBlock.Image image(Long id) {
        return new ContentBlock.Image(id, false, "Image", List.of(), ImageLayout.STANDARD, null);
    }

    private ContentDocument document(ContentBlock... blocks) {
        return new ContentDocument(2, List.of(blocks));
    }

    private CmsMediaAssetEntity asset(Long id, CmsMediaType type, CmsMediaStatus status) {
        return CmsMediaAssetEntity.builder()
                .id(id)
                .mediaType(type)
                .status(status)
                .storageBucket("private-bucket")
                .storageKey("cms/images/" + id + ".jpg")
                .originalFilename("asset-" + id + ".jpg")
                .contentType(type == CmsMediaType.IMAGE ? "image/jpeg" : "video/mp4")
                .declaredSizeBytes(100L)
                .sizeBytes(100L)
                .build();
    }

    private void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable, String code) {
        assertThatThrownBy(callable)
                .isInstanceOf(CmsContentApiException.class)
                .extracting(exception -> ((CmsContentApiException) exception).getCode())
                .isEqualTo(code);
    }
}
