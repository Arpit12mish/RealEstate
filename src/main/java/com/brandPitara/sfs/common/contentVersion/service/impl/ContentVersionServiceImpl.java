package com.brandPitara.sfs.common.contentVersion.service.impl;

import com.brandPitara.sfs.common.contentVersion.entity.ContentVersionEntity;
import com.brandPitara.sfs.common.contentVersion.repository.ContentVersionRepository;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentVersionServiceImpl implements ContentVersionService {

  private final ContentVersionRepository repo;

  @Override
  @Transactional
  public long bump(String key) {
    ContentVersionEntity row = repo.findById(key)
      .orElse(ContentVersionEntity.builder().key(key).version(0).build());

    row.setVersion(row.getVersion() + 1);
    repo.save(row);
    return row.getVersion();
  }

  @Override
  public Map<String, Long> getAll() {
    return repo.findAll().stream()
      .collect(Collectors.toMap(ContentVersionEntity::getKey, ContentVersionEntity::getVersion));
  }

  @Override
  public long get(String key) {
    return repo.findById(key).map(ContentVersionEntity::getVersion).orElse(0L);
  }
}
