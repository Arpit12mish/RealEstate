package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.dto.BrandMediaResponse;
import com.brandPitara.sfs.brand.dto.BrandMediaUpsertRequest;
import com.brandPitara.sfs.brand.entity.BrandMediaEntity.Placement;

import java.util.List;

public interface BrandMediaService {
  BrandMediaResponse addMedia(Long brandId, BrandMediaUpsertRequest request);
  List<BrandMediaResponse> listMedia(Long brandId, Placement placement);
  void softDeleteMedia(Long brandId, Long mediaId);
}
