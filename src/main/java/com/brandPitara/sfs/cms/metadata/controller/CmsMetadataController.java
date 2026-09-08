package com.brandPitara.sfs.cms.metadata.controller;

import com.brandPitara.sfs.cms.metadata.dto.*;
import com.brandPitara.sfs.cms.metadata.service.CmsMetadataService;
import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/cms")
@RequiredArgsConstructor
public class CmsMetadataController {
    private final CmsMetadataService service;
    private final DashboardActionAuditService audit;

    // Taxonomy MANAGEMENT (create/update) stays ADMIN-only. Taxonomy READ (list/by-id) is
    // granted to any authenticated CMS content-staff member via CMS_CONTENT_PREVIEW — see
    // CmsContentAccessPolicy#canReadTaxonomy for why that permission was chosen. Every
    // method below carries its own @PreAuthorize deliberately (no class-level default):
    // Spring Security fully overrides a class-level @PreAuthorize with a method-level one
    // rather than combining them, so a shared class-level annotation would silently stop
    // applying to any new method that forgot its own — explicit per-method is the fail-loud
    // choice, and matches this module's other CMS controllers (ContentPostController,
    // CmsMediaController), which never use a class-level @PreAuthorize either.

    @PostMapping("/authors") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')")
    public CmsAuthorResponse createAuthor(@Valid @RequestBody CmsAuthorRequest r){var v=service.createAuthor(r);audit.record(DashboardAuditAction.CMS_AUTHOR_CREATED,ReviewEntityType.CMS_PUBLIC_AUTHOR,v.id(),null);return v;}
    @GetMapping("/authors/{id}") @PreAuthorize("@cmsContentAccessPolicy.canReadTaxonomy(authentication)")
    public CmsAuthorResponse author(@PathVariable Long id){return service.getAuthor(id);}
    @PutMapping("/authors/{id}") @PreAuthorize("hasRole('ADMIN')")
    public CmsAuthorResponse updateAuthor(@PathVariable Long id,@Valid @RequestBody CmsAuthorRequest r){var v=service.updateAuthor(id,r);audit.record(DashboardAuditAction.CMS_AUTHOR_UPDATED,ReviewEntityType.CMS_PUBLIC_AUTHOR,id,null);return v;}
    @GetMapping("/authors") @PreAuthorize("@cmsContentAccessPolicy.canReadTaxonomy(authentication)")
    public Page<CmsAuthorResponse> authors(@RequestParam(required=false)Boolean active,@RequestParam(required=false)String search,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return service.authors(active,search,page(page,size,"displayName"));}

    @PostMapping("/categories") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')")
    public CmsCategoryResponse createCategory(@Valid @RequestBody CmsCategoryRequest r){var v=service.createCategory(r);audit.record(DashboardAuditAction.CMS_CATEGORY_CREATED,ReviewEntityType.CMS_CONTENT_CATEGORY,v.id(),null);return v;}
    @GetMapping("/categories/{id}") @PreAuthorize("@cmsContentAccessPolicy.canReadTaxonomy(authentication)")
    public CmsCategoryResponse category(@PathVariable Long id){return service.getCategory(id);}
    @PutMapping("/categories/{id}") @PreAuthorize("hasRole('ADMIN')")
    public CmsCategoryResponse updateCategory(@PathVariable Long id,@Valid @RequestBody CmsCategoryRequest r){var v=service.updateCategory(id,r);audit.record(DashboardAuditAction.CMS_CATEGORY_UPDATED,ReviewEntityType.CMS_CONTENT_CATEGORY,id,null);return v;}
    @GetMapping("/categories") @PreAuthorize("@cmsContentAccessPolicy.canReadTaxonomy(authentication)")
    public Page<CmsCategoryResponse> categories(@RequestParam(required=false)Boolean active,@RequestParam(required=false)String search,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return service.categories(active,search,page(page,size,"name"));}

    @PostMapping("/tags") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')")
    public CmsTagResponse createTag(@Valid @RequestBody CmsTagRequest r){var v=service.createTag(r);audit.record(DashboardAuditAction.CMS_TAG_CREATED,ReviewEntityType.CMS_CONTENT_TAG,v.id(),null);return v;}
    @GetMapping("/tags/{id}") @PreAuthorize("@cmsContentAccessPolicy.canReadTaxonomy(authentication)")
    public CmsTagResponse tag(@PathVariable Long id){return service.getTag(id);}
    @PutMapping("/tags/{id}") @PreAuthorize("hasRole('ADMIN')")
    public CmsTagResponse updateTag(@PathVariable Long id,@Valid @RequestBody CmsTagRequest r){var v=service.updateTag(id,r);audit.record(DashboardAuditAction.CMS_TAG_UPDATED,ReviewEntityType.CMS_CONTENT_TAG,id,null);return v;}
    @GetMapping("/tags") @PreAuthorize("@cmsContentAccessPolicy.canReadTaxonomy(authentication)")
    public Page<CmsTagResponse> tags(@RequestParam(required=false)Boolean active,@RequestParam(required=false)String search,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return service.tags(active,search,page(page,size,"name"));}

    // Sort property must be a real field on the target entity: CmsContentCategoryEntity/CmsContentTagEntity
    // have "name", but CmsPublicAuthorEntity has "displayName" (no "name" field). A single hardcoded "name"
    // here previously 500'd every GET /authors call — Spring Data appends this Sort onto the repository's
    // @Query, and Hibernate fails to resolve the property against CmsPublicAuthorEntity's metamodel.
    private PageRequest page(int page,int size,String sortProperty){return PageRequest.of(Math.max(0,page),Math.min(Math.max(1,size),100),Sort.by(Sort.Direction.ASC,sortProperty).and(Sort.by("id")));}
}
