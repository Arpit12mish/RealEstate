package com.brandPitara.sfs.cms.metadata.service;

import com.brandPitara.sfs.cms.metadata.dto.*;
import org.springframework.data.domain.*;

public interface CmsMetadataService {
    CmsAuthorResponse createAuthor(CmsAuthorRequest request);
    CmsAuthorResponse getAuthor(Long id);
    CmsAuthorResponse updateAuthor(Long id, CmsAuthorRequest request);
    Page<CmsAuthorResponse> authors(Boolean active, String search, Pageable pageable);
    CmsCategoryResponse createCategory(CmsCategoryRequest request);
    CmsCategoryResponse getCategory(Long id);
    CmsCategoryResponse updateCategory(Long id, CmsCategoryRequest request);
    Page<CmsCategoryResponse> categories(Boolean active, String search, Pageable pageable);
    CmsTagResponse createTag(CmsTagRequest request);
    CmsTagResponse getTag(Long id);
    CmsTagResponse updateTag(Long id, CmsTagRequest request);
    Page<CmsTagResponse> tags(Boolean active, String search, Pageable pageable);
}
