package com.brandPitara.sfs.cms.media.validation;

import com.brandPitara.sfs.cms.media.config.CmsMediaProperties;
import com.brandPitara.sfs.cms.media.domain.CmsMediaType;
import com.brandPitara.sfs.cms.media.exception.CmsMediaApiException;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

class CmsMediaValidatorTest {
    private CmsMediaValidator validator;

    @BeforeEach
    void setUp() { validator = new CmsMediaValidator(new CmsMediaProperties()); }

    @Test
    void declarationAllowsOnlyControlledFormatsAndMatchingSafeFilenames() {
        assertThat(validator.validateDeclaration(CmsMediaType.IMAGE, "cover.JPG", " IMAGE/JPEG ", 100)).isEqualTo("image/jpeg");
        assertThat(validator.validateDeclaration(CmsMediaType.IMAGE, "cover.png", "image/png", 100)).isEqualTo("image/png");
        assertThat(validator.validateDeclaration(CmsMediaType.IMAGE, "cover.webp", "image/webp", 100)).isEqualTo("image/webp");
        assertThat(validator.validateDeclaration(CmsMediaType.VIDEO, "tour.mp4", "video/mp4", 100)).isEqualTo("video/mp4");

        assertCode(() -> validator.validateDeclaration(CmsMediaType.IMAGE, "x.svg", "image/svg+xml", 100), "CMS_MEDIA_INVALID_TYPE");
        assertCode(() -> validator.validateDeclaration(CmsMediaType.IMAGE, "../x.jpg", "image/jpeg", 100), "CMS_MEDIA_INVALID_TYPE");
        assertCode(() -> validator.validateDeclaration(CmsMediaType.IMAGE, "x.png", "image/jpeg", 100), "CMS_MEDIA_INVALID_TYPE");
        assertCode(() -> validator.validateDeclaration(CmsMediaType.VIDEO, "x.webm", "video/webm", 100), "CMS_MEDIA_INVALID_TYPE");
    }

    @Test
    void declarationEnforcesImageAndVideoLimits() {
        assertCode(() -> validator.validateDeclaration(CmsMediaType.IMAGE, "x.jpg", "image/jpeg",
                CmsMediaProperties.DEFAULT_MAX_IMAGE_BYTES + 1), "CMS_MEDIA_TOO_LARGE");
        assertCode(() -> validator.validateDeclaration(CmsMediaType.VIDEO, "x.mp4", "video/mp4",
                CmsMediaProperties.DEFAULT_MAX_VIDEO_BYTES + 1), "CMS_MEDIA_TOO_LARGE");
    }

    @Test
    void extractsPngJpegAndWebpDimensionsFromBoundedHeaders() {
        assertThat(validator.validateObject(CmsMediaType.IMAGE, "image/png", png(1200, 630)))
                .isEqualTo(new CmsMediaValidationResult(1200, 630, null));
        assertThat(validator.validateObject(CmsMediaType.IMAGE, "image/jpeg", jpeg(1920, 1080)))
                .isEqualTo(new CmsMediaValidationResult(1920, 1080, null));
        assertThat(validator.validateObject(CmsMediaType.IMAGE, "image/webp", webp(800, 600)))
                .isEqualTo(new CmsMediaValidationResult(800, 600, null));
    }

    @Test
    void rejectsSpoofedOrPathologicalImages() {
        assertCode(() -> validator.validateObject(CmsMediaType.IMAGE, "image/jpeg", "not jpeg".getBytes()),
                "CMS_MEDIA_VALIDATION_FAILED");
        assertCode(() -> validator.validateObject(CmsMediaType.IMAGE, "image/png", png(30_001, 10)),
                "CMS_MEDIA_VALIDATION_FAILED");
    }

    @Test
    void validatesMp4ContainerSignatureWithoutReadingWholeVideo() {
        byte[] mp4 = new byte[24];
        mp4[3] = 24;
        System.arraycopy("ftyp".getBytes(), 0, mp4, 4, 4);
        System.arraycopy("isom".getBytes(), 0, mp4, 8, 4);
        assertThat(validator.validateObject(CmsMediaType.VIDEO, "video/mp4", mp4))
                .isEqualTo(new CmsMediaValidationResult(null, null, null));
        assertCode(() -> validator.validateObject(CmsMediaType.VIDEO, "video/mp4", new byte[24]),
                "CMS_MEDIA_VALIDATION_FAILED");
        byte[] unknownBrand = mp4.clone();
        System.arraycopy("evil".getBytes(), 0, unknownBrand, 8, 4);
        assertCode(() -> validator.validateObject(CmsMediaType.VIDEO, "video/mp4", unknownBrand),
                "CMS_MEDIA_VALIDATION_FAILED");
    }

    private byte[] png(int width, int height) {
        byte[] b = new byte[24];
        byte[] sig = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        System.arraycopy(sig, 0, b, 0, 8);
        System.arraycopy("IHDR".getBytes(), 0, b, 12, 4);
        be32(b, 16, width); be32(b, 20, height);
        return b;
    }

    private byte[] jpeg(int width, int height) {
        byte[] b = new byte[21];
        b[0] = (byte) 0xff; b[1] = (byte) 0xd8; b[2] = (byte) 0xff; b[3] = (byte) 0xc0;
        b[4] = 0; b[5] = 17; b[6] = 8;
        b[7] = (byte) (height >>> 8); b[8] = (byte) height;
        b[9] = (byte) (width >>> 8); b[10] = (byte) width;
        return b;
    }

    private byte[] webp(int width, int height) {
        byte[] b = new byte[30];
        System.arraycopy("RIFF".getBytes(), 0, b, 0, 4);
        System.arraycopy("WEBP".getBytes(), 0, b, 8, 4);
        System.arraycopy("VP8X".getBytes(), 0, b, 12, 4);
        le24(b, 24, width - 1); le24(b, 27, height - 1);
        return b;
    }

    private void be32(byte[] b, int p, int v) { b[p]=(byte)(v>>>24); b[p+1]=(byte)(v>>>16); b[p+2]=(byte)(v>>>8); b[p+3]=(byte)v; }
    private void le24(byte[] b, int p, int v) { b[p]=(byte)v; b[p+1]=(byte)(v>>>8); b[p+2]=(byte)(v>>>16); }

    private void assertCode(ThrowingCallable callable, String code) {
        assertThatThrownBy(callable).isInstanceOf(CmsMediaApiException.class)
                .extracting(e -> ((CmsMediaApiException) e).getCode()).isEqualTo(code);
    }

    private interface ThrowingCallable extends org.assertj.core.api.ThrowableAssert.ThrowingCallable {}
}
