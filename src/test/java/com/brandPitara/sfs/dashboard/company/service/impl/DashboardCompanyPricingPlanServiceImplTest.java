package com.brandPitara.sfs.dashboard.company.service.impl;

import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.entity.CompanyPricingPlanEntity;
import com.brandPitara.sfs.company.repository.CompanyPricingPlanRepository;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.dashboard.company.dto.CompanyPricingPlanCreateRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyPricingPlanResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyPricingPlanUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardCompanyPricingPlanServiceImplTest {

  @Mock private CompanyRepository companyRepository;
  @Mock private CompanyPricingPlanRepository companyPricingPlanRepository;

  private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
  private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

  private DashboardCompanyPricingPlanServiceImpl service() {
    return new DashboardCompanyPricingPlanServiceImpl(companyRepository, companyPricingPlanRepository);
  }

  private CompanyEntity company() {
    return CompanyEntity.builder().id(7L).name("Morphogenesis").deleted(false).build();
  }

  private CompanyPricingPlanEntity plan(Long id) {
    return CompanyPricingPlanEntity.builder()
        .id(id).company(company())
        .pricingType("SUBSCRIPTION").planName("Base")
        .priceAmount(new BigDecimal("49000"))
        .currency("INR").billingUnit("month")
        .features(List.of())
        .sortOrder(0).publicVisible(true).active(true).deleted(false)
        .build();
  }

  @Test
  void create_addsSubscriptionPlanWithFeatures() {
    when(companyRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(company()));
    when(companyPricingPlanRepository.save(any(CompanyPricingPlanEntity.class))).thenAnswer(inv -> {
      CompanyPricingPlanEntity e = inv.getArgument(0);
      e.setId(50L);
      return e;
    });

    CompanyPricingPlanCreateRequest request = CompanyPricingPlanCreateRequest.builder()
        .pricingType("subscription")
        .planName("Base")
        .priceAmount(new BigDecimal("49000"))
        .billingUnit("month")
        .features(List.of("3D Design", "Site Visits"))
        .build();

    CompanyPricingPlanResponse response = service().create(7L, request);

    assertThat(response.getId()).isEqualTo(50L);
    assertThat(response.getPricingType()).isEqualTo("SUBSCRIPTION");
    assertThat(response.getPlanName()).isEqualTo("Base");
    assertThat(response.getCurrency()).isEqualTo("INR");
    assertThat(response.getFeatures()).containsExactly("3D Design", "Site Visits");
  }

  @Test
  void create_persistsFeaturesAsRealListOnEntity_notAJsonString() {
    when(companyRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(company()));
    when(companyPricingPlanRepository.save(any(CompanyPricingPlanEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    CompanyPricingPlanCreateRequest request = CompanyPricingPlanCreateRequest.builder()
        .pricingType("SUBSCRIPTION")
        .planName("Base")
        .features(List.of("Consultation", "Design Suggestions", "Basic layout planning"))
        .build();

    service().create(7L, request);

    ArgumentCaptor<CompanyPricingPlanEntity> captor = ArgumentCaptor.forClass(CompanyPricingPlanEntity.class);
    verify(companyPricingPlanRepository).save(captor.capture());
    // Guards against the jsonb-vs-varchar regression: this must be a real
    // List<String> Hibernate can bind straight to the jsonb column, not a
    // serialized JSON string (which is what caused the production 500).
    assertThat(captor.getValue().getFeatures())
        .containsExactly("Consultation", "Design Suggestions", "Basic layout planning");
  }

  @Test
  void create_rejectsNegativePrice_viaBeanValidation() {
    CompanyPricingPlanCreateRequest request = CompanyPricingPlanCreateRequest.builder()
        .pricingType("SUBSCRIPTION")
        .planName("Base")
        .priceAmount(new BigDecimal("-100"))
        .build();

    Set<ConstraintViolation<CompanyPricingPlanCreateRequest>> violations = VALIDATOR.validate(request);

    assertThat(violations)
        .extracting(v -> v.getPropertyPath().toString())
        .contains("priceAmount");
  }

  @Test
  void create_rejectsMissingPlanName_viaBeanValidation() {
    CompanyPricingPlanCreateRequest request = CompanyPricingPlanCreateRequest.builder()
        .pricingType("SUBSCRIPTION")
        .planName("")
        .build();

    Set<ConstraintViolation<CompanyPricingPlanCreateRequest>> violations = VALIDATOR.validate(request);

    assertThat(violations)
        .extracting(v -> v.getPropertyPath().toString())
        .contains("planName");
  }

  @Test
  void update_rejectsBlankPlanName() {
    CompanyPricingPlanEntity existing = plan(50L);
    when(companyPricingPlanRepository.findByIdAndCompany_IdAndDeletedFalse(50L, 7L)).thenReturn(Optional.of(existing));

    CompanyPricingPlanUpdateRequest request = CompanyPricingPlanUpdateRequest.builder()
        .planName("   ")
        .build();

    assertThatThrownBy(() -> service().update(7L, 50L, request))
        .isInstanceOf(IllegalArgumentException.class);
    verify(companyPricingPlanRepository, never()).save(any());
  }

  @Test
  void create_addsProjectBasedPlan() {
    when(companyRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(company()));
    when(companyPricingPlanRepository.save(any(CompanyPricingPlanEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    CompanyPricingPlanCreateRequest request = CompanyPricingPlanCreateRequest.builder()
        .pricingType("project_based")
        .planName("Custom Build")
        .billingUnit("sqft")
        .build();

    CompanyPricingPlanResponse response = service().create(7L, request);

    assertThat(response.getPricingType()).isEqualTo("PROJECT_BASED");
    assertThat(response.getBillingUnit()).isEqualTo("sqft");
  }

  @Test
  void create_rejectsInvalidPricingType() {
    when(companyRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(company()));

    CompanyPricingPlanCreateRequest request = CompanyPricingPlanCreateRequest.builder()
        .pricingType("ANNUAL")
        .planName("Base")
        .build();

    assertThatThrownBy(() -> service().create(7L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("pricingType must be one of");
    verify(companyPricingPlanRepository, never()).save(any());
  }

  @Test
  void create_rejectsMissingCompany() {
    when(companyRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

    CompanyPricingPlanCreateRequest request = CompanyPricingPlanCreateRequest.builder()
        .pricingType("SUBSCRIPTION").planName("Base").build();

    assertThatThrownBy(() -> service().create(999L, request)).isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void update_changesFieldsAndFeatures() {
    CompanyPricingPlanEntity existing = plan(50L);
    when(companyPricingPlanRepository.findByIdAndCompany_IdAndDeletedFalse(50L, 7L)).thenReturn(Optional.of(existing));
    when(companyPricingPlanRepository.save(any(CompanyPricingPlanEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    CompanyPricingPlanUpdateRequest request = CompanyPricingPlanUpdateRequest.builder()
        .priceAmount(new BigDecimal("55000"))
        .features(List.of("Priority Support"))
        .build();

    CompanyPricingPlanResponse response = service().update(7L, 50L, request);

    assertThat(response.getPriceAmount()).isEqualByComparingTo("55000");
    assertThat(response.getFeatures()).containsExactly("Priority Support");
  }

  @Test
  void delete_softDeletesRow() {
    CompanyPricingPlanEntity existing = plan(50L);
    when(companyPricingPlanRepository.findByIdAndCompany_IdAndDeletedFalse(50L, 7L)).thenReturn(Optional.of(existing));

    service().delete(7L, 50L);

    ArgumentCaptor<CompanyPricingPlanEntity> captor = ArgumentCaptor.forClass(CompanyPricingPlanEntity.class);
    verify(companyPricingPlanRepository).save(captor.capture());
    assertThat(captor.getValue().getDeleted()).isTrue();
    assertThat(captor.getValue().getActive()).isFalse();
  }

  @Test
  void list_returnsPlansForCompany() {
    when(companyRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(company()));
    when(companyPricingPlanRepository.findByCompany_IdAndDeletedFalseOrderBySortOrderAscIdAsc(7L))
        .thenReturn(List.of(plan(1L)));

    List<CompanyPricingPlanResponse> result = service().list(7L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPlanName()).isEqualTo("Base");
  }
}
