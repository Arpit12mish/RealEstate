# CMS API Curl Smoke Test

Run only against an authorized local/staging environment. Requires `curl` and `jq`. Never paste real credentials into this file or shell history on shared machines.

## Variables and login

```bash
export BASE_URL='http://localhost:8080'
export ADMIN_EMAIL='admin@example.test'
export ADMIN_PASSWORD='replace-me'
export WRITER_EMAIL='writer@example.test'
export WRITER_PASSWORD='replace-me'
export EDITOR_EMAIL='editor@example.test'
export EDITOR_PASSWORD='replace-me'
export PUBLISHER_EMAIL='publisher@example.test'
export PUBLISHER_PASSWORD='replace-me'

export ADMIN_TOKEN="$(curl -fsS "$BASE_URL/api/dashboard/auth/login" \
  -H 'Content-Type: application/json' \
  --data "$(jq -nc --arg e "$ADMIN_EMAIL" --arg p "$ADMIN_PASSWORD" '{email:$e,password:$p}')" | jq -r .accessToken)"
export WRITER_TOKEN="$(curl -fsS "$BASE_URL/api/dashboard/auth/login" \
  -H 'Content-Type: application/json' \
  --data "$(jq -nc --arg e "$WRITER_EMAIL" --arg p "$WRITER_PASSWORD" '{email:$e,password:$p}')" | jq -r .accessToken)"
export EDITOR_TOKEN="$(curl -fsS "$BASE_URL/api/dashboard/auth/login" \
  -H 'Content-Type: application/json' \
  --data "$(jq -nc --arg e "$EDITOR_EMAIL" --arg p "$EDITOR_PASSWORD" '{email:$e,password:$p}')" | jq -r .accessToken)"
export PUBLISHER_TOKEN="$(curl -fsS "$BASE_URL/api/dashboard/auth/login" \
  -H 'Content-Type: application/json' \
  --data "$(jq -nc --arg e "$PUBLISHER_EMAIL" --arg p "$PUBLISHER_PASSWORD" '{email:$e,password:$p}')" | jq -r .accessToken)"

curl -fsS "$BASE_URL/api/dashboard/auth/me" \
  -H "Authorization: Bearer $WRITER_TOKEN" | jq
```

If content-team users do not exist, create them as ADMIN (choose unique emails):

```bash
curl -fsS "$BASE_URL/api/dashboard/users" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
  --data "$(jq -nc --arg e "$WRITER_EMAIL" --arg p "$WRITER_PASSWORD" \
    '{email:$e,displayName:"Smoke Writer",role:"CONTENT_STAFF",permissionProfiles:["WRITER"],initialPassword:$p}')" | jq
```

Repeat with `EDITOR` and `PUBLISHER`, then log in again.

## Author, category, and tags

```bash
export AUTHOR_ID="$(curl -fsS "$BASE_URL/api/dashboard/cms/authors" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
  --data '{"displayName":"Smoke Test Analyst","bio":"Staging-only profile.","designation":"Analyst","active":true}' | jq -r .id)"

export CATEGORY_ID="$(curl -fsS "$BASE_URL/api/dashboard/cms/categories" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
  --data '{"name":"Smoke Test Insights","description":"Staging-only taxonomy.","active":true}' | jq -r .id)"

export TAG_ID="$(curl -fsS "$BASE_URL/api/dashboard/cms/tags" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
  --data '{"name":"Smoke Test Gurgaon","active":true}' | jq -r .id)"

curl -fsS "$BASE_URL/api/dashboard/cms/authors?search=smoke&page=0&size=20" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

## Media upload

Choose a real JPEG under 15 MiB:

```bash
export IMAGE_FILE='/absolute/path/to/cover.jpg'
export IMAGE_SIZE="$(wc -c < "$IMAGE_FILE" | tr -d ' ')"

UPLOAD_JSON="$(curl -fsS "$BASE_URL/api/dashboard/cms/media/uploads" \
  -H "Authorization: Bearer $WRITER_TOKEN" -H 'Content-Type: application/json' \
  --data "$(jq -nc --argjson size "$IMAGE_SIZE" \
    '{mediaType:"IMAGE",filename:"cover.jpg",contentType:"image/jpeg",sizeBytes:$size}')")"
export MEDIA_ASSET_ID="$(jq -r .mediaAssetId <<< "$UPLOAD_JSON")"
export UPLOAD_URL="$(jq -r .uploadUrl <<< "$UPLOAD_JSON")"

curl -fsS -X PUT "$UPLOAD_URL" \
  -H 'Content-Type: image/jpeg' \
  -H 'Cache-Control: public, max-age=31536000, immutable' \
  -H 'If-None-Match: *' \
  --data-binary "@$IMAGE_FILE"

curl -fsS -X POST "$BASE_URL/api/dashboard/cms/media/$MEDIA_ASSET_ID/complete" \
  -H "Authorization: Bearer $WRITER_TOKEN" | jq

