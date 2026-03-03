package com.brandPitara.sfs.home.cards.assembler;

import com.brandPitara.sfs.home.cards.dto.FeedCardDto;
import com.brandPitara.sfs.home.entity.HomeSectionItemEntity;
import com.brandPitara.sfs.home.enums.HomeSectionItemType;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class CardAssemblerRegistry {

  private final Map<HomeSectionItemType, CardAssembler> byType;

  public CardAssemblerRegistry(List<CardAssembler> assemblers) {
    this.byType = assemblers.stream()
        .collect(Collectors.toMap(CardAssembler::supports, a -> a));
  }

  public List<FeedCardDto> buildCards(List<HomeSectionItemEntity> rows, String variant) {

    // group by itemType
    Map<HomeSectionItemType, Set<Long>> idsByType = rows.stream()
        .collect(Collectors.groupingBy(
            HomeSectionItemEntity::getItemType,
            Collectors.mapping(HomeSectionItemEntity::getRefId, Collectors.toSet())
        ));

    // prefetch per type (fast)
    Map<HomeSectionItemType, Map<Long, Object>> fetched = new EnumMap<>(HomeSectionItemType.class);
    for (var e : idsByType.entrySet()) {
      CardAssembler asm = byType.get(e.getKey());
      if (asm == null) continue;
      fetched.put(e.getKey(), asm.prefetchByIds(e.getValue()));
    }

    // build cards preserving order of rows
    List<FeedCardDto> out = new ArrayList<>();
    for (HomeSectionItemEntity r : rows) {
      CardAssembler asm = byType.get(r.getItemType());
      if (asm == null) continue;

      Object entity = Optional.ofNullable(fetched.get(r.getItemType()))
          .map(m -> m.get(r.getRefId()))
          .orElse(null);

      if (entity == null) continue;

      FeedCardDto card = asm.toCard(r, entity, variant);
      if (card != null) out.add(card);
    }
    return out;
  }
}