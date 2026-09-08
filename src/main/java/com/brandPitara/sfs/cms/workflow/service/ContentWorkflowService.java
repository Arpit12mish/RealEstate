package com.brandPitara.sfs.cms.workflow.service;

import com.brandPitara.sfs.cms.workflow.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface ContentWorkflowService {
    ContentWorkflowResponse submit(Long contentId, SubmitReviewRequest request, Authentication authentication);
    ContentWorkflowResponse requestChanges(Long contentId, RequestChangesRequest request, Authentication authentication);
    ContentWorkflowResponse approve(Long contentId, ApproveContentRequest request, Authentication authentication);
    ContentWorkflowResponse publish(Long contentId, PublishContentRequest request, Authentication authentication);
    ContentWorkflowResponse unpublish(Long contentId, UnpublishContentRequest request, Authentication authentication);
    ContentWorkflowResponse archive(Long contentId, ArchiveContentRequest request, Authentication authentication);
    ContentWorkflowResponse reopen(Long contentId, ReopenContentRequest request, Authentication authentication);
    Page<ContentRevisionListResponse> revisions(Long contentId, Pageable pageable, Authentication authentication);
    ContentRevisionDetailResponse revision(Long contentId, Long revisionId, Authentication authentication);
    Page<ContentWorkflowActivityResponse> history(Long contentId, Pageable pageable, Authentication authentication);
}
