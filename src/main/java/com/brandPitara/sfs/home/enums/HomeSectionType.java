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

  // Marketplace dealer cards (home "Marketplace" carousel); opens dealer details by id.
  MARKETPLACE_DEALERS,

  // Title/subtitle + tab list for the home "Festive picks" block under the hero. Card data for
  // each tab comes from the other sections / public list APIs; this only names the block and
  // lets ops switch it off (disable the home_section_config row) after the festive period.
  FESTIVE_PICKS,

  //Builder screen
  BUILDER_HERO,
  BUILDER_STATS,
  BUILDER_STORY,
  BUILDER_CREDIBILITY,
  BUILDER_CREDIBILITY_CARDS

}
