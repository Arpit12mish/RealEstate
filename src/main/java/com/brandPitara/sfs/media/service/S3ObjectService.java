package com.brandPitara.sfs.media.service;

import com.brandPitara.sfs.media.config.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3ObjectService {
    private final S3Client s3Client;
    private final S3Properties props;

    public void deleteQuietly(String key) {
        if (key == null || key.isBlank()) return;
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key)
                    .build());
        } catch (Exception e) {
            // don’t block user action because S3 delete failed
            log.warn("Failed to delete S3 object key={}", key, e);
        }
    }
}
