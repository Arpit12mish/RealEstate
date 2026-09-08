package com.brandPitara.sfs.cms.metadata.service;

import com.brandPitara.sfs.cms.author.entity.CmsPublicAuthorEntity;
import com.brandPitara.sfs.cms.author.repository.CmsPublicAuthorRepository;
import com.brandPitara.sfs.cms.content.exception.CmsContentApiException;
import com.brandPitara.sfs.cms.content.slug.ContentSlugService;
import com.brandPitara.sfs.cms.media.domain.*;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.cms.media.repository.CmsMediaAssetRepository;
import com.brandPitara.sfs.cms.metadata.dto.*;
import com.brandPitara.sfs.cms.taxonomy.entity.*;
import com.brandPitara.sfs.cms.taxonomy.repository.*;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.*;

@Service
@RequiredArgsConstructor
public class CmsMetadataServiceImpl implements CmsMetadataService {
    private final CmsPublicAuthorRepository authors;
    private final CmsContentCategoryRepository categories;
    private final CmsContentTagRepository tags;
    private final CmsMediaAssetRepository media;
    private final ContentSlugService slugService;

    @Override @Transactional
    public CmsAuthorResponse createAuthor(CmsAuthorRequest r) {
        String name = text(r.displayName(), 150, "Author display name");
        String slug = createSlug(r.slug(), name, authors::existsBySlug);
        CmsPublicAuthorEntity value = CmsPublicAuthorEntity.builder().displayName(name).slug(slug)
                .bio(optional(r.bio())).designation(optional(r.designation()))
                .profileMediaAsset(readyImage(r.profileMediaAssetId()))
                .active(r.active() == null || r.active()).build();
        return CmsAuthorResponse.from(save(() -> authors.saveAndFlush(value), slug));
    }

    @Override @Transactional(readOnly=true)
    public CmsAuthorResponse getAuthor(Long id) { return CmsAuthorResponse.from(author(id)); }

    @Override @Transactional
    public CmsAuthorResponse updateAuthor(Long id, CmsAuthorRequest r) {
        CmsPublicAuthorEntity a = author(id); version(a.getVersion(), r.version());
        String slug = updateSlug(r.slug(), r.displayName(), id, authors::existsBySlugAndIdNot);
        a.setDisplayName(text(r.displayName(), 150, "Author display name")); a.setSlug(slug);
        a.setBio(optional(r.bio())); a.setDesignation(optional(r.designation()));
        a.setProfileMediaAsset(readyImage(r.profileMediaAssetId()));
        if (r.active() != null) a.setActive(r.active());
        return CmsAuthorResponse.from(save(() -> authors.saveAndFlush(a), slug));
    }

    @Override @Transactional(readOnly=true)
    public Page<CmsAuthorResponse> authors(Boolean active, String search, Pageable p) {
        return authors.search(active, pattern(search), p).map(CmsAuthorResponse::from);
    }

    @Override @Transactional
    public CmsCategoryResponse createCategory(CmsCategoryRequest r) {
        String name=text(r.name(),150,"Category name"), slug=createSlug(r.slug(),name,categories::existsBySlug);
        var c=CmsContentCategoryEntity.builder().name(name).normalizedName(normalizedName(name)).slug(slug)
                .description(optional(r.description())).active(r.active()==null||r.active()).build();
        return CmsCategoryResponse.from(save(() -> categories.saveAndFlush(c), slug));
    }
    @Override @Transactional(readOnly=true)
    public CmsCategoryResponse getCategory(Long id) { return CmsCategoryResponse.from(category(id)); }
    @Override @Transactional
    public CmsCategoryResponse updateCategory(Long id, CmsCategoryRequest r) {
        var c=category(id); version(c.getVersion(),r.version()); String name=text(r.name(),150,"Category name");
        String slug=updateSlug(r.slug(),name,id,categories::existsBySlugAndIdNot);
        c.setName(name); c.setNormalizedName(normalizedName(name)); c.setSlug(slug);
        c.setDescription(optional(r.description())); if(r.active()!=null)c.setActive(r.active());
        return CmsCategoryResponse.from(save(() -> categories.saveAndFlush(c),slug));
    }
    @Override @Transactional(readOnly=true)
    public Page<CmsCategoryResponse> categories(Boolean active,String search,Pageable p){return categories.search(active,pattern(search),p).map(CmsCategoryResponse::from);}

