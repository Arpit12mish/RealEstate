package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.dto.PromoBannerResponse;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.entity.PromoBannerEntity;
import com.brandPitara.sfs.repository.CategoryRepository;
import com.brandPitara.sfs.repository.PromoBannerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromoBannerServiceImplTest {

    @Mock private PromoBannerRepository bannerRepository;
    @Mock private CategoryRepository categoryRepository;

    @InjectMocks private PromoBannerServiceImpl service;

    private static final Long CAT_ID = 0L;
    private static final String LOTTIE_URL = "https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/home/hero-animation.json";
    private static final String IMAGE_URL  = "https://cdn.example.com/banner1.jpg";

    private CategoryEntity category() {
        CategoryEntity c = new CategoryEntity();
        c.setId(CAT_ID);
        c.setName("Home");
        c.setSlug("home");
        return c;
    }

    private PromoBannerEntity imageBanner() {
        return PromoBannerEntity.builder()
                .id(1L)
                .category(category())
                .title("Top Projects")
                .imageUrl(IMAGE_URL)
                .mediaType("IMAGE")
                .targetUrl("/projects")
                .priority(1)
                .active(true)
                .slotKey("HERO")
                .build();
    }

    private PromoBannerEntity lottieBanner() {
        return PromoBannerEntity.builder()
                .id(999L)
                .category(category())
                .title("Compare Smarter")
                .subtitle("Compare projects side by side")
                .imageUrl(null)
                .mediaType("LOTTIE_JSON")
                .mediaUrl(LOTTIE_URL)
                .displayDurationMs(12000)
                .targetUrl("/compare-projects/select")
                .priority(2)
                .active(true)
                .slotKey("HERO")
                .build();
    }

    // 1. HERO section includes existing image banners
    @Test
    void heroBannersIncludeImageBanners() {
        when(categoryRepository.findById(CAT_ID)).thenReturn(Optional.of(category()));
        when(bannerRepository.findByCategory_IdAndSlotKeyAndActiveTrueOrderByPriorityAsc(CAT_ID, "HERO"))
                .thenReturn(List.of(imageBanner()));

        List<PromoBannerResponse> result = service.getBannersForCategoryAndSlot(CAT_ID, "HERO", 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getImageUrl()).isEqualTo(IMAGE_URL);
        assertThat(result.get(0).getDisplayDurationMs()).isNull();
    }

    // 2. HERO section includes one LOTTIE_JSON item
    @Test
    void heroBannersIncludeLottieItem() {
        when(categoryRepository.findById(CAT_ID)).thenReturn(Optional.of(category()));
        when(bannerRepository.findByCategory_IdAndSlotKeyAndActiveTrueOrderByPriorityAsc(CAT_ID, "HERO"))
                .thenReturn(List.of(imageBanner(), lottieBanner()));

        List<PromoBannerResponse> result = service.getBannersForCategoryAndSlot(CAT_ID, "HERO", 10);

        assertThat(result).hasSize(2);
        assertThat(result).anyMatch(r -> "LOTTIE_JSON".equals(r.getMediaType()));
        assertThat(result).anyMatch(r ->
                "LOTTIE_JSON".equals(r.getMediaType())
                        && Integer.valueOf(12000).equals(r.getDisplayDurationMs()));
    }

    // 3. Image banners return mediaType = IMAGE
    @Test
    void imageBanner_mediaTypeIsImage() {
        when(categoryRepository.findById(CAT_ID)).thenReturn(Optional.of(category()));
        when(bannerRepository.findByCategory_IdAndSlotKeyAndActiveTrueOrderByPriorityAsc(CAT_ID, "HERO"))
                .thenReturn(List.of(imageBanner()));

        PromoBannerResponse r = service.getBannersForCategoryAndSlot(CAT_ID, "HERO", 10).get(0);

        assertThat(r.getMediaType()).isEqualTo("IMAGE");
    }

    // 4. Image banners: mediaUrl equals imageUrl
    @Test
    void imageBanner_mediaUrlEqualsImageUrl() {
        when(categoryRepository.findById(CAT_ID)).thenReturn(Optional.of(category()));
        when(bannerRepository.findByCategory_IdAndSlotKeyAndActiveTrueOrderByPriorityAsc(CAT_ID, "HERO"))
                .thenReturn(List.of(imageBanner()));

        PromoBannerResponse r = service.getBannersForCategoryAndSlot(CAT_ID, "HERO", 10).get(0);

        assertThat(r.getMediaUrl()).isEqualTo(IMAGE_URL);
    }

    // 5. Lottie item returns correct mediaUrl
    @Test
    void lottieBanner_mediaUrlMatchesCdnUrl() {
        when(categoryRepository.findById(CAT_ID)).thenReturn(Optional.of(category()));
        when(bannerRepository.findByCategory_IdAndSlotKeyAndActiveTrueOrderByPriorityAsc(CAT_ID, "HERO"))
                .thenReturn(List.of(lottieBanner()));

        PromoBannerResponse r = service.getBannersForCategoryAndSlot(CAT_ID, "HERO", 10).get(0);

        assertThat(r.getMediaUrl()).isEqualTo(LOTTIE_URL);
        assertThat(r.getDisplayDurationMs()).isEqualTo(12000);
    }

    // 6. Lottie item has targetUrl = /compare-projects/select
    @Test
    void lottieBanner_targetUrlIsCompareSelection() {
        when(categoryRepository.findById(CAT_ID)).thenReturn(Optional.of(category()));
        when(bannerRepository.findByCategory_IdAndSlotKeyAndActiveTrueOrderByPriorityAsc(CAT_ID, "HERO"))
                .thenReturn(List.of(lottieBanner()));

        PromoBannerResponse r = service.getBannersForCategoryAndSlot(CAT_ID, "HERO", 10).get(0);

        assertThat(r.getTargetUrl()).isEqualTo("/compare-projects/select");
    }

    // 7. Banner with null mediaType defaults to IMAGE
    @Test
    void banner_nullMediaType_defaultsToImage() {
        PromoBannerEntity b = PromoBannerEntity.builder()
                .id(5L)
                .category(category())
                .title("Old Banner")
                .imageUrl(IMAGE_URL)
                .mediaType(null)
                .active(true)
                .slotKey("HERO")
                .build();

        when(categoryRepository.findById(CAT_ID)).thenReturn(Optional.of(category()));
        when(bannerRepository.findByCategory_IdAndSlotKeyAndActiveTrueOrderByPriorityAsc(CAT_ID, "HERO"))
                .thenReturn(List.of(b));

        PromoBannerResponse r = service.getBannersForCategoryAndSlot(CAT_ID, "HERO", 10).get(0);

        assertThat(r.getMediaType()).isEqualTo("IMAGE");
        assertThat(r.getMediaUrl()).isEqualTo(IMAGE_URL);
    }

    // 8. Both image and Lottie banners returned together maintain correct types
    @Test
    void mixedBanners_eachHasCorrectMediaType() {
        when(categoryRepository.findById(CAT_ID)).thenReturn(Optional.of(category()));
        when(bannerRepository.findByCategory_IdAndSlotKeyAndActiveTrueOrderByPriorityAsc(CAT_ID, "HERO"))
                .thenReturn(List.of(imageBanner(), lottieBanner()));

        List<PromoBannerResponse> result = service.getBannersForCategoryAndSlot(CAT_ID, "HERO", 10);

        PromoBannerResponse imageResult  = result.stream().filter(r -> "IMAGE".equals(r.getMediaType())).findFirst().orElseThrow();
        PromoBannerResponse lottieResult = result.stream().filter(r -> "LOTTIE_JSON".equals(r.getMediaType())).findFirst().orElseThrow();

        assertThat(imageResult.getImageUrl()).isEqualTo(IMAGE_URL);
        assertThat(imageResult.getMediaUrl()).isEqualTo(IMAGE_URL);
        assertThat(imageResult.getDisplayDurationMs()).isNull();
        assertThat(lottieResult.getImageUrl()).isNull();
        assertThat(lottieResult.getMediaUrl()).isEqualTo(LOTTIE_URL);
        assertThat(lottieResult.getDisplayDurationMs()).isEqualTo(12000);
    }
}
