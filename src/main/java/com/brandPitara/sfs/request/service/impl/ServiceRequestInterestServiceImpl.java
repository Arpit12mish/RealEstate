package com.brandPitara.sfs.request.service.impl;

import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.provider.entity.ProviderProfileEntity;
import com.brandPitara.sfs.provider.entity.ProviderServiceAreaEntity;
import com.brandPitara.sfs.provider.enums.ProviderMediaType;
import com.brandPitara.sfs.provider.repository.ProviderMediaRepository;
import com.brandPitara.sfs.provider.repository.ProviderProfileRepository;
import com.brandPitara.sfs.provider.repository.ProviderServiceAreaRepository;
import com.brandPitara.sfs.repository.UserRepository;
import com.brandPitara.sfs.request.dto.ServiceRequestInterestResponse;
import com.brandPitara.sfs.request.entity.ServiceRequestEntity;
import com.brandPitara.sfs.request.entity.ServiceRequestInterestEntity;
import com.brandPitara.sfs.request.enums.ServiceRequestInterestStatus;
import com.brandPitara.sfs.request.enums.ServiceRequestStatus;
import com.brandPitara.sfs.request.repository.ServiceRequestInterestRepository;
import com.brandPitara.sfs.request.repository.ServiceRequestRepository;
import com.brandPitara.sfs.request.service.ServiceRequestInterestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceRequestInterestServiceImpl implements ServiceRequestInterestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceRequestInterestRepository interestRepository;

    private final ProviderProfileRepository providerProfileRepository;
    private final ProviderServiceAreaRepository providerServiceAreaRepository;
    private final ProviderMediaRepository providerMediaRepository;

    private final UserRepository userRepository;

    @Override
    @Transactional
    public ServiceRequestInterestResponse expressInterest(Long providerUserId, Long requestId, String message) {

        ProviderProfileEntity provider = providerProfileRepository.findByUserId(providerUserId)
                .orElseThrow(() -> new NotFoundException("Provider profile not found"));

        ServiceRequestEntity req = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Service request not found: " + requestId));

        // must be OPEN to request for work
        if (req.getStatus() != ServiceRequestStatus.OPEN) {
            throw new IllegalStateException("Request is not OPEN");
        }

        // ✅ Validate provider is eligible to see this request (area + category)
        if (!matchesProvider(provider, req)) {
            throw new IllegalStateException("This request is not in your service area / category");
        }

        // avoid duplicates
        ServiceRequestInterestEntity existing = interestRepository.findByRequestIdAndProviderId(req.getId(), provider.getId())
                .orElse(null);

        if (existing != null) {
            // if previously rejected/withdrawn, allow re-request by setting to PENDING
            existing.setStatus(ServiceRequestInterestStatus.PENDING);
            existing.setMessage(message);
            return toResponse(interestRepository.save(existing));
        }

        ServiceRequestInterestEntity interest = ServiceRequestInterestEntity.builder()
                .request(req)
                .provider(provider)
                .status(ServiceRequestInterestStatus.PENDING)
                .message(message)
                .build();

        return toResponse(interestRepository.save(interest));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceRequestInterestResponse> listInterestsForCustomer(Long customerUserId, Long requestId) {

        ServiceRequestEntity req = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Service request not found: " + requestId));

        if (!req.getCustomer().getId().equals(customerUserId)) {
            throw new IllegalStateException("You are not the owner of this request");
        }

        return interestRepository.findByRequestIdOrderByCreatedAtDesc(requestId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void closeRequest(Long customerUserId, Long requestId) {

        ServiceRequestEntity req = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Service request not found: " + requestId));

        if (!req.getCustomer().getId().equals(customerUserId)) {
            throw new IllegalStateException("You are not the owner of this request");
        }

        // close
        req.setStatus(ServiceRequestStatus.CLOSED); // ensure enum has CLOSED
        serviceRequestRepository.save(req);

        // recommended: reject pending interests after close
        List<ServiceRequestInterestEntity> interests = interestRepository.findByRequestIdOrderByCreatedAtDesc(requestId);
        for (ServiceRequestInterestEntity i : interests) {
            if (i.getStatus() == ServiceRequestInterestStatus.PENDING) {
                i.setStatus(ServiceRequestInterestStatus.REJECTED);
            }
        }
        interestRepository.saveAll(interests);
    }

    private boolean matchesProvider(ProviderProfileEntity provider, ServiceRequestEntity req) {

        List<ProviderServiceAreaEntity> areas = providerServiceAreaRepository.findByProviderId(provider.getId());
        if (areas.isEmpty()) return false;

        Set<Long> cityIds = areas.stream().map(a -> a.getCity().getId()).collect(Collectors.toSet());

        Set<String> pincodes = areas.stream()
                .map(ProviderServiceAreaEntity::getPincode)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        boolean cityMatch = cityIds.contains(req.getCity().getId());
        if (!cityMatch) return false;

        boolean pincodeMatch = pincodes.isEmpty() || pincodes.contains(req.getPincode());
        if (!pincodeMatch) return false;

        // category match: provider primary category must be in request categories
        Long providerCatId = provider.getPrimaryCategory().getId();
        return req.getCategories().stream().anyMatch(c -> c.getId().equals(providerCatId));
    }

    private ServiceRequestInterestResponse toResponse(ServiceRequestInterestEntity i) {
        ProviderProfileEntity p = i.getProvider();
        User u = p.getUser();

        String profilePhotoUrl = providerMediaRepository
                .findFirstByProviderIdAndMediaType(p.getId(), ProviderMediaType.PROFILE_PHOTO)
                .map(m -> m.getUrl())
                .orElse(null);

        return new ServiceRequestInterestResponse(
                i.getId(),
                i.getStatus().name(),
                i.getCreatedAt(),
                new ServiceRequestInterestResponse.Provider(
                        p.getId(),
                        p.getDisplayName(),
                        p.getPrimaryCategory().getName(),
                        p.getProviderType().name(),
                        maskPhone(u.getPhoneNumber()),
                        profilePhotoUrl
                ),
                i.getMessage()
        );
    }

    private String maskPhone(String phone) {
        if (phone == null) return "******";
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 4) return "******";
        return "******" + digits.substring(digits.length() - 4);
    }
}
