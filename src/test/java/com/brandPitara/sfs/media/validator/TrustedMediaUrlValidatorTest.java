package com.brandPitara.sfs.media.validator;

import com.brandPitara.sfs.media.config.S3Properties;
import com.brandPitara.sfs.project.enums.FloorPlanVisualMediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

/** GAP-028 write-validation tests. */
class TrustedMediaUrlValidatorTest {

  private static final String APPROVED_IMAGE_URL =
      "https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/dashboard/projects/501/floor-plans/visual-analysis/photo.jpg";
  private static final String APPROVED_VIDEO_URL =
      "https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/dashboard/projects/501/floor-plans/visual-analysis/video.mp4";
  private static final String APPROVED_LOTTIE_URL =
      "https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/dashboard/projects/501/floor-plans/visual-analysis/animation.json";

  private TrustedMediaUrlValidator validator;

  @BeforeEach
  void setUp() {
    S3Properties s3Properties = new S3Properties();
    s3Properties.setBucket("sfs-s3bucket");
    s3Properties.setRegion("ap-south-1");
    validator = new TrustedMediaUrlValidator(s3Properties);
  }

  // 1. Approved HTTPS image URL accepted.
  @Test
  void acceptsApprovedHttpsImageUrl() {
    assertThatNoException().isThrownBy(() -> validator.validate(APPROVED_IMAGE_URL));
  }

  // 2. Approved HTTPS video URL accepted.
  @Test
  void acceptsApprovedHttpsVideoUrl() {
    assertThatNoException().isThrownBy(() -> validator.validate(APPROVED_VIDEO_URL));
  }

  // 3. Approved HTTPS Lottie URL accepted.
  @Test
  void acceptsApprovedHttpsLottieUrl() {
    assertThatNoException().isThrownBy(() -> validator.validate(APPROVED_LOTTIE_URL));
  }

