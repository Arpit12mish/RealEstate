package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.dto.BusinessCreateRequest;
import com.brandPitara.sfs.dto.BusinessEventRequest;
import com.brandPitara.sfs.dto.BusinessResponse;
import com.brandPitara.sfs.dto.PageResponse;
import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.entity.BusinessEventEntity;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.repository.BusinessEventRepository;
import com.brandPitara.sfs.repository.BusinessRepository;
import com.brandPitara.sfs.repository.CategoryRepository;
import com.brandPitara.sfs.repository.CityRepository;
import com.brandPitara.sfs.repository.FavoriteRepository;
import com.brandPitara.sfs.repository.UserRepository;
import com.brandPitara.sfs.search.BusinessSearchService;
import com.brandPitara.sfs.service.BusinessService;
import com.brandPitara.sfs.service.mapper.BusinessMapper;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.brandPitara.sfs.repository.FavoriteRepository;
import com.brandPitara.sfs.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;


import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class BusinessServiceImpl implements BusinessService {

    private final BusinessRepository businessRepository;
    private final CityRepository cityRepository;
    private final CategoryRepository categoryRepository;
    private final BusinessEventRepository businessEventRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;


    // 👉 Elasticsearch Search + Indexing
    private final BusinessSearchService businessSearchService;

    // ============================================================
    // CREATE BUSINESS
    // ============================================================

    @Override
    public BusinessResponse createBusiness(BusinessCreateRequest request) {

        CityEntity city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new NotFoundException("City not found: " + request.getCityId()));

        CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found: " + request.getCategoryId()));

        BusinessEntity entity = new BusinessEntity();

        applyRequestToEntity(request, entity, city, category);

        BusinessEntity saved = businessRepository.save(entity);

        // 🔍 Index into Elasticsearch
        businessSearchService.indexBusiness(saved);

        return BusinessMapper.toResponse(saved);
    }

    // ============================================================
    // UPDATE BUSINESS
    // ============================================================

    @Override
    public BusinessResponse updateBusiness(Long id, BusinessCreateRequest request) {

        BusinessEntity existing = businessRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Business not found: " + id));

        CityEntity city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new NotFoundException("City not found: " + request.getCityId()));

        CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found: " + request.getCategoryId()));

        applyRequestToEntity(request, existing, city, category);

        BusinessEntity saved = businessRepository.save(existing);

        // 🔍 Update Elasticsearch index
        businessSearchService.indexBusiness(saved);

        return BusinessMapper.toResponse(saved);
    }

    // ============================================================
    // GET BUSINESS BY ID
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public BusinessResponse getBusiness(Long id) {
        BusinessEntity entity = businessRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Business not found: " + id));
        BusinessResponse response = BusinessMapper.toResponse(entity);

        // ✅ favoriteCount always, isFavorite only if authenticated (else false)
        enrichFavorites(List.of(response));

        return response;
    }

    // ============================================================
    // LIST BUSINESSES (PostgreSQL-based paging)
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BusinessResponse> listBusinesses(Long cityId,
                                                         Long categoryId,
                                                         String query,
                                                         Pageable pageable) {

        if (cityId == null) {
            throw new IllegalArgumentException("cityId is required");
        }

        String trimmedQuery = query != null ? query.trim() : null;
        if (trimmedQuery != null && trimmedQuery.length() < 2) {
            trimmedQuery = null;
        }

        Page<BusinessEntity> page;

        if (categoryId != null && trimmedQuery != null) {

            page = businessRepository
                    .findByCity_IdAndCategory_IdAndActiveTrueAndNameContainingIgnoreCase(
                            cityId, categoryId, trimmedQuery, pageable);

        } else if (categoryId != null) {

            page = businessRepository
                    .findByCity_IdAndCategory_IdAndActiveTrue(cityId, categoryId, pageable);

        } else if (trimmedQuery != null) {

            page = businessRepository
                    .findByCity_IdAndActiveTrueAndNameContainingIgnoreCase(
                            cityId, trimmedQuery, pageable);

        } else {

            page = businessRepository
                    .findByCity_IdAndActiveTrue(cityId, pageable);
        }

        List<BusinessEntity> entities = page.getContent();

        List<BusinessResponse> responses = entities.stream()
                .map(BusinessMapper::toResponse)
                .toList();

        enrichFavorites(responses); // ✅ adds favoriteCount + isFavorite

        return PageResponse.<BusinessResponse>builder()
                .content(responses)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();

    }

    // ============================================================
    // INTERNAL MAPPER (REQUEST → ENTITY)
    // ============================================================

    private void applyRequestToEntity(BusinessCreateRequest req,
                                      BusinessEntity entity,
                                      CityEntity city,
                                      CategoryEntity category) {

        entity.setName(req.getName());
        entity.setPrimaryPhone(req.getPrimaryPhone());
        entity.setSecondaryPhone(req.getSecondaryPhone());
        entity.setWhatsappPhone(req.getWhatsappPhone());
        entity.setEmail(req.getEmail());
        entity.setWebsite(req.getWebsite());
        entity.setAddressLine1(req.getAddressLine1());
        entity.setAddressLine2(req.getAddressLine2());
        entity.setLandmark(req.getLandmark());
        entity.setPincode(req.getPincode());

        entity.setCity(city);
        entity.setCategory(category);

        entity.setLatitude(req.getLatitude());
        entity.setLongitude(req.getLongitude());
        entity.setActive(req.getActive() != null ? req.getActive() : Boolean.TRUE);

        entity.setOpenTime(req.getOpenTime());
        entity.setCloseTime(req.getCloseTime());
        entity.setEstablishedYear(req.getEstablishedYear());
        entity.setHighlightBadge(req.getHighlightBadge());
        entity.setTopRated(req.getTopRated() != null ? req.getTopRated() : Boolean.FALSE);
        entity.setNearAndFast(req.getNearAndFast() != null ? req.getNearAndFast() : Boolean.FALSE);
    }

    // ============================================================
    // RECORD EVENT
    // ============================================================

    @Override
    public void recordBusinessEvent(Long businessId, BusinessEventRequest request) {

        BusinessEntity business = businessRepository.findById(businessId)
                .orElseThrow(() -> new NotFoundException("Business not found: " + businessId));

        CityEntity city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new NotFoundException("City not found: " + request.getCityId()));

        CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found: " + request.getCategoryId()));

        BusinessEventEntity event = BusinessEventEntity.builder()
                .business(business)
                .city(city)
                .category(category)
                .eventType(request.getEventType())
                .source(request.getSource())
                .listingPosition(request.getListingPosition())
                .createdAt(Instant.now())
                .build();

        businessEventRepository.save(event);
    }

    private void enrichFavorites(List<BusinessResponse> responses) {
        if (responses == null || responses.isEmpty()) return;

        List<Long> businessIds = responses.stream()
                .map(BusinessResponse::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (businessIds.isEmpty()) return;

        // ✅ favoriteCount (batch)
        Map<Long, Long> countMap = new HashMap<>();
        List<Object[]> rows = favoriteRepository.countByBusinessIds(businessIds);
        for (Object[] row : rows) {
                Long businessId = (Long) row[0];
                Long cnt = (Long) row[1];
                countMap.put(businessId, cnt);
        }

        // ✅ isFavorite (batch, only if authenticated)
        Optional<Long> currentUserIdOpt = getCurrentUserIdOptional();
        Set<Long> favoritedIds = new HashSet<>();
        if (currentUserIdOpt.isPresent()) {
                favoritedIds.addAll(
                        favoriteRepository.findFavoritedBusinessIds(currentUserIdOpt.get(), businessIds)
                );
        }

        // Apply to each response
        for (BusinessResponse br : responses) {
                Long bid = br.getId();
                br.setFavoriteCount(countMap.getOrDefault(bid, 0L));
                br.setIsFavorite(currentUserIdOpt.isPresent() && favoritedIds.contains(bid));
        }
        }

        private Optional<Long> getCurrentUserIdOptional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();

        // When no JWT, Spring may set anonymous auth depending on config
        String name = auth.getName();
        if (name == null || name.isBlank() || "anonymousUser".equalsIgnoreCase(name)) {
                return Optional.empty();
        }

        // your JWT subject is phoneNumber
        return userRepository.findByPhoneNumber(name).map(User::getId);
        }

}

