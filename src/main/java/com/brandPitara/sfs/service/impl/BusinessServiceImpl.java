package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.dto.BusinessCreateRequest;
import com.brandPitara.sfs.dto.BusinessEventRequest;
import com.brandPitara.sfs.dto.BusinessResponse;
import com.brandPitara.sfs.dto.PageResponse;
import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.entity.BusinessEventEntity;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.repository.BusinessEventRepository;
import com.brandPitara.sfs.repository.BusinessRepository;
import com.brandPitara.sfs.repository.CategoryRepository;
import com.brandPitara.sfs.repository.CityRepository;
import com.brandPitara.sfs.search.BusinessSearchService;
import com.brandPitara.sfs.service.BusinessService;
import com.brandPitara.sfs.service.mapper.BusinessMapper;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class BusinessServiceImpl implements BusinessService {

    private final BusinessRepository businessRepository;
    private final CityRepository cityRepository;
    private final CategoryRepository categoryRepository;
    private final BusinessEventRepository businessEventRepository;

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

        return BusinessMapper.toResponse(entity);
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

        String trimmedQuery = (query != null && !query.trim().isEmpty())
                ? query.trim()
                : null;

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

        return PageResponse.<BusinessResponse>builder()
                .content(page.getContent().stream().map(BusinessMapper::toResponse).toList())
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
}