curl -fsS "$BASE_URL/api/dashboard/cms/media/$MEDIA_ASSET_ID" \
  -H "Authorization: Bearer $WRITER_TOKEN" | jq
```

Use the exact `requiredHeaders` returned by initiation if they differ. Repeat with `VIDEO`, `video/mp4`, and an MP4 under 250 MiB when video verification is required.

## Draft and metadata

```bash
DRAFT_JSON="$(curl -fsS "$BASE_URL/api/dashboard/cms/content" \
  -H "Authorization: Bearer $WRITER_TOKEN" -H 'Content-Type: application/json' \
  --data "$(jq -nc \
    --argjson author "$AUTHOR_ID" --argjson category "$CATEGORY_ID" \
    --argjson tag "$TAG_ID" --argjson cover "$MEDIA_ASSET_ID" \
    '{contentType:"ARTICLE",title:"Smoke Test Gurgaon Market Guide",excerpt:"Staging acceptance article.",robotsIndex:true,robotsFollow:true,publicAuthorId:$author,categoryId:$category,tagIds:[$tag],coverMediaAssetId:$cover,coverAltText:"Gurgaon skyline"}')")"
export CONTENT_ID="$(jq -r .id <<< "$DRAFT_JSON")"
export CONTENT_SLUG="$(jq -r .slug <<< "$DRAFT_JSON")"
export CONTENT_VERSION="$(jq -r .version <<< "$DRAFT_JSON")"

UPDATE_JSON="$(curl -fsS -X PUT "$BASE_URL/api/dashboard/cms/content/$CONTENT_ID" \
  -H "Authorization: Bearer $WRITER_TOKEN" -H 'Content-Type: application/json' \
  --data "$(jq -nc --argjson v "$CONTENT_VERSION" --arg slug "$CONTENT_SLUG" \
    --argjson author "$AUTHOR_ID" --argjson category "$CATEGORY_ID" \
    --argjson tag "$TAG_ID" --argjson cover "$MEDIA_ASSET_ID" \
    '{version:$v,contentType:"ARTICLE",title:"Smoke Test Gurgaon Market Guide",slug:$slug,excerpt:"Updated staging acceptance article.",seoTitle:null,seoDescription:null,canonicalUrl:null,robotsIndex:true,robotsFollow:true,publicAuthorId:$author,categoryId:$category,tagIds:[$tag],coverMediaAssetId:$cover,coverAltText:"Gurgaon skyline"}')")"
export CONTENT_VERSION="$(jq -r .version <<< "$UPDATE_JSON")"
```

## Rich document

```bash
DOCUMENT_JSON="$(curl -fsS -X PUT "$BASE_URL/api/dashboard/cms/content/$CONTENT_ID/document" \
  -H "Authorization: Bearer $WRITER_TOKEN" -H 'Content-Type: application/json' \
  --data "$(jq -nc --argjson v "$CONTENT_VERSION" --argjson image "$MEDIA_ASSET_ID" \
    '{version:$v,document:{schemaVersion:2,blocks:[
      {type:"HEADING",level:"H2",content:[{type:"TEXT",text:"Market overview",marks:[]}]},
      {type:"PARAGRAPH",content:[{type:"TEXT",text:"This is the reviewed smoke-test body.",marks:[{type:"BOLD"}]}]},
      {type:"BULLET_LIST",items:[{content:[{type:"TEXT",text:"A bounded list item",marks:[]}]}]},
      {type:"BLOCKQUOTE",content:[{type:"TEXT",text:"Exact immutable words",marks:[]}]},
      {type:"IMAGE",mediaAssetId:$image,decorative:false,altText:"Gurgaon skyline",caption:[],layout:"WIDE"},
      {type:"EMBED",provider:"YOUTUBE",externalId:"dQw4w9WgXcQ",caption:[]},
      {type:"DIVIDER"}
    ]}}')")"
export CONTENT_VERSION="$(jq -r .version <<< "$DOCUMENT_JSON")"

curl -fsS "$BASE_URL/api/dashboard/cms/content/$CONTENT_ID/document" \
  -H "Authorization: Bearer $WRITER_TOKEN" | jq
```

## Submit, review, approve, and publish

```bash
SUBMIT_JSON="$(curl -fsS -X POST "$BASE_URL/api/dashboard/cms/content/$CONTENT_ID/workflow/submit" \
  -H "Authorization: Bearer $WRITER_TOKEN" -H 'Content-Type: application/json' \
  --data "$(jq -nc --argjson v "$CONTENT_VERSION" '{version:$v}')")"
export CONTENT_VERSION="$(jq -r .version <<< "$SUBMIT_JSON")"
export REVISION_ID="$(jq -r .currentReviewRevisionId <<< "$SUBMIT_JSON")"

