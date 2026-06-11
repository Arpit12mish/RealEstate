package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.dto.BusinessResponse;
import com.brandPitara.sfs.dto.PageResponse;
import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.entity.FavoriteEntity;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.repository.BusinessRepository;
import com.brandPitara.sfs.repository.FavoriteRepository;
import com.brandPitara.sfs.repository.UserRepository;
import com.brandPitara.sfs.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;

    @Override
    public void addFavorite(Long businessId) {
        User user = getCurrentUserOrThrow();

        BusinessEntity business = businessRepository.findById(businessId)
                .orElseThrow(() -> new NotFoundException("Business not found: " + businessId));

        // Idempotent behavior: if already exists, do nothing
        if (favoriteRepository.existsByUser_IdAndBusiness_Id(user.getId(), businessId)) {
            return;
        }

        FavoriteEntity fav = FavoriteEntity.builder()
                .user(user)
                .business(business)
                .build();

        favoriteRepository.save(fav);
    }

    @Override
    public void removeFavorite(Long businessId) {
        User user = getCurrentUserOrThrow();

        // Idempotent behavior: if not found, do nothing
        favoriteRepository.findByUser_IdAndBusiness_Id(user.getId(), businessId)
                .ifPresent(favoriteRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavorite(Long businessId) {
        User user = getCurrentUserOrThrow();
        return favoriteRepository.existsByUser_IdAndBusiness_Id(user.getId(), businessId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BusinessResponse> listFavorites(int page, int size) {
        User user = getCurrentUserOrThrow();

        Page<FavoriteEntity> favPage =
                favoriteRepository.findByUser_Id(user.getId(), PageRequest.of(page, size));

        // Collect business IDs in this page
        var businesses = favPage.getContent().stream()
                .map(FavoriteEntity::getBusiness)
                .toList();

        var businessIds = businesses.stream()
                .map(BusinessEntity::getId)
                .toList();

        // ✅ favoriteCount for each business (batch)
        var countsRaw = favoriteRepository.countByBusinessIds(businessIds);
        java.util.Map<Long, Long> countMap = new java.util.HashMap<>();
        for (Object[] row : countsRaw) {
            Long businessId = (Long) row[0];
            Long cnt = (Long) row[1];
            countMap.put(businessId, cnt);
        }

        // ✅ isFavorite for current user (batch)
        var favoritedIds = new java.util.HashSet<>(
                favoriteRepository.findFavoritedBusinessIds(user.getId(), businessIds)
        );

        var content = businesses.stream()
                .map(b -> {
                    BusinessResponse br = toBusinessResponse(b);
                    br.setFavoriteCount(countMap.getOrDefault(b.getId(), 0L));
                    br.setIsFavorite(favoritedIds.contains(b.getId()));
                    return br;
                })
                .toList();

        return PageResponse.<BusinessResponse>builder()
                .content(content)
                .page(favPage.getNumber())
                .size(favPage.getSize())
                .totalElements(favPage.getTotalElements())
                .totalPages(favPage.getTotalPages())
                .last(favPage.isLast())
                .build();
    }


    private User getCurrentUserOrThrow() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName(); // ✅ phoneNumber (JWT subject)
        return userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new NotFoundException("User not found for phone: " + phone));
    }

    /**
     * Minimal mapping (safe) — we can expand it to match your card UI exactly.
     * This does NOT depend on BaseEntity fields, so it won't break compilation.
     */
    private BusinessResponse toBusinessResponse(BusinessEntity b) {
        return BusinessResponse.builder()
                .id(b.getId())
                .name(b.getName())

                .primaryPhone(b.getPrimaryPhone())
                .secondaryPhone(b.getSecondaryPhone())
                .whatsappPhone(b.getWhatsappPhone())

                .addressLine1(b.getAddressLine1())
                .addressLine2(b.getAddressLine2())
                .landmark(b.getLandmark())
                .pincode(b.getPincode())

                .cityId(b.getCity() != null ? b.getCity().getId() : null)
                .cityName(b.getCity() != null ? b.getCity().getName() : null)
                .cityState(b.getCity() != null ? b.getCity().getState() : null)

                .latitude(b.getLatitude())
                .longitude(b.getLongitude())

                .categoryId(b.getCategory() != null ? b.getCategory().getId() : null)
                .categoryName(b.getCategory() != null ? b.getCategory().getName() : null)
                .categorySlug(b.getCategory() != null ? b.getCategory().getSlug() : null)

                .openTime(b.getOpenTime())
                .closeTime(b.getCloseTime())

                .establishedYear(b.getEstablishedYear())
                .highlightBadge(b.getHighlightBadge())
                .topRated(b.getTopRated())
                .nearAndFast(b.getNearAndFast())

                .avgRating(b.getAvgRating())
                .totalRatings(b.getTotalRatings())
                .build();
    }

    @Override
    public void toggleFavorite(Long businessId) {
        User user = getCurrentUserOrThrow();

        // if exists -> delete, else -> insert
        var existing = favoriteRepository.findByUser_IdAndBusiness_Id(user.getId(), businessId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return;
        }

        BusinessEntity business = businessRepository.findById(businessId)
                .orElseThrow(() -> new NotFoundException("Business not found: " + businessId));

        FavoriteEntity fav = FavoriteEntity.builder()
                .user(user)
                .business(business)
                .build();

        // race-safe insert (in case two requests come together)
        try {
            favoriteRepository.save(fav);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // unique constraint hit -> treat as already favorited
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getFavoriteCount(Long businessId) {
        return favoriteRepository.countByBusiness_Id(businessId);
    }


}
