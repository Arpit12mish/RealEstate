package com.brandPitara.sfs.company.controller.publicapi;

import com.brandPitara.sfs.company.dto.ArchitectDesignerDetailResponse;
import com.brandPitara.sfs.company.dto.ArchitectDesignerListItemResponse;
import com.brandPitara.sfs.company.enums.ArchitectDesignerType;
import com.brandPitara.sfs.company.service.ArchitectDesignerPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/public/architect-designers")
@RequiredArgsConstructor
public class ArchitectDesignerPublicController {

  private static final int MAX_PAGE_SIZE = 20;

  private final ArchitectDesignerPublicService architectDesignerPublicService;

  @GetMapping("/{companyId}")
  public ArchitectDesignerDetailResponse get(@PathVariable Long companyId) {
    return architectDesignerPublicService.getDetail(companyId);
  }

  // Phase 8A-G (GAP-037). type is bound as a plain String and parsed
  // explicitly, not as @RequestParam ArchitectDesignerType - this codebase's
  // GlobalExceptionHandler has no handler for Spring's own enum-binding
  // failure (MethodArgumentTypeMismatchException) or a missing required
  // parameter (MissingServletRequestParameterException), so either would
  // otherwise fall through to a raw 500 (the same root cause as GAP-032).
  // Parsing here instead always produces a clean, safe 400.
  @GetMapping
  public Page<ArchitectDesignerListItemResponse> list(
      @RequestParam(required = false) String type,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    ArchitectDesignerType parsedType = requireType(type);
    Pageable pageable = PageRequest.of(
        page,
        Math.min(size, MAX_PAGE_SIZE),
        Sort.by("priority").ascending().and(Sort.by("id").descending())
    );
    return architectDesignerPublicService.list(parsedType, pageable);
  }

  // Canonical slug + normalized-type lookup for the rich detail contract
  // (GAP-003B). The path constraint mirrors CompanyPublicController's own
  // slug route exactly (requires at least one alphabetic character, so a
  // purely-numeric path can never match this pattern) - same established
  // convention, not a new one.
  @GetMapping("/slug/{slug:[a-z0-9-]*[a-z][a-z0-9-]*}")
  public ArchitectDesignerDetailResponse getBySlug(
      @PathVariable String slug,
      @RequestParam(required = false) String type
  ) {
    ArchitectDesignerType parsedType = requireType(type);
    return architectDesignerPublicService.getDetailBySlug(slug, parsedType);
  }

  private ArchitectDesignerType requireType(String rawType) {
    return ArchitectDesignerType.parse(rawType)
        .orElseThrow(() -> new ResponseStatusException(
            BAD_REQUEST,
            "type is required and must be one of: ARCHITECT, INTERIOR_DESIGNER"
        ));
  }
}
