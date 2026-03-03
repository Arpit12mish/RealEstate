// PromoBannerInjector.java
package com.brandPitara.sfs.home.service.impl;

import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.PromoBannerSlotConfigEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class PromoBannerInjector {

  /**
   * Injects banner sections into the base feed based on slot rules.
   *
   * Anchor matching order:
   * 1) section.key (future-proof: "ABOUT", "REVIEWS", "HERO", "CTA_1"...)
   * 2) section.type.name() (legacy enum: "TOP_PROJECTS", "TOP_BUILDERS"...)
   *
   * Rules:
   * - insertAfterSectionType null/blank => insert at top (index 0)
   * - positionIndex = which occurrence of the anchor to insert after (0 = after first match)
   */
  public List<HomeSectionDto<?>> inject(
      List<HomeSectionDto<?>> baseSections,
      List<PromoBannerSlotConfigEntity> rules,
      BannerSectionFactory bannerFactory
  ) {
    if (baseSections == null) baseSections = List.of();
    if (rules == null || rules.isEmpty()) return baseSections;

    List<HomeSectionDto<?>> out = new ArrayList<>(baseSections);

    int inserted = 0;
    int maxInsert = 2; // ✅ Step 5: hard enforce exactly 2 banners max (HERO + MID)

    Set<String> usedSlots = new HashSet<>(); // ✅ block duplicate slot keys

    for (var rule : rules) {
      if (inserted >= maxInsert) break;

      if (!Boolean.TRUE.equals(rule.getActive())) continue;
      if (!isInTimeWindow(rule)) continue;

      String slotKey = rule.getSlotKey();
      if (slotKey == null || slotKey.isBlank()) continue;

      String slotNorm = normalize(slotKey);
      if (usedSlots.contains(slotNorm)) continue; // ✅ prevent duplicate HERO/MID insertion

      HomeSectionDto<?> bannerSection = bannerFactory.build(rule);
      if (bannerSection == null || bannerSection.getItems() == null || bannerSection.getItems().isEmpty()) continue;

      int idx = findInsertIndex(out, rule.getInsertAfterSectionType(), rule.getPositionIndex());
      out.add(idx, bannerSection);

      usedSlots.add(slotNorm);
      inserted++;
    }

    return out;
  }

  private boolean isInTimeWindow(PromoBannerSlotConfigEntity rule) {
    OffsetDateTime now = OffsetDateTime.now();
    if (rule.getStartAt() != null && now.isBefore(rule.getStartAt())) return false;
    if (rule.getEndAt() != null && now.isAfter(rule.getEndAt())) return false;
    return true;
  }

  private int findInsertIndex(List<HomeSectionDto<?>> sections, String anchorRaw, Integer positionIndex) {
    if (anchorRaw == null || anchorRaw.isBlank()) return 0;

    String anchor = normalize(anchorRaw);

    int targetOccurrence = Math.max(0, positionIndex == null ? 0 : positionIndex);
    int occurrence = -1;

    for (int i = 0; i < sections.size(); i++) {
      if (matchesAnchor(sections.get(i), anchor)) {
        occurrence++;
        if (occurrence == targetOccurrence) {
          return i + 1;
        }
      }
    }

    return sections.size();
  }

  private boolean matchesAnchor(HomeSectionDto<?> section, String anchorNormalized) {
    if (section == null) return false;

    String key = section.getKey();
    if (key != null && !key.isBlank()) {
      if (normalize(key).equals(anchorNormalized)) return true;
    }

    var type = section.getType();
    if (type != null) {
      return normalize(type.name()).equals(anchorNormalized);
    }

    return false;
  }

  private String normalize(String s) {
    return s.trim().toUpperCase(Locale.ROOT);
  }

  @FunctionalInterface
  public interface BannerSectionFactory {
    HomeSectionDto<?> build(PromoBannerSlotConfigEntity rule);
  }
}