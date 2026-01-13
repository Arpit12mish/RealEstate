package com.brandPitara.sfs.builder.service;

import com.brandPitara.sfs.builder.dto.BuilderCardResponse;
import com.brandPitara.sfs.builder.dto.BuilderResponse;
import com.brandPitara.sfs.builder.dto.BuilderUpsertRequest;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BuilderService {

  // Admin
  BuilderResponse create(BuilderUpsertRequest request);

  BuilderResponse update(Long id, BuilderUpsertRequest request);

  BuilderResponse setPublished(Long id, boolean published);

  void softDelete(Long id);

  BuilderResponse getById(Long id);

  Page<BuilderResponse> adminList(Boolean published, Boolean active, Pageable pageable);

  // Public
  Page<BuilderResponse> listPublished(Pageable pageable);

  BuilderResponse publicGetById(Long id);

  List<BuilderCardResponse> publicHomeBuilders(Long cityId, int limit);

}
