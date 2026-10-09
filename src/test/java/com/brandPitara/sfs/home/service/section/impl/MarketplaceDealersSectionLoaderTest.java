package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.dto.PageResponse;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.service.section.SectionContext;
import com.brandPitara.sfs.marketplace.dto.DealerCardResponse;
import com.brandPitara.sfs.marketplace.service.DealerPublicService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketplaceDealersSectionLoaderTest {

    private final DealerPublicService dealerPublicService = mock(DealerPublicService.class);
    private final MarketplaceDealersSectionLoader loader = new MarketplaceDealersSectionLoader(dealerPublicService);

    private static PageResponse<DealerCardResponse> page(DealerCardResponse... cards) {
        return PageResponse.<DealerCardResponse>builder().content(List.of(cards)).build();
    }

    @Test
    void globalFeedListsDealersFromAllCitiesWithDefaultCopy() {
        DealerCardResponse card = DealerCardResponse.builder().id(7L).name("Gupta Colour House").build();
        when(dealerPublicService.listDealers(null, null, 0, 10)).thenReturn(page(card));
        HomeSectionConfigEntity cfg = new HomeSectionConfigEntity();
        cfg.setMaxItems(10);

        HomeSectionDto<?> section = loader.load(cfg, SectionContext.builder().cityId(0L).build());

        assertThat(loader.supports()).isEqualTo(HomeSectionType.MARKETPLACE_DEALERS);
        assertThat(section.getType()).isEqualTo(HomeSectionType.MARKETPLACE_DEALERS);
        assertThat(section.getKey()).isEqualTo("MARKETPLACE_DEALERS");
        assertThat(section.getTitle()).isEqualTo("Marketplace");
        assertThat(section.getSubtitle()).isEqualTo("See nearby marketplace");
        List<Object> items = new java.util.ArrayList<>(section.getItems());
        assertThat(items).containsExactly(card);
    }

    @Test
    void cityScopedFeedUsesCityAndConfigCopyAndCapsTheLimit() {
        when(dealerPublicService.listDealers(11L, null, 0, 20)).thenReturn(page());
        HomeSectionConfigEntity cfg = new HomeSectionConfigEntity();
        cfg.setMaxItems(500);
        cfg.setTitle("Shops near you");
        cfg.setSubtitle("Paint and hardware stores");

        HomeSectionDto<?> section = loader.load(cfg, SectionContext.builder().cityId(11L).build());

        verify(dealerPublicService).listDealers(11L, null, 0, 20);
        assertThat(section.getTitle()).isEqualTo("Shops near you");
        assertThat(section.getSubtitle()).isEqualTo("Paint and hardware stores");
        assertThat(section.getItems()).isEmpty();
    }
}
