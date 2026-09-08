package com.brandPitara.sfs.publiccontent.dto;
public record PublicContentCoverResponse(Long mediaAssetId, String altText, String deliveryUrl,
                                         String contentType, Integer width, Integer height) { }
