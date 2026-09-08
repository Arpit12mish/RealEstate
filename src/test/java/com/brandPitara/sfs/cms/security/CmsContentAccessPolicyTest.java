package com.brandPitara.sfs.cms.security;

import com.brandPitara.sfs.dashboard.auth.security.DashboardUserDetails;
import com.brandPitara.sfs.dashboard.common.enums.DashboardPermission;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.security.identity.DashboardAuthenticationUserSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CmsContentAccessPolicyTest {

    private final CmsContentAccessPolicy policy = new CmsContentAccessPolicy();

    @Test
    void writerCanCreateEditAndSubmitOwnButNotEditOtherReviewOrPublish() {
        Authentication writer = authentication(11L, DashboardRole.CONTENT_STAFF, true,
                CmsPermissionProfile.WRITER.permissions());

        assertThat(policy.canCreate(writer)).isTrue();
        assertThat(policy.canEdit(writer, 11L)).isTrue();
        assertThat(policy.canSubmitForReview(writer, 11L)).isTrue();
        assertThat(policy.canPreview(writer, 11L)).isTrue();
        assertThat(policy.canReadTaxonomy(writer)).isTrue();
        assertThat(policy.canView(writer, 11L)).isTrue();
        assertThat(policy.canView(writer, 12L)).isFalse();
        assertThat(policy.canViewAll(writer)).isFalse();
        assertThat(policy.canEdit(writer, 12L)).isFalse();
        assertThat(policy.canSubmitForReview(writer, 12L)).isFalse();
        assertThat(policy.canReview(writer)).isFalse();
        assertThat(policy.canPublish(writer)).isFalse();
    }

    @Test
    void editorCanCreateEditAnyReviewButCannotPublish() {
        Authentication editor = authentication(21L, DashboardRole.CONTENT_STAFF, true,
                CmsPermissionProfile.EDITOR.permissions());

        assertThat(policy.canCreate(editor)).isTrue();
        assertThat(policy.canEdit(editor, 99L)).isTrue();
        assertThat(policy.canReview(editor)).isTrue();
        assertThat(policy.canPreview(editor, 99L)).isTrue();
        assertThat(policy.canReadTaxonomy(editor)).isTrue();
        assertThat(policy.canViewAll(editor)).isTrue();
        assertThat(policy.canPublish(editor)).isFalse();
    }

    @Test
    void publisherCanPublishUnpublishAndArchiveWithoutEditPrivilege() {
        Authentication publisher = authentication(31L, DashboardRole.CONTENT_STAFF, true,
                CmsPermissionProfile.PUBLISHER.permissions());

        assertThat(policy.canPublish(publisher)).isTrue();
        assertThat(policy.canUnpublish(publisher)).isTrue();
        assertThat(policy.canArchive(publisher)).isTrue();
        assertThat(policy.canPreview(publisher, 99L)).isTrue();
        assertThat(policy.canReadTaxonomy(publisher)).isTrue();
        assertThat(policy.canViewAll(publisher)).isTrue();
        assertThat(policy.canEdit(publisher, 99L)).isFalse();
        assertThat(policy.canReview(publisher)).isFalse();
        assertThat(policy.canCreate(publisher)).isFalse();
    }

    @Test
    void adminHasAllCmsPermissionsWithoutStoredAssignments() {
        Authentication admin = authentication(1L, DashboardRole.ADMIN, true, Set.of());

        assertThat(policy.canCreate(admin)).isTrue();
        assertThat(policy.canEdit(admin, 99L)).isTrue();
        assertThat(policy.canReview(admin)).isTrue();
        assertThat(policy.canPublish(admin)).isTrue();
        assertThat(policy.canUnpublish(admin)).isTrue();
        assertThat(policy.canArchive(admin)).isTrue();
        assertThat(policy.canViewAll(admin)).isTrue();
        assertThat(policy.canReadTaxonomy(admin)).isTrue();
    }

    @Test
    void legacyDashboardRolesHaveNoCmsAccessWithoutExplicitPermission() {
        for (DashboardRole role : Set.of(DashboardRole.DATA_ENTRY, DashboardRole.REVIEWER)) {
            Authentication authentication = authentication(41L, role, true, Set.of());

            assertThat(policy.canCreate(authentication)).isFalse();
            assertThat(policy.canEdit(authentication, 41L)).isFalse();
            assertThat(policy.canReview(authentication)).isFalse();
            assertThat(policy.canPublish(authentication)).isFalse();
            assertThat(policy.canViewAll(authentication)).isFalse();
            assertThat(policy.canReadTaxonomy(authentication)).isFalse();
        }
    }

    @Test
    void explicitlyAssignedPermissionWorksIndependentlyOfProfile() {
        Authentication assigned = authentication(51L, DashboardRole.CONTENT_STAFF, true,
                Set.of(DashboardPermission.CMS_CONTENT_PUBLISH));

        assertThat(policy.canPublish(assigned)).isTrue();
        assertThat(policy.canCreate(assigned)).isFalse();
    }

    @Test
    void unauthenticatedAndDisabledAccountsHaveNoCmsAccess() {
        Authentication disabled = authentication(61L, DashboardRole.CONTENT_STAFF, false,
                CmsPermissionProfile.WRITER.permissions());

        assertThat(policy.canCreate(null)).isFalse();
        assertThat(policy.canCreate(disabled)).isFalse();
        assertThat(policy.canEdit(disabled, 61L)).isFalse();
        assertThat(policy.canReadTaxonomy(null)).isFalse();
        assertThat(policy.canReadTaxonomy(disabled)).isFalse();
    }

    private Authentication authentication(
            Long userId,
            DashboardRole role,
            boolean active,
            Set<DashboardPermission> permissions
    ) {
        DashboardUserDetails principal = new DashboardUserDetails(
                new DashboardAuthenticationUserSnapshot(
                        userId,
                        role.name().toLowerCase() + "@example.com",
                        role.name(),
                        role,
                        active,
                        permissions
                )
        );
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
    }
}
