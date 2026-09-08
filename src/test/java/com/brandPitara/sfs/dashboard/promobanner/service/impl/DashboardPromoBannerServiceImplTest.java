package com.brandPitara.sfs.dashboard.promobanner.service.impl;

import com.brandPitara.sfs.dashboard.promobanner.dto.DashboardPromoBannerResponse;
import com.brandPitara.sfs.dashboard.promobanner.dto.DashboardPromoBannerUpsertRequest;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.entity.PromoBannerEntity;
import com.brandPitara.sfs.enums.PromoBannerMediaType;
import com.brandPitara.sfs.repository.CategoryRepository;
import com.brandPitara.sfs.repository.PromoBannerRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DashboardPromoBannerServiceImplTest {

    private static final String MP4_URL =
            "https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/home/promo-banners/99/hero-animation.mp4";

    @Test
    void updateHeroFromLottieToVideoPersistsPermanentMp4Url() {
        PromoBannerRepository repository = mock(PromoBannerRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        DashboardPromoBannerServiceImpl service = new DashboardPromoBannerServiceImpl(repository, categoryRepository);
        CategoryEntity category = category();
        PromoBannerEntity entity = PromoBannerEntity.builder()
                .id(99L)
                .category(category)
                .slotKey("HERO")
                .title("Compare Smarter")
                .mediaType(PromoBannerMediaType.LOTTIE_JSON)
                .mediaUrl("https://cdn.example.com/hero.json")
                .priority(2)
                .active(true)
                .deleted(false)
                .build();

        when(repository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.of(entity));
        when(categoryRepository.findById(0L)).thenReturn(Optional.of(category));
        when(repository.save(any(PromoBannerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DashboardPromoBannerResponse response = service.update(99L, videoRequest(MP4_URL));

        assertThat(response.getMediaType()).isEqualTo(PromoBannerMediaType.VIDEO);
        assertThat(response.getMediaUrl()).isEqualTo(MP4_URL);
        assertThat(entity.getMediaType()).isEqualTo(PromoBannerMediaType.VIDEO);
        assertThat(entity.getMediaUrl()).isEqualTo(MP4_URL);
        assertThat(entity.getImageUrl()).isNull();
        assertThat(entity.getSlotKey()).isEqualTo("HERO");
    }

    @Test
    void videoAllowsQueryStringWhenPathEndsInMp4() {
        PromoBannerRepository repository = mock(PromoBannerRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        DashboardPromoBannerServiceImpl service = new DashboardPromoBannerServiceImpl(repository, categoryRepository);
        PromoBannerEntity entity = existingVideoBanner();

        when(repository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.of(entity));
        when(categoryRepository.findById(0L)).thenReturn(Optional.of(category()));
        when(repository.save(any(PromoBannerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String url = MP4_URL + "?version=2";
        assertThat(service.update(99L, videoRequest(url)).getMediaUrl()).isEqualTo(url);
    }

    @Test
    void videoRejectsWrongExtensionAndPresignedUploadUrl() {
        DashboardPromoBannerServiceImpl service = new DashboardPromoBannerServiceImpl(
                mock(PromoBannerRepository.class), mock(CategoryRepository.class));

        assertThatThrownBy(() -> service.create(videoRequest("https://cdn.example.com/hero.webm")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(".mp4");
        assertThatThrownBy(() -> service.create(videoRequest(MP4_URL + "?X-Amz-Signature=temporary")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("permanent public URL");
    }

    @Test
    void softDeleteAlsoDeactivatesBanner() {
        PromoBannerRepository repository = mock(PromoBannerRepository.class);
        DashboardPromoBannerServiceImpl service = new DashboardPromoBannerServiceImpl(
                repository, mock(CategoryRepository.class));
        PromoBannerEntity entity = existingVideoBanner();
        when(repository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.of(entity));

        service.softDelete(99L);

        assertThat(entity.getDeleted()).isTrue();
        assertThat(entity.getActive()).isFalse();
        verify(repository).save(entity);
    }

    private DashboardPromoBannerUpsertRequest videoRequest(String url) {
        DashboardPromoBannerUpsertRequest request = new DashboardPromoBannerUpsertRequest();
        request.setCategoryId(0L);
        request.setSlotKey("hero");
        request.setTitle("Compare Smarter");
        request.setSubtitle("Compare projects side by side");
        request.setMediaType(PromoBannerMediaType.VIDEO);
        request.setMediaUrl(url);
        request.setTargetUrl("/compare-projects/select");
        request.setPriority(2);
        request.setActive(true);
        request.setDisplayDurationMs(12000);
        return request;
    }

    private PromoBannerEntity existingVideoBanner() {
        return PromoBannerEntity.builder()
                .id(99L)
                .category(category())
                .slotKey("HERO")
                .title("Compare Smarter")
                .mediaType(PromoBannerMediaType.VIDEO)
                .mediaUrl(MP4_URL)
                .priority(2)
                .active(true)
                .deleted(false)
                .build();
    }

    private CategoryEntity category() {
        CategoryEntity category = new CategoryEntity();
        category.setId(0L);
        category.setName("Home");
        category.setSlug("home");
        return category;
    }
}
