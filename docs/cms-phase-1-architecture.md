# SFS CMS Phase 1 Architecture Contract

## Boundary

CMS dashboard APIs belong under `/api/dashboard/cms/**`. They use the existing
dashboard bearer-token filter chain and `DashboardUserEntity`; no separate
identity store, login endpoint, JWT, refresh-token implementation, or public DTO
is permitted. Future public read APIs belong under `/api/public/...` and must
have dedicated response DTOs.

Create only packages needed by an implemented capability. The intended module
shape is:

```text
cms/
  security/      authorization policy and permission profiles (Phase 1)
  content/       aggregate, repository, application service, dashboard DTOs/controllers
  workflow/      lifecycle transition policy
  revision/      immutable revisions
  media/         CMS media references and policy
  taxonomy/      tags/categories
```

The last five packages are future boundaries, not Phase 1 scaffolding.

## Authorization

`DashboardRole.CONTENT_STAFF` is a least-privilege login role. Writer, editor,
and publisher are permission assignment profiles, not Spring Security roles.
Accounts may combine profiles; controllers use `hasAuthority(...)` or
`@cmsContentAccessPolicy`, while services repeat ownership and transition checks
through the policy.

| Profile | Permissions |
|---|---|
| Writer | create, edit own, submit for review, preview permitted content, future media upload |
| Editor | writer capabilities plus edit any and review; no publish |
| Publisher | preview, publish, unpublish, archive; no implicit edit/review |
| Admin | all CMS permissions implicitly, including CMS user management |

Existing `DATA_ENTRY` and `REVIEWER` accounts receive no CMS permission unless
an administrator explicitly assigns one. `ADMIN` is the only legacy role with
implicit CMS access.

## Future aggregate contract

Use one `ContentPost` aggregate for `ARTICLE`, `BLOG`, and `INTERVIEW`. Its
identity/header contract is: id, contentType, title, slug, status,
contentOwnerDashboardUserId, publicAuthorId, createdByDashboardUserId,
updatedByDashboardUserId, publishedByDashboardUserId, createdAt, updatedAt, and
publishedAt. Rich-editor content, media, embeds, sanitization, and storage are
deliberately unspecified in Phase 1.

`contentOwnerDashboardUserId` governs writer ownership. The `createdBy`,
`updatedBy`, and `publishedBy` fields are immutable/updated audit identities.
`publicAuthorId` is presentation data and may identify someone other than any
dashboard actor.

## Workflow contract

```text
DRAFT -> IN_REVIEW -> APPROVED -> PUBLISHED
          |             |            |
          v             v            v
  CHANGES_REQUESTED   DRAFT       ARCHIVED
```

Writers may change their owned draft and submit it. Editors may edit team
content, request changes, and approve. Publishers may publish approved content,
unpublish it according to a future explicit target-state rule, and archive it.
Admins may perform every transition. Transition validation belongs in one
workflow policy/service and must not be inferred from request DTO values.

## Account administration still required

The existing backend has only environment-driven fixed-user seeding. A narrow
admin-only dashboard-user module is required before operations can onboard CMS
staff. It should create `CONTENT_STAFF` accounts with BCrypt passwords, list
accounts, assign validated permission sets/profiles, activate/deactivate, and
rotate credentials. Every mutation must invalidate the dashboard identity cache;
deactivation and credential reset must revoke all dashboard refresh tokens.
Credential reset must also define access-token invalidation (for example a
credentials/token version claim) before being exposed as production API.

## Audit reuse

Reuse `dashboard_action_audit`; do not create a CMS audit table. Phase 2 adds a
`CONTENT_POST` entity type and CMS action enum values for created, updated,
submitted, changes requested, approved, published, unpublished, archived, and
deleted. Audit writes should occur after successful domain transitions and keep
actor dashboard identity distinct from public author identity.
