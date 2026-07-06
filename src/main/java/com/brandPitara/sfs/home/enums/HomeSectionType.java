// com.brandPitara.sfs.home.dto.HomeSectionType.java
package com.brandPitara.sfs.home.enums;

public enum HomeSectionType {
  PROMO_BANNERS,
  TOP_PROJECTS,
  TOP_BUILDERS,

  // @Deprecated - superseded by CONNECTED_BRANDS (Phase 1D.6). Kept only so any
  // lingering home_section_config row or client still referencing the old value
  // deserializes safely; no HomeSectionLoader supports it anymore.
  TOP_BRANDS,

  // The home brand showcase, going forward: brands connected/collaborated with
  // projects, builders, architects, and designers - not a generic "top brands" list.
  CONNECTED_BRANDS,

  TOP_CATEGORIES,
  ARCHITECTS_AND_DESIGNERS,
  ARCHITECTS,
  DESIGNERS,
  TOP_DISTRIBUTORS,
  PROJECT_PLAN,
  PROJECT_ANALYTICS,
  NEARBY_LISTINGS,
  INSTAGRAM_REELS,
  TRENDING_CITIES,
  SMART_CALCULATORS,
  COMPANIES,

  // ✅ NEW (this is your BrandCarousel UI)
  FEATURED_CAROUSEL,
  GENERIC_CARDS,

  COMPARE_PROPERTIES,

  //Builder screen
  BUILDER_HERO,
  BUILDER_STATS,
  BUILDER_STORY,
  BUILDER_CREDIBILITY,
  BUILDER_CREDIBILITY_CARDS

}
