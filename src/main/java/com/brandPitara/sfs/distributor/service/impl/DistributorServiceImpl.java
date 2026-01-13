package com.brandPitara.sfs.distributor.service.impl;

import com.brandPitara.sfs.brand.entity.BrandDistributorEntity;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.repository.BrandDistributorRepository;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.distributor.dto.DistributorBrandCard;
import com.brandPitara.sfs.distributor.dto.DistributorMediaResponse;
import com.brandPitara.sfs.distributor.dto.DistributorProfileResponse;
import com.brandPitara.sfs.distributor.dto.DistributorResponse;
import com.brandPitara.sfs.distributor.dto.DistributorUpsertRequest;
import com.brandPitara.sfs.distributor.entity.DistributorEntity;
import com.brandPitara.sfs.distributor.repository.DistributorMediaRepository;
import com.brandPitara.sfs.distributor.repository.DistributorRepository;
import com.brandPitara.sfs.distributor.service.DistributorService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class DistributorServiceImpl implements DistributorService {

  private final DistributorRepository distributorRepository;
  private final BrandDistributorRepository brandDistributorRepository;
  private final ContentVersionService contentVersionService;
  private final DistributorMediaRepository distributorMediaRepository;


  private static final String KEY_BRANDS = "BRANDS";
  private static final String KEY_HOME = "HOME";

  @Override
  @Transactional
  public DistributorResponse create(DistributorUpsertRequest request) {
    DistributorEntity e = DistributorEntity.builder()
        .name(clean(request.getName()))
        .phone(clean(request.getPhone()))
        .whatsapp(clean(request.getWhatsapp()))
        .email(clean(request.getEmail()))
        .addressLine1(clean(request.getAddressLine1()))
        .addressLine2(clean(request.getAddressLine2()))
        .pincode(clean(request.getPincode()))
        .cityId(request.getCityId())
        .latitude(request.getLatitude())
        .longitude(request.getLongitude())
        .active(request.getActive() != null ? request.getActive() : true)
        .deleted(false)
        .build();

    DistributorEntity saved = distributorRepository.save(e);

    // admin-side data change
    contentVersionService.bump(KEY_HOME);

    return toResponse(saved);
  }

  @Override
  @Transactional
  public DistributorResponse update(Long id, DistributorUpsertRequest request) {
    DistributorEntity e = distributorRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new EntityNotFoundException("Distributor not found: " + id));

    if (StringUtils.hasText(request.getName())) e.setName(clean(request.getName()));
    if (request.getPhone() != null) e.setPhone(clean(request.getPhone()));
    if (request.getWhatsapp() != null) e.setWhatsapp(clean(request.getWhatsapp()));
    if (request.getEmail() != null) e.setEmail(clean(request.getEmail()));
    if (request.getAddressLine1() != null) e.setAddressLine1(clean(request.getAddressLine1()));
    if (request.getAddressLine2() != null) e.setAddressLine2(clean(request.getAddressLine2()));
    if (request.getPincode() != null) e.setPincode(clean(request.getPincode()));
    if (request.getCityId() != null) e.setCityId(request.getCityId());
    if (request.getLatitude() != null) e.setLatitude(request.getLatitude());
    if (request.getLongitude() != null) e.setLongitude(request.getLongitude());
    if (request.getActive() != null) e.setActive(request.getActive());

    DistributorEntity saved = distributorRepository.save(e);

    contentVersionService.bump(KEY_HOME);
    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public DistributorResponse getById(Long id) {
    DistributorEntity e = distributorRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new EntityNotFoundException("Distributor not found: " + id));
    return toResponse(e);
  }

  @Override
  @Transactional
  public void softDelete(Long id) {
    DistributorEntity e = distributorRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new EntityNotFoundException("Distributor not found: " + id));

    e.setDeleted(true);
    e.setActive(false);
    distributorRepository.save(e);

    contentVersionService.bump(KEY_HOME);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<DistributorResponse> adminList(Long cityId, Boolean active, Pageable pageable) {

    Page<DistributorEntity> page;

    if (cityId != null && active != null) {
      // easiest: filter using repository + in-memory is bad; prefer query methods when required later
      page = distributorRepository.findByCityIdAndActiveTrueAndDeletedFalse(cityId, pageable)
          .map(e -> e); // active=true only
      if (!active) {
        // if you want active=false support later, create another repo method
      }
    } else if (cityId != null) {
      page = distributorRepository.findByCityIdAndActiveTrueAndDeletedFalse(cityId, pageable);
    } else {
      page = distributorRepository.findByDeletedFalse(pageable);
      if (active != null) {
        // quick filter without extra repo methods; fine for admin small pages
        page = new PageImpl<>(
            page.getContent().stream().filter(d -> active.equals(Boolean.TRUE.equals(d.getActive()))).toList(),
            pageable,
            page.getTotalElements()
        );
      }
    }

    return page.map(this::toResponse);
  }

  // ---------------- PUBLIC ----------------

  @Override
  @Transactional(readOnly = true)
  public Page<DistributorResponse> publicListByBrand(Long brandId, Long cityId, Pageable pageable) {
    // This returns distributors mapped to brand where mapping is active, not deleted
    Page<BrandDistributorEntity> mappingPage =
        brandDistributorRepository.findByBrandIdAndActiveTrueAndDeletedFalseOrderByPriorityAsc(brandId, pageable);

    var filtered = mappingPage.getContent().stream()
        .filter(m -> m.getDistributor() != null)
        .filter(m -> Boolean.TRUE.equals(m.getDistributor().getActive()))
        .filter(m -> Boolean.FALSE.equals(m.getDistributor().getDeleted()))
        .filter(m -> cityId == null || cityId.equals(m.getDistributor().getCityId()))
        .map(m -> toResponse(m.getDistributor()))
        .toList();

    return new PageImpl<>(filtered, pageable, mappingPage.getTotalElements());
  }

  @Override
  @Transactional(readOnly = true)
  public DistributorResponse publicGetDistributor(Long distributorId) {
    DistributorEntity e = distributorRepository.findByIdAndDeletedFalse(distributorId)
        .orElseThrow(() -> new EntityNotFoundException("Distributor not found: " + distributorId));
    if (!Boolean.TRUE.equals(e.getActive())) {
      throw new EntityNotFoundException("Distributor inactive: " + distributorId);
    }
    return toResponse(e);
  }

  // ---------------- Helpers ----------------

  private DistributorResponse toResponse(DistributorEntity e) {
    return DistributorResponse.builder()
        .id(e.getId())
        .name(e.getName())
        .phone(e.getPhone())
        .whatsapp(e.getWhatsapp())
        .email(e.getEmail())
        .addressLine1(e.getAddressLine1())
        .addressLine2(e.getAddressLine2())
        .pincode(e.getPincode())
        .cityId(e.getCityId())
        .latitude(e.getLatitude())
        .longitude(e.getLongitude())
        .active(Boolean.TRUE.equals(e.getActive()))
        .build();
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }

    @Override
    @Transactional(readOnly = true)
    public DistributorProfileResponse publicGetProfile(Long distributorId) {

    // 1) Distributor
    DistributorEntity distributor = distributorRepository.findByIdAndDeletedFalse(distributorId)
        .orElseThrow(() -> new EntityNotFoundException("Distributor not found: " + distributorId));

    if (!Boolean.TRUE.equals(distributor.getActive())) {
        throw new EntityNotFoundException("Distributor inactive: " + distributorId);
    }

    // 2) Brand mappings + offers (single join fetch query)
    List<BrandDistributorEntity> mappings =
        brandDistributorRepository.findPublicBrandMappingsForDistributor(distributorId);

    List<DistributorBrandCard> brandCards = mappings.stream()
        .map(m -> {
        BrandEntity b = m.getBrand();
        return DistributorBrandCard.builder()
            .brandId(b.getId())
            .brandName(b.getName())
            .brandLogoUrl(b.getLogoUrl())
            .offerTitle(m.getOfferTitle())
            .offerDescription(m.getOfferDescription())
            .offerBannerUrl(m.getOfferBannerUrl())
            .validTill(m.getValidTill())
            .priority(m.getPriority() != null ? m.getPriority() : 0)
            .active(Boolean.TRUE.equals(m.getActive()))
            // .status(m.getStatus())  // comment for now, see Fix 3
            .build();
        })
        .collect(Collectors.toList());


    // 3) Gallery (single query)
    List<DistributorMediaResponse> media = distributorMediaRepository
        .findByDistributorIdAndDeletedFalseAndActiveTrueOrderBySortOrderAsc(distributorId)
        .stream()
        .map(m -> DistributorMediaResponse.builder()
            .id(m.getId())
            .mediaType(m.getMediaType())
            .url(m.getUrl())
            .caption(m.getCaption())
            .sortOrder(m.getSortOrder() != null ? m.getSortOrder() : 0)
            .active(Boolean.TRUE.equals(m.getActive()))
            .build()
        )
        .collect(Collectors.toList());


    return DistributorProfileResponse.builder()
        .distributor(toResponse(distributor))
        .brands(brandCards)
        .gallery(media)
        .build();

    }

}