  // 4. Malformed URL rejected.
  @Test
  void rejectsMalformedUrl() {
    assertThatThrownBy(() -> validator.validate("not a url at all ://"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // 5. HTTP rejected in production policy (no local-dev exception exists for media URLs).
  @Test
  void rejectsPlainHttp() {
    assertThatThrownBy(() -> validator.validate("http://sfs-s3bucket.s3.ap-south-1.amazonaws.com/photo.jpg"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("https");
  }

  // 6. javascript: URL rejected.
  @Test
  void rejectsJavascriptScheme() {
    assertThatThrownBy(() -> validator.validate("javascript:alert(1)"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // 7. data: URL rejected.
  @Test
  void rejectsDataScheme() {
    assertThatThrownBy(() -> validator.validate("data:text/plain;base64,SGVsbG8="))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // 8. file: URL rejected.
  @Test
  void rejectsFileScheme() {
    assertThatThrownBy(() -> validator.validate("file:///etc/passwd"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // 8b. ftp: URL rejected (explicit scheme case from GAP-028's own list).
  @Test
  void rejectsFtpScheme() {
    assertThatThrownBy(() -> validator.validate("ftp://sfs-s3bucket.s3.ap-south-1.amazonaws.com/photo.jpg"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // 9. URL with embedded credentials rejected.
  @Test
  void rejectsUrlWithEmbeddedCredentials() {
    assertThatThrownBy(() -> validator.validate("https://user:pass@sfs-s3bucket.s3.ap-south-1.amazonaws.com/photo.jpg"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("credentials");
  }

  // 10. Unapproved host rejected.
  @Test
  void rejectsUnapprovedHost() {
    assertThatThrownBy(() -> validator.validate("https://evil.example.com/photo.jpg"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("allowlist");
  }

  // 11. Localhost rejected under production policy.
  @Test
  void rejectsLocalhost() {
    assertThatThrownBy(() -> validator.validate("https://localhost/photo.jpg"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // 12. Private IPv4 address rejected.
  @Test
  void rejectsPrivateIpv4Address() {
    assertThatThrownBy(() -> validator.validate("https://10.0.0.5/photo.jpg"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> validator.validate("https://192.168.1.5/photo.jpg"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> validator.validate("https://172.16.0.5/photo.jpg"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> validator.validate("https://127.0.0.1/photo.jpg"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // 13. Private IPv6 address rejected where supported.
  @Test
  void rejectsPrivateIpv6AddressWhereSupported() {
    assertThatThrownBy(() -> validator.validate("https://[::1]/photo.jpg"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> validator.validate("https://[fd00::1]/photo.jpg"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // 14. Empty optional media URL behavior — null is accepted (optional field).
  @Test
  void acceptsNullMediaUrlAsOptional() {
    assertThatNoException().isThrownBy(() -> validator.validate(null));
  }

  // 14b. Blank/empty-string URL is rejected as invalid (not treated as "absent").
  @Test
  void rejectsEmptyStringUrl() {
    assertThatThrownBy(() -> validator.validate(""))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // 15. Media type missing — compatibility check is a no-op without a mediaType.
  @Test
  void mediaTypeCompatibilityCheckIsNoOpWhenMediaTypeMissing() {
    assertThatNoException().isThrownBy(() -> validator.validateMediaTypeCompatibility(null, APPROVED_VIDEO_URL));
  }

  // 16. Unsupported media type — not applicable to this validator (FloorPlanVisualMediaType
  // is a closed enum; an "unsupported" value can't be constructed at all, so this
  // domain-level guarantee is structural, not a runtime check to test here).

  // 17. Excessive title / 18. Excessive description / 19. Excessive tag count / tag length -
  // enforced by @Size on ProjectFloorPlanVisualAnalysisUpsertRequest/VisualAnalysisTagUpsertRequest
  // (Bean Validation, exercised via @Valid at the controller boundary) - this validator's own
  // scope is URL/host trust only; see DashboardProjectFloorPlanVisualAnalysisControllerTest-style
  // coverage note in the phase report for why no duplicate test exists here.

  // Media-type compatibility: obvious mismatches rejected.
  @Test
  void rejectsImageMediaTypeWithVideoExtension() {
    assertThatThrownBy(() -> validator.validateMediaTypeCompatibility(FloorPlanVisualMediaType.IMAGE, APPROVED_VIDEO_URL))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsVideoMediaTypeWithImageExtension() {
    assertThatThrownBy(() -> validator.validateMediaTypeCompatibility(FloorPlanVisualMediaType.VIDEO, APPROVED_IMAGE_URL))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsLottieJsonMediaTypeWithVideoExtension() {
    assertThatThrownBy(() -> validator.validateMediaTypeCompatibility(FloorPlanVisualMediaType.LOTTIE_JSON, APPROVED_VIDEO_URL))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // Never trusts an extension as proof — an ambiguous/no-extension URL always passes.
  @Test
  void passesMediaTypeCompatibilityWhenUrlHasNoRecognizedExtension() {
    String noExtensionUrl = "https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/dashboard/projects/501/floor-plans/visual-analysis/media";
    assertThatNoException().isThrownBy(
        () -> validator.validateMediaTypeCompatibility(FloorPlanVisualMediaType.IMAGE, noExtensionUrl));
  }

  // 20. Invalid legacy URL safely omitted during public mapping — covered by
  // ProjectFloorPlanInsightServiceImplTest#publicGetDetailNullsOutAnUntrustedLegacyMediaUrlButKeepsTitleAndTags,
  // via this validator's own isValid() method exercised below.
  @Test
  void isValidNeverThrowsAndReturnsFalseForAnUntrustedUrl() {
    assertThat(validator.isValid(APPROVED_IMAGE_URL)).isTrue();
    assertThat(validator.isValid("https://evil.example.com/photo.jpg")).isFalse();
    assertThat(validator.isValid(null)).isTrue();
  }

  // Raw signed URL/query data not logged — this validator never logs anything
  // itself (no logger dependency at all); confirmed by inspection of this
  // class's own source rather than a runtime test, since there is nothing to
  // assert against an absent logging call.
}
