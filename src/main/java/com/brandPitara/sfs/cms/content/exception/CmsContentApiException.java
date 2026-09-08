package com.brandPitara.sfs.cms.content.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import com.brandPitara.sfs.cms.content.domain.ContentStatus;

@Getter
public class CmsContentApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public CmsContentApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static CmsContentApiException notFound(Long contentId) {
        return new CmsContentApiException(
                HttpStatus.NOT_FOUND,
                "CONTENT_NOT_FOUND",
                "Content post not found: " + contentId
        );
    }

    public static CmsContentApiException slugConflict(String slug) {
        return new CmsContentApiException(
                HttpStatus.CONFLICT,
                "CONTENT_SLUG_CONFLICT",
                "Content slug already exists: " + slug
        );
    }

    public static CmsContentApiException versionConflict() {
        return new CmsContentApiException(
                HttpStatus.CONFLICT,
                "CONTENT_VERSION_CONFLICT",
                "Content was modified by another user. Reload it before saving again."
        );
    }

    public static CmsContentApiException validation(String message) {
        return new CmsContentApiException(HttpStatus.BAD_REQUEST, "CONTENT_VALIDATION_ERROR", message);
    }

    public static CmsContentApiException metadataNotFound(String kind, Long id) {
        return new CmsContentApiException(HttpStatus.NOT_FOUND, "CMS_METADATA_NOT_FOUND",
                kind + " not found: " + id);
    }

    public static CmsContentApiException metadataConflict(String message) {
        return new CmsContentApiException(HttpStatus.CONFLICT, "CMS_METADATA_CONFLICT", message);
    }

    public static CmsContentApiException documentInvalid(String message) {
        return new CmsContentApiException(HttpStatus.BAD_REQUEST, "CONTENT_DOCUMENT_INVALID", message);
    }

    public static CmsContentApiException documentTooLarge(int maximumBytes) {
        return new CmsContentApiException(
                HttpStatus.CONTENT_TOO_LARGE,
                "CONTENT_DOCUMENT_TOO_LARGE",
                "Content document exceeds the maximum serialized size of " + maximumBytes + " bytes."
        );
    }

    public static CmsContentApiException documentSchemaUnsupported(int schemaVersion) {
        return new CmsContentApiException(
                HttpStatus.BAD_REQUEST,
                "CONTENT_DOCUMENT_SCHEMA_UNSUPPORTED",
                "Unsupported content document schema version: " + schemaVersion
        );
    }

    public static CmsContentApiException mediaNotFound(Long mediaId) {
        return new CmsContentApiException(
                HttpStatus.BAD_REQUEST,
                "CONTENT_MEDIA_NOT_FOUND",
                "Referenced CMS media asset was not found: " + mediaId
        );
    }

    public static CmsContentApiException mediaNotReady(Long mediaId) {
        return new CmsContentApiException(
                HttpStatus.CONFLICT,
                "CONTENT_MEDIA_NOT_READY",
                "Referenced CMS media asset is not READY: " + mediaId
        );
    }

    public static CmsContentApiException mediaTypeMismatch(Long mediaId, String expectedType) {
        return new CmsContentApiException(
                HttpStatus.BAD_REQUEST,
                "CONTENT_MEDIA_TYPE_MISMATCH",
                "Referenced CMS media asset " + mediaId + " must be " + expectedType + "."
        );
    }

    public static CmsContentApiException notEditable() {
        return new CmsContentApiException(
                HttpStatus.CONFLICT,
                "CONTENT_NOT_EDITABLE",
                "Only DRAFT or CHANGES_REQUESTED content may be edited."
        );
    }

    public static CmsContentApiException invalidTransition(ContentStatus from, ContentStatus to) {
        return new CmsContentApiException(
                HttpStatus.CONFLICT, "CONTENT_WORKFLOW_INVALID_TRANSITION",
                "Content cannot transition from " + from + " to " + to + "."
        );
    }

    public static CmsContentApiException notReviewReady(String message) {
        return new CmsContentApiException(HttpStatus.CONFLICT, "CONTENT_NOT_REVIEW_READY", message);
    }

    public static CmsContentApiException reviewRevisionMissing() {
        return new CmsContentApiException(
                HttpStatus.CONFLICT, "CONTENT_REVIEW_REVISION_MISSING",
                "The current review revision is missing."
        );
    }

    public static CmsContentApiException approvedRevisionMissing() {
        return new CmsContentApiException(
                HttpStatus.CONFLICT, "CONTENT_APPROVED_REVISION_MISSING",
                "The approved revision is missing."
        );
    }

    public static CmsContentApiException publishedRevisionMissing() {
        return new CmsContentApiException(
                HttpStatus.CONFLICT, "CONTENT_PUBLISHED_REVISION_MISSING",
                "The current published revision is missing."
        );
    }

    public static CmsContentApiException revisionNotFound(Long revisionId) {
        return new CmsContentApiException(
                HttpStatus.NOT_FOUND, "CONTENT_REVISION_NOT_FOUND",
                "Content revision not found: " + revisionId
        );
    }

    public static CmsContentApiException reviewCommentRequired() {
        return new CmsContentApiException(
                HttpStatus.BAD_REQUEST, "CONTENT_REVIEW_COMMENT_REQUIRED",
                "A review comment is required when requesting changes."
        );
    }

    public static CmsContentApiException reviewCommentInvalid() {
        return new CmsContentApiException(
                HttpStatus.BAD_REQUEST, "CONTENT_REVIEW_COMMENT_INVALID",
                "Review comment must be plain text of at most 4000 characters."
        );
    }

    public static CmsContentApiException publishDependencyInvalid() {
        return new CmsContentApiException(
                HttpStatus.CONFLICT, "CONTENT_PUBLISH_DEPENDENCY_INVALID",
                "Approved content references media that is no longer publishable."
        );
    }

    public static CmsContentApiException slugLocked() {
        return new CmsContentApiException(
                HttpStatus.CONFLICT, "CONTENT_SLUG_LOCKED",
                "The slug is immutable after first publication until redirect history is supported."
        );
    }

    public static CmsContentApiException workflowConflict() {
        return new CmsContentApiException(
                HttpStatus.CONFLICT, "CONTENT_WORKFLOW_CONFLICT",
                "Content workflow changed concurrently. Reload before retrying."
        );
    }
}
