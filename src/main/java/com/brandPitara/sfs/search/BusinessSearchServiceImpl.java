package com.brandPitara.sfs.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.brandPitara.sfs.dto.BusinessResponse;
import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.repository.BusinessRepository;
import com.brandPitara.sfs.repository.FavoriteRepository;
import com.brandPitara.sfs.repository.UserRepository;
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

    private final ElasticsearchClient esClient;
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
            esClient.index(i -> i
                    .index(BusinessIndexInitializer.INDEX_NAME)
                    .id(entity.getId().toString())
                    .document(docWithGeo(doc))
            );
            log.info("[ES INDEX] indexed businessId={} index={}", entity.getId(), BusinessIndexInitializer.INDEX_NAME);
        } catch (Exception e) {
            // ✅ never crash your save/update flow
            log.warn("[ES INDEX] skipped (ES down) businessId={} reason={}", entity.getId(), e.getMessage());
        }
    }

    @Override
    public void deleteBusiness(Long businessId) {
        if (!searchEnabled) return;

        try {
            esClient.delete(d -> d
                    .index(BusinessIndexInitializer.INDEX_NAME)
                    .id(businessId.toString())
            );
            log.info("[ES DELETE] deleted businessId={} from index={}", businessId, BusinessIndexInitializer.INDEX_NAME);
        } catch (Exception e) {
            // ✅ never crash your delete flow
            log.warn("[ES DELETE] skipped (ES down) businessId={} reason={}", businessId, e.getMessage());
        }
    }

    private Map<String, Object> docWithGeo(BusinessSearchDocument doc) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", doc.getId());
        map.put("name", doc.getName());
        map.put("cityId", doc.getCityId());
        map.put("cityName", doc.getCityName());
        map.put("categoryId", doc.getCategoryId());
        map.put("categoryName", doc.getCategoryName());
        map.put("addressText", doc.getAddressText());
        map.put("keywords", doc.getKeywords());

        if (doc.getLatitude() != null && doc.getLongitude() != null) {
            Map<String, Object> loc = new HashMap<>();
            loc.put("lat", doc.getLatitude());
            loc.put("lon", doc.getLongitude());
            map.put("location", loc);
        }

        map.put("avgRating", doc.getAvgRating());
        map.put("totalRatings", doc.getTotalRatings());
        map.put("active", doc.getActive());
        map.put("sponsored", doc.getSponsored());
        map.put("sponsoredPriority", doc.getSponsoredPriority());
        return map;
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

        // ✅ If ES is disabled, do DB fallback directly
        if (!searchEnabled) {
            log.warn("[SEARCH] ES disabled (sfs.search.enabled=false). Using DB fallback.");
            return fallbackDbSearch(cityId, categoryId, text, page, size);
        }

        log.info("[ES SEARCH] cityId={} categoryId={} q='{}' userLat={} userLon={} page={} size={}",
                cityId, categoryId, text, userLat, userLon, page, size);

        List<Query> must = new ArrayList<>();
        List<Query> filter = new ArrayList<>();

        // full-text
        if (StringUtils.hasText(text)) {
            must.add(Query.of(q -> q.multiMatch(m -> m
                    .query(text)
                    .fields("name^3", "categoryName^2", "cityName", "addressText", "keywords")
                    .fuzziness("AUTO")
            )));
        }

        // filters
        if (cityId != null) {
            filter.add(Query.of(q -> q.term(t -> t.field("cityId").value(cityId))));
        }
        if (categoryId != null) {
            filter.add(Query.of(q -> q.term(t -> t.field("categoryId").value(categoryId))));
        }

        // sort: sponsored first, then geo-distance (if provided)
        List<SortOptions> sort = new ArrayList<>();
        sort.add(SortOptions.of(s -> s.field(f -> f.field("sponsoredPriority").order(SortOrder.Desc))));

        if (userLat != null && userLon != null) {
            sort.add(SortOptions.of(s -> s.geoDistance(g -> g
                    .field("location")
                    .location(l -> l.latlon(ll -> ll.lat(userLat).lon(userLon)))
                    .order(SortOrder.Asc)
            )));
        }

        SearchResponse<BusinessSearchDocument> response;
        try {
            response = esClient.search(s -> s
                            .index(BusinessIndexInitializer.INDEX_NAME)
                            .query(q -> q.bool(b -> {
                                if (!must.isEmpty()) b.must(must);
                                if (!filter.isEmpty()) b.filter(filter);
                                return b;
                            }))
                            .from(page * size)
                            .size(size)
                            .sort(sort),
                    BusinessSearchDocument.class);

            log.info("[ES SEARCH] took={}ms totalHits={}",
                    response.took(),
                    response.hits().total() != null ? response.hits().total().value() : null);

        } catch (Exception e) {
            // ✅ IMPORTANT: no crash, fallback to DB
            log.error("[ES SEARCH] Failed. Falling back to DB. reason={}", e.getMessage());
            return fallbackDbSearch(cityId, categoryId, text, page, size);
        }

        List<Long> ids = response.hits().hits().stream()
                .map(h -> Long.valueOf(h.id()))
                .toList();

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
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));

        Page<BusinessEntity> p;

        boolean hasText = StringUtils.hasText(text);

        // With category filters
        if (cityId != null && categoryId != null) {
            p = hasText
                    ? businessRepository.findByCity_IdAndCategory_IdAndActiveTrueAndNameContainingIgnoreCase(cityId, categoryId, text.trim(), pageable)
                    : businessRepository.findByCity_IdAndCategory_IdAndActiveTrue(cityId, categoryId, pageable);

            // City only
        } else if (cityId != null) {
            p = hasText
                    ? businessRepository.findByCity_IdAndActiveTrueAndNameContainingIgnoreCase(cityId, text.trim(), pageable)
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
