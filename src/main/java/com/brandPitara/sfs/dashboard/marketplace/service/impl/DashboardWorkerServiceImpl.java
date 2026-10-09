package com.brandPitara.sfs.dashboard.marketplace.service.impl;

import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.marketplace.dto.WorkerListItemResponse;
import com.brandPitara.sfs.dashboard.marketplace.dto.WorkerManagementResponse;
import com.brandPitara.sfs.dashboard.marketplace.dto.WorkerProfileUpdateRequest;
import com.brandPitara.sfs.dashboard.marketplace.dto.WorkerVerificationRequest;
import com.brandPitara.sfs.dashboard.marketplace.service.DashboardWorkerService;
import com.brandPitara.sfs.dto.PageResponse;
import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.marketplace.service.MarketplacePaging;
import com.brandPitara.sfs.provider.dto.WorkerAvailabilityUpdateRequest;
import com.brandPitara.sfs.provider.dto.WorkerOfferingsResponse;
import com.brandPitara.sfs.provider.dto.WorkerRatesReplaceRequest;
import com.brandPitara.sfs.provider.dto.WorkerServicesReplaceRequest;
import com.brandPitara.sfs.provider.entity.ProviderProfileEntity;
import com.brandPitara.sfs.provider.entity.ProviderServiceAreaEntity;
import com.brandPitara.sfs.provider.repository.ProviderProfileRepository;
import com.brandPitara.sfs.provider.repository.ProviderServiceAreaRepository;
import com.brandPitara.sfs.provider.service.WorkerProfileMutationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DashboardWorkerServiceImpl implements DashboardWorkerService {

    private final ProviderProfileRepository providerProfileRepository;
    private final ProviderServiceAreaRepository serviceAreaRepository;
    private final WorkerProfileMutationService mutationService;
    private final DashboardActionAuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WorkerListItemResponse> list(String query, int page, int size) {
        Page<ProviderProfileEntity> result = providerProfileRepository.searchWorkersForManagement(
                StringUtils.hasText(query) ? query.trim() : "", MarketplacePaging.page(page, size));
        return MarketplacePaging.response(result, result.getContent().stream()
                .map(p -> new WorkerListItemResponse(p.getId(), p.getDisplayName(), p.getPrimaryCategory().getName(),
                        p.getVerificationStatus(), p.getAvailabilityStatus()))
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerManagementResponse get(Long workerId) {
        return toResponse(requireWorker(workerId));
    }

    @Override
    @Transactional
    public WorkerManagementResponse updateProfile(Long workerId, WorkerProfileUpdateRequest r) {
        ProviderProfileEntity p = requireWorker(workerId);
        if (r.displayName() != null) {
            if (!StringUtils.hasText(r.displayName())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "displayName cannot be blank");
            }
            p.setDisplayName(r.displayName().trim());
        }
        if (r.headline() != null) p.setHeadline(StringUtils.hasText(r.headline()) ? r.headline().trim() : null);
        if (r.bio() != null) p.setBio(StringUtils.hasText(r.bio()) ? r.bio().trim() : null);
        if (r.experienceYears() != null) p.setExperienceYears(r.experienceYears());
        providerProfileRepository.save(p);
        if (r.availability() != null) {
            mutationService.updateAvailability(workerId, new WorkerAvailabilityUpdateRequest(r.availability()));
        }
        auditService.record(DashboardAuditAction.WORKER_PROFILE_UPDATED, ReviewEntityType.WORKER, workerId, null);
        return toResponse(requireWorker(workerId));
    }

    @Override
    @Transactional
    public WorkerManagementResponse updateVerification(Long workerId, WorkerVerificationRequest request) {
        ProviderProfileEntity p = requireWorker(workerId);
        p.setVerificationStatus(request.status());
        providerProfileRepository.save(p);
        auditService.record(DashboardAuditAction.WORKER_VERIFICATION_CHANGED, ReviewEntityType.WORKER, workerId, null);
        return toResponse(p);
    }

    @Override
    @Transactional
    public WorkerManagementResponse replaceServices(Long workerId, WorkerServicesReplaceRequest request) {
        mutationService.replaceServices(workerId, request);
        auditService.record(DashboardAuditAction.WORKER_SERVICES_REPLACED, ReviewEntityType.WORKER, workerId, null);
        return toResponse(requireWorker(workerId));
    }

    @Override
    @Transactional
    public WorkerManagementResponse replaceRates(Long workerId, WorkerRatesReplaceRequest request) {
        mutationService.replaceRates(workerId, request);
        auditService.record(DashboardAuditAction.WORKER_RATES_REPLACED, ReviewEntityType.WORKER, workerId, null);
        return toResponse(requireWorker(workerId));
    }

    private ProviderProfileEntity requireWorker(Long workerId) {
        return providerProfileRepository.findWorkerForManagement(workerId)
                .orElseThrow(() -> new NotFoundException("Worker not found: " + workerId));
    }

    private WorkerManagementResponse toResponse(ProviderProfileEntity p) {
        WorkerOfferingsResponse offerings = mutationService.get(p.getId());
        BusinessEntity listing = p.getBusiness();
        List<String> areas = serviceAreaRepository.findByProviderIdIn(Set.of(p.getId())).stream()
                .sorted(Comparator.comparing(ProviderServiceAreaEntity::getId))
                .map(a -> StringUtils.hasText(a.getLocality())
                        ? a.getLocality() + ", " + a.getCity().getName()
                        : a.getCity().getName())
                .toList();
        return WorkerManagementResponse.builder()
                .id(p.getId())
                .displayName(p.getDisplayName())
                .trade(p.getPrimaryCategory().getName())
                .headline(p.getHeadline())
                .bio(p.getBio())
                .experienceYears(p.getExperienceYears())
                .verificationStatus(p.getVerificationStatus())
                .availability(offerings.availability())
                .availabilityUpdatedAt(offerings.availabilityUpdatedAt())
                .listingBusinessId(listing == null ? null : listing.getId())
                .listingPhone(listing == null ? null : listing.getPrimaryPhone())
                .listingWhatsapp(listing == null ? null : listing.getWhatsappPhone())
                .listingActive(listing == null || Boolean.TRUE.equals(listing.getActive()))
                .serviceAreas(areas)
                .services(offerings.services())
                .rates(offerings.rates())
                .build();
    }
}
