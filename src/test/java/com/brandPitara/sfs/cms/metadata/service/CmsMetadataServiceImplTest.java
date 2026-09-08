package com.brandPitara.sfs.cms.metadata.service;

import com.brandPitara.sfs.cms.author.entity.CmsPublicAuthorEntity;
import com.brandPitara.sfs.cms.author.repository.CmsPublicAuthorRepository;
import com.brandPitara.sfs.cms.content.slug.ContentSlugService;
import com.brandPitara.sfs.cms.media.domain.*;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.cms.media.repository.CmsMediaAssetRepository;
import com.brandPitara.sfs.cms.metadata.dto.*;
import com.brandPitara.sfs.cms.taxonomy.entity.CmsContentCategoryEntity;
import com.brandPitara.sfs.cms.taxonomy.entity.CmsContentTagEntity;
import com.brandPitara.sfs.cms.taxonomy.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CmsMetadataServiceImplTest {
    @Mock CmsPublicAuthorRepository authors;
    @Mock CmsContentCategoryRepository categories;
    @Mock CmsContentTagRepository tags;
    @Mock CmsMediaAssetRepository media;
    @Mock ContentSlugService slugs;
    CmsMetadataServiceImpl service;

    @BeforeEach void setUp(){service=new CmsMetadataServiceImpl(authors,categories,tags,media,slugs);}

    @Test void authorUsesNormalizedUniqueSlugAndOnlyReadyImageProfile(){
        when(slugs.normalize("Arpit Mishra")).thenReturn("arpit-mishra");
        when(media.findById(8L)).thenReturn(Optional.of(CmsMediaAssetEntity.builder().id(8L)
                .status(CmsMediaStatus.READY).mediaType(CmsMediaType.IMAGE).build()));
        when(authors.saveAndFlush(any())).thenAnswer(i->{var a=(CmsPublicAuthorEntity)i.getArgument(0);a.setId(4L);return a;});
        var result=service.createAuthor(new CmsAuthorRequest(" Arpit Mishra ",null,"Bio","Analyst",8L,null,null));
        assertThat(result.slug()).isEqualTo("arpit-mishra");
        assertThat(result.profileMediaAssetId()).isEqualTo(8L);
        verify(authors).existsBySlug("arpit-mishra");
    }

    @Test void videoCannotBecomeAuthorProfile(){
        when(slugs.normalize("Author")).thenReturn("author");
        when(media.findById(9L)).thenReturn(Optional.of(CmsMediaAssetEntity.builder().id(9L)
                .status(CmsMediaStatus.READY).mediaType(CmsMediaType.VIDEO).build()));
        assertThatThrownBy(()->service.createAuthor(new CmsAuthorRequest("Author",null,null,null,9L,true,null)))
                .hasMessageContaining("IMAGE");
    }

    @Test void updatesRequireExactOptimisticVersion(){
        var author=CmsPublicAuthorEntity.builder().id(4L).displayName("Old").slug("old").active(true).version(3L).build();
        when(authors.findWithProfileMediaById(4L)).thenReturn(Optional.of(author));
        assertThatThrownBy(()->service.updateAuthor(4L,new CmsAuthorRequest("New",null,null,null,null,true,2L)))
                .hasMessageContaining("modified by another user");
        verify(authors,never()).saveAndFlush(any());
    }

    // getAuthor/getCategory/getTag are plain findById lookups with no active filter — an
    // authorized CMS reader must still be able to resolve an author/category/tag that a
    // piece of existing content references but that was later deactivated (D3 backend
    // follow-up, "TAXONOMY LOOKUP READ AUTHORIZATION" Step 8). Only the *list* endpoints
    // filter by active, via the explicit active query param.
    @Test void getAuthorResolvesInactiveRecordById(){
        var author=CmsPublicAuthorEntity.builder().id(5L).displayName("Retired Author").slug("retired-author")
                .active(false).build();
        when(authors.findWithProfileMediaById(5L)).thenReturn(Optional.of(author));
        var result=service.getAuthor(5L);
        assertThat(result.active()).isFalse();
        assertThat(result.displayName()).isEqualTo("Retired Author");
    }

    @Test void getCategoryResolvesInactiveRecordById(){
        var category=CmsContentCategoryEntity.builder().id(6L).name("Retired Category").slug("retired-category")
                .active(false).build();
        when(categories.findById(6L)).thenReturn(Optional.of(category));
        var result=service.getCategory(6L);
        assertThat(result.active()).isFalse();
        assertThat(result.name()).isEqualTo("Retired Category");
    }

    @Test void getTagResolvesInactiveRecordById(){
        var tag=CmsContentTagEntity.builder().id(7L).name("Retired Tag").slug("retired-tag").active(false).build();
        when(tags.findById(7L)).thenReturn(Optional.of(tag));
        var result=service.getTag(7L);
        assertThat(result.active()).isFalse();
        assertThat(result.name()).isEqualTo("Retired Tag");
    }
}
