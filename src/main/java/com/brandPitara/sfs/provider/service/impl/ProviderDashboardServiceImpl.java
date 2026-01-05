package com.brandPitara.sfs.provider.service.impl;

import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.provider.dto.ProviderDashboardResponse;
import com.brandPitara.sfs.provider.entity.ProviderProfileEntity;
import com.brandPitara.sfs.provider.entity.ProviderServiceAreaEntity;
import com.brandPitara.sfs.provider.enums.ProviderMediaType;
import com.brandPitara.sfs.provider.repository.ProviderMediaRepository;
import com.brandPitara.sfs.provider.repository.ProviderProfileRepository;
import com.brandPitara.sfs.provider.repository.ProviderProjectRepository;
import com.brandPitara.sfs.provider.repository.ProviderServiceAreaRepository;
import com.brandPitara.sfs.provider.service.ProviderDashboardService;
import com.brandPitara.sfs.request.entity.ServiceRequestEntity;
import com.brandPitara.sfs.request.enums.ServiceRequestStatus;
import com.brandPitara.sfs.request.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProviderDashboardServiceImpl implements ProviderDashboardService {

    private final ProviderProfileRepository providerProfileRepository;
    private final ProviderMediaRepository providerMediaRepository;
    private final ProviderProjectRepository providerProjectRepository;
    private final ProviderServiceAreaRepository providerServiceAreaRepository;

    // ✅ NEW
    private final ServiceRequestRepository serviceRequestRepository;

    @Override
    @Transactional(readOnly = true)
    public ProviderDashboardResponse getMyDashboard(Long currentUserId) {

        ProviderProfileEntity provider = providerProfileRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new NotFoundException("Provider profile not found"));

        long galleryCount = providerMediaRepository.countByProviderIdAndMediaType(
                provider.getId(), ProviderMediaType.GALLERY
        );

        boolean hasProfilePhoto = providerMediaRepository
                .findFirstByProviderIdAndMediaType(provider.getId(), ProviderMediaType.PROFILE_PHOTO)
                .isPresent();

        long projectCount = providerProjectRepository.countByProviderId(provider.getId());
        long serviceAreaCount = providerServiceAreaRepository.findByProviderId(provider.getId()).size();

        List<String> missing = new ArrayList<>();

        // ===== Completion scoring (total 100) =====
        int score = 35;

        if (hasProfilePhoto) score += 20; else missing.add("PROFILE_PHOTO");
        if (galleryCount > 0) score += 15; else missing.add("GALLERY");
        if (serviceAreaCount > 0) score += 15; else missing.add("SERVICE_AREA");
        if (projectCount > 0) score += 15; else missing.add("PROJECTS");

        int completionPercent = Math.min(score, 100);

        String recommendedNext = "ALL_DONE";
        if (missing.contains("PROFILE_PHOTO")) recommendedNext = "ADD_PROFILE_PHOTO";
        else if (missing.contains("GALLERY")) recommendedNext = "ADD_GALLERY";
        else if (missing.contains("SERVICE_AREA")) recommendedNext = "ADD_SERVICE_AREA";
        else if (missing.contains("PROJECTS")) recommendedNext = "ADD_PROJECTS";

        // ✅ NEW: provider request feed summary
        List<ProviderServiceAreaEntity> areas = providerServiceAreaRepository.findByProviderId(provider.getId());

        long openRequestsCount = 0;
        List<ProviderDashboardResponse.ServiceRequestCard> latestRequests = List.of();

        if (!areas.isEmpty()) {
            Set<Long> cityIds = areas.stream().map(a -> a.getCity().getId()).collect(Collectors.toSet());

            Set<String> pincodes = areas.stream()
                    .map(ProviderServiceAreaEntity::getPincode)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());

            boolean ignorePincode = pincodes.isEmpty();

            openRequestsCount = serviceRequestRepository.countProviderFeed(
                    ServiceRequestStatus.OPEN,
                    cityIds,
                    ignorePincode,
                    pincodes,
                    provider.getPrimaryCategory().getId()
            );

            Pageable top10 = PageRequest.of(0, 10);
            List<ServiceRequestEntity> top = serviceRequestRepository.findTopProviderFeed(
                    ServiceRequestStatus.OPEN,
                    cityIds,
                    ignorePincode,
                    pincodes,
                    provider.getPrimaryCategory().getId(),
                    top10
            );

            latestRequests = top.stream().map(r -> {
                String requirement = r.getCategories().stream()
                        .map(c -> c.getName())
                        .sorted()
                        .collect(Collectors.joining(", "));

                String customerName = (r.getCustomer().getName() == null || r.getCustomer().getName().isBlank())
                        ? "Customer"
                        : r.getCustomer().getName().trim();

                return new ProviderDashboardResponse.ServiceRequestCard(
                        r.getId(),
                        customerName,
                        requirement,
                        r.getLocality(),
                        r.getPincode()
                );
            }).toList();
        }

        return new ProviderDashboardResponse(
                provider.getId(),
                provider.getProviderType().name(),
                provider.getDisplayName(),
                provider.getPrimaryCategory().getName(),
                completionPercent,
                missing,
                recommendedNext,
                new ProviderDashboardResponse.Counts(galleryCount, projectCount, serviceAreaCount, hasProfilePhoto),

                // ✅ NEW
                openRequestsCount,
                latestRequests
        );
    }
}
