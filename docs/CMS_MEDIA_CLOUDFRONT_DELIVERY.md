# CMS media delivery: private S3 to CloudFront

## Decision and scope

Published CMS media is delivered as immutable objects through CloudFront:

```text
browser -> HTTPS CloudFront/custom media domain -> OAC -> private S3 REST origin
```

The application derives a stable URL from `CMS_MEDIA_PUBLIC_BASE_URL` and the
server-generated `CmsMediaAsset.storageKey`. It makes no CloudFront or S3 API
call during a public content read. Dashboard uploads continue to use short-lived
presigned S3 PUT URLs, and dashboard previews continue to use short-lived
presigned S3 GET URLs.

Application URL resolution emits `cms.public.media.resolve` with only
`mediaType` and `result` tags. Asset IDs, keys, slugs, filenames, and users are
never metric tags; CloudFront remains the source for edge request/cache metrics.

The repository contains an existing CloudFront client and distribution ID for
project API cache invalidation. It does not establish an S3 CMS-media origin,
OAC, media hostname, or `/cms/*` behavior. Therefore that distribution is **not
proven reusable**. An operator may reuse it only after confirming the origin and
cache behavior can be isolated safely; otherwise create a dedicated media
distribution.

## S3 controls

For the bucket named by `CMS_MEDIA_S3_BUCKET`:

1. Keep all four S3 Block Public Access settings enabled.
2. Keep default encryption enabled (SSE-S3 or the organization's SSE-KMS policy).
3. Enable bucket versioning for recovery from accidental operator overwrite.
   Runtime URLs and application records do not depend on S3 version IDs.
4. Permit CMS objects only below `cms/images/` and `cms/videos/`.
5. Do not configure an S3 website endpoint or anonymous `GetObject` access.
6. Preserve the signed `Content-Type` and
   `Cache-Control: public, max-age=31536000, immutable` metadata supplied on PUT.
   The signed `If-None-Match: *` header makes creation one-shot and prevents a
   still-valid upload URL from replacing an existing key.

No S3 CORS policy is needed for ordinary `<img>`/`<video>` display through
CloudFront. The direct dashboard PUT flow needs an S3 CORS rule for the exact
dashboard origins, PUT, and the signed request headers. Add GET/HEAD and expose
only required headers later if a trusted SFS origin needs programmatic `fetch`
or canvas access. Do not add a wildcard origin by default.

## CloudFront origin and behavior

- Use the private bucket's S3 REST endpoint, not its website endpoint.
- Create an Origin Access Control with request signing set to `always` and
  SigV4. Require HTTPS to the origin.
- Create a `/cms/*` behavior allowing only `GET` and `HEAD`; redirect viewer
  HTTP to HTTPS.
- Do not enable PUT, POST, PATCH, or DELETE. Upload does not pass through
  CloudFront.
- Use a cache policy with no cookies, no query strings, and no Authorization
  header. The immutable storage key is the complete cache identity.
- Query strings are not part of the Phase 6B media contract. Image transforms
  need a separate, explicit design.
- Honor the origin `Cache-Control` value. A response headers policy may enforce
  the same one-year immutable browser policy if infrastructure standards
  require it.
- Add `X-Content-Type-Options: nosniff` through a response headers policy.
- Preserve the validated S3 `Content-Type`. Do not override objects as
  `text/html`, and do not derive disposition from the original filename.
- CloudFront and the S3 REST origin support byte-range requests. Do not strip
  `Range`; verify MP4 seeking returns `206`, `Content-Range`, and an appropriate
  `Accept-Ranges` response. No transcoding or HLS is introduced here.
- Automatic compression may remain enabled for compressible types. It should
  not be treated as video transcoding and must not interfere with range reads.

The cache key should not forward cookies, query strings, Authorization, or
unnecessary viewer headers. Configure CORS only if a real browser API use case
exists, and then allow only the required SFS origins/methods/headers.

## Least-privilege OAC bucket policy

Replace every angle-bracket placeholder. Keep the resource limited to the CMS
prefix and the condition limited to the exact distribution ARN.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowExactCloudFrontDistributionReadCmsMedia",
      "Effect": "Allow",
      "Principal": {
        "Service": "cloudfront.amazonaws.com"
      },
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::<CMS_BUCKET>/cms/*",
      "Condition": {
        "StringEquals": {
          "AWS:SourceArn": "arn:aws:cloudfront::<AWS_ACCOUNT_ID>:distribution/<DISTRIBUTION_ID>"
        }
      }
    }
  ]
}
```

Do not grant `s3:*`, use an account-wide CloudFront condition, or disable Block
Public Access. Existing writer permissions for the backend upload role remain a
separate IAM concern.

## Domain, TLS, and runtime configuration

For a custom hostname such as `media.squarefootstory.com`:

1. Request or import an ACM certificate in `us-east-1` covering the hostname.
2. Attach the alternate domain name and certificate to CloudFront.
3. At any DNS provider, create the provider's CNAME/ALIAS equivalent pointing
   the hostname at the CloudFront distribution domain.
4. Set the production runtime value:

   ```text
   CMS_MEDIA_PUBLIC_BASE_URL=https://media.squarefootstory.com
   ```

Production rejects a missing, malformed, credential-bearing, or non-HTTPS base
URL at startup. Local and test environments may omit it; text-only and
YouTube-only articles remain readable, while an article containing CMS media
fails safely with `503 CONTENT_TEMPORARILY_UNAVAILABLE`. There is no fallback to
an S3 URL.

## Operator verification

Run these only after configuring a non-production or approved production
distribution. Substitute real values without printing credentials:

```bash
curl -sS -D - -o /dev/null https://<MEDIA_HOST>/cms/images/<OBJECT>.jpg
curl -sS -I https://<MEDIA_HOST>/cms/videos/<OBJECT>.mp4
curl -sS -r 0-1023 -D - -o /dev/null https://<MEDIA_HOST>/cms/videos/<OBJECT>.mp4
curl -sS -I https://<CMS_BUCKET>.s3.<REGION>.amazonaws.com/cms/images/<OBJECT>.jpg
```

Verify:

- CloudFront GET/HEAD succeeds and uses HTTPS.
- `Content-Type` matches the validated asset type.
- `Cache-Control` is `public, max-age=31536000, immutable`.
- `X-Content-Type-Options` is `nosniff`.
- the range request returns `206` with `Content-Range` and supports seeking.
- the direct anonymous S3 request returns `403` (or otherwise denies content).
- the public content JSON exposes `deliveryUrl`, but no bucket, storage key,
  ETag, creator, or presigned URL.

## Invalidation boundary

Immutable media needs no normal invalidation: changed bytes create a new asset,
key, and URL. Never replace bytes at a READY asset's key.

The repository's current CloudFront invalidator targets project API paths, not
the new public CMS API. Do not call it unless deployment proves the CMS public
API is behind that exact distribution. A future after-commit publication hook
may invalidate `/api/public/content/{slug}` and the bounded list pages on
publish, unpublish, or republish. The existing `s-maxage=60` policy remains the
safe default until that topology is known.
