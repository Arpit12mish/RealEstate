package com.brandPitara.sfs.media.validator;

import com.brandPitara.sfs.media.config.S3Properties;
import com.brandPitara.sfs.project.enums.FloorPlanVisualMediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * GAP-028 fix: {@code ProjectFloorPlanVisualAnalysisUpsertRequest.mediaUrl}
 * previously had no format/host validation on the write side, so a
 * dashboard operator could save an arbitrary string that the public read
 * endpoint would then return as-is. This validator is called explicitly
 * from {@code ProjectFloorPlanVisualAnalysisServiceImpl.upsert()}, mirroring
 * {@link com.brandPitara.sfs.dashboard.validator.DashboardMediaUploadValidator}'s
 * own established convention (a plain {@code @Service}, thrown
 * {@link IllegalArgumentException} mapped to {@code 400} by the existing
 * {@code DashboardExceptionHandler}/{@code GlobalExceptionHandler} — no new
 * exception type introduced).
 *
 * <p>The allowlist is configuration-driven, reusing {@link S3Properties}
 * exactly the way {@code S3MediaStorageServiceImpl#buildPublicUrl} already
 * derives the canonical public media host — this validator never hardcodes
 * a bucket/region/hostname literal.
 *
 * <p>Placed in {@code com.brandPitara.sfs.media.validator} (a new package,
 * sibling to the existing {@code media.config}/{@code media.service}
 * packages) rather than under {@code dashboard.validator}, since URL/host
 * trust is a general media concern, not a dashboard-only one — a future
 * write path for a different media-bearing field could reuse this same
 * class without an odd cross-package dependency on the dashboard package.
 *
 * <p><b>Phase MEDIA-H1 (2026-08-12):</b> also wired into
 * {@code BuilderServiceImpl.create()}/{@code update()}/{@code updateLogo()}
 * for {@code BuilderEntity.logoUrl} — a legacy, unvalidated Builder logo
 * URL pointing at a third-party host (confirmed live: `architectureideas.info`)
 * took down the public website's Home page (a `next/image` host-allowlist
 * rejection surfaced as an unhandled `500`, entirely a website-side
 * enforcement gap this validator doesn't cause or fix by itself — see the
 * website's own `resolveTrustedImageUrl()` fix). This backend validation
 * is defense-in-depth for the write side: the current dashboard UI already
 * only submits real S3-uploaded URLs for this field (confirmed via source
 * read of `BuilderForm.tsx` — presigned-upload only, no free-text URL
 * entry), so this closes the gap for any direct API write (Admin
 * controller, scripts, future UI regressions) that bypasses that specific
 * form. Does not retroactively touch any already-persisted legacy value —
 * see {@code backend-gaps.md}'s MEDIA-H1 entry for the read-side/legacy-data
 * handling decision.
 */
@Service
@RequiredArgsConstructor
public class TrustedMediaUrlValidator {

  private static final Pattern PRIVATE_IPV4 = Pattern.compile(
      "^(10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"
          + "|172\\.(1[6-9]|2\\d|3[0-1])\\.\\d{1,3}\\.\\d{1,3}"
          + "|192\\.168\\.\\d{1,3}\\.\\d{1,3}"
          + "|169\\.254\\.\\d{1,3}\\.\\d{1,3}"
          + "|127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})$");

  private static final List<String> VIDEO_ONLY_EXTENSIONS = List.of(".mp4", ".mov", ".webm", ".m4v");
  private static final List<String> IMAGE_ONLY_EXTENSIONS = List.of(".jpg", ".jpeg", ".png", ".webp", ".gif");
  private static final List<String> JSON_ONLY_EXTENSIONS = List.of(".json");

  private final S3Properties s3Properties;

  /**
   * Validates a media URL destined for {@code ProjectFloorPlanVisualAnalysisEntity.mediaUrl}.
   * {@code null} is accepted — the field is optional on partial updates —
   * but a non-null value must be a syntactically valid, HTTPS, credential-free,
   * non-local/private-host URL whose host is on the approved media allowlist.
   */
  public void validate(String mediaUrl) {
    if (mediaUrl == null) {
      return;
    }

    URI uri = parse(mediaUrl);
    requireHttps(uri);
    requireNoEmbeddedCredentials(uri);
    String host = requireHost(uri);
    requireNotLocalOrPrivate(host);
    requireApprovedHost(host);
  }

  /**
   * Read-side defence (never throws): used by {@code
   * ProjectFloorPlanInsightServiceImpl#publicGetDetail} to decide whether an
   * already-persisted (possibly legacy, pre-dating this validator)
   * {@code mediaUrl} is safe to return publicly. {@code null} is considered
   * valid (nothing to hide). Deliberately does not call {@code
   * validateMediaTypeCompatibility} — that check exists to catch obvious
   * write-time authoring mistakes, not to retroactively hide already-stored
   * legitimate media over a soft heuristic.
   */
  public boolean isValid(String mediaUrl) {
    if (mediaUrl == null) {
      return true;
    }
    try {
      validate(mediaUrl);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Soft, best-effort media-type compatibility check — deliberately never
   * treats a file extension as proof of the real content type (no remote
   * download/Content-Type sniff is performed, per this validator's own
   * explicit scope). Only rejects an UNAMBIGUOUS mismatch (e.g. a
   * {@code .mp4} URL declared {@code IMAGE}); a missing, unknown, or
   * ambiguous extension always passes, since this domain genuinely lacks
   * enough information to say more than that.
   */
  public void validateMediaTypeCompatibility(FloorPlanVisualMediaType mediaType, String mediaUrl) {
    if (mediaType == null || mediaUrl == null) {
      return;
    }
    String lower = mediaUrl.toLowerCase(Locale.ROOT);

    if (mediaType == FloorPlanVisualMediaType.IMAGE && endsWithAny(lower, VIDEO_ONLY_EXTENSIONS)) {
      throw new IllegalArgumentException("mediaUrl looks like a video file but mediaType is IMAGE");
    }
    if (mediaType == FloorPlanVisualMediaType.IMAGE && endsWithAny(lower, JSON_ONLY_EXTENSIONS)) {
      throw new IllegalArgumentException("mediaUrl looks like a JSON file but mediaType is IMAGE");
    }
    if (mediaType == FloorPlanVisualMediaType.VIDEO && endsWithAny(lower, IMAGE_ONLY_EXTENSIONS)) {
      throw new IllegalArgumentException("mediaUrl looks like an image file but mediaType is VIDEO");
    }
    if (mediaType == FloorPlanVisualMediaType.VIDEO && endsWithAny(lower, JSON_ONLY_EXTENSIONS)) {
      throw new IllegalArgumentException("mediaUrl looks like a JSON file but mediaType is VIDEO");
    }
    if (mediaType == FloorPlanVisualMediaType.LOTTIE_JSON && endsWithAny(lower, VIDEO_ONLY_EXTENSIONS)) {
      throw new IllegalArgumentException("mediaUrl looks like a video file but mediaType is LOTTIE_JSON");
    }
  }

  private boolean endsWithAny(String value, List<String> suffixes) {
    for (String suffix : suffixes) {
      if (value.endsWith(suffix)) {
        return true;
      }
    }
    return false;
  }

  private URI parse(String mediaUrl) {
    try {
      URI uri = new URI(mediaUrl);
      if (uri.getScheme() == null || uri.getHost() == null) {
        throw new IllegalArgumentException("mediaUrl is not a valid absolute URL");
      }
      return uri;
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("mediaUrl is not a valid URL");
    }
  }

  private void requireHttps(URI uri) {
    if (!"https".equalsIgnoreCase(uri.getScheme())) {
      throw new IllegalArgumentException("mediaUrl must use https");
    }
  }

  private void requireNoEmbeddedCredentials(URI uri) {
    if (uri.getRawUserInfo() != null) {
      throw new IllegalArgumentException("mediaUrl must not contain embedded credentials");
    }
  }

  private String requireHost(URI uri) {
    String host = uri.getHost();
    if (!StringUtils.hasText(host)) {
      throw new IllegalArgumentException("mediaUrl is not a valid URL");
    }
    return host.toLowerCase(Locale.ROOT);
  }

  private void requireNotLocalOrPrivate(String host) {
    if (host.equals("localhost")
        || host.equals("127.0.0.1")
        || host.equals("::1")
        || host.startsWith("fc00:")
        || host.startsWith("fd00:")
        || host.startsWith("fe80:")
        || PRIVATE_IPV4.matcher(host).matches()) {
      throw new IllegalArgumentException("mediaUrl host is not permitted");
    }
  }

  private void requireApprovedHost(String host) {
    if (!approvedHosts().contains(host)) {
      throw new IllegalArgumentException("mediaUrl host is not on the approved media host allowlist");
    }
  }

  /**
   * Derived entirely from {@link S3Properties} — never a hardcoded
   * hostname literal, matching this fix's own explicit "configuration-driven,
   * not one developer-machine URL" requirement. Mirrors
   * {@code S3MediaStorageServiceImpl#buildPublicUrl}'s own two branches
   * exactly: a configured {@code publicBaseUrl} (future CDN) takes
   * precedence in that code, but here both are accepted since either could
   * be the host of an already-authored legacy URL.
   */
  private List<String> approvedHosts() {
    List<String> hosts = new ArrayList<>();
    if (StringUtils.hasText(s3Properties.getBucket()) && StringUtils.hasText(s3Properties.getRegion())) {
      hosts.add((s3Properties.getBucket() + ".s3." + s3Properties.getRegion() + ".amazonaws.com").toLowerCase(Locale.ROOT));
    }
    if (StringUtils.hasText(s3Properties.getPublicBaseUrl())) {
      try {
        URI base = new URI(s3Properties.getPublicBaseUrl());
        if (base.getHost() != null) {
          hosts.add(base.getHost().toLowerCase(Locale.ROOT));
        }
      } catch (URISyntaxException ignored) {
        // A malformed publicBaseUrl config value simply contributes no host -
        // not this validator's job to fail application startup over a typo
        // in an unrelated config value.
      }
    }
    return hosts;
  }
}