curl -fsS "$BASE_URL/api/dashboard/cms/content/$CONTENT_ID/revisions/$REVISION_ID" \
  -H "Authorization: Bearer $EDITOR_TOKEN" | jq

# Optional request-changes branch:
# curl -fsS -X POST "$BASE_URL/api/dashboard/cms/content/$CONTENT_ID/workflow/request-changes" \
#   -H "Authorization: Bearer $EDITOR_TOKEN" -H 'Content-Type: application/json' \
#   --data "$(jq -nc --argjson v "$CONTENT_VERSION" --arg c 'Please revise.' '{version:$v,comment:$c}')" | jq

APPROVE_JSON="$(curl -fsS -X POST "$BASE_URL/api/dashboard/cms/content/$CONTENT_ID/workflow/approve" \
  -H "Authorization: Bearer $EDITOR_TOKEN" -H 'Content-Type: application/json' \
  --data "$(jq -nc --argjson v "$CONTENT_VERSION" '{version:$v}')")"
export CONTENT_VERSION="$(jq -r .version <<< "$APPROVE_JSON")"

PUBLISH_JSON="$(curl -fsS -X POST "$BASE_URL/api/dashboard/cms/content/$CONTENT_ID/workflow/publish" \
  -H "Authorization: Bearer $PUBLISHER_TOKEN" -H 'Content-Type: application/json' \
  --data "$(jq -nc --argjson v "$CONTENT_VERSION" '{version:$v}')")"
export CONTENT_VERSION="$(jq -r .version <<< "$PUBLISH_JSON")"
```

## Public read, conditional GET, and filters

```bash
curl -fsS -D /tmp/sfs-cms-public-headers.txt \
  "$BASE_URL/api/public/content/$CONTENT_SLUG" | jq
grep -iE '^(etag|cache-control):' /tmp/sfs-cms-public-headers.txt

export PUBLIC_ETAG="$(awk 'BEGIN{IGNORECASE=1} /^etag:/{sub(/\r$/,""); sub(/^etag:[[:space:]]*/,""); print}' /tmp/sfs-cms-public-headers.txt)"
curl -sS -o /dev/null -w '%{http_code}\n' \
  "$BASE_URL/api/public/content/$CONTENT_SLUG" -H "If-None-Match: $PUBLIC_ETAG"

curl -fsS "$BASE_URL/api/public/content?contentType=ARTICLE&page=0&size=20" | jq
curl -fsS "$BASE_URL/api/public/content?category=smoke-test-insights&tag=smoke-test-gurgaon&page=0&size=20" | jq
```

The conditional request should print `304`.

## Unpublish and verify public 404

```bash
UNPUBLISH_JSON="$(curl -fsS -X POST "$BASE_URL/api/dashboard/cms/content/$CONTENT_ID/workflow/unpublish" \
  -H "Authorization: Bearer $PUBLISHER_TOKEN" -H 'Content-Type: application/json' \
  --data "$(jq -nc --argjson v "$CONTENT_VERSION" '{version:$v}')")"
export CONTENT_VERSION="$(jq -r .version <<< "$UNPUBLISH_JSON")"

curl -sS -D - "$BASE_URL/api/public/content/$CONTENT_SLUG" | jq
```

Expect HTTP 404, `Cache-Control: no-store`, and `code: CONTENT_NOT_FOUND`. To republish the same approved revision:

```bash
curl -fsS -X POST "$BASE_URL/api/dashboard/cms/content/$CONTENT_ID/workflow/publish" \
  -H "Authorization: Bearer $PUBLISHER_TOKEN" -H 'Content-Type: application/json' \
  --data "$(jq -nc --argjson v "$CONTENT_VERSION" '{version:$v}')" | jq
```

## Negative checks

```bash
# No token: 401
curl -sS -o /dev/null -w '%{http_code}\n' "$BASE_URL/api/dashboard/cms/content"

# Publisher cannot create: 403
curl -sS -o /dev/null -w '%{http_code}\n' "$BASE_URL/api/dashboard/cms/content" \
  -H "Authorization: Bearer $PUBLISHER_TOKEN" -H 'Content-Type: application/json' \
  --data '{"contentType":"BLOG","title":"Forbidden draft","robotsIndex":true,"robotsFollow":true}'

# Unsafe link: 400 CONTENT_DOCUMENT_INVALID
curl -sS -X PUT "$BASE_URL/api/dashboard/cms/content/$CONTENT_ID/document" \
  -H "Authorization: Bearer $WRITER_TOKEN" -H 'Content-Type: application/json' \
  --data "$(jq -nc --argjson v "$CONTENT_VERSION" '{version:$v,document:{schemaVersion:2,blocks:[{type:"PARAGRAPH",content:[{type:"TEXT",text:"bad",marks:[{type:"LINK",href:"javascript:alert(1)",openInNewTab:false,nofollow:false,sponsored:false}]}]}]}}')" | jq
```