    @Override @Transactional
    public CmsTagResponse createTag(CmsTagRequest r) {
        String name=text(r.name(),100,"Tag name"),slug=createSlug(r.slug(),name,tags::existsBySlug);
        var t=CmsContentTagEntity.builder().name(name).normalizedName(normalizedName(name)).slug(slug)
                .active(r.active()==null||r.active()).build();
        return CmsTagResponse.from(save(() -> tags.saveAndFlush(t),slug));
    }
    @Override @Transactional(readOnly=true)
    public CmsTagResponse getTag(Long id){return CmsTagResponse.from(tag(id));}
    @Override @Transactional
    public CmsTagResponse updateTag(Long id,CmsTagRequest r){var t=tag(id);version(t.getVersion(),r.version());String name=text(r.name(),100,"Tag name");String slug=updateSlug(r.slug(),name,id,tags::existsBySlugAndIdNot);t.setName(name);t.setNormalizedName(normalizedName(name));t.setSlug(slug);if(r.active()!=null)t.setActive(r.active());return CmsTagResponse.from(save(()->tags.saveAndFlush(t),slug));}
    @Override @Transactional(readOnly=true)
    public Page<CmsTagResponse> tags(Boolean active,String search,Pageable p){return tags.search(active,pattern(search),p).map(CmsTagResponse::from);}

    private CmsPublicAuthorEntity author(Long id){return authors.findWithProfileMediaById(id).orElseThrow(()->CmsContentApiException.metadataNotFound("Public author",id));}
    private CmsContentCategoryEntity category(Long id){return categories.findById(id).orElseThrow(()->CmsContentApiException.metadataNotFound("CMS category",id));}
    private CmsContentTagEntity tag(Long id){return tags.findById(id).orElseThrow(()->CmsContentApiException.metadataNotFound("CMS tag",id));}
    private CmsMediaAssetEntity readyImage(Long id){if(id==null)return null;var a=media.findById(id).orElseThrow(()->CmsContentApiException.mediaNotFound(id));if(a.getStatus()!=CmsMediaStatus.READY)throw CmsContentApiException.mediaNotReady(id);if(a.getMediaType()!=CmsMediaType.IMAGE)throw CmsContentApiException.mediaTypeMismatch(id,"IMAGE");return a;}
    private String createSlug(String requested,String name,Predicate<String> exists){String base=slugService.normalize(StringUtils.hasText(requested)?requested:name);requireSlug(base);for(int i=1;i<=10000;i++){String candidate=i==1?base:trim(base,180-("-"+i).length())+"-"+i;if(!exists.test(candidate))return candidate;}throw CmsContentApiException.validation("Unable to allocate metadata slug.");}
    private String updateSlug(String requested,String name,Long id,BiPredicate<String,Long> exists){String value=slugService.normalize(StringUtils.hasText(requested)?requested:name);requireSlug(value);if(exists.test(value,id))throw CmsContentApiException.validation("CMS metadata slug already exists: "+value);return value;}
    private void requireSlug(String value){if(value.length()<2)throw CmsContentApiException.validation("CMS metadata slug must contain at least 2 URL-safe characters.");}
    private String trim(String value,int max){return value.length()<=max?value:value.substring(0,max).replaceAll("-+$","");}
    private String text(String value,int max,String label){if(!StringUtils.hasText(value))throw CmsContentApiException.validation(label+" is required.");String n=value.trim();if(n.length()>max||n.codePoints().anyMatch(Character::isISOControl))throw CmsContentApiException.validation(label+" is invalid.");return n;}
    private String optional(String value){if(!StringUtils.hasText(value))return null;String n=value.trim();if(n.codePoints().anyMatch(c->Character.isISOControl(c)&&c!='\n'&&c!='\t'))throw CmsContentApiException.validation("CMS metadata contains control characters.");return n;}
    private String normalizedName(String value){return value.trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);}
    private String pattern(String value){return StringUtils.hasText(value)?"%"+value.trim().toLowerCase(Locale.ROOT).replace("%","\\%").replace("_","\\_")+"%":null;}
    private void version(Long actual,Long expected){if(expected==null||!Objects.equals(actual,expected))throw CmsContentApiException.versionConflict();}
    private <T>T save(Supplier<T> action,String slug){try{return action.get();}catch(ObjectOptimisticLockingFailureException|OptimisticLockException e){throw CmsContentApiException.versionConflict();}catch(DataIntegrityViolationException e){throw CmsContentApiException.metadataConflict("CMS metadata name or slug already exists: "+slug);}}
}
