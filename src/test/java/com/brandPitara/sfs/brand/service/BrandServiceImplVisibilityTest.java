package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.repository.BrandDistributorRepository;
import com.brandPitara.sfs.brand.repository.BrandMediaRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.brand.service.impl.BrandServiceImpl;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceImplVisibilityTest {

    @Mock private BrandRepository brandRepository;
    @Mock private BrandMediaRepository brandMediaRepository;
    @Mock private BrandDistributorRepository brandDistributorRepository;
    @Mock private ContentVersionService contentVersionService;

    @InjectMocks private BrandServiceImpl brandService;

    private BrandEntity publishedActiveBrand() {
        return BrandEntity.builder()
                .id(1L)
                .name("TestBrand")
                .published(true)
                .active(true)
                .deleted(false)
                .priority(0)
                .promoEnabled(false)
                .build();
    }

    @Test
    void getById_returnsResponse_forPublishedActiveBrand() {
        when(brandRepository.findByIdAndPublishedTrueAndActiveTrueAndDeletedFalse(1L))
                .thenReturn(Optional.of(publishedActiveBrand()));

        var result = brandService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("TestBrand");
    }

    @Test
    void getById_throws_forUnpublishedBrand() {
        when(brandRepository.findByIdAndPublishedTrueAndActiveTrueAndDeletedFalse(2L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.getById(2L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getById_throws_forInactiveBrand() {
        when(brandRepository.findByIdAndPublishedTrueAndActiveTrueAndDeletedFalse(3L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.getById(3L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
