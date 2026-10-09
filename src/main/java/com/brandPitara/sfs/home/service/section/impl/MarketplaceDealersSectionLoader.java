package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.HomeSectionReadTransaction;
import com.brandPitara.sfs.home.service.section.SectionContext;
import com.brandPitara.sfs.marketplace.dto.DealerCardResponse;
import com.brandPitara.sfs.marketplace.service.DealerPublicService;
import com.brandPitara.sfs.marketplace.service.MarketplacePaging;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Home "Marketplace" carousel: the same public dealer cards as the dealer listing (only active
 * dealers, never worker listings), scoped to the request's city when one is known and otherwise
 * across all cities. Ordering comes from the dealer query (sponsored, rating, id).
 */
@Component
@RequiredArgsConstructor
@HomeSectionReadTransaction
public class MarketplaceDealersSectionLoader implements HomeSectionLoader {

    static final String KEY = "MARKETPLACE_DEALERS";
    private static final String DEFAULT_TITLE = "Marketplace";
    private static final String DEFAULT_SUBTITLE = "See nearby marketplace";

    private final DealerPublicService dealerPublicService;

    @Override
    public HomeSectionType supports() {
        return HomeSectionType.MARKETPLACE_DEALERS;
    }

    @Override
    public HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx) {
        int limit = Math.min(
                Math.max(1, cfg.getMaxItems() != null ? cfg.getMaxItems() : 10),
                MarketplacePaging.MAX_PAGE_SIZE);
        Long cityId = ctx.cityId() != null && ctx.cityId() > 0 ? ctx.cityId() : null;
        List<DealerCardResponse> items = dealerPublicService.listDealers(cityId, null, 0, limit).getContent();

        return HomeSectionDto.<DealerCardResponse>builder()
                .type(HomeSectionType.MARKETPLACE_DEALERS)
                .key(KEY)
                .title(StringUtils.hasText(cfg.getTitle()) ? cfg.getTitle() : DEFAULT_TITLE)
                .subtitle(StringUtils.hasText(cfg.getSubtitle()) ? cfg.getSubtitle() : DEFAULT_SUBTITLE)
                .items(items)
                .build();
    }
}
