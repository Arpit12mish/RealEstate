package com.brandPitara.sfs.cms.content.service;

import com.brandPitara.sfs.cms.content.dto.ContentDocumentResponse;
import com.brandPitara.sfs.cms.content.dto.ContentDocumentUpdateRequest;
import org.springframework.security.core.Authentication;

public interface ContentDocumentService {
    ContentDocumentResponse get(Long contentId, Authentication authentication);

    ContentDocumentResponse update(
            Long contentId,
            ContentDocumentUpdateRequest request,
            Authentication authentication
    );
}
