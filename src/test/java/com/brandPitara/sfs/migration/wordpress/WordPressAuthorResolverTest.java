package com.brandPitara.sfs.migration.wordpress;

import com.brandPitara.sfs.cms.author.entity.CmsPublicAuthorEntity;
import com.brandPitara.sfs.cms.author.repository.CmsPublicAuthorRepository;
import com.brandPitara.sfs.cms.content.slug.ContentSlugService;
import com.brandPitara.sfs.cms.metadata.dto.CmsAuthorRequest;
import com.brandPitara.sfs.cms.metadata.dto.CmsAuthorResponse;
import com.brandPitara.sfs.cms.metadata.service.CmsMetadataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WordPressAuthorResolverTest {

    @Mock private CmsMetadataService metadataService;
    @Mock private CmsPublicAuthorRepository authorRepository;

    private WordPressMigrationProperties properties;
    private WordPressAuthorResolver resolver;

    @BeforeEach
    void setUp() {
        properties = new WordPressMigrationProperties();
        properties.setAuthors(List.of(
                new WordPressMigrationProperties.AuthorMapping(1, "Square Foot Story", null, 100),
                new WordPressMigrationProperties.AuthorMapping(2, "Kavita Chawla", null, 16),
                new WordPressMigrationProperties.AuthorMapping(3, "Bhavna Satsangi", null, 28)
        ));
        resolver = new WordPressAuthorResolver(properties, metadataService, authorRepository, new StubSlugService());
    }

    @Test
    void createsAuthorProfileWhenNoneExistsYet() {
        when(authorRepository.findBySlug("kavita-chawla")).thenReturn(Optional.empty());
        when(metadataService.createAuthor(any())).thenReturn(response(42L, "Kavita Chawla", "kavita-chawla"));
        CmsPublicAuthorEntity entity = entity(42L, "Kavita Chawla", "kavita-chawla");
        when(authorRepository.findWithProfileMediaById(42L)).thenReturn(Optional.of(entity));

        CmsPublicAuthorEntity resolved = resolver.resolve(2L);

        assertThat(resolved.getId()).isEqualTo(42L);
        ArgumentCaptor<CmsAuthorRequest> captor = ArgumentCaptor.forClass(CmsAuthorRequest.class);
        verify(metadataService).createAuthor(captor.capture());
        assertThat(captor.getValue().displayName()).isEqualTo("Kavita Chawla");
        assertThat(captor.getValue().slug()).isEqualTo("kavita-chawla");
    }

    @Test
    void reusesExistingAuthorProfileWithoutCreatingADuplicate() {
        CmsPublicAuthorEntity existing = entity(7L, "Square Foot Story", "square-foot-story");
        when(authorRepository.findBySlug("square-foot-story")).thenReturn(Optional.of(existing));

        CmsPublicAuthorEntity first = resolver.resolve(1L);
        CmsPublicAuthorEntity second = resolver.resolve(1L);

        assertThat(first.getId()).isEqualTo(7L);
        assertThat(second.getId()).isEqualTo(7L);
        verify(metadataService, never()).createAuthor(any());
    }

    @Test
    void blockingConflictWhenExistingSlugHasADifferentDisplayName() {
        CmsPublicAuthorEntity collision = entity(55L, "Kavita Chawla Jr.", "kavita-chawla");
        when(authorRepository.findBySlug("kavita-chawla")).thenReturn(Optional.of(collision));

        assertThatThrownBy(() -> resolver.resolve(2L))
                .isInstanceOf(WordPressAuthorResolutionException.class)
                .hasMessageContaining("Kavita Chawla")
                .hasMessageContaining("Kavita Chawla Jr.")
                .hasMessageContaining("55");
        verify(metadataService, never()).createAuthor(any());
    }

    @Test
    void blockingConflictWhenExistingSlugHasAConfiguredButDifferentDesignation() {
        properties.setAuthors(List.of(
                new WordPressMigrationProperties.AuthorMapping(2, "Kavita Chawla", "Founding Editor", 16)
        ));
        CmsPublicAuthorEntity existing = entity(55L, "Kavita Chawla", "kavita-chawla");
        existing.setDesignation("Contributing Writer");
        when(authorRepository.findBySlug("kavita-chawla")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> resolver.resolve(2L)).isInstanceOf(WordPressAuthorResolutionException.class);
    }

    @Test
    void noConflictWhenMappingHasNoDesignationEvenIfExistingAuthorHasOne() {
        // The migration only configures displayName for these three authors (designation is
        // null) - a human-added designation on the existing profile is not a conflict to flag.
        CmsPublicAuthorEntity existing = entity(7L, "Square Foot Story", "square-foot-story");
        existing.setDesignation("Editorial Team");
        when(authorRepository.findBySlug("square-foot-story")).thenReturn(Optional.of(existing));

        CmsPublicAuthorEntity resolved = resolver.resolve(1L);

        assertThat(resolved.getId()).isEqualTo(7L);
        verify(metadataService, never()).createAuthor(any());
    }

    @Test
    void resolvingARepresentativeSubsetOfPostsNeverTriggersFullReconciliation() {
        // A representative-sample import (e.g. 10 posts, far fewer than 144) must be able to
        // resolve authors freely - reconcile() is a separate, explicitly-invoked operation.
        CmsPublicAuthorEntity square = entity(7L, "Square Foot Story", "square-foot-story");
        CmsPublicAuthorEntity bhavna = entity(9L, "Bhavna Satsangi", "bhavna-satsangi");
        when(authorRepository.findBySlug("square-foot-story")).thenReturn(Optional.of(square));
        when(authorRepository.findBySlug("bhavna-satsangi")).thenReturn(Optional.of(bhavna));

        resolver.resolve(1L);
        resolver.resolve(1L);
        resolver.resolve(3L);

        // Only 2 of the 3 configured authors were touched, and nowhere near their configured
        // expected totals (100 / 28) - none of that is checked unless reconcile() is called.
    }

    @Test
    void unknownWordPressAuthorIdFailsInsteadOfDefaulting() {
        assertThatThrownBy(() -> resolver.resolve(99L))
                .isInstanceOf(WordPressAuthorResolutionException.class)
                .hasMessageContaining("99");
        verify(authorRepository, never()).findBySlug(any());
    }

    @Test
    void reconcileAcceptsExactPerAuthorAndTotalCounts() {
        resolver.reconcile(Map.of(1L, 100L, 2L, 16L, 3L, 28L));
    }

    @Test
    void reconcileRejectsPerAuthorMismatch() {
        assertThatThrownBy(() -> resolver.reconcile(Map.of(1L, 99L, 2L, 16L, 3L, 28L)))
                .isInstanceOf(WordPressAuthorResolutionException.class)
                .hasMessageContaining("Square Foot Story")
                .hasMessageContaining("100")
                .hasMessageContaining("99");
    }

    @Test
    void reconcileRejectsWrongTotalEvenWhenEveryPresentAuthorMatches() {
        // Author 3 (Bhavna Satsangi, 28 posts) missing entirely from the actual data -
        // no per-author entry to mismatch against, but the total must still fail loudly.
        assertThatThrownBy(() -> resolver.reconcile(Map.of(1L, 100L, 2L, 16L)))
                .isInstanceOf(WordPressAuthorResolutionException.class)
                .hasMessageContaining("144")
                .hasMessageContaining("116");
    }

    @Test
    void reconcileRejectsAnUnconfiguredAuthorIdInTheSourceData() {
        assertThatThrownBy(() -> resolver.reconcile(Map.of(1L, 100L, 2L, 16L, 3L, 28L, 4L, 5L)))
                .isInstanceOf(WordPressAuthorResolutionException.class)
                .hasMessageContaining("4");
    }

    private CmsAuthorResponse response(Long id, String displayName, String slug) {
        return new CmsAuthorResponse(id, displayName, slug, null, null, null, true,
                OffsetDateTime.now(), OffsetDateTime.now(), 0L);
    }

    private CmsPublicAuthorEntity entity(Long id, String displayName, String slug) {
        return CmsPublicAuthorEntity.builder().id(id).displayName(displayName).slug(slug).active(true).build();
    }

    private static final class StubSlugService implements ContentSlugService {
        @Override
        public String normalize(String value) {
            return value.toLowerCase().replace(" ", "-");
        }

        @Override
        public String resolveForCreate(String requestedSlug, String title) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String resolveForUpdate(String requestedSlug, Long contentId) {
            throw new UnsupportedOperationException();
        }
    }
}
