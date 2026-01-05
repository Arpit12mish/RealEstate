package com.brandPitara.sfs.request.service.impl;

import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.provider.entity.ProviderProfileEntity;
import com.brandPitara.sfs.provider.entity.ProviderServiceAreaEntity;
import com.brandPitara.sfs.provider.repository.ProviderProfileRepository;
import com.brandPitara.sfs.provider.repository.ProviderServiceAreaRepository;
import com.brandPitara.sfs.repository.CategoryRepository;
import com.brandPitara.sfs.repository.CityRepository;
import com.brandPitara.sfs.repository.UserRepository;
import com.brandPitara.sfs.request.dto.ServiceRequestCreateRequest;
import com.brandPitara.sfs.request.dto.ServiceRequestResponse;
import com.brandPitara.sfs.request.entity.ServiceRequestEntity;
import com.brandPitara.sfs.request.enums.ServiceRequestStatus;
import com.brandPitara.sfs.request.repository.ServiceRequestRepository;
import com.brandPitara.sfs.request.service.ServiceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceRequestServiceImpl implements ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final UserRepository userRepository;
    private final CityRepository cityRepository;
    private final CategoryRepository categoryRepository;

    private final ProviderProfileRepository providerProfileRepository;
    private final ProviderServiceAreaRepository providerServiceAreaRepository;

    @Override
    @Transactional
    public ServiceRequestResponse create(Long customerUserId, ServiceRequestCreateRequest request) {

        User customer = userRepository.findById(customerUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        CityEntity city = cityRepository.findById(request.cityId())
                .orElseThrow(() -> new NotFoundException("City not found: " + request.cityId()));

        List<CategoryEntity> categories = categoryRepository.findAllById(request.categoryIds());
        if (categories.size() != request.categoryIds().size()) {
            throw new NotFoundException("One or more categories not found");
        }

        ServiceRequestEntity entity = ServiceRequestEntity.builder()
                .customer(customer)
                .city(city)
                .locality(request.locality())
                .pincode(request.pincode())
                .notes(request.notes())
                .status(ServiceRequestStatus.OPEN)
                .categories(new HashSet<>(categories))
                .build();

        return toResponse(serviceRequestRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> myRequests(Long customerUserId, ServiceRequestStatus status, Pageable pageable) {
        Page<ServiceRequestEntity> page = (status == null)
                ? serviceRequestRepository.findByCustomerIdOrderByCreatedAtDesc(customerUserId, pageable)
                : serviceRequestRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc(customerUserId, status, pageable);

        return page.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> providerFeed(Long providerUserId, ServiceRequestStatus status, Pageable pageable) {

        ProviderProfileEntity provider = providerProfileRepository.findByUserId(providerUserId)
                .orElseThrow(() -> new NotFoundException("Provider profile not found"));

        List<ProviderServiceAreaEntity> areas = providerServiceAreaRepository.findByProviderId(provider.getId());
        if (areas.isEmpty()) return Page.empty(pageable);

        Set<Long> cityIds = areas.stream().map(a -> a.getCity().getId()).collect(Collectors.toSet());

        Set<String> pincodes = areas.stream()
                .map(ProviderServiceAreaEntity::getPincode)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        boolean ignorePincode = pincodes.isEmpty();
        ServiceRequestStatus effectiveStatus = (status == null) ? ServiceRequestStatus.OPEN : status;

        Page<ServiceRequestEntity> page = serviceRequestRepository.findProviderFeed(
                effectiveStatus,
                cityIds,
                ignorePincode,
                pincodes,
                provider.getPrimaryCategory().getId(),
                pageable
        );

        return page.map(this::toResponse);
    }

    private ServiceRequestResponse toResponse(ServiceRequestEntity r) {
        return new ServiceRequestResponse(
                r.getId(),
                r.getStatus().name(),
                r.getCreatedAt(),
                new ServiceRequestResponse.City(
                        r.getCity().getId(),
                        r.getCity().getName(),
                        r.getCity().getState()
                ),
                r.getLocality(),
                r.getPincode(),
                r.getNotes(),
                r.getCategories().stream()
                        .map(c -> new ServiceRequestResponse.Category(c.getId(), c.getName()))
                        .toList(),
                new ServiceRequestResponse.Customer(
                        r.getCustomer().getId(),
                        r.getCustomer().getName(),
                        maskPhone(r.getCustomer().getPhoneNumber())
                )
        );
    }

    private String maskPhone(String phone) {
        if (phone == null) return "******";
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 4) return "******";
        return "******" + digits.substring(digits.length() - 4);
    }
}
