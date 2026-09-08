package com.brandPitara.sfs.cms.media.service;

import com.brandPitara.sfs.cms.media.config.CmsMediaProperties;
import com.brandPitara.sfs.cms.media.domain.*;
import com.brandPitara.sfs.cms.media.dto.*;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.cms.media.exception.CmsMediaApiException;
import com.brandPitara.sfs.cms.media.security.CmsMediaAccessPolicy;
import com.brandPitara.sfs.cms.media.validation.CmsMediaValidator;
import com.brandPitara.sfs.cms.security.CmsPermissionProfile;
import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.auth.security.DashboardUserDetails;
import com.brandPitara.sfs.dashboard.common.enums.*;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.media.service.*;
import com.brandPitara.sfs.security.identity.DashboardAuthenticationUserSnapshot;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CmsMediaServiceImplTest {
    @Mock CmsMediaPersistenceService persistence;
    @Mock MediaObjectStorageService storage;
    @Mock CmsMediaMetrics metrics;
    @Mock DashboardActionAuditService audit;
    private CmsMediaServiceImpl service;
    private CmsMediaProperties properties;

    @BeforeEach
    void setUp() {
        properties = new CmsMediaProperties();
        properties.setBucket("private-cms-bucket");
        service = new CmsMediaServiceImpl(persistence, new CmsMediaValidator(properties),
                new CmsMediaAccessPolicy(), storage, properties, metrics, audit);
    }

    @Test
    void writerCreatesScopedImageUploadWithServerGeneratedKeyAndSafeContract() {
        CmsMediaAssetEntity asset = asset(51L, CmsMediaType.IMAGE, CmsMediaStatus.PENDING_UPLOAD, 11L);
        when(persistence.create(eq(CmsMediaType.IMAGE), eq("private-cms-bucket"), anyString(),
                eq("cover.jpg"), eq("image/jpeg"), eq(1024L), eq(11L))).thenReturn(asset);
        when(storage.createImmutablePresignedUpload(eq("private-cms-bucket"), anyString(), eq("image/jpeg"), eq(1024L),
                eq(CmsMediaProperties.IMMUTABLE_CACHE_CONTROL)))
                .thenReturn(new PresignedUploadResult("https://signed.example/upload", null, "ignored", 300,
                        Map.of("Content-Type", "image/jpeg", "Content-Length", "1024",
                                "Cache-Control", CmsMediaProperties.IMMUTABLE_CACHE_CONTROL,
                                "If-None-Match", "*")));

        CmsMediaUploadResponse response = service.createUpload(
                new CmsMediaUploadRequest(CmsMediaType.IMAGE, "cover.jpg", "image/jpeg", 1024L), writer(11L));

        assertThat(response.mediaAssetId()).isEqualTo(51L);
        assertThat(response.uploadUrl()).isEqualTo("https://signed.example/upload");
        assertThat(response.requiredHeaders()).containsEntry("Content-Length", "1024");
        assertThat(response.requiredHeaders())
                .containsEntry("Cache-Control", CmsMediaProperties.IMMUTABLE_CACHE_CONTROL)
                .containsEntry("If-None-Match", "*");
        var key = ArgumentCaptor.forClass(String.class);
        verify(persistence).create(eq(CmsMediaType.IMAGE), eq("private-cms-bucket"), key.capture(),
                eq("cover.jpg"), eq("image/jpeg"), eq(1024L), eq(11L));
        assertThat(key.getValue()).matches("cms/images/\\d{4}/\\d{2}/[0-9a-f-]{36}\\.jpg").doesNotContain("cover");
        verify(audit).record(DashboardAuditAction.CMS_MEDIA_UPLOAD_CREATED, ReviewEntityType.CMS_MEDIA_ASSET, 51L, null);
        verify(metrics).uploadRequested(CmsMediaType.IMAGE, "success");
    }

    @Test
    void publisherAndLegacyCannotUploadButAdminCan() {
        var request = new CmsMediaUploadRequest(CmsMediaType.VIDEO, "tour.mp4", "video/mp4", 1024L);
        assertThatThrownBy(() -> service.createUpload(request, publisher(31L))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.createUpload(request, legacy(41L))).isInstanceOf(AccessDeniedException.class);

        CmsMediaAssetEntity asset = asset(52L, CmsMediaType.VIDEO, CmsMediaStatus.PENDING_UPLOAD, 1L);
        when(persistence.create(any(), anyString(), anyString(), anyString(), anyString(), anyLong(), eq(1L))).thenReturn(asset);
        when(storage.createImmutablePresignedUpload(anyString(), anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(new PresignedUploadResult("signed", null, "key", 300, Map.of()));
        assertThat(service.createUpload(request, admin(1L)).mediaAssetId()).isEqualTo(52L);

        CmsMediaAssetEntity editorAsset = asset(53L, CmsMediaType.VIDEO, CmsMediaStatus.PENDING_UPLOAD, 21L);
        when(persistence.create(any(), anyString(), anyString(), anyString(), anyString(), anyLong(), eq(21L))).thenReturn(editorAsset);
        assertThat(service.createUpload(request, editor(21L)).mediaAssetId()).isEqualTo(53L);
    }

    @Test
    void validPngFinalizationVerifiesMetadataSignatureAndBecomesReady() {
        CmsMediaAssetEntity pending = asset(61L, CmsMediaType.IMAGE, CmsMediaStatus.PENDING_UPLOAD, 11L);
        pending.setContentType("image/png"); pending.setDeclaredSizeBytes(24L);
        CmsMediaAssetEntity ready = asset(61L, CmsMediaType.IMAGE, CmsMediaStatus.READY, 11L);
        ready.setContentType("image/png"); ready.setSizeBytes(24L); ready.setWidth(1200); ready.setHeight(630);
        when(persistence.get(61L)).thenReturn(pending, ready);
        when(storage.head("private-cms-bucket", pending.getStorageKey()))
                .thenReturn(new StoredObjectMetadata(24, "image/png", "etag"));
        when(storage.readPrefix("private-cms-bucket", pending.getStorageKey(), properties.getValidationPrefixBytes()))
                .thenReturn(png(1200, 630));

        CmsMediaAssetResponse response = service.complete(61L, writer(11L));

        assertThat(response.status()).isEqualTo("READY");
        assertThat(response.width()).isEqualTo(1200);
        verify(persistence).markReady(eq(61L), eq(24L), eq("etag"), any());
        verify(audit).record(DashboardAuditAction.CMS_MEDIA_READY, ReviewEntityType.CMS_MEDIA_ASSET, 61L, null);
        verify(metrics).uploadCompleted(CmsMediaType.IMAGE, "success");
    }

    @Test
    void mismatchFailsAssetAndDoesNotLeakStorageDetails() {
        CmsMediaAssetEntity pending = asset(62L, CmsMediaType.IMAGE, CmsMediaStatus.PENDING_UPLOAD, 11L);
        pending.setDeclaredSizeBytes(100L);
        when(persistence.get(62L)).thenReturn(pending);
        when(storage.head(anyString(), anyString())).thenReturn(new StoredObjectMetadata(99, "image/jpeg", "etag"));

        assertCode(() -> service.complete(62L, writer(11L)), "CMS_MEDIA_VALIDATION_FAILED");
        verify(persistence).markFailed(62L, "OBJECT_VALIDATION_FAILED");
        verify(storage, never()).readPrefix(anyString(), anyString(), anyInt());
        verify(audit).record(DashboardAuditAction.CMS_MEDIA_FAILED, ReviewEntityType.CMS_MEDIA_ASSET, 62L, null);
        verify(metrics).validationFailed(CmsMediaType.IMAGE);
    }

    @Test
    void missingObjectIsRetryableAndReadyFinalizeIsIdempotent() {
        CmsMediaAssetEntity pending = asset(63L, CmsMediaType.IMAGE, CmsMediaStatus.PENDING_UPLOAD, 11L);
        when(persistence.get(63L)).thenReturn(pending);
        when(storage.head(anyString(), anyString())).thenThrow(S3Exception.builder().statusCode(404).message("missing").build());
        assertCode(() -> service.complete(63L, writer(11L)), "CMS_MEDIA_UPLOAD_INCOMPLETE");
        verify(persistence, never()).markFailed(anyLong(), anyString());

        CmsMediaAssetEntity ready = asset(64L, CmsMediaType.IMAGE, CmsMediaStatus.READY, 11L);
        when(persistence.get(64L)).thenReturn(ready);
        assertThat(service.complete(64L, writer(11L)).status()).isEqualTo("READY");
        verify(storage, never()).head(eq(ready.getStorageBucket()), eq(ready.getStorageKey()));
    }

    @Test
    void anotherWriterCannotFinalizeOwnersUploadWhileAdminCan() {
        CmsMediaAssetEntity pending = asset(65L, CmsMediaType.IMAGE, CmsMediaStatus.PENDING_UPLOAD, 11L);
        when(persistence.get(65L)).thenReturn(pending);
        assertThatThrownBy(() -> service.complete(65L, writer(12L))).isInstanceOf(AccessDeniedException.class);

        when(storage.head(anyString(), anyString())).thenThrow(S3Exception.builder().statusCode(404).build());
        assertCode(() -> service.complete(65L, admin(1L)), "CMS_MEDIA_UPLOAD_INCOMPLETE");
    }

    private CmsMediaAssetEntity asset(Long id, CmsMediaType type, CmsMediaStatus status, Long creatorId) {
        DashboardUserEntity creator = DashboardUserEntity.builder().id(creatorId).name("User").active(true).role(DashboardRole.CONTENT_STAFF).build();
        CmsMediaAssetEntity asset = CmsMediaAssetEntity.builder().id(id).mediaType(type).status(status)
                .storageBucket("private-cms-bucket").storageKey("cms/test/" + id)
                .originalFilename(type == CmsMediaType.IMAGE ? "cover.jpg" : "tour.mp4")
                .contentType(type == CmsMediaType.IMAGE ? "image/jpeg" : "video/mp4")
                .declaredSizeBytes(1024L).createdBy(creator).build();
        asset.setCreatedAt(OffsetDateTime.now()); asset.setUpdatedAt(OffsetDateTime.now());
        return asset;
    }

    private byte[] png(int w, int h) {
        byte[] b = new byte[24]; byte[] s={(byte)0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a};
        System.arraycopy(s,0,b,0,8); System.arraycopy("IHDR".getBytes(),0,b,12,4);
        be32(b,16,w); be32(b,20,h); return b;
    }
    private void be32(byte[] b,int p,int v){b[p]=(byte)(v>>>24);b[p+1]=(byte)(v>>>16);b[p+2]=(byte)(v>>>8);b[p+3]=(byte)v;}

    private Authentication writer(Long id) { return auth(id, DashboardRole.CONTENT_STAFF, CmsPermissionProfile.WRITER.permissions()); }
    private Authentication publisher(Long id) { return auth(id, DashboardRole.CONTENT_STAFF, CmsPermissionProfile.PUBLISHER.permissions()); }
    private Authentication editor(Long id) { return auth(id, DashboardRole.CONTENT_STAFF, CmsPermissionProfile.EDITOR.permissions()); }
    private Authentication legacy(Long id) { return auth(id, DashboardRole.REVIEWER, Set.of()); }
    private Authentication admin(Long id) { return auth(id, DashboardRole.ADMIN, Set.of()); }
    private Authentication auth(Long id, DashboardRole role, Set<DashboardPermission> permissions) {
        DashboardUserDetails d = new DashboardUserDetails(new DashboardAuthenticationUserSnapshot(
                id, "u@example.com", "User", role, true, permissions));
        return new UsernamePasswordAuthenticationToken(d, null, d.getAuthorities());
    }
    private void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable c, String code) {
        assertThatThrownBy(c).isInstanceOf(CmsMediaApiException.class)
                .extracting(e -> ((CmsMediaApiException)e).getCode()).isEqualTo(code);
    }
}
