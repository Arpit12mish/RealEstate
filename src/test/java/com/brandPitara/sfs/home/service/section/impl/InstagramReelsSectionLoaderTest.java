package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.instagram.dto.PublicInstagramReelItemResponse;
import com.brandPitara.sfs.instagram.service.InstagramReelService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InstagramReelsSectionLoaderTest {

    @Test
    void loadBuildsInstagramReelsSection() {
        InstagramReelService service = mock(InstagramReelService.class);
        InstagramReelsSectionLoader loader = new InstagramReelsSectionLoader(service);
        HomeSectionConfigEntity cfg = HomeSectionConfigEntity.builder()
            .title("Watch Reels")
            .subtitle("Latest from Instagram")
            .maxItems(8)
            .build();
        PublicInstagramReelItemResponse item = PublicInstagramReelItemResponse.builder()
            .id(1L)
            .instagramUrl("https://www.instagram.com/reel/ABC123/")
            .thumbnailUrl("https://cdn.example.com/thumb.webp")
            .build();
        when(service.publicHomeItems(8)).thenReturn(List.of(item));

        var section = loader.load(cfg, null);

        assertThat(section.getType()).isEqualTo(HomeSectionType.INSTAGRAM_REELS);
        assertThat(section.getKey()).isEqualTo("INSTAGRAM_REELS");
        assertThat(section.getTitle()).isEqualTo("Watch Reels");
        assertThat(section.getSubtitle()).isEqualTo("Latest from Instagram");
        assertThat(section.getItems()).hasSize(1);
        verify(service).publicHomeItems(8);
    }

    @Test
    void loadUsesDefaultCopy() {
        InstagramReelService service = mock(InstagramReelService.class);
        InstagramReelsSectionLoader loader = new InstagramReelsSectionLoader(service);
        HomeSectionConfigEntity cfg = HomeSectionConfigEntity.builder()
            .title(" ")
            .subtitle(null)
            .maxItems(null)
            .build();
        when(service.publicHomeItems(10)).thenReturn(List.of());

        var section = loader.load(cfg, null);

        assertThat(section.getTitle()).isEqualTo("Instagram Reels");
        assertThat(section.getSubtitle()).isEqualTo("See our latest reels on instagram");
        assertThat(section.getItems()).isEmpty();
        verify(service).publicHomeItems(10);
    }
}
