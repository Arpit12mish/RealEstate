package com.brandPitara.sfs.common.contentVersion.controller;

import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class ContentVersionController {

  private final ContentVersionService contentVersionService;

  @GetMapping("/version")
  public Map<String, Long> versions() {
    return contentVersionService.getAll();
  }
}
