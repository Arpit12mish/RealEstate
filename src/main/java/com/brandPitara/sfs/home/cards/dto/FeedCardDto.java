package com.brandPitara.sfs.home.cards.dto;

import com.brandPitara.sfs.home.enums.HomeSectionItemType;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class FeedCardDto {

  private Long id;                 // row id (HomeSectionItemEntity.id)
  private HomeSectionItemType itemType;  // BRAND/BUILDER/BUSINESS/PROJECT/...
  private Long refId;              // entity id

  private String variant;          // controls UI layout (known variants only)

  // Main media
  private String imageUrl;         // main cover image
  private String logoUrl;          // small logo overlay (optional)

  // Text
  private String title;
  private String subtitle;

  // Optional “structured extras” used by many designs
  private String badgeText;        // eg: "Nearest", "1.1 KM"
  private String badgeIcon;        // optional (frontend can ignore)

  private String primaryValue;     // eg: "₹ 9.63 Cr+"
  private String primaryLabel;     // eg: "Starting"
  private String secondaryValue;   // eg: "4.8"
  private String secondaryLabel;   // eg: "Ratings"

  private String ctaText;          // eg: "View", "Connect"
  private CardActionDto action;    // what happens on click

  private List<CardActionDto> quickActions; // eg: Call/WhatsApp (optional)
}