package com.brandPitara.sfs.brand.util;

import org.springframework.util.StringUtils;

/**
 * Classifies a company.companyType free-text value into architect/designer for the brand
 * detail page's "topArchitects"/"interiorDesigners" sections. company_type has no enum or
 * CHECK constraint (see the Phase 0 backend analysis), so values are messy/overlapping
 * (e.g. "ARCHITECT&DESIGNERS") - the two checks are intentionally independent, not
 * mutually exclusive, so a company matching both patterns is counted in both sections.
 */
public final class BrandCompanyClassifier {

  private BrandCompanyClassifier() {
  }

  public static boolean isArchitect(String companyType) {
    if (!StringUtils.hasText(companyType)) return false;
    return companyType.toUpperCase().contains("ARCHITECT");
  }

  public static boolean isDesigner(String companyType) {
    if (!StringUtils.hasText(companyType)) return false;
    String normalized = companyType.toUpperCase();
    return normalized.contains("DESIGNER") || normalized.contains("INTERIOR");
  }
}
