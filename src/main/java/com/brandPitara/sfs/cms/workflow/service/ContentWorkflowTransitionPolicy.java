package com.brandPitara.sfs.cms.workflow.service;

import com.brandPitara.sfs.cms.content.domain.ContentStatus;
import com.brandPitara.sfs.cms.content.exception.CmsContentApiException;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ContentWorkflowTransitionPolicy {

    public void require(ContentStatus current, ContentStatus target) {
        if (!allowedTargets(current).contains(target)) {
            throw CmsContentApiException.invalidTransition(current, target);
        }
    }

    public boolean isEditable(ContentStatus status) {
        return status == ContentStatus.DRAFT || status == ContentStatus.CHANGES_REQUESTED;
    }

    private Set<ContentStatus> allowedTargets(ContentStatus current) {
        return switch (current) {
            case DRAFT -> Set.of(ContentStatus.IN_REVIEW, ContentStatus.ARCHIVED);
            case CHANGES_REQUESTED -> Set.of(ContentStatus.IN_REVIEW, ContentStatus.ARCHIVED);
            case IN_REVIEW -> Set.of(ContentStatus.CHANGES_REQUESTED, ContentStatus.APPROVED);
            case APPROVED -> Set.of(ContentStatus.PUBLISHED, ContentStatus.ARCHIVED);
            case PUBLISHED -> Set.of(ContentStatus.UNPUBLISHED);
            case UNPUBLISHED -> Set.of(ContentStatus.PUBLISHED, ContentStatus.ARCHIVED, ContentStatus.DRAFT);
            case ARCHIVED -> Set.of();
        };
    }
}
