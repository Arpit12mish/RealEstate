package com.brandPitara.sfs.home.cards.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CardActionDto {
  private String type;     // "NAVIGATE" | "DEEPLINK" | "CALL" | "WHATSAPP"
  private String target;   // "/projects/3" or "tel:+91..." or "https://wa.me/..."
}