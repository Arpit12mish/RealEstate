package com.brandPitara.sfs.company.controller.publicapi;

import com.brandPitara.sfs.company.dto.ArchitectDesignerDetailResponse;
import com.brandPitara.sfs.company.service.ArchitectDesignerPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/architect-designers")
@RequiredArgsConstructor
public class ArchitectDesignerPublicController {

  private final ArchitectDesignerPublicService architectDesignerPublicService;

  @GetMapping("/{companyId}")
  public ArchitectDesignerDetailResponse get(@PathVariable Long companyId) {
    return architectDesignerPublicService.getDetail(companyId);
  }
}