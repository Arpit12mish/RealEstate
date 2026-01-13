package com.brandPitara.sfs.common.contentVersion.service;

import java.util.Map;

public interface ContentVersionService {
  long bump(String key);
  Map<String, Long> getAll();
  long get(String key);
}
