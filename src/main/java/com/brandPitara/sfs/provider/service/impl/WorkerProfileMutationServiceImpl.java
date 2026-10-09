package com.brandPitara.sfs.provider.service.impl;

import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.marketplace.dto.NamedItemResponse;
import com.brandPitara.sfs.marketplace.service.MarketplaceMappers;
import com.brandPitara.sfs.provider.dto.WorkerAvailabilityUpdateRequest;
import com.brandPitara.sfs.provider.dto.WorkerOfferingsResponse;
import com.brandPitara.sfs.provider.dto.WorkerRateRequest;
import com.brandPitara.sfs.provider.dto.WorkerRatesReplaceRequest;
import com.brandPitara.sfs.provider.dto.WorkerServicesReplaceRequest;
import com.brandPitara.sfs.provider.entity.ProviderProfileEntity;
import com.brandPitara.sfs.provider.entity.ProviderRateEntity;
import com.brandPitara.sfs.provider.entity.ProviderServiceOfferingEntity;
import com.brandPitara.sfs.provider.enums.ProviderRateType;
import com.brandPitara.sfs.provider.enums.ProviderType;
import com.brandPitara.sfs.provider.repository.ProviderProfileRepository;
import com.brandPitara.sfs.provider.repository.ProviderRateRepository;
import com.brandPitara.sfs.provider.repository.ProviderServiceOfferingRepository;
import com.brandPitara.sfs.provider.service.WorkerProfileMutationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Currency;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WorkerProfileMutationServiceImpl implements WorkerProfileMutationService {

    private final ProviderProfileRepository providerProfileRepository;
    private final ProviderServiceOfferingRepository serviceOfferingRepository;
    private final ProviderRateRepository rateRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Long requireWorkerIdForUser(Long userId) {
        ProviderProfileEntity profile = providerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Provider profile not found"));
        if (profile.getProviderType() != ProviderType.WORKER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only worker profiles have services and rates");
        }
        return profile.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerOfferingsResponse get(Long workerId) {
        return toResponse(requireWorker(workerId));
    }

    @Override
    @Transactional
    public WorkerOfferingsResponse replaceServices(Long workerId, WorkerServicesReplaceRequest request) {
        ProviderProfileEntity worker = requireWorker(workerId);
        Set<String> seen = new HashSet<>();
        List<ProviderServiceOfferingEntity> rows = new ArrayList<>();
        int order = 0;
        for (String raw : request.services()) {
            String name = raw.trim();
            if (!seen.add(name.toLowerCase(Locale.ROOT))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate service: " + name);
            }
            rows.add(ProviderServiceOfferingEntity.builder().provider(worker).name(name).sortOrder(order++).build());
        }
        serviceOfferingRepository.deleteByProviderId(workerId);
        serviceOfferingRepository.flush();
        serviceOfferingRepository.saveAll(rows);
        return toResponse(worker);
    }

    @Override
    @Transactional
    public WorkerOfferingsResponse replaceRates(Long workerId, WorkerRatesReplaceRequest request) {
        ProviderProfileEntity worker = requireWorker(workerId);
        Set<ProviderRateType> seen = EnumSet.noneOf(ProviderRateType.class);
        List<ProviderRateEntity> rows = new ArrayList<>();
        int order = 0;
        for (WorkerRateRequest r : request.rates()) {
            if (!seen.add(r.type())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only one rate per type is allowed: " + r.type());
            }
            String currency = r.currency() == null ? "INR" : r.currency();
            try {
                Currency.getInstance(currency);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown currency: " + currency);
            }
            rows.add(ProviderRateEntity.builder()
                    .provider(worker)
                    .rateType(r.type())
                    .amount(r.amount().setScale(2, RoundingMode.UNNECESSARY))
                    .currency(currency)
                    .unit(r.unit())
                    .note(r.note() == null || r.note().isBlank() ? null : r.note().trim())
                    .sortOrder(order++)
                    .build());
        }
        rateRepository.deleteByProviderId(workerId);
        rateRepository.flush();
        rateRepository.saveAll(rows);
        return toResponse(worker);
    }

    @Override
    @Transactional
    public WorkerOfferingsResponse updateAvailability(Long workerId, WorkerAvailabilityUpdateRequest request) {
        ProviderProfileEntity worker = requireWorker(workerId);
        worker.setAvailabilityStatus(request.status());
        worker.setAvailabilityUpdatedAt(OffsetDateTime.now(clock));
        providerProfileRepository.save(worker);
        return toResponse(worker);
    }

    private ProviderProfileEntity requireWorker(Long workerId) {
        return providerProfileRepository.findWorkerForManagement(workerId)
                .orElseThrow(() -> new NotFoundException("Worker not found: " + workerId));
    }

    private WorkerOfferingsResponse toResponse(ProviderProfileEntity worker) {
        return new WorkerOfferingsResponse(
                worker.getId(),
                worker.getAvailabilityStatus(),
                worker.getAvailabilityUpdatedAt(),
                serviceOfferingRepository.findByProvider_IdOrderBySortOrderAscIdAsc(worker.getId()).stream()
                        .map(s -> new NamedItemResponse(s.getId(), s.getName()))
                        .toList(),
                rateRepository.findByProviderIds(List.of(worker.getId())).stream()
                        .map(MarketplaceMappers::rate)
                        .toList()
        );
    }
}
