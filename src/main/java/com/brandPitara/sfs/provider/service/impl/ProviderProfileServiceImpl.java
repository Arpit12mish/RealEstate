package com.brandPitara.sfs.provider.service.impl;

import com.brandPitara.sfs.entity.*;
import com.brandPitara.sfs.enums.OnboardingStatus;
import com.brandPitara.sfs.enums.Role;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.provider.dto.ProviderProfileResponse;
import com.brandPitara.sfs.provider.dto.ProviderProfileUpsertRequest;
import com.brandPitara.sfs.provider.entity.ProviderProfileEntity;
import com.brandPitara.sfs.provider.entity.ProviderServiceAreaEntity;
import com.brandPitara.sfs.provider.enums.VerificationStatus;
import com.brandPitara.sfs.provider.repository.ProviderProfileRepository;
import com.brandPitara.sfs.provider.repository.ProviderServiceAreaRepository;
import com.brandPitara.sfs.provider.service.ProviderProfileService;
import com.brandPitara.sfs.repository.BusinessRepository;
import com.brandPitara.sfs.repository.CategoryRepository;
import com.brandPitara.sfs.repository.CityRepository;
import com.brandPitara.sfs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProviderProfileServiceImpl implements ProviderProfileService {

    private final ProviderProfileRepository providerProfileRepository;
    private final ProviderServiceAreaRepository serviceAreaRepository;

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CityRepository cityRepository;
    private final BusinessRepository businessRepository;

    @Override
    @Transactional
    public ProviderProfileResponse onboardOrUpdateMyProfile(Long currentUserId, ProviderProfileUpsertRequest request) {

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        CategoryEntity category = categoryRepository.findById(request.primaryCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found: " + request.primaryCategoryId()));

        // ✅ Set role based on providerType (onboarding truth)
        Role nextRole = (request.providerType().name().equals("WORKER")) ? Role.WORKER : Role.BRAND;
        if (user.getRole() != nextRole) {
            user.setRole(nextRole);
        }
        // set onboarding status (provider finished basic profile)
        user.setOnboardingStatus(OnboardingStatus.PROVIDER_READY);
        if (user.getRoleSelectedAt() == null) {
            user.setRoleSelectedAt(OffsetDateTime.now());
        }
        
        userRepository.save(user);

        ProviderProfileEntity profile = providerProfileRepository.findByUserId(currentUserId)
                .orElseGet(() -> ProviderProfileEntity.builder().user(user).build());

        profile.setProviderType(request.providerType());
        profile.setDisplayName(request.displayName());
        profile.setHeadline(request.headline());
        profile.setBio(request.bio());
        profile.setPrimaryCategory(category);
        profile.setExperienceYears(request.experienceYears());
        profile.setBusinessName(request.businessName());
        profile.setGstNumber(request.gstNumber());

        if (profile.getVerificationStatus() == null) {
                profile.setVerificationStatus(VerificationStatus.UNVERIFIED);
        }

        // ✅ Use first area as the primary listing area (UI order)
        ProviderProfileUpsertRequest.ServiceAreaRequest firstArea = request.serviceAreas().get(0);

        BusinessEntity business = ensureBusinessLinked(profile, user, firstArea, request, category);
        profile.setBusiness(business);

        ProviderProfileEntity saved = providerProfileRepository.save(profile);

        // Replace service areas
        serviceAreaRepository.deleteByProviderId(saved.getId());
        for (ProviderProfileUpsertRequest.ServiceAreaRequest ar : request.serviceAreas()) {
            CityEntity city = cityRepository.findById(ar.cityId())
                    .orElseThrow(() -> new NotFoundException("City not found: " + ar.cityId()));

            ProviderServiceAreaEntity area = ProviderServiceAreaEntity.builder()
                    .provider(saved)
                    .city(city)
                    .locality(ar.locality())
                    .pincode(ar.pincode())
                    .latitude(ar.latitude())
                    .longitude(ar.longitude())
                    .build();

            serviceAreaRepository.save(area);
        }

        return toResponse(saved, serviceAreaRepository.findByProviderId(saved.getId()));
    }

    private BusinessEntity ensureBusinessLinked(
            ProviderProfileEntity profile,
            User user,
            ProviderProfileUpsertRequest.ServiceAreaRequest firstArea,
            ProviderProfileUpsertRequest request,
            CategoryEntity category
    ) {
        BusinessEntity business = profile.getBusiness();

        CityEntity city = cityRepository.findById(firstArea.cityId())
                .orElseThrow(() -> new NotFoundException("City not found: " + firstArea.cityId()));

        if (business == null) {
            business = BusinessEntity.builder()
                    .name(request.displayName())
                    .primaryPhone(user.getPhoneNumber())
                    .whatsappPhone(user.getPhoneNumber())
                    .email(user.getEmail())
                    .addressLine1(firstArea.locality())
                    .pincode(firstArea.pincode())
                    .city(city)
                    .category(category)
                    .active(true)
                    .sponsored(false)
                    .sponsoredPriority(0)

                    // ✅ add these defaults
                .avgRating(0.0)          // or 0 / BigDecimal.ZERO depending on your entity type
                .totalRatings(0)         // only if NOT NULL in DB
                .topRated(false)       // only if NOT NULL in DB
                .nearAndFast(false)      // only if NOT NULL in DB
                        .build();
        }

        business.setName(request.displayName());
        business.setPrimaryPhone(user.getPhoneNumber());
        business.setWhatsappPhone(user.getPhoneNumber());
        business.setEmail(user.getEmail());
        business.setCity(city);
        business.setCategory(category);
        business.setAddressLine1(firstArea.locality());
        business.setPincode(firstArea.pincode());
        business.setOwner(user);

        return businessRepository.save(business);
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderProfileResponse getMyProfile(Long currentUserId) {
        ProviderProfileEntity profile = providerProfileRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new NotFoundException("Provider profile not found"));
        return toResponse(profile, serviceAreaRepository.findByProviderId(profile.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderProfileResponse getPublicProfile(Long providerId) {
        ProviderProfileEntity profile = providerProfileRepository.findById(providerId)
                .orElseThrow(() -> new NotFoundException("Provider not found"));
        return toResponse(profile, serviceAreaRepository.findByProviderId(profile.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProviderProfileResponse> getSimilarProviders(Long providerId, int limit) {
        ProviderProfileEntity base = providerProfileRepository.findById(providerId)
                .orElseThrow(() -> new NotFoundException("Provider not found"));

        List<ProviderServiceAreaEntity> baseAreas = serviceAreaRepository.findByProviderId(base.getId());
        if (baseAreas.isEmpty()) return List.of();

        Long categoryId = base.getPrimaryCategory().getId();
        Long cityId = baseAreas.get(0).getCity().getId();

        List<ProviderProfileEntity> similar = providerProfileRepository.findSimilar(
                providerId, categoryId, cityId, PageRequest.of(0, limit));

        if (similar.isEmpty()) return List.of();

        List<Long> ids = similar.stream().map(ProviderProfileEntity::getId).toList();
        Map<Long, List<ProviderServiceAreaEntity>> areasByProvider = serviceAreaRepository
                .findByProviderIdIn(ids).stream()
                .collect(Collectors.groupingBy(a -> a.getProvider().getId()));

        return similar.stream()
                .map(p -> toResponse(p, areasByProvider.getOrDefault(p.getId(), List.of())))
                .toList();
    }

    private ProviderProfileResponse toResponse(ProviderProfileEntity p, List<ProviderServiceAreaEntity> areas) {
        return new ProviderProfileResponse(
                p.getId(),
                p.getUser().getId(),
                p.getProviderType(),
                p.getDisplayName(),
                p.getHeadline(),
                p.getBio(),
                p.getPrimaryCategory().getId(),
                p.getPrimaryCategory().getName(),
                p.getExperienceYears(),
                p.getBusinessName(),
                p.getGstNumber(),
                p.getVerificationStatus(),
                p.isFeatured(),
                p.getBusiness() != null ? p.getBusiness().getId() : null,
                areas.stream().map(a -> new ProviderProfileResponse.ServiceAreaResponse(
                        a.getCity().getId(),
                        a.getCity().getName(),
                        a.getLocality(),
                        a.getPincode(),
                        a.getLatitude(),
                        a.getLongitude()
                )).toList()
        );
    }
}
