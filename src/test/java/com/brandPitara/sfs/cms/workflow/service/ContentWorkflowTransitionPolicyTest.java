package com.brandPitara.sfs.cms.workflow.service;

import com.brandPitara.sfs.cms.content.domain.ContentStatus;
import com.brandPitara.sfs.cms.content.exception.CmsContentApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContentWorkflowTransitionPolicyTest {

    private final ContentWorkflowTransitionPolicy policy = new ContentWorkflowTransitionPolicy();

    @Test
    void permitsOnlyTheExplicitWorkflowMatrix() {
        allowed(ContentStatus.DRAFT, ContentStatus.IN_REVIEW);
        allowed(ContentStatus.DRAFT, ContentStatus.ARCHIVED);
        allowed(ContentStatus.CHANGES_REQUESTED, ContentStatus.IN_REVIEW);
        allowed(ContentStatus.CHANGES_REQUESTED, ContentStatus.ARCHIVED);
        allowed(ContentStatus.IN_REVIEW, ContentStatus.CHANGES_REQUESTED);
        allowed(ContentStatus.IN_REVIEW, ContentStatus.APPROVED);
        allowed(ContentStatus.APPROVED, ContentStatus.PUBLISHED);
        allowed(ContentStatus.APPROVED, ContentStatus.ARCHIVED);
        allowed(ContentStatus.PUBLISHED, ContentStatus.UNPUBLISHED);
        allowed(ContentStatus.UNPUBLISHED, ContentStatus.PUBLISHED);
        allowed(ContentStatus.UNPUBLISHED, ContentStatus.ARCHIVED);

        for (ContentStatus status : ContentStatus.values()) {
            assertThat(policy.isEditable(status)).isEqualTo(
                    status == ContentStatus.DRAFT || status == ContentStatus.CHANGES_REQUESTED
            );
        }
    }

    @Test
    void rejectsBypassesAndRequiresUnpublishBeforeArchive() {
        denied(ContentStatus.DRAFT, ContentStatus.PUBLISHED);
        denied(ContentStatus.IN_REVIEW, ContentStatus.PUBLISHED);
        denied(ContentStatus.PUBLISHED, ContentStatus.ARCHIVED);
        denied(ContentStatus.ARCHIVED, ContentStatus.DRAFT);
    }

    private void allowed(ContentStatus from, ContentStatus to) {
        policy.require(from, to);
    }

    private void denied(ContentStatus from, ContentStatus to) {
        assertThatThrownBy(() -> policy.require(from, to))
                .isInstanceOf(CmsContentApiException.class)
                .extracting(exception -> ((CmsContentApiException) exception).getCode())
                .isEqualTo("CONTENT_WORKFLOW_INVALID_TRANSITION");
    }
}
