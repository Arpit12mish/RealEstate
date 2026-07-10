package com.brandPitara.sfs.dashboard.company.service.impl;

import com.brandPitara.sfs.company.entity.CompanyCertificateEntity;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.repository.CompanyCertificateRepository;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.dashboard.company.dto.CompanyCertificateCreateRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyCertificateResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyCertificateUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardCompanyCertificateServiceImplTest {

  @Mock private CompanyRepository companyRepository;
  @Mock private CompanyCertificateRepository companyCertificateRepository;

  private DashboardCompanyCertificateServiceImpl service() {
    return new DashboardCompanyCertificateServiceImpl(companyRepository, companyCertificateRepository);
  }

  private CompanyEntity company() {
    return CompanyEntity.builder().id(7L).name("Morphogenesis").deleted(false).build();
  }

  private CompanyCertificateEntity certificate(Long id) {
    return CompanyCertificateEntity.builder()
        .id(id).company(company())
        .title("ISO Certificate").issuer("ISO")
        .displayOrder(0).verified(false).publicVisible(true).active(true).deleted(false)
        .build();
  }

  @Test
  void create_addsCertificate() {
    when(companyRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(company()));
    when(companyCertificateRepository.save(any(CompanyCertificateEntity.class))).thenAnswer(inv -> {
      CompanyCertificateEntity e = inv.getArgument(0);
      e.setId(50L);
      return e;
    });

    CompanyCertificateCreateRequest request = CompanyCertificateCreateRequest.builder()
        .title("Design Council")
        .issuer("Design Council UK")
        .year(2020)
        .verified(true)
        .build();

    CompanyCertificateResponse response = service().create(7L, request);

    assertThat(response.getId()).isEqualTo(50L);
    assertThat(response.getTitle()).isEqualTo("Design Council");
    assertThat(response.getYear()).isEqualTo(2020);
    assertThat(response.isVerified()).isTrue();
    assertThat(response.isPublicVisible()).isTrue();
  }

  @Test
  void create_rejectsMissingCompany() {
    when(companyRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

    CompanyCertificateCreateRequest request = CompanyCertificateCreateRequest.builder().title("x").build();

    assertThatThrownBy(() -> service().create(999L, request)).isInstanceOf(EntityNotFoundException.class);
    verify(companyCertificateRepository, never()).save(any());
  }

  @Test
  void update_changesFields() {
    CompanyCertificateEntity existing = certificate(50L);
    when(companyCertificateRepository.findByIdAndCompany_IdAndDeletedFalse(50L, 7L)).thenReturn(Optional.of(existing));
    when(companyCertificateRepository.save(any(CompanyCertificateEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    CompanyCertificateUpdateRequest request = CompanyCertificateUpdateRequest.builder()
        .description("Certified quality management")
        .verified(true)
        .build();

    CompanyCertificateResponse response = service().update(7L, 50L, request);

    assertThat(response.getDescription()).isEqualTo("Certified quality management");
    assertThat(response.isVerified()).isTrue();
  }

  @Test
  void delete_softDeletesRow() {
    CompanyCertificateEntity existing = certificate(50L);
    when(companyCertificateRepository.findByIdAndCompany_IdAndDeletedFalse(50L, 7L)).thenReturn(Optional.of(existing));

    service().delete(7L, 50L);

    ArgumentCaptor<CompanyCertificateEntity> captor = ArgumentCaptor.forClass(CompanyCertificateEntity.class);
    verify(companyCertificateRepository).save(captor.capture());
    assertThat(captor.getValue().getDeleted()).isTrue();
    assertThat(captor.getValue().getActive()).isFalse();
    assertThat(captor.getValue().getPublicVisible()).isFalse();
  }

  @Test
  void list_returnsCertificatesForCompany() {
    when(companyRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(company()));
    when(companyCertificateRepository.findByCompany_IdAndDeletedFalseOrderByDisplayOrderAscIdAsc(7L))
        .thenReturn(List.of(certificate(1L)));

    List<CompanyCertificateResponse> result = service().list(7L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTitle()).isEqualTo("ISO Certificate");
  }
}
