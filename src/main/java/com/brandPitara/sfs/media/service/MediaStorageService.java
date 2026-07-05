package com.brandPitara.sfs.media.service;

/**
 * App-owned port for presigned media uploads. Business services depend on
 * this, not on any AWS SDK type directly - {@link com.brandPitara.sfs.media.service.impl.S3MediaStorageServiceImpl}
 * is the only class allowed to touch {@code S3Presigner}.
 */
public interface MediaStorageService {

    PresignedUploadResult createPresignedUpload(PresignedUploadRequest request);
}
