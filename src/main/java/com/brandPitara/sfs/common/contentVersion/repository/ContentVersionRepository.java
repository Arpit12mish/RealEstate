package com.brandPitara.sfs.common.contentVersion.repository;

import com.brandPitara.sfs.common.contentVersion.entity.ContentVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentVersionRepository extends JpaRepository<ContentVersionEntity, String> {}
