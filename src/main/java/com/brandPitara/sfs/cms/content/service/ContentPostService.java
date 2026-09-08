package com.brandPitara.sfs.cms.content.service;

import com.brandPitara.sfs.cms.content.domain.ContentStatus;
import com.brandPitara.sfs.cms.content.domain.ContentType;
import com.brandPitara.sfs.cms.content.dto.ContentPostCreateRequest;
import com.brandPitara.sfs.cms.content.dto.ContentPostDetailResponse;
import com.brandPitara.sfs.cms.content.dto.ContentPostListResponse;
import com.brandPitara.sfs.cms.content.dto.ContentPostUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface ContentPostService {
    ContentPostDetailResponse create(ContentPostCreateRequest request, Authentication authentication);

    ContentPostDetailResponse get(Long contentId, Authentication authentication);

    Page<ContentPostListResponse> list(
            ContentType contentType,
            ContentStatus status,
            Long ownerId,
            String search,
            Pageable pageable,
            Authentication authentication
    );

    ContentPostDetailResponse update(
            Long contentId,
            ContentPostUpdateRequest request,
            Authentication authentication
    );
}
