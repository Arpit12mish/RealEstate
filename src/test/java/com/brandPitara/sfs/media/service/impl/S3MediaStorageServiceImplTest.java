package com.brandPitara.sfs.media.service.impl;

import com.brandPitara.sfs.media.config.S3Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class S3MediaStorageServiceImplTest {
    @Test
    void cmsPutPresignIsScopedToExactPrivateBucketKeyTypeAndLength() throws Exception {
        S3Presigner presigner = mock(S3Presigner.class);
        S3Properties properties = new S3Properties();
        properties.setBucket("legacy-bucket"); properties.setRegion("ap-south-1"); properties.setPresignExpirySeconds(300);
        PresignedPutObjectRequest signed = mock(PresignedPutObjectRequest.class);
        when(signed.url()).thenReturn(new URL("https://signed.example/upload"));
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(signed);
        var service = new S3MediaStorageServiceImpl(presigner, mock(S3Client.class), properties);

        var result = service.createPresignedUpload("private-cms", "cms/images/2026/08/id.jpg", "image/jpeg", 4096);

        var captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(presigner).presignPutObject(captor.capture());
        var object = captor.getValue().putObjectRequest();
        assertThat(object.bucket()).isEqualTo("private-cms");
        assertThat(object.key()).isEqualTo("cms/images/2026/08/id.jpg");
        assertThat(object.contentType()).isEqualTo("image/jpeg");
        assertThat(object.contentLength()).isEqualTo(4096);
        assertThat(result.publicUrl()).isNull();
        assertThat(result.requiredHeaders()).containsEntry("Content-Length", "4096");
    }

    @Test
    void immutableCmsPutBindsCachePolicyAndCannotOverwriteExistingKey() throws Exception {
        S3Presigner presigner = mock(S3Presigner.class);
        S3Properties properties = new S3Properties();
        properties.setPresignExpirySeconds(300);
        PresignedPutObjectRequest signed = mock(PresignedPutObjectRequest.class);
        when(signed.url()).thenReturn(new URL("https://signed.example/upload"));
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(signed);
        var service = new S3MediaStorageServiceImpl(presigner, mock(S3Client.class), properties);

        var result = service.createImmutablePresignedUpload(
                "private-cms", "cms/videos/2026/08/id.mp4", "video/mp4", 8192,
                "public, max-age=31536000, immutable"
        );

        var captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(presigner).presignPutObject(captor.capture());
        var object = captor.getValue().putObjectRequest();
        assertThat(object.cacheControl()).isEqualTo("public, max-age=31536000, immutable");
        assertThat(object.overrideConfiguration().flatMap(configuration ->
                configuration.headers().getOrDefault("If-None-Match", java.util.List.of()).stream().findFirst()))
                .contains("*");
        assertThat(result.requiredHeaders())
                .containsEntry("Cache-Control", "public, max-age=31536000, immutable")
                .containsEntry("If-None-Match", "*");
    }

    @Test
    void previewPresignIsScopedToExactObjectAndShortExpiry() throws Exception {
        S3Presigner presigner = mock(S3Presigner.class);
        S3Properties properties = new S3Properties(); properties.setPresignExpirySeconds(300);
        PresignedGetObjectRequest signed = mock(PresignedGetObjectRequest.class);
        when(signed.url()).thenReturn(new URL("https://signed.example/read"));
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(signed);
        var service = new S3MediaStorageServiceImpl(presigner, mock(S3Client.class), properties);

        var result = service.createPresignedRead("private-cms", "cms/images/x.jpg");

        var captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(presigner).presignGetObject(captor.capture());
        assertThat(captor.getValue().getObjectRequest().bucket()).isEqualTo("private-cms");
        assertThat(captor.getValue().getObjectRequest().key()).isEqualTo("cms/images/x.jpg");
        assertThat(result.expiresInSeconds()).isEqualTo(300);
    }
}
