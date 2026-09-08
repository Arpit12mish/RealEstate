package com.brandPitara.sfs.dashboard.common.enums;

/**
 * Fine-grained dashboard authorities. Roles remain the coarse, legacy access
 * model; new modules should authorize with permissions and a domain policy.
 */
public enum DashboardPermission {
    CMS_CONTENT_CREATE,
    CMS_CONTENT_EDIT_OWN,
    CMS_CONTENT_EDIT_ANY,
    CMS_CONTENT_SUBMIT_REVIEW,
    CMS_CONTENT_REVIEW,
    CMS_CONTENT_PREVIEW,
    CMS_CONTENT_PUBLISH,
    CMS_CONTENT_UNPUBLISH,
    CMS_CONTENT_ARCHIVE,
    CMS_MEDIA_UPLOAD,
    CMS_USER_MANAGE,
    ANALYTICS_VIEW
}
