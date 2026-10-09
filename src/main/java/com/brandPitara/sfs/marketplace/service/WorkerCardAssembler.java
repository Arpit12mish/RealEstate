package com.brandPitara.sfs.marketplace.service;

import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.marketplace.dto.PublicContactResponse;
import com.brandPitara.sfs.marketplace.dto.WorkerCardResponse;
import com.brandPitara.sfs.provider.entity.ProviderMediaEntity;
import com.brandPitara.sfs.provider.entity.ProviderProfileEntity;
import com.brandPitara.sfs.provider.entity.ProviderRateEntity;
import com.brandPitara.sfs.provider.enums.ProviderMediaType;
import com.brandPitara.sfs.provider.repository.ProviderMediaRepository;
import com.brandPitara.sfs.provider.repository.ProviderRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Builds worker cards for a page of workers with batched rate and avatar lookups. */
@Component
@RequiredArgsConstructor
public class WorkerCardAssembler {

    private final ProviderRateRepository rateRepository;
    private final ProviderMediaRepository mediaRepository;

    public List<WorkerCardResponse> toCards(List<ProviderProfileEntity> workers) {
        if (workers.isEmpty()) return List.of();
        List<Long> ids = workers.stream().map(ProviderProfileEntity::getId).toList();
        Map<Long, List<ProviderRateEntity>> ratesByWorker = ratesByWorker(ids);
        Map<Long, String> avatars = avatarUrls(ids);

        return workers.stream()
                .map(p -> WorkerCardResponse.builder()
                        .id(p.getId())
                        .displayName(p.getDisplayName())
                        .initials(MarketplaceMappers.initials(p.getDisplayName()))
                        .avatarUrl(avatars.get(p.getId()))
                        .trade(p.getPrimaryCategory().getName())
                        .availability(p.getAvailabilityStatus())
                        .primaryRate(MarketplaceMappers.primaryRate(ratesByWorker.getOrDefault(p.getId(), List.of())))
                        .experienceYears(p.getExperienceYears())
                        .contact(contact(p))
                        .build())
                .toList();
    }

    public Map<Long, List<ProviderRateEntity>> ratesByWorker(Collection<Long> ids) {
        return rateRepository.findByProviderIds(ids).stream()
                .collect(Collectors.groupingBy(r -> r.getProvider().getId()));
    }

    public Map<Long, String> avatarUrls(Collection<Long> ids) {
        return mediaRepository.findByProviderIdInAndMediaTypeOrderBySortOrderAscIdAsc(ids, ProviderMediaType.PROFILE_PHOTO)
                .stream()
                .collect(Collectors.toMap(m -> m.getProvider().getId(), ProviderMediaEntity::getUrl, (first, ignored) -> first));
    }

    /** A worker's public contact is its own linked listing, which onboarding fills from the worker's number. */
    public static PublicContactResponse contact(ProviderProfileEntity p) {
        BusinessEntity listing = p.getBusiness();
        if (listing == null) return PublicContactResponse.NONE;
        return MarketplaceMappers.contact(listing.getPrimaryPhone(), listing.getWhatsappPhone());
    }
}
