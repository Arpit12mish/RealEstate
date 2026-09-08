package com.brandPitara.sfs.cms.content.service.impl;

import com.brandPitara.sfs.cms.content.document.ContentDocument;
import com.brandPitara.sfs.cms.content.document.CmsMediaReferenceService;
import com.brandPitara.sfs.cms.content.document.ContentDocumentValidator;
import com.brandPitara.sfs.cms.content.document.ContentDocumentWordCounter;
import com.brandPitara.sfs.cms.content.dto.ContentDocumentResponse;
import com.brandPitara.sfs.cms.content.dto.ContentDocumentUpdateRequest;
import com.brandPitara.sfs.cms.content.entity.ContentPostEntity;
import com.brandPitara.sfs.cms.content.exception.CmsContentApiException;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.cms.content.repository.ContentPostRepository;
import com.brandPitara.sfs.cms.content.service.ContentDocumentService;
import com.brandPitara.sfs.cms.security.CmsContentAccessPolicy;
import com.brandPitara.sfs.cms.workflow.service.ContentWorkflowTransitionPolicy;
import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ContentDocumentServiceImpl implements ContentDocumentService {

    private final ContentPostRepository contentPostRepository;
    private final DashboardUserRepository dashboardUserRepository;
    private final DashboardCurrentUserService currentUserService;
    private final CmsContentAccessPolicy accessPolicy;
    private final ContentDocumentValidator documentValidator;
    private final ContentDocumentWordCounter wordCounter;
    private final CmsMediaReferenceService mediaReferenceService;
    private final ContentWorkflowTransitionPolicy transitionPolicy;

    @Override
    @Transactional(readOnly = true)
    public ContentDocumentResponse get(Long contentId, Authentication authentication) {
        ContentPostEntity post = findDetailed(contentId);
        accessPolicy.assertCanView(authentication, post.getContentOwner().getId());
        return response(post);
    }

    @Override
    @Transactional
    public ContentDocumentResponse update(
            Long contentId,
            ContentDocumentUpdateRequest request,
            Authentication authentication
    ) {
        ContentPostEntity post = findDetailed(contentId);
        accessPolicy.assertCanEdit(authentication, post.getContentOwner().getId());
        if (!transitionPolicy.isEditable(post.getStatus())) {
            throw CmsContentApiException.notEditable();
        }
        if (!Objects.equals(post.getVersion(), request.version())) {
            throw CmsContentApiException.versionConflict();
        }

        ContentDocument normalized = documentValidator.validateAndNormalize(request.document());
        Map<Long, CmsMediaAssetEntity> resolvedMedia = mediaReferenceService.validateAndResolve(normalized);
        DashboardUserEntity currentUser = currentUserService.getCurrentUserOrThrow();
        DashboardUserEntity actor = dashboardUserRepository.getReferenceById(currentUser.getId());
        post.setContentDocument(normalized);
        post.setContentDocumentSchemaVersion((short) normalized.schemaVersion());
        post.setUpdatedBy(actor);

        try {
            return response(contentPostRepository.saveAndFlush(post), resolvedMedia);
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException exception) {
            throw CmsContentApiException.versionConflict();
        }
    }

    private ContentPostEntity findDetailed(Long contentId) {
        return contentPostRepository.findDetailedById(contentId)
                .orElseThrow(() -> CmsContentApiException.notFound(contentId));
    }

    private ContentDocumentResponse response(ContentPostEntity post) {
        return response(post, mediaReferenceService.validateAndResolve(post.getContentDocument()));
    }

    private ContentDocumentResponse response(
            ContentPostEntity post,
            Map<Long, CmsMediaAssetEntity> resolvedMedia
    ) {
        return ContentDocumentResponse.from(
                post,
                wordCounter.count(post.getContentDocument()),
                mediaReferenceService.createPreviewMap(resolvedMedia)
        );
    }
}
