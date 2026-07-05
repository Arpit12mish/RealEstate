package com.brandPitara.sfs.search;

import com.brandPitara.sfs.dto.BusinessResponse;
import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.repository.BusinessRepository;
import com.brandPitara.sfs.repository.FavoriteRepository;
import com.brandPitara.sfs.repository.UserRepository;
import com.brandPitara.sfs.search.gateway.BusinessSearchGateway;
import com.brandPitara.sfs.search.gateway.BusinessSearchQuery;
import com.brandPitara.sfs.search.model.BusinessSearchDocument;
import com.brandPitara.sfs.service.mapper.BusinessMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessSearchServiceImpl implements BusinessSearchService {

    private final BusinessSearchGateway businessSearchGateway;
    private final BusinessRepository businessRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;

    @Value("${sfs.search.enabled:false}")
    private boolean searchEnabled;

    @Override
    public void indexBusiness(BusinessEntity entity) {
        if (!searchEnabled) return;

        BusinessSearchDocument doc = BusinessSearchDocument.fromEntity(entity);

        try {
            businessSearchGateway.indexBusiness(doc);
        } catch (Exception e) {
            // ✅ never crash your save/update flow
            log.warn("[ES INDEX] skipped (ES down) businessId={} reason={}", entity.getId(), e.getMessage());
        }
    }

    @Override
    public void deleteBusiness(Long businessId) {
        if (!searchEnabled) return;

        try {
            businessSearchGateway.deleteBusiness(businessId);
        } catch (Exception e) {
            // ✅ never crash your delete flow
            log.warn("[ES DELETE] skipped (ES down) businessId={} reason={}", businessId, e.getMessage());
        }
    }

    @Override
    public List<BusinessResponse> search(
            Long cityId,
            Long categoryId,
            String text,
            Double userLat,
            Double userLon,
            int page,
            int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), 30);

        // If ES is disabled, do DB fallback directly
        if (!searchEnabled) {
            log.warn("[SEARCH] ES disabled (sfs.search.enabled=false). Using DB fallback.");
            return fallbackDbSearch(cityId, categoryId, text, page, safeSize);
        }

        log.info("[ES SEARCH] cityId={} categoryId={} q='{}' userLat={} userLon={} page={} size={}",
                cityId, categoryId, text, userLat, userLon, page, size);

        List<Long> ids;
        try {
            ids = businessSearchGateway.search(
                    new BusinessSearchQuery(cityId, categoryId, text, userLat, userLon, page, safeSize)
            );
        } catch (Exception e) {
            log.error("[ES SEARCH] Failed. Falling back to DB. reason={}", e.getMessage());
            return fallbackDbSearch(cityId, categoryId, text, page, safeSize);
        }

        if (ids.isEmpty()) return List.of();

        List<BusinessEntity> entities = businessRepository.findAllById(ids);

        Map<Long, BusinessEntity> map = new HashMap<>();
        for (BusinessEntity e : entities) map.put(e.getId(), e);

        List<BusinessResponse> result = new ArrayList<>();
        for (Long id : ids) {
            BusinessEntity e = map.get(id);
            if (e != null) result.add(BusinessMapper.toResponse(e));
        }

        enrichFavorites(result);
        return result;
    }

    /**
     * DB fallback using your existing repository methods (Page-based).
     */
    private List<BusinessResponse> fallbackDbSearch(Long cityId, Long categoryId, String text, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 30);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        String safeText = (StringUtils.hasText(text) && text.trim().length() >= 2) ? text.trim() : null;
        boolean hasText = safeText != null;

        Page<BusinessEntity> p;

        // With category filters
        if (cityId != null && categoryId != null) {
            p = hasText
                    ? businessRepository.findByCity_IdAndCategory_IdAndActiveTrueAndNameContainingIgnoreCase(cityId, categoryId, safeText, pageable)
                    : businessRepository.findByCity_IdAndCategory_IdAndActiveTrue(cityId, categoryId, pageable);

            // City only
        } else if (cityId != null) {
            p = hasText
                    ? businessRepository.findByCity_IdAndActiveTrueAndNameContainingIgnoreCase(cityId, safeText, pageable)
                    : businessRepository.findByCity_IdAndActiveTrue(cityId, pageable);

        } else {
            // No city provided — you don’t have a repo method for this case.
            // Keep it safe: return empty for now.
            return List.of();
        }

        List<BusinessResponse> result = p.getContent().stream()
                .map(BusinessMapper::toResponse)
                .toList();

        enrichFavorites(result);
        return result;
    }

    private void enrichFavorites(List<BusinessResponse> responses) {
        if (responses == null || responses.isEmpty()) return;

        List<Long> businessIds = responses.stream()
                .map(BusinessResponse::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (businessIds.isEmpty()) return;

        // favoriteCount (batch)
        Map<Long, Long> countMap = new HashMap<>();
        List<Object[]> rows = favoriteRepository.countByBusinessIds(businessIds);
        for (Object[] row : rows) {
            Long businessId = (Long) row[0];
            Long cnt = (Long) row[1];
            countMap.put(businessId, cnt);
        }

        // isFavorite (batch, only if authenticated)
        Optional<Long> currentUserIdOpt = getCurrentUserIdOptional();
        Set<Long> favoritedIds = new HashSet<>();
        if (currentUserIdOpt.isPresent()) {
            favoritedIds.addAll(
                    favoriteRepository.findFavoritedBusinessIds(currentUserIdOpt.get(), businessIds)
            );
        }

        for (BusinessResponse br : responses) {
            Long bid = br.getId();
            br.setFavoriteCount(countMap.getOrDefault(bid, 0L));
            br.setIsFavorite(currentUserIdOpt.isPresent() && favoritedIds.contains(bid));
        }
    }

    private Optional<Long> getCurrentUserIdOptional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();

        String name = auth.getName();
        if (name == null || name.isBlank() || "anonymousUser".equalsIgnoreCase(name)) {
            return Optional.empty();
        }

        // JWT subject = phoneNumber
        return userRepository.findByPhoneNumber(name).map(User::getId);
    }
}
