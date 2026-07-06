package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.dto.BrandCertificateResponse;
import com.brandPitara.sfs.brand.dto.BrandCertificateUpsertRequest;
import com.brandPitara.sfs.brand.entity.BrandCertificateEntity;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.repository.BrandCertificateRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.brand.service.impl.BrandCertificateServiceImpl;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandCertificateServiceImplTest {

  @Mock private BrandRepository brandRepository;
  @Mock private BrandCertificateRepository brandCertificateRepository;
  @Mock private ContentVersionService contentVersionService;

  @InjectMocks private BrandCertificateServiceImpl brandCertificateService;

  private BrandEntity brand() {
    return BrandEntity.builder().id(1L).name("Berger").deleted(false).build();
  }

  @Test
  void create_savesCertificateForBrand() {
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand()));
    when(brandCertificateRepository.save(any(BrandCertificateEntity.class))).thenAnswer(inv -> {
      BrandCertificateEntity e = inv.getArgument(0);
      e.setId(20L);
      return e;
    });

    BrandCertificateUpsertRequest request = BrandCertificateUpsertRequest.builder()
        .title("ISO 9001")
        .issuer("BSI")
        .certificateUrl("https://cdn.example/iso9001.pdf")
        .build();

    BrandCertificateResponse response = brandCertificateService.create(1L, request);

    assertThat(response.getTitle()).isEqualTo("ISO 9001");
    assertThat(response.getIssuer()).isEqualTo("BSI");
    assertThat(response.isActive()).isTrue();
  }

  @Test
  void update_appliesPartialChanges() {
    BrandCertificateEntity existing = BrandCertificateEntity.builder()
        .id(20L)
        .brand(brand())
        .title("ISO 9001")
        .issuer("BSI")
        .active(true)
        .deleted(false)
        .build();

    when(brandCertificateRepository.findByIdAndBrand_IdAndDeletedFalse(20L, 1L)).thenReturn(Optional.of(existing));
    when(brandCertificateRepository.save(any(BrandCertificateEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    BrandCertificateUpsertRequest request = BrandCertificateUpsertRequest.builder()
        .title("ISO 9001:2015")
        .build();

    BrandCertificateResponse response = brandCertificateService.update(1L, 20L, request);

    assertThat(response.getTitle()).isEqualTo("ISO 9001:2015");
    assertThat(response.getIssuer()).isEqualTo("BSI"); // untouched field preserved
  }

  @Test
  void softDelete_marksDeletedAndInactive() {
    BrandCertificateEntity existing = BrandCertificateEntity.builder()
        .id(20L)
        .brand(brand())
        .title("ISO 9001")
        .active(true)
        .deleted(false)
        .build();

    when(brandCertificateRepository.findByIdAndBrand_IdAndDeletedFalse(20L, 1L)).thenReturn(Optional.of(existing));
    when(brandCertificateRepository.save(any(BrandCertificateEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    brandCertificateService.softDelete(1L, 20L);

    ArgumentCaptor<BrandCertificateEntity> captor = ArgumentCaptor.forClass(BrandCertificateEntity.class);
    verify(brandCertificateRepository).save(captor.capture());
    assertThat(captor.getValue().getDeleted()).isTrue();
    assertThat(captor.getValue().getActive()).isFalse();
  }

  @Test
  void update_throwsNotFound_whenCertificateMissing() {
    when(brandCertificateRepository.findByIdAndBrand_IdAndDeletedFalse(99L, 1L)).thenReturn(Optional.empty());

    BrandCertificateUpsertRequest request = BrandCertificateUpsertRequest.builder().title("x").build();

    assertThatThrownBy(() -> brandCertificateService.update(1L, 99L, request))
        .isInstanceOf(EntityNotFoundException.class);
  }
}
